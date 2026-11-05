# GlobalNewsAI Backend Service

Python FastAPI backend for GlobalNewsAI.

## Quickstart

### Prerequisites
- Python 3.10+
- PostgreSQL 15+

### Local Setup

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### Run Tests and Quality Checks

```bash
# Run pytest
PYTHONPATH=. pytest tests

# Run Ruff linter and formatter
ruff check .
ruff format --check .
```

### Run Server

```bash
uvicorn app.main:app --reload --port 8000
```
