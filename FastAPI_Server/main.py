import hashlib
import json
import sqlite3
import time
from contextlib import closing
from pathlib import Path

from fastapi import FastAPI, Header, HTTPException, Query
from pydantic import BaseModel, Field, field_validator

app = FastAPI(title="Offline-First Task Manager API", version="1.1.0")
DB_PATH = Path(__file__).resolve().parent / "tasks.db"


class TaskCreate(BaseModel):
    id: str = Field(min_length=8, max_length=128)
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(default="", max_length=4000)
    isCompleted: bool = False

    @field_validator("title")
    @classmethod
    def nonblank_title(cls, value):
        if not value.strip():
            raise ValueError("Ten cong viec khong duoc de trong")
        return value.strip()


class TaskUpdate(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(default="", max_length=4000)
    isCompleted: bool = False
    expectedVersion: int = Field(ge=1)

    @field_validator("title")
    @classmethod
    def nonblank_title(cls, value):
        if not value.strip():
            raise ValueError("Ten cong viec khong duoc de trong")
        return value.strip()


class TaskOut(BaseModel):
    id: str
    title: str
    description: str
    isCompleted: bool
    isDeleted: bool
    updatedAt: int
    serverVersion: int


def connect_db():
    db = sqlite3.connect(DB_PATH, timeout=30)
    db.row_factory = sqlite3.Row
    return db


def init_db():
    with closing(connect_db()) as db:
        db.execute("""
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                isCompleted INTEGER NOT NULL,
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL,
                serverVersion INTEGER NOT NULL
            )
        """)
        db.execute("""
            CREATE TABLE IF NOT EXISTS request_log (
                idempotencyKey TEXT PRIMARY KEY,
                fingerprint TEXT NOT NULL,
                responseJson TEXT NOT NULL
            )
        """)
        db.commit()


def to_task(row):
    task = dict(row)
    task["isCompleted"] = bool(task["isCompleted"])
    task["isDeleted"] = bool(task["isDeleted"])
    return task


def now_ms(previous=0):
    return max(time.time_ns() // 1_000_000, previous + 1)


def request_fingerprint(method, task_id, data):
    raw = json.dumps(
        {"method": method, "taskId": task_id, "data": data},
        sort_keys=True,
        ensure_ascii=False
    )
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def replay_request(db, key, fingerprint):
    if not key:
        return None
    row = db.execute(
        "SELECT fingerprint, responseJson FROM request_log WHERE idempotencyKey = ?",
        (key,)
    ).fetchone()
    if row is None:
        return None
    if row["fingerprint"] != fingerprint:
        raise HTTPException(status_code=409, detail="Idempotency-Key da duoc dung cho yeu cau khac")
    return json.loads(row["responseJson"])


def save_request(db, key, fingerprint, result):
    if key:
        db.execute(
            "INSERT INTO request_log (idempotencyKey, fingerprint, responseJson) VALUES (?, ?, ?)",
            (key, fingerprint, json.dumps(result, ensure_ascii=False))
        )


init_db()


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/tasks", response_model=list[TaskOut])
def get_tasks():
    with closing(connect_db()) as db:
        rows = db.execute("SELECT * FROM tasks ORDER BY updatedAt DESC").fetchall()
        return [to_task(row) for row in rows]


@app.get("/tasks/{task_id}", response_model=TaskOut)
def get_task(task_id: str):
    with closing(connect_db()) as db:
        row = db.execute("SELECT * FROM tasks WHERE id = ?", (task_id,)).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Khong tim thay cong viec")
        return to_task(row)


@app.post("/tasks", response_model=TaskOut, status_code=201)
def create_task(
    task: TaskCreate,
    idempotency_key: str | None = Header(default=None, alias="Idempotency-Key")
):
    fingerprint = request_fingerprint("POST", task.id, task.model_dump())
    with closing(connect_db()) as db:
        db.execute("BEGIN IMMEDIATE")
        replay = replay_request(db, idempotency_key, fingerprint)
        if replay is not None:
            db.commit()
            return replay

        row = db.execute("SELECT * FROM tasks WHERE id = ?", (task.id,)).fetchone()
        if row is not None:
            current = to_task(row)
            if (current["title"] != task.title
                    or current["description"] != task.description
                    or current["isCompleted"] != task.isCompleted
                    or current["isDeleted"]):
                raise HTTPException(status_code=409, detail="ID da ton tai voi du lieu khac")
            result = current
        else:
            db.execute(
                "INSERT INTO tasks VALUES (?, ?, ?, ?, ?, ?, ?)",
                (task.id, task.title, task.description, int(task.isCompleted), 0, now_ms(), 1)
            )
            result = to_task(db.execute("SELECT * FROM tasks WHERE id = ?", (task.id,)).fetchone())

        save_request(db, idempotency_key, fingerprint, result)
        db.commit()
        return result


@app.put("/tasks/{task_id}", response_model=TaskOut)
def update_task(
    task_id: str,
    task: TaskUpdate,
    idempotency_key: str | None = Header(default=None, alias="Idempotency-Key")
):
    fingerprint = request_fingerprint("PUT", task_id, task.model_dump())
    with closing(connect_db()) as db:
        db.execute("BEGIN IMMEDIATE")
        replay = replay_request(db, idempotency_key, fingerprint)
        if replay is not None:
            db.commit()
            return replay

        row = db.execute("SELECT * FROM tasks WHERE id = ?", (task_id,)).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Khong tim thay cong viec")
        if row["isDeleted"]:
            raise HTTPException(status_code=409, detail="Cong viec da bi xoa")
        if row["serverVersion"] != task.expectedVersion:
            raise HTTPException(status_code=409, detail="Xung dot phien ban du lieu")

        db.execute("""
            UPDATE tasks
            SET title = ?, description = ?, isCompleted = ?, updatedAt = ?, serverVersion = serverVersion + 1
            WHERE id = ?
        """, (task.title, task.description, int(task.isCompleted), now_ms(row["updatedAt"]), task_id))
        result = to_task(db.execute("SELECT * FROM tasks WHERE id = ?", (task_id,)).fetchone())
        save_request(db, idempotency_key, fingerprint, result)
        db.commit()
        return result


@app.delete("/tasks/{task_id}", response_model=TaskOut)
def delete_task(
    task_id: str,
    expectedVersion: int = Query(ge=1),
    idempotency_key: str | None = Header(default=None, alias="Idempotency-Key")
):
    fingerprint = request_fingerprint("DELETE", task_id, {"expectedVersion": expectedVersion})
    with closing(connect_db()) as db:
        db.execute("BEGIN IMMEDIATE")
        replay = replay_request(db, idempotency_key, fingerprint)
        if replay is not None:
            db.commit()
            return replay

        row = db.execute("SELECT * FROM tasks WHERE id = ?", (task_id,)).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Khong tim thay cong viec")
        if row["isDeleted"]:
            result = to_task(row)
        else:
            if row["serverVersion"] != expectedVersion:
                raise HTTPException(status_code=409, detail="Xung dot phien ban du lieu")
            db.execute("""
                UPDATE tasks
                SET isDeleted = 1, updatedAt = ?, serverVersion = serverVersion + 1
                WHERE id = ?
            """, (now_ms(row["updatedAt"]), task_id))
            result = to_task(db.execute("SELECT * FROM tasks WHERE id = ?", (task_id,)).fetchone())

        save_request(db, idempotency_key, fingerprint, result)
        db.commit()
        return result
