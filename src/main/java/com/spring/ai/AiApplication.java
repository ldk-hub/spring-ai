package com.spring.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI 애플리케이션의 진입점(Entry Point)입니다.
 * <p>
 * 이 클래스는 Spring Boot 애플리케이션을 부트스트랩하고 실행하는 역할을 담당합니다.
 * 주요 설정과 컴포넌트 스캔이 여기서 시작됩니다.
 * </p>
 */
@SpringBootApplication
public class AiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiApplication.class, args);
	}

}
