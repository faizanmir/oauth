# OAuth 2.0 Authorization Server with Android App Links

## Overview

Production-ready OAuth 2.0 Authorization Server built with **Spring Boot 3.3.5**, **Spring Security 6.x**, and **Kotlin**. Provides secure JWT token authentication with special support for **Android App Links** integration for seamless mobile app authentication.

### Key Features

- ✅ **OAuth 2.0 Authorization Server** with multiple grant types
- ✅ **JWT Tokens** (RSA-2048 signed, self-contained)
- ✅ **Android App Links** support for mobile apps
- ✅ **Web-based login** via Chrome Custom Tabs
- ✅ **User registration** and management
- ✅ **Authorization Code + PKCE** for the mobile app (tokens never appear in a URL)

---

## Architecture

```
Android App (generates PKCE verifier + state)
    ↓
Chrome Custom Tab → GET /oauth2/authorize?response_type=code&code_challenge=…&code_challenge_method=S256
    ↓
OAuth Server (this project)
    ├─ Not signed in → 302 /auth/login (or /auth/register)
    ├─ Login succeeds → resumes the saved /oauth2/authorize request
    ├─ Consent page
    └─ 302 https://web-production-2b788.up.railway.app/oauth2/callback?code=…&state=…
    ↓  (intercepted by Android App Links)
Android App → POST /oauth2/token (code + code_verifier) → access/refresh tokens
```

Only a short-lived, single-use authorization code travels through the browser; tokens are
returned exclusively from the back-channel `/oauth2/token` call.

---

## Prerequisites

- **Java**: JDK 17 or higher
- **Gradle**: 8.x (included via wrapper)
- **Database**: H2 (development) or PostgreSQL (production)
- **Deployment**: HTTPS-enabled platform (Railway, Heroku, AWS, etc.)

---

## Project Structure

```
backend/
├── src/main/
│   ├── kotlin/com/liftrr/backend/
│   │   ├── api/controller/
│   │   │   ├── WebAuthController.kt      # Web login/register
│   │   │   ├── OAuth2CallbackController.kt # /oauth2/callback fallback page
│   │   │   └── UserController.kt         # User API
│   │   ├── security/
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.kt     # Security configuration
│   │   │   │   └── AuthorizationServerConfig.kt # OAuth2 config
│   │   │   └── service/
│   │   │       ├── CustomUserDetailsService.kt
│   │   │       └── JpaRegisteredClientRepository.kt
│   │   ├── domain/entity/
│   │   │   ├── User.kt
│   │   │   ├── Role.kt
│   │   │   └── OAuth2Client.kt
│   │   └── config/
│   │       └── DataSeeder.kt             # Pre-seed users & clients
│   └── resources/
│       ├── templates/
│       │   ├── login.html                # Web login page
│       │   ├── register.html             # Registration page
│       │   ├── success.html              # Simple status page (logout)
│       │   ├── mobile-callback.html      # Static fallback if App Links are not verified
│       │   └── home.html                 # Authenticated home
│       ├── static/
│       │   ├── css/auth.css
│       │   └── .well-known/
│       │       └── assetlinks.json       # Android App Links config
│       └── application.yml               # App configuration
└── build.gradle.kts
```

---

## Quick Start

### 1. Clone and Build

```bash
git clone <your-repo-url>
cd backend
./gradlew clean build
```

### 2. Run Locally

```bash
./gradlew bootRun
```

Server starts at `http://localhost:8080`

### 3. Access H2 Console (Development)

```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:oauth2db
Username: sa
Password: (leave empty)
```

### 4. Test Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Login page
open http://localhost:8080/auth/login

