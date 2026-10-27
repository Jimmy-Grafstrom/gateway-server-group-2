# Gateway server - Group 2

Gateway for the webshop microservices architecture. It is the single entry point for the frontend and foward requests to the right backend service. Build with Java and Spring Boot.

---

## 1. Responsibility

The gateway gives the frontend one address to talk to, instead of one adress per service.

- Receives requests from the frontend under the `/gateway/..` prefix.
- Forwards each request to auth-service, product-service, order-service or payment-service
- Handles the login cookie: when a user logs in, the gateway gets the JWT from auth-service and stores it in an `httpOnly` cookie named `access_token`.
- Returns only `expiresIn`, `subject` and `roles` to the frontend after successful login.
- Removes the authentication cookie when the user logs out.
- Passes the `Authorization` and `Content-Type` headers on, and returnes the staus code, body and `Content-Type` of the answer
- Handles CORS for the frontend in one place

---

## 2. Technologies 

| Area | Technology |
| :--- | :--- |
| Language | Java 17 |
| Framework | Spring Boot 4.1.1 |
| API Gateway | Spring Cloud Gateway Server Web MVC |
| Security | Spring Security, OAuth2 resource server |
| HTTP client | `java.net.http.HttpClient` with a 5 second connect timeout |
| JSON | Jackson |
| Build tool | Maven (Maven Wrapper included) |
| Container | Docker |
| CI/CD | GitHub Actions, Docker Hub |
| Hosting | Render |

---

## 3. Database

Gateway has no database.

---

## 4. Environment variables 

All variables have a default the points at a service on `localhost`, som the gateway starta without any configuration. On render set all of them.

| Variable | Description | Default |
| :--- | :--- | :--- |
| `GATEWAY_SERVER_PORT` | Port the gateway listens on | `9003` |
| `AUTH_SERVER_URL` | Base URL of auth-service | `http://localhost:8080` |
| `PRODUCT_SERVICE_URL` | Base URL of product-service | `http://localhost:5002` |
| `ORDER_SERVICE_URL` | Base URL of order-service | `http://localhost:9000` |
| `PAYMENT_SERVICE_URL` | Base URL of payment-service | `http://localhost:5005` |
| `APP_COOKIE_SECURE` | `Secure` flag of the login cookie (property `app.cookie.secure`) | `false` |
| `APP_COOKIE_SAME_SITE` | `SameSite` value of the login cookie (property `app.cookie.same-site`) | `Lax` |

---

## 5. Prerequisites

- **JAVA**: 17
- **Maven**: the Maven wrapper (`mvnw`) is included
- **Docker**: if you want to build image

---

## 6. Getting started

### Clone the repository
Clone the repository and navigate inte the project directory:
```bash
cd gateway-server-group-2
```

### Build application
```bash
.\mvnw.cmd clean package
```

### Start the application 
```bash
./mvnw spring-boot:run
```
The gateway runs on `http://localhost:9003`.

---

## 7. Endpoints 

The frontend calls these. Every path starts with `/gateway`

| Method | Gateway path | Forwarded to |
| :--- | :--- | :--- |
| `POST` | `/gateway/auth/login` | auth-service `POST /auth/login`. The token is moved to the `access_token` cookie, and the answer contains only `expiresIn`, `subject` and `roles`. |
| `POST` | `/gateway/auth/logout` | Handled by the gateway. Removes the `access_token` cookie and answers `204`. |
| `GET` | `/gateway/auth/me` | auth-service `GET /auth/me` |
| `POST` | `/gateway/auth/register` | auth-service `POST /auth/register` |
| `GET` | `/gateway/auth/jwks` | auth-service `GET /auth/jwks` |
| `GET` | `/gateway/products` | product-service `GET /api/products` |
| `GET` | `/gateway/products/{id}` | product-service `GET /api/products/{id}` |
| `POST` | `/gateway/products/add` | product-service `POST /api/products/add` |
| `DELETE` | `/gateway/products/delete/{id}` | product-service `DELETE /api/products/delete/{id}` |
| `POST` | `/gateway/order` | order-service `POST /order` |
| `POST` | `/gateway/payment/checkout` | payment-service `POST /payment/checkout` |

---

## 8. Security and CORS 

| Component | Setting/Behavior | Description |
| :--- | :--- | :--- |
| Security | Stateless & permitAll | CSRF is disabled and session management is set to stateless (SessionCreationPolicy.STATELESS). All endpoints under `/gateway/**` are set to `permitAll()` since authentication tokens/cookies are managed and verified via proxying.|
| CORS | CorsConfig.java | Explicitly allows requests from local development (`http://localhost:5173`) and production ([https://webshop-frontend-group-2.onrender.com](https://webshop-frontend-group-2.onrender.com)). Supports credentials (setAllowCredentials(true)) and methods (GET, POST, PUT, DELETE). |

---

## 9. Build, test and Docker 

| Action | Command | Description |
| :--- | :--- | :--- |
| Run local build & tests | `./mvnw clean verify` | Executes the full build and test lifecycle locally (matches the CI `build-and-test.yml` workflow). |
| Package JAR | ./mvnw clean package -DskipTests | Packages the application into a JAR file (used in the Docker build step). |
| Build Docker image locally | `docker build -t gateway-server`. | Builds the Docker image locally using the `Dockerfile`. |
| Automated Deployment | Github Actions (`deploy-docker.yml`) | Automatically builds and pushes the image to Docker Hub (${{ secrets.DOCKERHUB_USERNAME }}/gateway-server:latest) whenever a pull request is merged into main. |

---

## 10. How system fits 

```text
Frontend (React)
     |
     v
Gateway  --->  Auth-service
         --->  Product-service
         --->  Order-service   --->  RabbitMQ  --->  Email-service
         --->  Payment-service <-->  Stripe (checkout and webhook)
                      |
                      +--->  RabbitMQ
```
| Component | Responsibility |
| :--- | :--- |
| Frontend | The user interface. Only talks to the gateway. |
| Gateway | Single entry point. Forwards requests under `/gateway/...` to the right service. |
| Auth-service | Registration and login. Issues JWTs and publishes its public key (JWKS). |
| Product-service | Products, categories, image URL and stock. |
| Order-service | Creates orders. Fetches product data from product-service. |
| Payment-service | Creates Stripe Checkout sessions and receives Stripe's webhook. |
| Email-service | Listens to RabbitMQ and sends order confirmations. |
 
Default local ports: gateway 9003, auth 8080, product 5002, order 9000, payment 5005, frontend 5173.

---

## 11. Project Structure

```text
.
├── .github/workflows/
│   └── build-and-test.yml          # CI: build, test and push the Docker image
├── src/
│   ├── main/
│   │   ├── java/se/iths/gatewayservergroup2/
│   │   │   ├── client/             # HttpClientConfig (HttpClient bean, 5 s connect timeout)
│   │   │   ├── config/             # SecurityConfig, CorsConfig
│   │   │   ├── controller/         # GatewayController (the /gateway/... endpoints)
│   │   │   ├── dto/                # AuthServiceResponse, LoginResponse
│   │   │   ├── service/            # GatewayProxyService (forwards requests, handles the login cookie)
│   │   │   └── GatewayServerGroup2Application.java   # Application entry point
│   │   └── resources/
│   │       └── application.properties   # Port and service URLs
│   └── test/                       # Tests
├── Dockerfile                      # Image built from the jar in target/
├── mvnw, mvnw.cmd                  # Maven Wrapper (Linux/macOS and Windows)
└── pom.xml                         # Dependencies and build configuration
```
