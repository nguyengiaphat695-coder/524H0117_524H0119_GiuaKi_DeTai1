import argparse
import csv
import sqlite3
import sys
import tempfile
import time
import uuid
from pathlib import Path

from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import main


def run(trials, batch):
    original_path = main.DB_PATH
    rows = []
    try:
        with tempfile.TemporaryDirectory(prefix="offlinefirst-benchmark-") as temp:
            main.DB_PATH = Path(temp) / "test_only.db"
            main.init_db()
            with TestClient(main.app) as client:
                for trial in range(1, trials + 1):
                    expected_ids = set()
                    errors = 0
                    start = time.perf_counter()
                    for number in range(batch):
                        task_id = str(uuid.uuid4())
                        expected_ids.add(task_id)
                        payload = {
                            "id": task_id,
                            "title": f"API Benchmark {trial:02d}-{number + 1:02d}",
                            "description": "Temporary backend-only benchmark",
                            "isCompleted": False,
                        }
                        headers = {"Idempotency-Key": f"benchmark-{task_id}"}
                        first = client.post("/tasks", json=payload, headers=headers)
                        replay = client.post("/tasks", json=payload, headers=headers)
                        if first.status_code != 201 or replay.status_code != 201:
                            errors += 1
                        elif first.json() != replay.json():
                            errors += 1
                    elapsed = time.perf_counter() - start
                    data = client.get("/tasks").json()
                    found = [task for task in data if task["id"] in expected_ids]
                    distinct = len({task["id"] for task in found})
                    status = "PASS" if errors == 0 and len(found) == batch and distinct == batch else "FAIL"
                    rows.append({
                        "run": trial,
                        "expected_tasks": batch,
                        "post_requests_including_retries": batch * 2,
                        "unique_found": distinct,
                        "errors": errors,
                        "duration_seconds": round(elapsed, 4),
                        "tasks_per_second": round(batch / elapsed, 2) if elapsed else 0,
                        "result": status,
                    })
            with sqlite3.connect(main.DB_PATH) as db:
                count = db.execute("SELECT COUNT(*) FROM tasks").fetchone()[0]
                assert count == trials * batch
    finally:
        main.DB_PATH = original_path
    return rows


def main_cli():
    parser = argparse.ArgumentParser(description="Backend-only benchmark using temporary SQLite")
    parser.add_argument("--trials", type=int, default=5)
    parser.add_argument("--batch", type=int, default=20)
    parser.add_argument("--csv", type=Path)
    args = parser.parse_args()
    if not (1 <= args.trials <= 100 and 1 <= args.batch <= 1000):
        parser.error("trials must be 1..100 and batch 1..1000")
    rows = run(args.trials, args.batch)
    writer = csv.DictWriter(sys.stdout, fieldnames=list(rows[0]))
    writer.writeheader()
    writer.writerows(rows)
    success = sum(row["result"] == "PASS" for row in rows)
    print(f"API-only successful rounds: {success}/{len(rows)} ({100 * success / len(rows):.1f}%)", file=sys.stderr)
    print("This is not the Android end-to-end sync success rate.", file=sys.stderr)
    if args.csv:
        args.csv.parent.mkdir(parents=True, exist_ok=True)
        with args.csv.open("w", encoding="utf-8-sig", newline="") as file:
            csv_writer = csv.DictWriter(file, fieldnames=list(rows[0]))
            csv_writer.writeheader()
            csv_writer.writerows(rows)


if __name__ == "__main__":
    main_cli()
