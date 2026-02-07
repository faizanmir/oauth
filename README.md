# OAuth 2.0 Authorization Server with Android App Links

## Overview

Production-ready OAuth 2.0 Authorization Server built with **Spring Boot 3.3.5**, **Spring Security 6.x**, and **Kotlin**. Provides secure JWT token authentication with special support for **Android App Links** integration for seamless mobile app authentication.

### Key Features

- ✅ **OAuth 2.0 Authorization Server** with multiple grant types
- ✅ **JWT Tokens** (RSA-2048 signed, self-contained)
- ✅ **Android App Links** support for mobile apps
- ✅ **Web-based login** via Chrome Custom Tabs
- ✅ **User registration** and management
- ✅ **Secure token delivery** to mobile apps via HTTPS deep links

---

## Architecture

```
Android App
    ↓
Chrome Custom Tab (HTTPS)
    ↓
OAuth Server (this project)
    ├─ Web Login (/auth/login)
    ├─ Generate JWT Tokens
    └─ Redirect with tokens → Android App Links
    ↓
Mobile App (tokens stored securely)
```

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
│   │   │   ├── OAuth2CallbackController.kt # Mobile callbacks
│   │   │   └── UserController.kt         # User API
│   │   ├── security/
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.kt     # Security configuration
│   │   │   │   └── AuthorizationServerConfig.kt # OAuth2 config
│   │   │   └── service/
│   │   │       └── TokenService.kt       # JWT token generation
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
│       │   ├── success.html              # Success page with redirect
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

For Android App Links to work correctly and redirect tokens to your mobile app, you **MUST** complete these steps:

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
    clientSecret = passwordEncoder.encode("mobile-secret"),  // Must match Android app
    clientName = "Liftrr Mobile Application",
    grantTypes = mutableSetOf("password", "refresh_token"),
    redirectUris = mutableSetOf(
        "https://YOUR-PRODUCTION-DOMAIN.com/oauth2/callback",  // CHANGE THIS
        "https://YOUR-PRODUCTION-DOMAIN.com/mobile/callback"   // CHANGE THIS
    ),
    scopes = mutableSetOf("read", "write", "openid"),
    accessTokenTimeToLive = 3600,      // 1 hour
    refreshTokenTimeToLive = 86400,    // 24 hours
    requireProofKey = false
)
```

**⚠️ IMPORTANT**: The redirect URIs must match:
- Your production domain (not localhost)
- The domain in your Android app's `WebAuthHelper.kt`

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

### Step 4: Disable Form Login (CRITICAL)

**File**: `src/main/kotlin/com/liftrr/backend/security/config/SecurityConfig.kt`

Ensure this configuration is in place:

```kotlin
@Bean
@Order(2)
fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http
        .authorizeHttpRequests { authorize ->
            authorize
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/oauth2/callback", "/mobile/callback").permitAll()
                .requestMatchers("/.well-known/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
        }
        // CRITICAL: formLogin() is DISABLED
        // If enabled, it redirects to /home instead of success.html
        // This breaks mobile app redirect flow
        .httpBasic(Customizer.withDefaults())
        // ... rest of config
}
```

**Why this matters**: If `.formLogin()` is enabled, Spring Security automatically redirects authenticated users to the home page (`/`) instead of rendering `success.html`, which breaks the mobile redirect flow.

### Step 5: Verify WebAuthController

**File**: `src/main/kotlin/com/liftrr/backend/api/controller/WebAuthController.kt`

Ensure the login endpoint returns the success template:

```kotlin
@PostMapping("/login")
fun login(..., redirectUrl: String?, ...): String {
    // ... authentication logic ...

    // Generate JWT tokens
    val tokens = tokenService.generateTokens(authentication, user.username)

    // CRITICAL: Add tokens to model
    model.addAttribute("accessToken", tokens.accessToken)
    model.addAttribute("refreshToken", tokens.refreshToken)
    model.addAttribute("expiresIn", tokens.expiresIn)

    // CRITICAL: Pass redirect URL to template
    redirectUrl?.let { model.addAttribute("redirectUrl", it) }

    return "success"  // Must return success.html template
}
```

### Step 6: Verify success.html Redirect Logic

**File**: `src/main/resources/templates/success.html`

The redirect logic should use HTTPS URLs (NOT Intent URIs):

```javascript
function forceRedirect() {
    if (!baseRedirectUrl) {
        console.error("Redirect failed: No redirect URL provided");
        return;
    }

    // Build URL parameters with tokens
    const params = new URLSearchParams();
    if (accessToken) params.set('access_token', accessToken);
    if (refreshToken) params.set('refresh_token', refreshToken);
    if (expiresIn) params.set('expires_in', expiresIn);
    if (userData && userData.username) params.set('username', userData.username);

    // Build HTTPS URL - Android App Links will intercept this
    const fullHttpsUrl = baseRedirectUrl + '?' + params.toString();

    // Redirect - this triggers Android App Links
    window.location.href = fullHttpsUrl;
}