# Pre-seeded test users
# admin / admin123 (ROLE_ADMIN, ROLE_USER)
# user / user123 (ROLE_USER)
```

---

## Android App Links Integration

### Critical: What You Must Configure

For Android App Links to work correctly and deliver the authorization code to your mobile app, you **MUST** complete these steps:

### Step 1: Create assetlinks.json

**Location**: `src/main/resources/static/.well-known/assetlinks.json`

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "org.learn.tokenviaoauth2",
      "sha256_cert_fingerprints": [
        "YOUR_DEBUG_KEYSTORE_SHA256_HERE",
        "YOUR_RELEASE_KEYSTORE_SHA256_HERE"
      ]
    }
  }
]
```

**How to get your SHA-256 fingerprints:**

```bash
# Debug keystore
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android | grep SHA256

# Release keystore
keytool -list -v -keystore /path/to/release.keystore \
  -alias your-key-alias \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD | grep SHA256
```

**Example output:**
```
SHA256: 08:DC:DE:1F:35:81:EB:D9:C2:D6:58:F7:EC:E8:98:8C:BB:D8:93:A5:1E:31:18:17:88:5D:7C:73:4D:A6:9C:C8
```

### Step 2: Update OAuth Client Configuration

**File**: `src/main/kotlin/com/liftrr/backend/config/DataSeeder.kt`

```kotlin
// Mobile client configuration
val mobileClient = OAuth2Client(
    clientId = "liftrr-mobile-client",           // Must match Android app
    clientSecret = passwordEncoder.encode("mobile-secret"),  // client_secret_basic
    clientName = "Liftrr Mobile Application",
    grantTypes = mutableSetOf("authorization_code", "refresh_token"),
    redirectUris = mutableSetOf(
        "https://web-production-2b788.up.railway.app/oauth2/callback"  // Android App Links
    ),
    scopes = mutableSetOf("openid", "profile", "read", "write"),
    accessTokenTimeToLive = 3600,      // 1 hour
    refreshTokenTimeToLive = 86400,    // 24 hours
    requireProofKey = true             // PKCE (S256) required
)
```

The client also requires the user's consent on `/oauth2/authorize`.

**⚠️ IMPORTANT**: The redirect URI must match exactly:
- Your production domain (not localhost)
- The redirect URI the Android app sends in the authorize and token requests

### Step 3: Update Issuer URL

**File**: `src/main/resources/application.yml`

```yaml
spring:
  security:
    oauth2:
      authorizationserver:
        issuer: https://YOUR-PRODUCTION-DOMAIN.com  # CHANGE THIS
```

This must match your deployed URL (e.g., `https://web-production-2b788.up.railway.app`)

### Step 4: Login Page for the Authorization Server

**File**: `src/main/kotlin/com/liftrr/backend/security/config/SecurityConfig.kt`

`/auth/login` is the authorization server's login page. `AuthorizationServerConfig` sends
unauthenticated HTML requests to `/oauth2/authorize` to it via
`LoginUrlAuthenticationEntryPoint("/auth/login")`, and Spring saves the original authorize request
in the session. The default chain uses Spring Security `formLogin`:

```kotlin
.formLogin { formLogin ->
    formLogin
        .loginPage("/auth/login")
        .loginProcessingUrl("/auth/login")
        .failureUrl("/auth/login?error")
        .permitAll()
}
```

- `POST /auth/login` (`username`, `password`, CSRF token) is handled by Spring Security; on success
  the saved `/oauth2/authorize` request is resumed, on failure the user lands on `/auth/login?error`
  and a later correct login still resumes the saved request.
- `POST /auth/register` (in `WebAuthController`) creates the user, signs them in and redirects to the
  saved request URL (or `/` if there is none).
- `GET /oauth2/callback` renders `mobile-callback.html`, a static page shown only if App Links are not
  verified. It never echoes the code or state.

---

## Deployment

### Option 1: Railway (Recommended)

1. **Create Railway project**:
   ```bash
   railway login
   railway init
   ```

2. **Configure environment variables**:
   ```bash
   railway variables set SPRING_DATASOURCE_URL=<postgresql-url>
   railway variables set SPRING_DATASOURCE_USERNAME=<db-user>
   railway variables set SPRING_DATASOURCE_PASSWORD=<db-password>
   ```

