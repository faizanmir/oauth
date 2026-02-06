# 🚀 GitHub Deployment Guide for Beginners

This guide helps you deploy your OAuth 2.0 server to rabbit.com (or any platform) using GitHub.

## 📋 What You Need

- ✅ GitHub account
- ✅ Your code pushed to GitHub
- ✅ Access to rabbit.com deployment dashboard

## 🎯 Step-by-Step Deployment

### Step 1: Push Your Code to GitHub

#### First Time Setup:

```bash
# Navigate to your project
cd /Users/faizanmir/Desktop/backend

# Initialize git (if not already done)
git init

# Add all files
git add .

# Commit
git commit -m "Initial commit: OAuth 2.0 Authorization Server"

# Create a repository on GitHub, then link it:
git remote add origin https://github.com/YOUR_USERNAME/oauth-server.git

# Push to GitHub
git push -u origin main
```

#### If You Already Have a Repository:

```bash
# Add your changes
git add .

# Commit
git commit -m "Updated configuration for rabbit.com deployment"

# Push
git push origin main
```

### Step 2: Set Up Environment Variables on rabbit.com

When deploying from GitHub, rabbit.com will ask you to set environment variables. Here's what you need:

#### Required Environment Variables:

| Variable Name | Value | Description |
|---------------|-------|-------------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates production configuration |
| `SPRING_DATASOURCE_URL` | *Provided by platform* | Database connection URL |
| `SPRING_DATASOURCE_USERNAME` | *Provided by platform* | Database username |
| `SPRING_DATASOURCE_PASSWORD` | *Provided by platform* | Database password |

#### Example Configuration on rabbit.com Dashboard:

```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://dpg-xxxxx.rabbit.com:5432/oauth_db
SPRING_DATASOURCE_USERNAME=oauth_user
SPRING_DATASOURCE_PASSWORD=randomly_generated_password
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

**Important Notes:**
- ⚠️ Use `SPRING_JPA_HIBERNATE_DDL_AUTO=update` for first deployment (creates tables)
- ⚠️ After first successful deployment, change to `validate` to prevent accidental schema changes
- ✅ Most platforms auto-provide database credentials if you add a PostgreSQL addon

### Step 3: Platform-Specific Instructions

#### Option A: If rabbit.com Uses Heroku-Style Deployment

1. **Connect GitHub Repository:**
   - Go to rabbit.com dashboard
   - Click "New App" or "Deploy"
   - Choose "Connect to GitHub"
   - Authorize GitHub access
   - Select your repository: `oauth-server`

2. **Configure Build:**
   - Build method: Gradle
   - Build command: `./gradlew build -x test`
   - Start command: `java -jar build/libs/backend-0.0.1-SNAPSHOT.jar`

3. **Add Database:**
   - Click "Resources" or "Add-ons"
   - Add "PostgreSQL" (or whatever database they offer)
   - The database URL will be auto-set as environment variables

4. **Set Environment Variables:**
   - Go to "Settings" → "Config Vars" or "Environment Variables"
   - Add each variable from the table above
   - Click "Save"

5. **Deploy:**
   - Click "Deploy Branch" or "Manual Deploy"
   - Select branch: `main`
   - Click "Deploy"

#### Option B: If rabbit.com Uses Railway-Style Deployment

1. **New Project from GitHub:**
   - Click "New Project"
   - Select "Deploy from GitHub repo"
   - Authorize and select `oauth-server`

2. **Add PostgreSQL:**
   - Click "+ New"
   - Select "Database" → "PostgreSQL"
   - Railway auto-links it to your app

3. **Configure Variables:**
   - Click on your service
   - Go to "Variables" tab
   - Add all environment variables
   - Railway auto-provides `DATABASE_URL`, convert it:
   ```bash
   SPRING_DATASOURCE_URL=${{DATABASE_URL}}
   ```

4. **Deploy:**
   - Railway auto-deploys on git push
   - Or click "Deploy" manually

#### Option C: If rabbit.com Uses Render-Style Deployment

1. **New Web Service:**
   - Dashboard → "New" → "Web Service"
   - Connect GitHub repository

2. **Configure Service:**
   - Name: `oauth-server`
   - Environment: `Java 21`
   - Build Command: `./gradlew build -x test`
   - Start Command: `java -jar build/libs/backend-0.0.1-SNAPSHOT.jar`

3. **Add Database:**
   - "New" → "PostgreSQL"
   - Name it `oauth-db`
   - Copy the "Internal Database URL"

4. **Environment Variables:**
   - Paste Internal Database URL into `SPRING_DATASOURCE_URL`
   - Add other variables

5. **Deploy:**
   - Click "Create Web Service"
   - Auto-deploys on push to main

### Step 4: Verify Deployment

After deployment completes, test these endpoints:

```bash
# Replace YOUR_APP_URL with your actual URL
# (e.g., https://oauth-server.rabbit.com)

# 1. Health check
curl https://YOUR_APP_URL/actuator/health

# 2. Check assetlinks.json
curl https://YOUR_APP_URL/.well-known/assetlinks.json

# 3. Test OAuth token endpoint
curl -X POST https://YOUR_APP_URL/oauth2/token \
  -u "liftrr-service-client:service-secret" \
  -d "grant_type=client_credentials&scope=service.read"

