# Deployment Guide for rabbit.com

This guide covers deploying your OAuth 2.0 Authorization Server to **rabbit.com** with HTTPS.

## ✅ Configuration Complete

Your application has been configured for production deployment:

- **Domain**: `rabbit.com`
- **OAuth Issuer**: `https://rabbit.com`
- **Android Package**: `com.liftrr.android`
- **SHA-256 Fingerprint**: `59:7A:CC:F1:0B:F5:54:7A:25:A0:BA:EC:4E:A5:78:A1:2B:A3:43:6B`

## 🚀 Pre-Deployment Checklist

### 1. SSL/TLS Certificate
You **must** have HTTPS enabled. Android App Links require HTTPS.

```bash
# Verify your SSL certificate
openssl s_client -connect rabbit.com:443 -servername rabbit.com

# Check certificate expiry
echo | openssl s_client -connect rabbit.com:443 2>/dev/null | openssl x509 -noout -dates
```

### 2. Update Database Configuration

**Option A: Environment Variables (Recommended)**
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/oauth_db
export SPRING_DATASOURCE_USERNAME=your_db_user
export SPRING_DATASOURCE_PASSWORD=your_db_password
export SPRING_JPA_HIBERNATE_DDL_AUTO=validate  # Don't auto-create in production!
```

**Option B: Create `application-prod.yml`**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/oauth_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate  # Use Flyway/Liquibase for migrations
    show-sql: false  # Disable SQL logging in production

  h2:
    console:
      enabled: false  # Disable H2 console in production

logging:
  level:
    root: INFO
    org.springframework.security: INFO  # Less verbose in production
```

### 3. Build for Production

```bash
# Clean build
./gradlew clean build -x test

# Or with tests
./gradlew clean build

# The JAR will be at:
# build/libs/backend-0.0.1-SNAPSHOT.jar
```

## 🔧 Deployment Methods

### Method 1: Direct JAR Deployment

```bash
# Copy JAR to server
scp build/libs/backend-0.0.1-SNAPSHOT.jar user@rabbit.com:/opt/oauth-server/

# On the server
cd /opt/oauth-server
java -jar backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Method 2: Docker Deployment

Create `Dockerfile`:
```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:
```bash
# Build Docker image
docker build -t oauth-server:latest .

# Run container
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/oauth_db \
  -e SPRING_DATASOURCE_USERNAME=oauth_user \
  -e SPRING_DATASOURCE_PASSWORD=secure_password \
  -e SPRING_PROFILES_ACTIVE=prod \
  --name oauth-server \
  oauth-server:latest
```

### Method 3: Systemd Service

Create `/etc/systemd/system/oauth-server.service`:
```ini
[Unit]
Description=OAuth 2.0 Authorization Server
After=network.target postgresql.service

[Service]
Type=simple
User=oauth
WorkingDirectory=/opt/oauth-server
ExecStart=/usr/bin/java -jar /opt/oauth-server/backend-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/oauth_db"
Environment="SPRING_DATASOURCE_USERNAME=oauth_user"
Environment="SPRING_DATASOURCE_PASSWORD=secure_password"

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable oauth-server
sudo systemctl start oauth-server
sudo systemctl status oauth-server
```

## 🌐 Nginx Configuration

Create `/etc/nginx/sites-available/rabbit.com`:

```nginx
server {
    listen 80;
    server_name rabbit.com;

    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name rabbit.com;

    # SSL Certificate
    ssl_certificate /etc/letsencrypt/live/rabbit.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/rabbit.com/privkey.pem;

    # SSL Configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;

    # Proxy to Spring Boot
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket support (if needed later)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # Serve assetlinks.json with correct content type
    location /.well-known/assetlinks.json {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        add_header Content-Type application/json;
        add_header Access-Control-Allow-Origin *;
    }
}
```

Enable the site:
```bash
sudo ln -s /etc/nginx/sites-available/rabbit.com /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## 🔐 SSL/TLS Setup with Let's Encrypt

```bash
# Install Certbot
sudo apt update
sudo apt install certbot python3-certbot-nginx

# Get certificate
sudo certbot --nginx -d rabbit.com

# Auto-renewal (already set up by certbot)
sudo certbot renew --dry-run
```

## 📊 Database Setup (PostgreSQL)

```bash
# Install PostgreSQL
sudo apt install postgresql postgresql-contrib

# Create database and user
sudo -u postgres psql

postgres=# CREATE DATABASE oauth_db;
postgres=# CREATE USER oauth_user WITH ENCRYPTED PASSWORD 'secure_password';
postgres=# GRANT ALL PRIVILEGES ON DATABASE oauth_db TO oauth_user;
postgres=# \q
```

## ✅ Verify Deployment

### 1. Test assetlinks.json
```bash
curl https://rabbit.com/.well-known/assetlinks.json

