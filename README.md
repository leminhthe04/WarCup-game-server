# WarCup Game Server

A high-performance multiplayer online game server built with Spring Boot 3.4.6 and Java 17. This server provides WebSocket-based real-time communication with MongoDB persistence and Redis caching.

## 📋 Overview

WarCup Game Server is a robust backend solution for multiplayer gaming applications. It features:

- **Real-time Communication**: WebSocket support for instant player interactions
- **Scalable Architecture**: Built with Spring Boot and Netty
- **Data Persistence**: MongoDB for reliable data storage
- **High Performance**: Redis caching for optimized response times
- **Containerized Deployment**: Docker and Docker Compose support

## 🛠 Technology Stack

### Core Technologies
- **Java 17**: Latest LTS version for reliability and performance
- **Spring Boot 3.4.6**: Modern web framework with comprehensive ecosystem
- **Maven 3.9.10**: Build automation and dependency management

### Key Dependencies
- **Spring Data MongoDB**: Document-oriented database integration
- **Spring WebSocket**: Real-time bidirectional communication
- **Spring Security OAuth2**: Authentication and authorization
- **Redis**: Distributed caching and session management
- **Netty 4.1.109**: High-performance network application framework
- **Lombok 1.18.30**: Reduces boilerplate code
- **MapStruct 1.5.5**: Bean mapping framework
- **Jackson**: JSON processing

### Infrastructure
- **MongoDB 6.0**: NoSQL database for flexible data models
- **Redis 7**: In-memory data store for caching
- **Amazon Corretto 17**: Java runtime for production

## 📦 Project Structure

```
WarCup-game-server/
├── src/
│   ├── main/
│   │   ├── java/          # Java source code
│   │   └── resources/     # Configuration files
│   └── test/              # Unit and integration tests
├── pom.xml                # Maven configuration
├── Dockerfile             # Container image definition
├── docker-compose.yml     # Multi-container setup
├── mvnw & mvnw.cmd       # Maven wrapper scripts
└── README.md             # This file
```

## 🚀 Getting Started

### Prerequisites
- Java 17 (JDK)
- Maven 3.9.0 or higher
- Docker & Docker Compose (for containerized setup)
- MongoDB 6.0
- Redis 7

### Local Development Setup

1. **Clone the repository**
```bash
git clone https://github.com/leminhthe04/WarCup-game-server.git
cd WarCup-game-server
```

2. **Install dependencies**
```bash
./mvnw clean install
```

3. **Configure environment**
Create a `.env` file in the project root:
```env
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/warcup
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
```

4. **Build the application**
```bash
./mvnw clean package -DskipTests
```

5. **Run locally**
```bash
./mvnw spring-boot:run
```

The server will start on port 8080 (HTTP) and port 8386 (WebSocket).

## 🐳 Docker Deployment

### Using Docker Compose (Recommended)

1. **Build and run all services**
```bash
docker-compose up -d
```

This starts:
- Game Server on port 8080 (HTTP) and 8386 (WebSocket)
- MongoDB on port 27017
- Redis on port 6379

2. **View logs**
```bash
docker-compose logs -f app
```

3. **Stop services**
```bash
docker-compose down
```

### Using Docker Directly

1. **Build the image**
```bash
docker build -t leminhthe04/game-server:latest .
```

2. **Run the container**
```bash
docker run -p 8080:8080 -p 8386:8386 \
  -e SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/warcup \
  -e SPRING_REDIS_HOST=redis \
  leminhthe04/game-server:latest
```

## 🔧 Configuration

### Environment Variables
- `SPRING_DATA_MONGODB_URI`: MongoDB connection string
- `SPRING_REDIS_HOST`: Redis server hostname
- `SPRING_REDIS_PORT`: Redis server port (default: 6379)
- `SERVER_PORT`: HTTP server port (default: 8080)

### WebSocket Configuration
- **HTTP Port**: 8080 (REST API and Spring Boot)
- **WebSocket Port**: 8386 (Real-time communication)

## 📝 Build & Test

### Compile
```bash
./mvnw clean compile
```

### Run Tests
```bash
./mvnw test
```

### Package
```bash
./mvnw clean package
```

### Install Locally
```bash
./mvnw install
```

## 🔐 Security

- OAuth2 Resource Server for API authentication
- Spring Security Crypto for password encoding
- Input validation using Spring Validation
- Netty for secure network communication

## 📊 Monitoring & Logging

The application uses Spring Boot's built-in logging configuration. Logs are output to console and can be directed to files based on your configuration.

## 🤝 Contributing

Contributions are welcome! Please ensure:
- Code follows Spring Boot best practices
- Tests are added for new features
- Documentation is updated accordingly

## 📄 License

This project is open source and available under the MIT License.

## 📞 Support

For issues, questions, or suggestions, please open an issue on GitHub.

---

**Author**: Le Minh The  
**Repository**: [leminhthe04/WarCup-game-server](https://github.com/leminhthe04/WarCup-game-server)  
**Last Updated**: 2026-03-11 15:56:01