3. **Deploy**:
   ```bash
   git push railway main
   ```

4. **Get your URL**:
   ```bash
   railway domain
   # Output: https://web-production-xxxxx.up.railway.app
   ```

5. **Update configurations** with this URL:
   - `application.yml` → issuer
   - `DataSeeder.kt` → redirectUris (`https://<domain>/oauth2/callback`)
   - Android app's `WebAuthHelper.kt` → BASE_URL

### Option 2: Heroku

```bash
heroku create your-app-name
heroku addons:create heroku-postgresql:essential-0
git push heroku main
```

### Option 3: Docker

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

```bash
./gradlew clean build
docker build -t oauth-server .
docker run -p 8080:8080 oauth-server
```

---

## Verification Checklist

After deployment, verify these items:

### 1. assetlinks.json is Accessible

```bash
curl https://YOUR-DOMAIN.com/.well-known/assetlinks.json
```

**Expected**: Returns JSON with your package name and SHA-256 fingerprints

**If 404**: Check that the file is in `src/main/resources/static/.well-known/assetlinks.json`

### 2. OAuth Client is Configured

Check database or DataSeeder output:
```sql
SELECT * FROM oauth2_client WHERE client_id = 'liftrr-mobile-client';
```

Verify:
- ✅ `redirect_uris` contains your production domain
- ✅ `grant_types` is "authorization_code" and "refresh_token"
- ✅ `scopes` includes "openid", "profile", "read", "write"
- ✅ PKCE is required (`require_proof_key`)

### 3. Issuer URL is Correct

```bash
curl https://YOUR-DOMAIN.com/.well-known/oauth-authorization-server
```

Should return OAuth server metadata with correct issuer.

### 4. Login Flow Works

```bash
# Test login page loads
curl -I https://YOUR-DOMAIN.com/auth/login
# Expected: HTTP 200

# Start an authorization request in a browser (PKCE values from the app or openssl)
open "https://YOUR-DOMAIN.com/oauth2/authorize?response_type=code&client_id=liftrr-mobile-client&scope=openid%20profile%20read%20write&redirect_uri=https://YOUR-DOMAIN.com/oauth2/callback&state=xyz&code_challenge=$CODE_CHALLENGE&code_challenge_method=S256"
```

**Expected**: Redirects to `/auth/login`; after login and consent the browser is redirected to
`/oauth2/callback?code=…&state=xyz`, which the app intercepts.

### 5. Callback Fallback Page

`GET /oauth2/callback` returns a static page that does not contain the code, state, or any script.

---

## Troubleshooting

### Problem: Login lands on the home page instead of returning to the app

**Symptom**: After login, user sees home.html instead of the consent page / app

**Cause**: The login was not started from `/oauth2/authorize`, so there is no saved request to resume

**Solution**: Always start the flow from the app's `/oauth2/authorize` request, not by opening
`/auth/login` directly.

### Problem: App Links not verified

**Symptom**: Browser opens instead of app after redirect

**Solutions**:
1. **Check SHA-256 matches**:
   ```bash
   # From Android keystore
   keytool -list -v -keystore ~/.android/debug.keystore ...

   # From deployed assetlinks.json
   curl https://YOUR-DOMAIN.com/.well-known/assetlinks.json
   ```

2. **Verify assetlinks.json is accessible via HTTPS** (not HTTP)

3. **Check package name matches** exactly

### Problem: Browser shows "Android App Links are not verified"

**Symptom**: `/oauth2/callback` opens in the browser instead of the app

**Cause**: App Links verification failed, so the `/oauth2/callback` fallback page is shown

**Solution**: Fix App Links verification (see above), then retry login from the app.

---

## OAuth 2.0 Grant Types Supported

### 1. Authorization Code + PKCE (Mobile App)

**Use case**: The Android app, via Chrome Custom Tabs and Android App Links

