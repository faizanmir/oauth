# OAuth 2.0 Authorization Server

A complete OAuth 2.0 Authorization Server implementation using **Spring Boot 3.3.5**, **Spring Security 6.x**, and **Kotlin**.

## 🚀 Features

- ✅ **OAuth 2.0 Authorization Server** with JWT tokens
- ✅ **Client Credentials Grant** for service-to-service authentication
- ✅ **Authorization Code Flow** with PKCE support
- ✅ **User Registration & Authentication**
- ✅ **JWT-based Resource Server** protection
- ✅ **H2 In-Memory Database** (easily switchable to PostgreSQL/MySQL)
- ✅ **Spring Data JPA** for database access
- ✅ **Pre-seeded Test Data** (users, roles, OAuth clients)

## 📋 Tech Stack

- **Spring Boot**: 3.3.5
- **Kotlin**: 1.9.25
- **Spring Security**: 6.x
- **Spring Authorization Server**: 1.x
- **Database**: H2 (in-memory)
- **Java**: 21

## 🛠️ Getting Started

### Prerequisites
- JDK 21
- Gradle 9.3.0+

### Running the Application

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## 🔐 Seeded Test Data

### Users
| Username | Password | Email | Roles |
|----------|----------|-------|-------|
| `admin` | `admin123` | admin@liftrr.com | ROLE_ADMIN, ROLE_USER |
| `user` | `user123` | user@liftrr.com | ROLE_USER |

### OAuth 2.0 Clients
| Client ID | Client Secret | Grant Types | Scopes | Description |
|-----------|---------------|-------------|---------|-------------|
| `liftrr-web-client` | `secret` | authorization_code, refresh_token | read, write, openid, profile, email | Web application with PKCE |
| `liftrr-service-client` | `service-secret` | client_credentials | service.read, service.write | Service-to-service |
| `liftrr-mobile-client` | `mobile-secret` | refresh_token | read, write, openid | Mobile application |

## 📖 API Usage

### 1. Client Credentials Grant (Service-to-Service)

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -u "liftrr-service-client:service-secret" \
  -d "grant_type=client_credentials&scope=service.read"
```

**Response:**
```json
{
  "access_token": "eyJraWQiOiI...",
  "token_type": "Bearer",
  "expires_in": 3599,
  "scope": "service.read"
}
```

### 2. User Registration

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "test123",
    "email": "test@example.com"
  }'
```

**Response:**
```json
{
  "id": 3,
  "username": "testuser",
  "email": "test@example.com",
  "roles": ["ROLE_USER"]
}
```

### 3. Form Login (Get Session Cookie)

```bash
curl -X POST http://localhost:8080/login \
  -d "username=user&password=user123" \
  -c cookies.txt
```

### 4. Authorization Code Flow (with PKCE)

#### Step 1: Generate PKCE code verifier and challenge
```bash
# Generate code verifier (random string)
CODE_VERIFIER=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-43)

# Generate code challenge (SHA256 hash of verifier, base64url encoded)
CODE_CHALLENGE=$(echo -n $CODE_VERIFIER | openssl dgst -sha256 -binary | base64 | tr -d "=+/" | tr -d '\n')
```

#### Step 2: Get authorization code (requires user authentication)
```
http://localhost:8080/oauth2/authorize?
  response_type=code&
  client_id=liftrr-web-client&
  redirect_uri=http://localhost:3000/authorized&
  scope=read write&
  code_challenge=$CODE_CHALLENGE&
  code_challenge_method=S256
```

#### Step 3: Exchange code for tokens
```bash
curl -X POST http://localhost:8080/oauth2/token \
  -u "liftrr-web-client:secret" \
  -d "grant_type=authorization_code&code=AUTHORIZATION_CODE&redirect_uri=http://localhost:3000/authorized&code_verifier=$CODE_VERIFIER"
```

### 5. Access Protected API with JWT

```bash
# Get token first
ACCESS_TOKEN=$(curl -s -X POST http://localhost:8080/oauth2/token \
  -u "liftrr-service-client:service-secret" \
  -d "grant_type=client_credentials&scope=service.read" \
  | jq -r '.access_token')

# Access protected endpoint
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/user/me
```

## 🗄️ Database Access

