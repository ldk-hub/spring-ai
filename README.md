# Spring AI RAG Project with React Frontend

Spring AI 기반의 RAG(Retrieval-Augmented Generation) 백엔드와 React 기반의 모바일 하이브리드 반응형 프론트엔드 프로젝트입니다.

## 📋 사전 요구사항

- **Java 21** 이상
- **Node.js** (LTS 버전)
- **PostgreSQL** (pgvector 확장 필요)
- **OpenAI API Key**

## 🚀 실행 방법

### 1. 백엔드 (Spring Boot)

`OPENAI_API_KEY` 환경 변수가 필요합니다.

**Windows (PowerShell):**
```powershell
$env:OPENAI_API_KEY = "sk-..."
./gradlew bootRun
```

또는 `src/main/resources/application.yaml` 파일에 직접 키를 입력하거나 IDE 환경 변수로 설정하세요.

서버는 `http://localhost:8080`에서 실행됩니다.

### 2. 프론트엔드 (React)

```bash
cd frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`으로 접속하세요.

## 📂 주요 구조

- `src/main/java/com/spring/ai/`
  - `service/RagService.java`: RAG 로직 (VectorStore + ChatClient)
  - `controller/ChatController.java`: 채팅 API (`POST /api/chat`)
  - `controller/IngestionController.java`: 문서 수집 API (`POST /api/ingest`)
- `frontend/`
  - `src/App.jsx`: 채팅 인터페이스 로직
  - `src/index.css`: 모바일 반응형 디자인 시스템