# 4. Verify with Google's tool
curl "https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://YOUR_APP_URL&relation=delegate_permission/common.handle_all_urls"
```

### Step 5: Check Logs

If something goes wrong:

**On Heroku-style platforms:**
```bash
# View logs from dashboard or CLI
heroku logs --tail -a your-app-name
```

**On Railway:**
- Click your service → "Deployments" → Click latest deployment → "View Logs"

**On Render:**
- Click your service → "Logs" tab

**Common Issues:**

| Error | Solution |
|-------|----------|
| "Port already in use" | Platform sets `PORT` env var automatically |
| "Database connection failed" | Check `SPRING_DATASOURCE_URL` is correct |
| "Build failed" | Make sure `./gradlew build` works locally first |
| "assetlinks.json not found" | Check file is in `src/main/resources/static/.well-known/` |

## 🔄 Updating Your Deployment

### Automatic Deployment (Recommended):

Most platforms auto-deploy when you push to GitHub:

```bash
# Make your changes
git add .
git commit -m "Updated feature X"
git push origin main

# Platform automatically detects changes and deploys
```

### Manual Deployment:

Some platforms require manual trigger:
- Go to dashboard → "Deploy" → "Deploy Branch"

## 🗄️ Database Management

### First Deployment (Creates Tables):

Set this environment variable:
```
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

This will:
1. Create all tables automatically
2. Seed initial data (users, roles, OAuth clients)

### After First Successful Deployment:

Change to:
```
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

This prevents accidental schema changes in production.

### Accessing the Database:

Most platforms provide a way to access your database:

**Heroku:**
```bash
heroku pg:psql -a your-app-name
```

**Railway/Render:**
- Dashboard → Database → "Connect" → Copy connection string
- Use with a PostgreSQL client

### Viewing Seeded Data:

```sql
-- Connect to your database, then:

-- View users
SELECT id, username, email, enabled FROM users;

-- View roles
SELECT * FROM roles;

-- View OAuth clients
SELECT id, client_id, client_name FROM oauth2_clients;
```

## 🔐 Security Checklist

Before going live:

- [ ] All environment variables set correctly
- [ ] Database credentials are secret (not in git!)
- [ ] HTTPS is enabled (should be automatic on most platforms)
- [ ] `assetlinks.json` is accessible
- [ ] Test all OAuth flows work
- [ ] Changed default passwords for seeded users
- [ ] `SPRING_JPA_HIBERNATE_DDL_AUTO` set to `validate` after first deploy

## 📱 Update Android App

After deployment, update your Android app's redirect URLs:

### In your Kotlin code:

```kotlin
// Replace with your actual deployed URL
val authUrl = "https://your-app.rabbit.com/oauth2/authorize?" +
    "response_type=code&" +
    "client_id=liftrr-web-client&" +
    "redirect_uri=https://your-app.rabbit.com/oauth2/callback&" +
    "scope=read write openid&" +
    "code_challenge=$codeChallenge&" +
    "code_challenge_method=S256"
```

### In AndroidManifest.xml:

```xml
<data
    android:scheme="https"
    android:host="your-app.rabbit.com"
    android:pathPrefix="/oauth2/callback" />
```

## 🆘 Troubleshooting Guide

### Problem: Build Fails

**Check:**
```bash
# Test build locally first
./gradlew clean build

# If it works locally, check platform build logs for specific error
```

### Problem: App Crashes on Startup

**Common causes:**
1. Database connection issue → Check `SPRING_DATASOURCE_URL`
2. Missing environment variable → Review logs for "property not found"
3. Port binding issue → Platform should set `PORT` automatically

**Check logs for:**
```
Failed to configure a DataSource
No qualifying bean of type
Port already in use
```

### Problem: assetlinks.json Returns 404

**Fix:**
1. Verify file exists: `src/main/resources/static/.well-known/assetlinks.json`
2. Rebuild: `git add . && git commit -m "Add assetlinks" && git push`
3. Check Security config allows `/.well-known/**`

### Problem: OAuth Endpoints Return Errors

**Test locally first:**
```bash
./gradlew bootRun

# Then test
curl http://localhost:8080/oauth2/token \
  -u "liftrr-service-client:service-secret" \
  -d "grant_type=client_credentials&scope=service.read"
```

If it works locally but not in production:
- Check `SPRING_PROFILES_ACTIVE=prod`
- Verify database has seeded data
- Check application logs

## 📞 Getting Help

### Platform-Specific Help:

- **Heroku**: https://devcenter.heroku.com/
- **Railway**: https://docs.railway.app/
- **Render**: https://render.com/docs

### Check Application Logs:

All platforms provide logging. Look for:
```
ERROR
Exception
Failed to
Connection refused
```

### Test Locally First:

Always test changes locally before pushing:
```bash
./gradlew clean build
./gradlew bootRun
# Test all endpoints
```

## 🎉 Success Checklist

Your deployment is successful when:

- ✅ Health endpoint responds: `https://your-app.rabbit.com/actuator/health`
- ✅ assetlinks.json accessible: `https://your-app.rabbit.com/.well-known/assetlinks.json`
- ✅ Can get OAuth token: `POST /oauth2/token` returns token
- ✅ Google's verification tool validates your domain
- ✅ Android app successfully opens links from your domain

---

## 📝 Quick Reference

### Environment Variables Template:
```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/dbname
SPRING_DATASOURCE_USERNAME=username
SPRING_DATASOURCE_PASSWORD=password
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

### Test Endpoints:
```bash
# Health
curl https://your-app.rabbit.com/actuator/health

# assetlinks
curl https://your-app.rabbit.com/.well-known/assetlinks.json

# Get token
curl -X POST https://your-app.rabbit.com/oauth2/token \
  -u "liftrr-service-client:service-secret" \
  -d "grant_type=client_credentials&scope=service.read"
```

### Common Git Commands:
```bash
git add .
git commit -m "Your message"
git push origin main
```

**You're all set! Your OAuth server is ready to deploy from GitHub! 🚀**
