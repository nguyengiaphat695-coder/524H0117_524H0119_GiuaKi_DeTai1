from pathlib import Path
import sys
import uuid

import pytest
from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import main


@pytest.fixture
def client(tmp_path, monkeypatch):
    monkeypatch.setattr(main, "DB_PATH", tmp_path / "test_tasks.db")
    main.init_db()
    with TestClient(main.app) as api:
        yield api


def task_payload(title="Task A", id=None):
    return {
        "id": id or str(uuid.uuid4()),
        "title": title,
        "description": "Demonstration",
        "isCompleted": False,
    }


def create(client, title="Task A", key=None, task_id=None):
    data = task_payload(title, task_id)
    response = client.post(
        "/tasks", json=data, headers={"Idempotency-Key": key or f"create-{data['id']}"}
    )
    assert response.status_code == 201, response.text
    return data, response.json()


def test_health(client):
    assert client.get("/health").json() == {"status": "ok"}


def test_initial_list_is_empty(client):
    assert client.get("/tasks").json() == []


def test_create_read_and_list(client):
    data, created = create(client)
    assert created["id"] == data["id"]
    assert created["serverVersion"] == 1
    assert created["isDeleted"] is False
    assert client.get(f"/tasks/{data['id']}").json() == created
    assert len(client.get("/tasks").json()) == 1


def test_update_version_increments(client):
    data, created = create(client)
    changed = client.put(
        f"/tasks/{data['id']}",
        json={"title": "After update", "description": "Edited", "isCompleted": True, "expectedVersion": 1},
        headers={"Idempotency-Key": f"update-{data['id']}"},
    )
    assert changed.status_code == 200
    assert changed.json()["serverVersion"] == 2
    assert changed.json()["isCompleted"] is True
    assert changed.json()["title"] == "After update"


def test_conflict_on_stale_update(client):
    data, _ = create(client)
    payload = {"title": "A newer", "description": "", "isCompleted": False, "expectedVersion": 1}
    ok = client.put(f"/tasks/{data['id']}", json=payload, headers={"Idempotency-Key": "update-1"})
    stale = client.put(f"/tasks/{data['id']}", json={**payload, "title": "B stale"}, headers={"Idempotency-Key": "update-2"})
    assert ok.status_code == 200
    assert stale.status_code == 409
    assert client.get(f"/tasks/{data['id']}").json()["title"] == "A newer"


def test_create_idempotency_replay(client):
    data = task_payload()
    headers = {"Idempotency-Key": "same-create-key"}
    first = client.post("/tasks", json=data, headers=headers)
    second = client.post("/tasks", json=data, headers=headers)
    assert (first.status_code, second.status_code) == (201, 201)
    assert first.json() == second.json()
    assert len(client.get("/tasks").json()) == 1


def test_idempotency_key_reuse_for_other_payload_returns_409(client):
    data = task_payload()
    headers = {"Idempotency-Key": "cannot-reuse-key"}
    assert client.post("/tasks", json=data, headers=headers).status_code == 201
    other = client.post("/tasks", json={**data, "title": "Changed"}, headers=headers)
    assert other.status_code == 409


def test_duplicate_id_with_different_data_returns_409(client):
    data, _ = create(client)
    other = client.post(
        "/tasks", json={**data, "title": "Collision"}, headers={"Idempotency-Key": "new-key"}
    )
    assert other.status_code == 409


def test_soft_delete_and_repeated_delete(client):
    data, _ = create(client)
    url = f"/tasks/{data['id']}?expectedVersion=1"
    first = client.delete(url, headers={"Idempotency-Key": "delete-key"})
    second = client.delete(url, headers={"Idempotency-Key": "delete-key"})
    assert first.status_code == 200
    assert second.status_code == 200
    assert first.json() == second.json()
    assert first.json()["isDeleted"] is True
    assert first.json()["serverVersion"] == 2
    assert len(client.get("/tasks").json()) == 1


def test_update_deleted_record_returns_409(client):
    data, _ = create(client)
    client.delete(f"/tasks/{data['id']}?expectedVersion=1", headers={"Idempotency-Key": "d1"})
    result = client.put(
        f"/tasks/{data['id']}",
        json={"title": "Restore?", "description": "", "isCompleted": False, "expectedVersion": 2},
        headers={"Idempotency-Key": "u1"},
    )
    assert result.status_code == 409


