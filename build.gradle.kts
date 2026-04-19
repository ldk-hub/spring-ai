plugins {
	java
	id("org.springframework.boot") version "3.4.2"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.spring"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
}

// Spring AI 1.0.0-M6 은 Spring Boot 3.4.x 와 호환
extra["springAiVersion"] = "1.0.0-M6"

dependencies {
	// 서블릿 기반 MVC 웹 스택 명시 (Reactive 모드 방지)
	implementation("org.springframework.boot:spring-boot-starter-web")
	// Spring Data JPA 및 JDBC
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	// Spring AI OpenAI 스타터 (Gemini의 OpenAI 호환 API와 연동)
	implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")
	// PostgreSQL JDBC 드라이버 및 pgvector 지원
	runtimeOnly("org.postgresql:postgresql")
	implementation("com.pgvector:pgvector:0.1.6")

    // RAG: PDF 문서 파싱 및 텍스트 추출 어드바이저
	implementation("org.springframework.ai:spring-ai-pdf-document-reader")
	
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