# Should return:
# [{
#   "relation": ["delegate_permission/common.handle_all_urls"],
#   "target": {
#     "namespace": "android_app",
#     "package_name": "com.liftrr.android",
#     "sha256_cert_fingerprints": ["59:7A:CC:F1:0B:F5:54:7A:25:A0:BA:EC:4E:A5:78:A1:2B:A3:43:6B"]
#   }
# }]
```

### 2. Verify with Google's Tool
```bash
curl "https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://rabbit.com&relation=delegate_permission/common.handle_all_urls"
```

### 3. Test OAuth Endpoints
```bash
# Get OAuth metadata
curl https://rabbit.com/.well-known/oauth-authorization-server | jq

# Test token endpoint
curl -X POST https://rabbit.com/oauth2/token \
  -u "liftrr-service-client:service-secret" \
  -d "grant_type=client_credentials&scope=service.read"
```

### 4. Test JWT Token
```bash
TOKEN=$(curl -s -X POST https://rabbit.com/oauth2/token \
  -u "liftrr-service-client:service-secret" \
  -d "grant_type=client_credentials&scope=service.read" \
  | jq -r '.access_token')

# Decode JWT (paste at https://jwt.io)
echo $TOKEN

# Should show:
# {
#   "iss": "https://rabbit.com",
#   "sub": "liftrr-service-client",
#   ...
# }
```

## 📱 Android App Configuration

Update your Android app's `AndroidManifest.xml`:

```xml
<activity android:name=".OAuthCallbackActivity">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />

        <data
            android:scheme="https"
            android:host="rabbit.com"
            android:pathPrefix="/oauth2/callback" />
    </intent-filter>
</activity>
```

Test OAuth flow in your app:
```kotlin
val authUrl = "https://rabbit.com/oauth2/authorize?" +
    "response_type=code&" +
    "client_id=liftrr-web-client&" +
    "redirect_uri=https://rabbit.com/oauth2/callback&" +
    "scope=read write openid&" +
    "code_challenge=$codeChallenge&" +
    "code_challenge_method=S256"

val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
startActivity(intent)
```

## 🔍 Monitoring & Logs

### View Logs
```bash
# If using systemd
sudo journalctl -u oauth-server -f

# If running directly
tail -f /var/log/oauth-server/application.log
```

### Health Check
```bash
curl https://rabbit.com/actuator/health
```

### Add Spring Boot Actuator endpoints
Update `application-prod.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

## 🔒 Security Best Practices

### 1. Keep Secrets Secure
```bash
# Use environment variables, not hardcoded values
# Never commit secrets to git

# Example: .env file (don't commit this!)
DB_PASSWORD=your_secure_password
JWT_SECRET=your_jwt_secret
```

### 2. Enable Rate Limiting
Consider adding rate limiting to prevent abuse:
```yaml
# Add to application-prod.yml
spring:
  cloud:
    gateway:
      routes:
        - id: oauth
          predicates:
            - Path=/oauth2/**
          filters:
            - name: RequestRateLimiter
```

### 3. Monitor Failed Login Attempts
Check logs regularly for suspicious activity:
```bash
grep "Authentication failed" /var/log/oauth-server/application.log
```

## 🚨 Troubleshooting

### assetlinks.json not accessible
```bash
# Check Nginx config
sudo nginx -t

# Check if Spring Boot is serving it
curl http://localhost:8080/.well-known/assetlinks.json

# Check file permissions
ls -la src/main/resources/static/.well-known/
```

### App Links not working
```bash
# On Android device
adb shell pm get-app-links com.liftrr.android

# Reset verification
adb shell pm set-app-links --package com.liftrr.android 0 all
adb shell pm verify-app-links --re-verify com.liftrr.android

# Check verification status
adb shell pm get-app-links com.liftrr.android
```

### Database connection issues
```bash
# Test PostgreSQL connection
psql -h localhost -U oauth_user -d oauth_db

# Check if PostgreSQL is running
sudo systemctl status postgresql
```

## 📋 Post-Deployment Checklist

- [ ] HTTPS certificate installed and working
- [ ] Database migrated and backed up
- [ ] Environment variables set correctly
- [ ] assetlinks.json accessible at `https://rabbit.com/.well-known/assetlinks.json`
- [ ] OAuth endpoints working (test all grant types)
- [ ] Android app successfully verifying domain
- [ ] Monitoring and logging configured
- [ ] Backups scheduled
- [ ] Firewall rules configured
- [ ] SSL certificate auto-renewal tested

## 📞 Support

If you encounter issues:
1. Check application logs
2. Verify all endpoints are accessible
3. Test with Google's Digital Asset Links tool
4. Use `adb` commands to verify Android app link verification

---

**Your OAuth 2.0 server is ready for production at https://rabbit.com! 🚀**