def test_stale_delete_returns_409(client):
    data, _ = create(client)
    client.put(
        f"/tasks/{data['id']}",
        json={"title": "New", "description": "", "isCompleted": False, "expectedVersion": 1},
        headers={"Idempotency-Key": "u1"},
    )
    result = client.delete(f"/tasks/{data['id']}?expectedVersion=1", headers={"Idempotency-Key": "d1"})
    assert result.status_code == 409


def test_missing_resource_returns_404(client):
    assert client.get("/tasks/nonexistent-id").status_code == 404
    assert client.delete("/tasks/nonexistent-id?expectedVersion=1", headers={"Idempotency-Key": "x"}).status_code == 404


def test_invalid_input_returns_422(client):
    assert client.post("/tasks", json={"id": "12345678", "title": ""}, headers={"Idempotency-Key": "invalid"}).status_code == 422
    data, _ = create(client)
    assert client.put(f"/tasks/{data['id']}", json={"title": "New", "expectedVersion": 0}).status_code == 422


def test_twenty_tasks_repeated_creates_have_unique_ids(client):
    data = [task_payload(f"TC09 API {i:02d}") for i in range(1, 21)]
    for index, item in enumerate(data):
        for _ in range(2):
            response = client.post(
                "/tasks", json=item, headers={"Idempotency-Key": f"tc09-{index}"}
            )
            assert response.status_code == 201
    all_tasks = client.get("/tasks").json()
    ids = [task["id"] for task in all_tasks]
    assert len(all_tasks) == 20
    assert len(ids) == len(set(ids))


def test_updated_data_persists_in_sqlite(client):
    data, _ = create(client)
    import sqlite3
    with sqlite3.connect(main.DB_PATH) as db:
        row = db.execute("SELECT id, title, serverVersion FROM tasks WHERE id = ?", (data["id"],)).fetchone()
    assert row == (data["id"], data["title"], 1)


def test_same_update_key_does_not_increment_version_twice(client):
    task, _ = create(client)
    request = {
        "title": "Updated once", "description": "Retried", "isCompleted": True,
        "expectedVersion": 1,
    }
    url = f"/tasks/{task['id']}"
    headers = {"Idempotency-Key": "same-update-key"}
    first = client.put(url, json=request, headers=headers)
    second = client.put(url, json=request, headers=headers)
    assert first.status_code == 200
    assert second.status_code == 200
    assert second.json() == first.json()
    assert client.get(url).json()["serverVersion"] == 2


def test_concurrent_writers_cannot_both_commit_same_version(client):
    from concurrent.futures import ThreadPoolExecutor
    task, _ = create(client)
    url = f"/tasks/{task['id']}"

    def write(index):
        return client.put(
            url,
            json={"title": f"Writer {index}", "description": "", "isCompleted": False, "expectedVersion": 1},
            headers={"Idempotency-Key": f"writer-{index}"},
        )

    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(write, [1, 2]))
    assert sorted(response.status_code for response in results) == [200, 409]
    stored = client.get(url).json()
    assert stored["serverVersion"] == 2
    assert stored["title"] in {"Writer 1", "Writer 2"}


def test_concurrent_retry_of_same_create_does_not_duplicate(client):
    from concurrent.futures import ThreadPoolExecutor
    payload = task_payload("Simultaneous retry")

    def send(_):
        return client.post("/tasks", json=payload, headers={"Idempotency-Key": "concurrent-create"})

    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(send, [1, 2]))
    assert all(response.status_code == 201 for response in results)
    assert results[0].json() == results[1].json()
    assert len(client.get("/tasks").json()) == 1


def test_two_writers_can_resolve_conflict_using_current_version(client):
    task, _ = create(client)
    url = f"/tasks/{task['id']}"
    first = client.put(
        url,
        json={"title": "Server edit", "description": "", "isCompleted": False, "expectedVersion": 1},
        headers={"Idempotency-Key": "server-edited"},
    )
    stale = client.put(
        url,
        json={"title": "Android edit", "description": "", "isCompleted": False, "expectedVersion": 1},
        headers={"Idempotency-Key": "stale-android"},
    )
    assert first.status_code == 200
    assert stale.status_code == 409
    latest = client.get(url).json()
    resolved = client.put(
        url,
        json={"title": "Android edit", "description": "", "isCompleted": False, "expectedVersion": latest["serverVersion"]},
        headers={"Idempotency-Key": "resolved-android"},
    )
    assert resolved.status_code == 200
    assert resolved.json()["title"] == "Android edit"
    assert resolved.json()["serverVersion"] == 3