```bash
# 1. Generate code verifier (32 random bytes, base64url, 43 chars) and S256 challenge
CODE_VERIFIER=$(openssl rand 32 | base64 | tr '+/' '-_' | tr -d '=')
CODE_CHALLENGE=$(echo -n $CODE_VERIFIER | openssl dgst -binary -sha256 | base64 | tr '+/' '-_' | tr -d '=')

# 2. Get authorization code (sign in on /auth/login, approve consent)
open "https://YOUR-DOMAIN.com/oauth2/authorize?response_type=code&client_id=liftrr-mobile-client&scope=openid%20profile%20read%20write&redirect_uri=https://YOUR-DOMAIN.com/oauth2/callback&state=STATE&code_challenge=$CODE_CHALLENGE&code_challenge_method=S256"

# 3. Exchange code for tokens (client_secret_basic)
curl -X POST https://YOUR-DOMAIN.com/oauth2/token \
  -u "liftrr-mobile-client:mobile-secret" \
  -d "grant_type=authorization_code" \
  -d "code=AUTHORIZATION_CODE" \
  -d "redirect_uri=https://YOUR-DOMAIN.com/oauth2/callback" \
  -d "code_verifier=$CODE_VERIFIER"
```

Codes are single-use; replaying one fails with `invalid_grant`. If the user denies consent the
app receives `?error=access_denied&state=…`.

### 2. Refresh Token Grant

```bash
curl -X POST https://YOUR-DOMAIN.com/oauth2/token \
  -u "liftrr-mobile-client:mobile-secret" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=REFRESH_TOKEN"
```

### 3. Authorization Code + PKCE (Web Apps)

```bash
# 1. Generate code challenge
CODE_VERIFIER=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-43)
CODE_CHALLENGE=$(echo -n $CODE_VERIFIER | openssl dgst -binary -sha256 | base64 | tr -d "=+/" | tr -d "=")

# 2. Get authorization code
open "https://YOUR-DOMAIN.com/oauth2/authorize?response_type=code&client_id=liftrr-web-client&redirect_uri=https://YOUR-DOMAIN.com/authorized&code_challenge=$CODE_CHALLENGE&code_challenge_method=S256"

# 3. Exchange code for token
curl -X POST https://YOUR-DOMAIN.com/oauth2/token \
  -u "liftrr-web-client:secret" \
  -d "grant_type=authorization_code" \
  -d "code=AUTHORIZATION_CODE" \
  -d "redirect_uri=https://YOUR-DOMAIN.com/authorized" \
  -d "code_verifier=$CODE_VERIFIER"
```

### 4. Client Credentials (Service-to-Service)

```bash
curl -X POST https://YOUR-DOMAIN.com/oauth2/token \
  -u "liftrr-service-client:service-secret" \
  -d "grant_type=client_credentials"
```

---

## Pre-seeded Data

### Test Users

| Username | Password | Roles | Email |
|----------|----------|-------|-------|
| admin | admin123 | ROLE_ADMIN, ROLE_USER | admin@liftrr.com |
| user | user123 | ROLE_USER | user@liftrr.com |

### OAuth Clients

| Client ID | Secret | Grant Types | Purpose |
|-----------|--------|-------------|---------|
| liftrr-web-client | secret | authorization_code, refresh_token | Web applications |
| liftrr-mobile-client | mobile-secret | authorization_code (PKCE required), refresh_token | Android app |
| liftrr-service-client | service-secret | client_credentials | Service-to-service |

---

## Security Features

### ✅ Implemented

- **JWT Tokens**: RSA-2048 signed, self-contained
- **Password Encryption**: BCrypt with strength 10
- **HTTPS Only**: All production URLs require HTTPS
- **Token Expiry**: Access 1h, Refresh 24h
- **CSRF Protection**: Enabled for form submissions
- **SQL Injection Protection**: Parameterized queries via JPA
- **XSS Protection**: Content Security Policy headers

