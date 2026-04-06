import asyncio
import json
import logging
import os
import uuid
import xml.etree.ElementTree as ET
from functools import lru_cache
from io import BytesIO
from pathlib import Path
from typing import Any
from urllib.parse import urlparse
from urllib.request import Request, urlopen
from zipfile import ZipFile

from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity

app = FastAPI()
logger = logging.getLogger(__name__)

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
ANALYSIS_REQUEST_TOPIC = os.getenv("RESUME_ANALYSIS_REQUEST_TOPIC", "resume-analysis-requests")
ANALYSIS_RESULT_TOPIC = os.getenv("RESUME_ANALYSIS_RESULT_TOPIC", "resume-analysis-results")
JOB_SERVICE_BASE_URL = os.getenv("JOB_SERVICE_BASE_URL", "http://localhost:8081")

producer: AIOKafkaProducer | None = None
consumer_task: asyncio.Task | None = None
result_cache: dict[str, dict[str, Any]] = {}


class SimilarityRequest(BaseModel):
    text1: str
    text2: str


class MatchRequest(BaseModel):
    text1: str
    text2: str


class AsyncAnalysisRequest(BaseModel):
    applicationId: int
    jobId: int
    candidateEmail: str | None = None
    resumeUrl: str
    requestId: str | None = None


@lru_cache(maxsize=1)
def get_model() -> SentenceTransformer:
    return SentenceTransformer("all-MiniLM-L6-v2")


def get_embedding(text: str) -> list[float]:
    if text is None or not str(text).strip():
        raise ValueError("Text cannot be empty")
    return get_model().encode(text, normalize_embeddings=True).tolist()


def calculate_similarity(text1: str, text2: str) -> float:
    emb1 = get_embedding(text1)
    emb2 = get_embedding(text2)
    return float(cosine_similarity([emb1], [emb2])[0][0])


def build_verdict(score: float) -> str:
    if score >= 0.8:
        return "strong_match"
    if score >= 0.6:
        return "good_match"
    if score >= 0.4:
        return "partial_match"
    return "weak_match"


def fetch_json(url: str) -> dict[str, Any]:
    request = Request(url, headers={"User-Agent": "resume-ai-service/1.0"})
    with urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def fetch_bytes(url: str) -> bytes:
    request = Request(url, headers={"User-Agent": "resume-ai-service/1.0"})
    with urlopen(request, timeout=30) as response:
        return response.read()


def extract_pdf_text(data: bytes) -> str:
    import pdfplumber

    parts: list[str] = []
    with pdfplumber.open(BytesIO(data)) as pdf:
        for page in pdf.pages:
            page_text = page.extract_text() or ""
            if page_text.strip():
                parts.append(page_text)
    return "\n".join(parts).strip()


def extract_docx_text(data: bytes) -> str:
    parts: list[str] = []
    with ZipFile(BytesIO(data)) as archive:
        xml_data = archive.read("word/document.xml")
        root = ET.fromstring(xml_data)
        namespace = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
        for paragraph in root.findall(".//w:p", namespace):
            runs = [node.text for node in paragraph.findall('.//w:t', namespace) if node.text]
            if runs:
                parts.append("".join(runs))
    return "\n".join(parts).strip()


def extract_resume_text(resume_url: str) -> str:
    if resume_url is None or not str(resume_url).strip():
        raise ValueError("resumeUrl is required")

    data = fetch_bytes(resume_url)
    suffix = Path(urlparse(resume_url).path).suffix.lower()
    if suffix == ".pdf":
        return extract_pdf_text(data)
    if suffix == ".docx":
        return extract_docx_text(data)

    # Fallback for text-like content
    text = data.decode("utf-8", errors="ignore").strip()
    if text:
        return text
    raise ValueError("Unsupported resume format; only PDF and DOCX are supported")


def fetch_job_text(job_id: int) -> str:
    job = fetch_json(f"{JOB_SERVICE_BASE_URL}/api/jobs/public/{job_id}")
    if not job:
        raise ValueError(f"Job not found for id: {job_id}")

    skills = job.get("skillsRequired") or []
    skills_text = ", ".join(str(skill) for skill in skills) if isinstance(skills, list) else str(skills)
    parts = [
        str(job.get("role") or job.get("title") or ""),
        str(job.get("companyName") or ""),
        str(job.get("description") or ""),
        skills_text,
        str(job.get("location") or ""),
        str(job.get("experienceRequired") or ""),
    ]
    return " ".join(part for part in parts if part).strip()


def build_result_payload(request_id: str, application_id: int, score: float) -> dict[str, Any]:
    return {
        "applicationId": application_id,
        "requestId": request_id,
        "similarity": round(score, 6),
        "matchPercentage": round(score * 100, 2),
        "verdict": build_verdict(score),
    }


async def start_kafka_producer() -> AIOKafkaProducer:
    kafka_producer = AIOKafkaProducer(bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS)
    await kafka_producer.start()
    return kafka_producer


async def kafka_consumer_loop() -> None:
    consumer = AIOKafkaConsumer(
        ANALYSIS_REQUEST_TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        group_id="resume-ai-service",
        auto_offset_reset="earliest",
        enable_auto_commit=True,
    )
    await consumer.start()
    try:
        async for message in consumer:
            try:
                payload = json.loads(message.value.decode("utf-8"))
                request = AsyncAnalysisRequest.model_validate(payload)
                request_id = request.requestId or str(uuid.uuid4())
                resume_text = extract_resume_text(request.resumeUrl)
                job_text = fetch_job_text(request.jobId)
                score = calculate_similarity(resume_text, job_text)

                result = build_result_payload(request_id, request.applicationId, score)
                result_cache[request_id] = result

                if producer is not None:
                    await producer.send_and_wait(ANALYSIS_RESULT_TOPIC, json.dumps(result).encode("utf-8"))
            except Exception as exc:
                logger.warning("Resume analysis request failed: %s", exc)
    finally:
        await consumer.stop()


@app.on_event("startup")
async def startup_event() -> None:
    global producer, consumer_task
    try:
        producer = await start_kafka_producer()
        consumer_task = asyncio.create_task(kafka_consumer_loop())
        logger.info("Kafka async analysis started on %s", KAFKA_BOOTSTRAP_SERVERS)
    except Exception as exc:
        producer = None
        consumer_task = None
        logger.warning("Kafka unavailable; async analysis disabled: %s", exc)


@app.on_event("shutdown")
async def shutdown_event() -> None:
    global producer, consumer_task
    if consumer_task is not None:
        consumer_task.cancel()
        try:
            await consumer_task
        except asyncio.CancelledError:
            pass
    if producer is not None:
        await producer.stop()


@app.get("/")
def home() -> dict[str, str]:
    return {"message": "AI Service Running"}


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/similarity")
def similarity(payload: SimilarityRequest) -> dict[str, float]:
    try:
        score = calculate_similarity(payload.text1, payload.text2)
        return {"similarity": score}
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/match-resume-job")
def match_resume_job(payload: MatchRequest) -> dict[str, Any]:
    try:
        score = calculate_similarity(payload.text1, payload.text2)
        return {
            "similarity": round(score, 6),
            "matchPercentage": round(score * 100, 2),
            "verdict": build_verdict(score),
        }
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.get("/analysis/{request_id}")
def get_analysis_result(request_id: str) -> dict[str, Any]:
    result = result_cache.get(request_id)
    if result is None:
        return {"requestId": request_id, "status": "pending"}
    return {"requestId": request_id, "status": "completed", "result": result}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)