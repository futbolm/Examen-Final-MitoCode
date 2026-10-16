# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 4.1.1 web application (`com.examen:examen-final`), Java 25, built with Maven (wrapper included). Currently a freshly generated skeleton: only the `@SpringBootApplication` entry point (`src/main/java/com/examen/ExamenFinalApplication.java`), a minimal `application.yaml` (just `spring.application.name`), and a context-loads smoke test exist. No controllers, persistence, or security layers yet.

Dependencies: `spring-boot-starter-webmvc` (Spring MVC), Lombok (optional), and `spring-boot-starter-webmvc-test` (test scope). Note Boot 4 uses modular starters (`webmvc`, `webmvc-test`) rather than the older `web`/`test` starters.

## Commands

Use the wrapper (on Windows PowerShell: `.\mvnw.cmd`; in Bash: `./mvnw`):

- Run the app: `./mvnw spring-boot:run`
- Build / package: `./mvnw clean package`
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=ExamenFinalApplicationTests`
- Run a single test method: `./mvnw test -Dtest=ExamenFinalApplicationTests#methodName`

No linter or formatter is configured.

## Build notes

- Lombok is wired through explicit `annotationProcessorPaths` in the `maven-compiler-plugin` executions (`default-compile` and `default-testCompile`) in `pom.xml`; keep that config if modifying the compiler plugin, otherwise Lombok annotations stop working.
- Requires a JDK 25 toolchain.
- Base package is `com.examen`; component scanning starts there, so new code must live under it.

Agents are located in: .agents/subagents
Skills are located in: .agents/skills