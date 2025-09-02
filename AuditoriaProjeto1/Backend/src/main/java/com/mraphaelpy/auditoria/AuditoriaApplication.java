package com.mraphaelpy.auditoria;

import com.mraphaelpy.auditoria.service.logging.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
@RequiredArgsConstructor
public class AuditoriaApplication implements CommandLineRunner {

	private final StructuredLoggingService structuredLoggingService;

	public static void main(String[] args) {
		SpringApplication.run(AuditoriaApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		structuredLoggingService.logApplicationStart("1.0.0", "development");

		structuredLoggingService.logInfo("system",
				"Sistema de auditoria iniciado com sucesso",
				Map.of("features", "logging,security,audit", "startup_complete", true));
	}
}
