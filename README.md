# Mizuki-Web

Mizuki-Web is a project based on the idea of **[mizuki-player](https://github.com/Rediska4445/mizuki-player)** and created as a personal pet project for practical learning and gaining experience in Spring, Kafka, Redis, Docker, S3, SQL (PostgreSQL/H2), tests, Maven, and Git.   
It is a simple streaming platform for interacting with music, including downloading, uploading, listening, and other related features.  
Spring Boot is commonly used to build production-grade applications with minimal setup.  

> [!IMPORTANT]
> **Educational Purpose Only**  
> This project is not a consumer-grade product and is not intended for commercial sale or distribution (at least not without explicit attribution to the author). It is strictly an educational project designed for learning purposes and does not claim to be a production-ready application. It may, and likely will, contain numerous bugs, security flaws, performance issues, and incomplete features.

## About the project

This project is a practical backend and web-development exercise.  
Its purpose is to improve real-world development skills while building a music-oriented platform.  
The exact feature set may evolve, so only the described core idea is reflected here.

## Technology stack

| Area         | Technologies                                                                                |
|:-------------|:--------------------------------------------------------------------------------------------|
| **Web**      | Spring Web, Thymeleaf, Thymeleaf extras for Spring Security 6                               |
| **Security** | Spring Security, OAuth2 resource server                                                     |
| **Database** | Spring Data JPA, H2, PostgreSQL                                                             |
| **Storage**  | S3 (MinIO)                                                                                  |
| **DevOps**   | Docker, Docker Compose                                                                      |
| **Tests**    | Spring Boot test starter, Web MVC test starter, Data JPA test starter, Spring Security test |
| **Utility**  | Lombok                                                                                      |
| **Build**    | Maven, Maven Compiler Plugin, Spring Boot Maven Plugin                                      |

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

### Docker

The project uses the standard **Spring Boot Docker Compose support** for infrastructure management. All environment configurations are pre-defined in the `dockerfile` and `docker-compose.yml`.

* **Prerequisites:** Make sure **Docker Desktop** is installed and running on your machine.

* **Automatic Lifecycle:**
  When you run the application via your IDE or Maven, Spring Boot will automatically detect the `docker-compose.yml` file, spin up the required containers (e.g., database), and shut them down when the application stops.

* **Manual Management :**
  If you prefer to manage the containers manually, use the following standard commands:
  
  * **Start containers in the background:**
    ```bash
    docker-compose up -d
    ```
  * **Stop and remove containers:**
    ```bash
    docker-compose down
    ```
  * **Rebuild the custom Docker image:**
    ```bash
    docker-compose up --build
    ```

---
## Testing

* **Run all tests:**
  ```bash
  ./mvnw test
  ```

* **Run a single specific test class:**
  ```bash
  ./mvnw test -Dtest=TrackRepositoryTest
  ```

* **Run a single specific test method:**
  ```bash
  ./mvnw test -Dtest=TrackRepositoryTest#testMethodName
  ```

* **Clean project and run all tests:**
  ```bash
  ./mvnw clean test
  ```

* **Build the project package skipping tests:**
  ```bash
  ./mvnw package -DskipTests
  ```
  
---
## Documentation
  The project documentation is structured into the following formats:

* **JavaDocs**: Technical documentation generated automatically from the source code comments.
  * *To generate JavaDocs manually using JDK tool, run the following command in your terminal:*
    ```bash
    javadoc -d docs/javadoc -sourcepath src/main/java -subpackages rf.mizuka.web.application
    ```
* **Official Documentation**: Comprehensive user guides and system architecture overviews. *(Under development / Not available yet)*
* **Wiki**: Collaborative knowledge base for developers and contributors. *(Under development / Not available yet)*
