# Spring AI + Google Gemini + React RAG 프로젝트

Spring AI와 Google Vertex AI (Gemini)를 기반으로 구축된 RAG(Retrieval-Augmented Generation) 백엔드와 React 프론트엔드로 구성된 모바일 반응형 하이브리드 웹 애플리케이션입니다.

## 🏗️ 아키텍처 개요

사용자의 질문은 React 프론트엔드를 통해 전송되며, Spring Boot 백엔드에서 벡터 저장소(pgvector)를 조회하여 관련 문서를 찾습니다. 그 후, 검색된 컨텍스트와 함께 Gemini 모델에 질의하여 답변을 생성합니다.

```mermaid
graph LR
    User[사용자] -->|질문| React[React Frontend]
    React -->|API 요청| Spring[Spring Boot Backend]
    Spring -->|검색| DB[(PostgreSQL\npgvector)]
    Spring -->|생성 요청\n(질문 + 컨텍스트)| Gemini[Google Vertex AI\n(Gemini Pro)]
    Gemini -->|답변| Spring
    Spring -->|최종 응답| React
```

---

## 📋 사전 요구사항 (Prerequisites)

이 프로젝트를 실행하기 위해 다음 환경이 필요합니다.

- **Java 21** 이상
- **Node.js** (LTS 버전 권장)
- **Docker Desktop** (PostgreSQL 실행용)
- **Google Cloud Platform (GCP) 프로젝트**
  - Vertex AI API 활성화
  - 서비스 계정 키 (JSON) 생성 및 다운로드

---

## 🛠️ 기술 스택 (Tech Stack)

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.x
- **AI Integration**: Spring AI (Vertex AI Gemini)
- **Database**: PostgreSQL + pgvector (Vector Store)
- **Build Tool**: Gradle (Kotlin DSL)

### Frontend
- **Library**: React 19
- **Build Tool**: Vite
- **Styling**: CSS (Mobile Responsive Design)

---

## 🚀 시작하기 (Getting Started)

### 1. 데이터베이스 실행 (Database)

Docker Compose를 사용하여 PostgreSQL(pgvector)을 실행합니다.

```bash
docker-compose up -d
```

### 2. 백엔드 설정 및 실행 (Backend)

Google Cloud 인증을 위해 환경 변수 설정이 필요합니다.

**Windows (PowerShell):**
서비스 계정 키 파일(`service-account.json`)의 경로를 환경 변수로 설정하세요.

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\path\to\your\service-account-key.json"
$env:SPRING_AI_VERTEX_AI_GEMINI_PROJECT_ID = "your-gcp-project-id"
$env:SPRING_AI_VERTEX_AI_GEMINI_LOCATION = "us-central1"

./gradlew bootRun
```

또는 `src/main/resources/application.yaml` 파일을 수정하여 설정을 관리할 수 있습니다.

서버는 `http://localhost:8080`에서 실행됩니다.

### 2. 프론트엔드 설정 및 실행 (Frontend)

프론트엔드 디렉토리로 이동하여 의존성을 설치하고 개발 서버를 실행합니다.

```bash
cd frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`으로 접속하여 애플리케이션을 확인하세요.

---

## 📡 API 레퍼런스

### 채팅 (Chat)
사용자의 질문을 받아 Gemini 모델을 통해 답변을 반환합니다.

- **URL**: `/api/chat`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "message": "안녕하세요, AI에 대해 알려주세요."
  }
  ```
- **Response**:
  ```json
  {
    "response": "AI(인공지능)는 인간의 지능을 모방하여..."
  }
  ```

### 문서 수집 (Ingestion)
RAG 검색을 위한 문서를 벡터 저장소에 저장합니다.

- **URL**: `/api/ingest`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "content": "저장할 문서의 내용입니다."
  }
  ```
- **Response**: `200 OK`

---

## 📂 프로젝트 구조

```
root/
├── src/main/java/com/spring/ai/
│   ├── controller/
│   │   ├── ChatController.java       # 채팅 엔드포인트
│   │   └── IngestionController.java  # 문서 수집 엔드포인트
│   └── service/
│       └── RagService.java           # RAG 로직 (VectorStore + ChatClient)
├── frontend/
│   ├── src/
│   │   ├── App.jsx                   # 메인 UI 및 로직
│   │   └── index.css                 # 스타일 및 반응형 디자인
│   └── package.json
├── build.gradle.kts                  # 백엔드 의존성 관리
└── README.md                         # 프로젝트 문서
```
