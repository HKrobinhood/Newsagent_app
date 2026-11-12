# GlobalNewsAI Agent Guidelines & Workflow Rules

This document defines the mandatory operating guidelines and workflow protocols for AI agents working in this repository.

---

## 1. Remote Repository Synchronization (Pre-Task Mandatory Rule)

Before commencing any development task, refactoring step, or milestone:
1. **Always check for remote updates first**:
   ```bash
   git fetch origin
   git status
   ```
2. **Synchronize if behind**:
   - If the remote branch (`origin/main`) contains new commits, inspect them (`git log HEAD..origin/main`) and integrate them cleanly (`git pull --rebase origin main`) before making local modifications.
   - Never develop on a stale or conflicting local baseline.

---

## 2. Single-Branch Milestone Commit Policy (No Branch Sprawl)

To maintain a clean, linear, and production-ready Git history for GitHub:
1. **All milestones must be developed and committed on the SAME designated development branch**:
   - Work directly on the active development branch (`scDev`, or `main` when explicitly instructed).
   - **Do NOT** create separate branches for each individual milestone (e.g., no `feature/s0`, `feature/s1`, `refactor/r1`, etc.).
2. **Milestone Commit Standard**:
   - Every milestone or functional increment must be established as structured Conventional Commits on this single development branch (`scDev`):
     - `feat(<scope>): <description>`
     - `test(<scope>): <description>`
     - `fix(<scope>): <description>`
     - `refactor(<scope>): <description>`
   - Maintain a clean, progressive, and bisectable commit log.

---

## 3. Architecture & Coding Standards

1. **Android Client (COMP7506 Java / XML)**:
   - Primary language: **Java 17** following [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
   - UI Paradigm: Classic **XML layouts** (`ConstraintLayout`, `LinearLayout`, `FrameLayout`) + `AndroidX View` components.
   - List views: `RecyclerView` + `Adapter` + `ViewHolder` with `DiffUtil`.
   - Concurrency: Dedicated `AppExecutors.java` (`networkIO` 4-thread pool, `diskIO` single-thread executor, and `mainThread` `Handler` bound to `Looper.getMainLooper()`). Never block the UI thread; never update UI from background threads.
   - Preferences: `SharedPreferences` via `NewsPreferences.java`.
   - Navigation: `BottomNavigationView` with `FragmentManager.beginTransaction()`.
   - External links: Implicit `Intent(Intent.ACTION_VIEW, Uri.parse(url))`.

2. **Backend Services (Python FastAPI)**:
   - Language: **Python 3.12** following [Google Python Style Guide](https://google.github.io/styleguide/pyguide.html).
   - Code formatting and linting: `Ruff`.
   - Database: SQLAlchemy ORM models + Alembic migrations + PostgreSQL 16 / SQLite.
   - External API contract: Keep `/api/v1/news`, `/api/v1/news/geo`, and `/api/v1/ai/chat` 100% backwards compatible.

---

## 4. Verification & Quality Gates

Every milestone must satisfy all quality checks before committing:
```bash
# 1. Android Unit Tests
./gradlew testDebugUnitTest

# 2. Android Lint Analysis (0 errors)
./gradlew lintDebug

# 3. Python Backend Automated Tests
PYTHONPATH=backend backend/.venv/bin/pytest backend/tests -v
```