def test_lost_create_ack_then_delete_using_recovered_server_version(client):
    task = task_payload("Created but response lost")
    first = client.post(
        "/tasks", json=task, headers={"Idempotency-Key": f"first-{task['id']}"}
    )
    assert first.status_code == 201
    recovered = client.get(f"/tasks/{task['id']}")
    assert recovered.status_code == 200
    recovered_version = recovered.json()["serverVersion"]

    deleted = client.delete(
        f"/tasks/{task['id']}?expectedVersion={recovered_version}",
        headers={"Idempotency-Key": f"recovered-delete-{task['id']}"},
    )
    assert deleted.status_code == 200
    assert deleted.json()["isDeleted"] is True
    assert deleted.json()["serverVersion"] == 2
    assert len(client.get("/tasks").json()) == 1


def test_new_client_fetches_tombstone_in_full_pull(client):
    task, _ = create(client, title="Deleted on other device")
    response = client.delete(
        f"/tasks/{task['id']}?expectedVersion=1",
        headers={"Idempotency-Key": f"delete-{task['id']}"},
    )
    assert response.status_code == 200
    listed = client.get("/tasks")
    assert listed.status_code == 200
    assert len(listed.json()) == 1
    assert listed.json()[0]["id"] == task["id"]
    assert listed.json()[0]["isDeleted"] is True


def test_stale_delete_can_retry_after_refreshing_version(client):
    task, _ = create(client, title="Change first")
    changed = client.put(
        f"/tasks/{task['id']}",
        json={"title": "Updated remotely", "description": "", "isCompleted": False, "expectedVersion": 1},
        headers={"Idempotency-Key": f"server-write-{task['id']}"},
    )
    assert changed.status_code == 200
    stale = client.delete(
        f"/tasks/{task['id']}?expectedVersion=1",
        headers={"Idempotency-Key": f"stale-delete-{task['id']}"},
    )
    assert stale.status_code == 409
    latest = client.get(f"/tasks/{task['id']}").json()
    retried = client.delete(
        f"/tasks/{task['id']}?expectedVersion={latest['serverVersion']}",
        headers={"Idempotency-Key": f"retry-delete-{task['id']}"},
    )
    assert retried.status_code == 200
    assert retried.json()["isDeleted"] is True
    assert retried.json()["serverVersion"] == 3


def test_blank_or_whitespace_task_names_are_rejected(client):
    original = task_payload("   ")
    result = client.post("/tasks", json=original, headers={"Idempotency-Key": "blank-name"})
    assert result.status_code == 422
    assert client.get("/tasks").json() == []


def test_title_validation_on_update(client):
    data, original = create(client)
    result = client.put(
        f"/tasks/{data['id']}",
        json={"title": "  ", "description": "", "isCompleted": False, "expectedVersion": 1},
        headers={"Idempotency-Key": "invalid-title-update"},
    )
    assert result.status_code == 422
    assert client.get(f"/tasks/{data['id']}").json() == original


def test_updated_at_increases_even_when_device_server_clock_is_frozen(client, monkeypatch):
    frozen_ns = 1_783_000_000_000_000_000
    monkeypatch.setattr(main.time, "time_ns", lambda: frozen_ns)
    data, created = create(client, "Clock collision")
    changed = client.put(
        f"/tasks/{data['id']}",
        json={"title": "One change", "description": "", "isCompleted": False, "expectedVersion": 1},
        headers={"Idempotency-Key": "fixed-clock-1"},
    )
    assert changed.status_code == 200
    assert changed.json()["updatedAt"] > created["updatedAt"]
    deleted = client.delete(
        f"/tasks/{data['id']}?expectedVersion=2",
        headers={"Idempotency-Key": "fixed-clock-2"},
    )
    assert deleted.status_code == 200
    assert deleted.json()["updatedAt"] > changed.json()["updatedAt"]


def test_update_key_replay_is_stable_after_another_write(client):
    data, _ = create(client)
    path = f"/tasks/{data['id']}"
    req1 = {"title": "First", "description": "", "isCompleted": False, "expectedVersion": 1}
    req2 = {"title": "Second", "description": "", "isCompleted": False, "expectedVersion": 2}
    first = client.put(path, json=req1, headers={"Idempotency-Key": "first-write"})
    second = client.put(path, json=req2, headers={"Idempotency-Key": "second-write"})
    retried = client.put(path, json=req1, headers={"Idempotency-Key": "first-write"})
    assert first.status_code == 200
    assert second.status_code == 200
    assert retried.status_code == 200
    assert retried.json() == first.json()
    assert client.get(path).json()["serverVersion"] == 3
    assert client.get(path).json()["title"] == "Second"