### 🔒 Production Recommendations

1. **Use PostgreSQL** (not H2) in production
2. **Enable HTTPS** on all endpoints
3. **Rotate keys** regularly for JWT signing
4. **Implement rate limiting** on login endpoints
5. **Add audit logging** for security events
6. **Shorter token expiry**: 15 min access, 1h refresh
7. **Implement token revocation** (blacklist)

---

## API Endpoints

### Public Endpoints (No Authentication)

```
GET  /auth/login                    # Web login page
POST /auth/login                    # Process login (Spring Security formLogin)
GET  /auth/register                 # Registration page
POST /auth/register                 # Process registration
GET  /oauth2/callback               # App Links fallback page (static)
GET  /oauth2/authorize              # Authorization endpoint (redirects to /auth/login)
GET  /.well-known/assetlinks.json   # Android App Links verification
GET  /.well-known/oauth-authorization-server  # OAuth metadata
GET  /.well-known/jwks.json         # Public keys for JWT verification
POST /oauth2/token                  # Token endpoint
GET  /actuator/health               # Health check
```

### Protected Endpoints (JWT Required)

```
GET  /api/user/me                   # Get current user info
GET  /api/user/profile              # Get user profile
GET  /                              # Home page (authenticated)
```

---

## Environment Variables

For production deployment, set these environment variables:

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database
SPRING_DATASOURCE_USERNAME=dbuser
SPRING_DATASOURCE_PASSWORD=dbpassword

# OAuth Issuer
SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER=https://your-domain.com

# Server Port (optional, defaults to 8080)
SERVER_PORT=8080

# Logging Level (optional)
LOGGING_LEVEL_ROOT=INFO
```

---

## Testing

### Run Tests

```bash
./gradlew test
```

### Integration Tests

```bash
./gradlew integrationTest
```

### Manual Testing

```bash
# Password grant is not supported; obtain tokens via Authorization Code + PKCE (see above)

# Test user info endpoint
curl http://localhost:8080/api/user/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## Development

### Hot Reload

```bash
./gradlew bootRun --continuous
```

### Database Console

```
http://localhost:8080/h2-console
```

### View Logs

```bash
tail -f logs/spring-boot-application.log
```

---

## Summary: Android App Links Checklist

To ensure Android App Links work correctly:

### Backend (This Project)
- [ ] Created `assetlinks.json` with correct package name and SHA-256 fingerprints
- [ ] Placed file at `src/main/resources/static/.well-known/assetlinks.json`
- [ ] Configured OAuth client with production redirect URIs
- [ ] Updated issuer URL in `application.yml`
- [ ] `formLogin` uses `/auth/login`; authorize entry point is `/auth/login`
- [ ] Mobile client: authorization_code + refresh_token, PKCE required, redirect `https://<domain>/oauth2/callback`
- [ ] Deployed to HTTPS-enabled platform
- [ ] Verified assetlinks.json is accessible at `https://YOUR-DOMAIN/.well-known/assetlinks.json`

### Android App
- [ ] Updated `BASE_URL` in `WebAuthHelper.kt` to match backend
- [ ] Updated `CLIENT_ID` and `CLIENT_SECRET` to match backend
- [ ] Updated `AndroidManifest.xml` with production domain
- [ ] Verified App Links with `adb shell pm get-app-links`

### Testing
- [ ] Authorize request redirects to `/auth/login`, then consent, then `/oauth2/callback?code=…&state=…`
- [ ] App automatically opens (no manual "Open in app" needed)
- [ ] Tokens are received and saved in Android app
- [ ] Can make authenticated API calls with tokens

---

**🎉 Once all items are checked, your OAuth 2.0 + Android App Links integration is complete!**

## Support

For issues:
1. Check the Troubleshooting section
2. Verify all checklist items are complete
3. Check server logs for errors
4. Verify database connections

---

## License

This project is provided as-is for educational and demonstration purposes.