### H2 Console
- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:oauth2db`
- **Username**: `sa`
- **Password**: (leave empty)

## 🏗️ Project Structure

```
src/main/kotlin/com/liftrr/backend/
├── LiftrrApplication.kt
├── api/
│   ├── controller/
│   │   ├── AuthController.kt        # Registration endpoint
│   │   └── UserController.kt        # JWT-protected endpoints
│   └── dto/
│       └── AuthDtos.kt              # DTOs for requests/responses
├── config/
│   └── DataSeeder.kt                # Seeds initial data
├── domain/
│   ├── entity/
│   │   ├── User.kt                  # User entity (implements UserDetails)
│   │   ├── Role.kt                  # Role entity
│   │   ├── OAuth2Client.kt          # OAuth client configuration
│   │   └── RefreshToken.kt          # Refresh token management
│   └── repository/
│       ├── UserRepository.kt
│       ├── RoleRepository.kt
│       ├── OAuth2ClientRepository.kt
│       └── RefreshTokenRepository.kt
└── security/
    ├── config/
    │   ├── AuthorizationServerConfig.kt  # OAuth server configuration
    │   ├── SecurityConfig.kt             # Default security
    │   └── ResourceServerConfig.kt       # JWT resource server
    └── service/
        ├── CustomUserDetailsService.kt
        └── JpaRegisteredClientRepository.kt
```

## 🔧 Configuration

### Application Configuration (`application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:oauth2db
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

  h2:
    console:
      enabled: true
      path: /h2-console

  security:
    oauth2:
      authorizationserver:
        issuer: http://localhost:8080

server:
  port: 8080
```

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests OAuth2AuthorizationCodeFlowTest
```

## 📝 API Endpoints

### OAuth 2.0 Endpoints
- `GET /oauth2/authorize` - Authorization endpoint
- `POST /oauth2/token` - Token endpoint
- `POST /oauth2/introspect` - Token introspection
- `POST /oauth2/revoke` - Token revocation
- `GET /.well-known/oauth-authorization-server` - Server metadata
- `GET /.well-known/jwks.json` - JSON Web Key Set

### Authentication Endpoints
- `POST /api/auth/register` - User registration
- `POST /login` - Form login

### Protected API Endpoints (require JWT)
- `GET /api/user/me` - Current user information
- `GET /api/user/profile` - User profile details
- `GET /api/admin/**` - Admin-only endpoints (requires ROLE_ADMIN)

## 🔑 JWT Token Structure

Tokens are signed with RSA-2048 and contain:

```json
{
  "sub": "user",
  "aud": "liftrr-service-client",
  "scope": ["service.read"],
  "iss": "http://localhost:8080",
  "exp": 1738757234,
  "iat": 1738753634
}
```

## 🚀 Production Deployment

### Important Changes for Production:

1. **Switch to Production Database**
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/oauth_db
       username: ${DB_USERNAME}
       password: ${DB_PASSWORD}
     jpa:
       hibernate:
         ddl-auto: validate  # Use Flyway/Liquibase
   ```

2. **Externalize RSA Keys**
   - Current implementation generates keys on startup
   - For production, load keys from secure vault or file system

3. **Enable HTTPS**
   ```yaml
   server:
     ssl:
       enabled: true
       key-store: classpath:keystore.p12
       key-store-password: ${KEYSTORE_PASSWORD}
   ```

4. **Update Issuer URL**
   ```yaml
   spring:
     security:
       oauth2:
         authorizationserver:
           issuer: https://your-domain.com
   ```

5. **Disable H2 Console**
   ```yaml
   spring:
     h2:
       console:
         enabled: false
   ```

6. **Add Rate Limiting** - Protect token endpoints from abuse

7. **Implement Token Revocation** - Use RefreshToken entity for blacklisting

## 📚 Additional Resources

- [Spring Authorization Server Documentation](https://docs.spring.io/spring-authorization-server/reference/)
- [OAuth 2.0 RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)
- [JWT RFC 7519](https://datatracker.ietf.org/doc/html/rfc7519)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)

## 🐛 Troubleshooting

### Port Already in Use
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9
```

### Database Connection Issues
- Check H2 console at `/h2-console`
- Verify JDBC URL: `jdbc:h2:mem:oauth2db`

### Token Validation Failures
- Ensure issuer URL matches configuration
- Verify token hasn't expired
- Check JWT signature with public key from `/oauth2/jwks`

## 📄 License

This project is for educational/demonstration purposes.

## 👥 Contributors

Built with Spring Boot 3.3.5 and Spring Security OAuth 2.0 Authorization Server.