// Auto-redirect after 500ms
setTimeout(forceRedirect, 500);
```

**Why HTTPS URLs**: Android App Links only work with HTTPS URLs. The verified app will automatically intercept the HTTPS callback URL and open.

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
   - `DataSeeder.kt` → redirectUris
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
- ✅ `grant_types` includes "password" and "refresh_token"
- ✅ `scopes` includes "read", "write", "openid"

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

# Test login with redirect_url
curl -X POST https://YOUR-DOMAIN.com/auth/login \
  -d "username=admin" \
  -d "password=admin123" \
  -d "redirect_url=https://YOUR-DOMAIN.com/oauth2/callback" \
  --cookie-jar cookies.txt \
  --location
```

**Expected**: Redirects to success page, not home page

### 5. Success Page Has Redirect Logic

View source of success page and verify:
- ✅ `window.location.href` is called
- ✅ Tokens are appended as URL parameters
- ✅ Uses HTTPS URL (not Intent URI)

---

## Troubleshooting

### Problem: "Welcome to Liftrr" page shows instead of success page

**Symptom**: After login, user sees home.html instead of success.html with redirect countdown

**Cause**: Spring Security's `formLogin()` is redirecting to default success URL

**Solution**: Disable `formLogin()` in `SecurityConfig.kt`:
```kotlin
// Remove or comment out:
// .formLogin { ... }
```

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

### Problem: Tokens not in redirect URL

**Symptom**: App receives redirect but no tokens

**Cause**: WebAuthController not passing tokens to model

**Solution**: Verify WebAuthController adds tokens to model:
```kotlin
model.addAttribute("accessToken", tokens.accessToken)
model.addAttribute("refreshToken", tokens.refreshToken)
model.addAttribute("expiresIn", tokens.expiresIn)
```

### Problem: No redirect happens at all

**Symptom**: Success page shows but doesn't redirect

**Cause**: `redirectUrl` is null in success.html

**Solution**: Ensure Android app passes `redirect_url` parameter:
```kotlin
// In WebAuthHelper.kt
fun openLoginWithRedirect(redirectUrl: String = REDIRECT_URI) {
    val uri = LOGIN_URL.toUri().buildUpon()
        .appendQueryParameter("redirect_url", redirectUrl)  // This line
        .build()
    openCustomTab(uri.toString())
}
```

---

## OAuth 2.0 Grant Types Supported

### 1. Password Grant (Mobile Apps)

**Use case**: Native mobile apps with secure credential storage

```bash
curl -X POST https://YOUR-DOMAIN.com/oauth2/token \
  -u "liftrr-mobile-client:mobile-secret" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123"
```

**Response**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1Ni...",
  "refresh_token": "eyJhbGciOiJSUzI1Ni...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

### 2. Refresh Token Grant

```bash
curl -X POST https://YOUR-DOMAIN.com/oauth2/token \
  -u "liftrr-mobile-client:mobile-secret" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=eyJhbGciOiJSUzI1Ni..."
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
| liftrr-mobile-client | mobile-secret | password, refresh_token | Mobile apps |
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
POST /auth/login                    # Process login
GET  /auth/register                 # Registration page
POST /auth/register                 # Process registration
GET  /oauth2/callback               # OAuth callback (mobile)
GET  /mobile/callback               # Alternative callback
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
# Test password grant
curl -X POST http://localhost:8080/oauth2/token \
  -u "liftrr-mobile-client:mobile-secret" \
  -d "grant_type=password&username=admin&password=admin123"

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
- [ ] Disabled `formLogin()` in `SecurityConfig.kt`
- [ ] WebAuthController returns "success" template (not redirect)
- [ ] success.html uses HTTPS URLs (not Intent URIs)
- [ ] Deployed to HTTPS-enabled platform
- [ ] Verified assetlinks.json is accessible at `https://YOUR-DOMAIN/.well-known/assetlinks.json`

### Android App
- [ ] Updated `BASE_URL` in `WebAuthHelper.kt` to match backend
- [ ] Updated `CLIENT_ID` and `CLIENT_SECRET` to match backend
- [ ] Updated `AndroidManifest.xml` with production domain
- [ ] Verified App Links with `adb shell pm get-app-links`

### Testing
- [ ] Login redirects to success page (not home page)
- [ ] Success page shows countdown and redirects
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
