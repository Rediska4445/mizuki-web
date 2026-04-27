# Mizuki-Web

Mizuki-Web is a project based on the idea of **[mizuki-player](https://github.com/Rediska4445/mizuki-player)** and created as a personal pet project for practical learning and gaining experience in Spring, Kafka, SQL, tests, Maven, and Git.   
It is a simple streaming platform for interacting with music, including downloading, uploading, listening, and other related features.  
Spring Boot is commonly used to build production-grade applications with minimal setup.  

## About the project

This project is a practical backend and web-development exercise.  
Its purpose is to improve real-world development skills while building a music-oriented platform.  
The exact feature set may evolve, so only the described core idea is reflected here.

## Technology stack

| Area     | Technologies                                                                                |
|----------|---------------------------------------------------------------------------------------------|
| Tests    | Spring Boot test starter, Web MVC test starter, Data JPA test starter, Spring Security test |
| Web      | Spring Web, Thymeleaf, Thymeleaf extras for Spring Security 6                               |
| Security | Spring Security, OAuth2 resource server                                                     |
| Database | Spring Data JPA, H2                                                                         |
| Utility  | Lombok                                                                                      |
| Build    | Maven, Maven Compiler Plugin, Spring Boot Maven Plugin                                      |

## Maven setup

| Part     | Description                                                                                                 |
|----------|-------------------------------------------------------------------------------------------------------------|
| Tests    | test dependencies for application, web layer, and security layer                                            |
| Web      | dependencies for server-side web rendering and HTTP handling                                                |
| Security | dependencies for authentication and authorization support                                                   |
| Database | JPA for persistence and H2 for runtime database                                                             |
| Build    | compiler plugin with Lombok annotation processing, Boot plugin with Lombok excluded from the final artifact |

Lombok needs annotation processing during compilation, and the Maven setup should reflect that.

## Build configuration

The Maven configuration is set up to compile the project with Lombok support through annotation processing.  
The Spring Boot Maven Plugin excludes Lombok from the packaged application.  
This keeps Lombok available at build time without shipping it in the final jar.

## How to run the project

Since you created the project with **Spring Initializr** and only added dependencies to `pom.xml`, the launch flow is the standard Spring Boot one: open the generated project as a Maven project, wait for dependency sync, and run the main application class with `@SpringBootApplication`. Spring Initializr is the usual way to generate a Spring Boot project scaffold, and a Spring Boot app is typically started from its main class or through the IDE run configuration.

### In the IDE

1. Open the project folder that contains `pom.xml`.
2. Let the IDE import the Maven project and download dependencies.
3. Find the main class with `@SpringBootApplication`.
4. Run that class as a Java application.

If your IDE detects the Spring Boot project correctly, it usually creates the launch configuration automatically. In IntelliJ IDEA, this is normally enough to start the app from the main class directly.

### From the command line

1. Go to the project root, where `pom.xml` is located.
2. Build the project with Maven.
3. Run the Spring Boot application from the generated artifact or with the Maven Spring Boot workflow.

That is the standard Spring Boot flow for a Maven-based project created from Spring Initializr.