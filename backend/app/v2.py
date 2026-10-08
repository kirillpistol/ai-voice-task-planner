from datetime import datetime, timezone
from typing import Optional
from uuid import uuid4

from fastapi import APIRouter, Depends, Header, HTTPException, status
from firebase_admin import auth as firebase_auth
import firebase_admin
from google.cloud import firestore
from pydantic import BaseModel, Field

router = APIRouter(prefix="/api/v2", tags=["v2"])
MAX_PAGE_SIZE = 100


def current_uid(authorization: Optional[str] = Header(default=None)) -> str:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Bearer token required")
    token = authorization[7:].strip()
    if not token:
        raise HTTPException(status_code=401, detail="Bearer token required")
    try:
        firebase_admin.get_app()
    except ValueError:
        firebase_admin.initialize_app()
    try:
        decoded = firebase_auth.verify_id_token(token, check_revoked=True)
        uid = decoded.get("uid")
        if not isinstance(uid, str) or not uid:
            raise ValueError("Invalid uid")
        return uid
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid or revoked token")


def database():
    return firestore.Client()


class TaskInput(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(default="", max_length=5000)
    due_at: Optional[datetime] = None
    completed: bool = False


class TaskOutput(TaskInput):
    id: str


def task_collection(uid: str, db):
    return db.collection("users").document(uid).collection("tasks")


@router.get("/session")
def session(uid: str = Depends(current_uid)):
    return {"uid": uid}


@router.post("/tasks", response_model=TaskOutput, status_code=status.HTTP_201_CREATED)
def create_task(task: TaskInput, uid: str = Depends(current_uid), db=Depends(database)):
    task_id = uuid4().hex
    payload = task.model_dump(mode="json")
    payload["created_at"] = datetime.now(timezone.utc).isoformat()
    task_collection(uid, db).document(task_id).set(payload)
    return TaskOutput(id=task_id, **task.model_dump())


@router.get("/tasks", response_model=list[TaskOutput])
def list_tasks(limit: int = 50, uid: str = Depends(current_uid), db=Depends(database)):
    if limit < 1 or limit > MAX_PAGE_SIZE:
        raise HTTPException(status_code=422, detail="limit must be between 1 and 100")
    docs = task_collection(uid, db).limit(limit).stream()
    return [TaskOutput(id=doc.id, **{k: v for k, v in doc.to_dict().items()
                                     if k in TaskInput.model_fields}) for doc in docs]


@router.put("/tasks/{task_id}", response_model=TaskOutput)
def update_task(task_id: str, task: TaskInput, uid: str = Depends(current_uid),
                db=Depends(database)):
    ref = task_collection(uid, db).document(task_id)
    if not ref.get().exists:
        raise HTTPException(status_code=404, detail="Task not found")
    ref.update(task.model_dump(mode="json"))
    return TaskOutput(id=task_id, **task.model_dump())


@router.delete("/tasks/{task_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_task(task_id: str, uid: str = Depends(current_uid), db=Depends(database)):
    ref = task_collection(uid, db).document(task_id)
    if not ref.get().exists:
        raise HTTPException(status_code=404, detail="Task not found")
    ref.delete()
    return None
