# 🚀 Quick Deployment Summary

## ✅ Your App is Ready to Deploy!

### What's Configured:
- ✅ **Domain:** rabbit.com
- ✅ **Android Package:** org.learn.tokenviaoauth2
- ✅ **SHA-256 Fingerprint:** 59:7A:CC:F1:0B:F5:54:7A:25:A0:BA:EC:4E:A5:78:A1:2B:A3:43:6B
- ✅ **OAuth Issuer:** https://rabbit.com
- ✅ **App Links:** Configured and ready

---

## 📚 3-Step Deployment Process

### Step 1: Push to GitHub (5 minutes)

```bash
cd /Users/faizanmir/Desktop/backend

# If first time:
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USERNAME/oauth-server.git
git push -u origin main

# If updating:
git add .
git commit -m "Ready for deployment"
git push origin main
```

### Step 2: Deploy on rabbit.com (10 minutes)

1. Go to rabbit.com dashboard
2. Click "Deploy from GitHub" (or similar button)
3. Connect your GitHub repository
4. Add a **PostgreSQL database**
5. Set **5 environment variables** (see below)
6. Click "Deploy"

### Step 3: Verify It Works (2 minutes)

```bash
# Test health
curl https://your-app.rabbit.com/actuator/health

# Test assetlinks
curl https://your-app.rabbit.com/.well-known/assetlinks.json

# Test OAuth
curl -X POST https://your-app.rabbit.com/oauth2/token \
  -u "liftrr-service-client:service-secret" \
  -d "grant_type=client_credentials&scope=service.read"
```

---

## 🔑 Environment Variables You Need

Copy these into rabbit.com dashboard:

```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=<provided-by-rabbit-com>
SPRING_DATASOURCE_USERNAME=<provided-by-rabbit-com>
SPRING_DATASOURCE_PASSWORD=<provided-by-rabbit-com>
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

**Where to get database values?**
When you add PostgreSQL on rabbit.com, they'll give you:
- Database URL
- Username
- Password

Just copy them into the environment variables.

---

## 📖 Detailed Guides

If you need more help, read these guides:

1. **GITHUB_DEPLOYMENT_GUIDE.md** ← START HERE
   - Complete beginner-friendly guide
   - Screenshots and examples
   - Troubleshooting tips

2. **ENV_VARIABLES_TEMPLATE.md**
   - Detailed explanation of each variable
   - Copy-paste examples
   - Common mistakes to avoid

3. **ANDROID_APP_LINKS.md**
   - How to configure your Android app
   - Testing app links
   - Debugging tips

4. **DEPLOYMENT_RABBIT_COM.md**
   - Advanced deployment options
   - SSL/HTTPS configuration
   - Production best practices

---

## 🎯 What Happens After Deployment

### Automatic Setup:

When your app starts for the first time, it will automatically:

1. ✅ Create all database tables
2. ✅ Seed 2 users:
   - `admin` / `admin123`
   - `user` / `user123`
3. ✅ Seed 2 roles:
   - `ROLE_ADMIN`
   - `ROLE_USER`
4. ✅ Seed 3 OAuth clients:
   - `liftrr-web-client` (for web apps)
   - `liftrr-service-client` (for backend services)
   - `liftrr-mobile-client` (for mobile apps)

### Your Endpoints:

After deployment, these URLs will be available:

```
# OAuth Endpoints
https://rabbit.com/oauth2/authorize
https://rabbit.com/oauth2/token
https://rabbit.com/oauth2/introspect
https://rabbit.com/oauth2/revoke

# App Links
https://rabbit.com/.well-known/assetlinks.json

# API Endpoints
https://rabbit.com/api/auth/register
https://rabbit.com/api/user/me

# OAuth Discovery
https://rabbit.com/.well-known/oauth-authorization-server
```

---

## ⚠️ Important After First Deployment

After your app successfully deploys and starts:

1. Go to environment variables on rabbit.com
2. Change `SPRING_JPA_HIBERNATE_DDL_AUTO` from `update` to `validate`
3. Save (this prevents accidental database changes)

---

## 📱 Update Your Android App

After deployment, update your Android code:

```kotlin
// Replace YOUR_APP_URL with your actual URL from rabbit.com
val authUrl = "https://your-app.rabbit.com/oauth2/authorize?" +
    "response_type=code&" +
    "client_id=liftrr-web-client&" +
    "redirect_uri=https://your-app.rabbit.com/oauth2/callback&" +
    "scope=read write openid&" +
    "code_challenge=$codeChallenge&" +
    "code_challenge_method=S256"
```

In `AndroidManifest.xml`:
```xml
<data
    android:scheme="https"
    android:host="your-app.rabbit.com"
    android:pathPrefix="/oauth2/callback" />
```

---

## 🆘 If Something Goes Wrong

### Can't Connect to Database?
- Check `SPRING_DATASOURCE_URL` starts with `jdbc:postgresql://`
- Verify username and password are correct
- Check logs on rabbit.com dashboard

### assetlinks.json Returns 404?
- Verify file exists in: `src/main/resources/static/.well-known/`
- Rebuild and redeploy: `git push origin main`

### Build Fails?
- Test locally first: `./gradlew clean build`
- Check you have Java 21 installed
- Review build logs on rabbit.com

### OAuth Endpoints Don't Work?
- Check database has seeded data
- View logs for errors
- Test the token endpoint with curl

---

## ✅ Success Checklist

Your deployment is successful when:

- [ ] App shows "Status: UP" at `/actuator/health`
- [ ] Can access assetlinks.json at `/.well-known/assetlinks.json`
- [ ] Can get OAuth token from `/oauth2/token`
- [ ] Google's verification tool validates your domain
- [ ] Android app links work (test with adb)

---

## 🎉 You're Ready!

### Files Created for Deployment:
- ✅ `Procfile` - Tells platform how to run your app
- ✅ `system.properties` - Specifies Java 21
- ✅ `app.json` - Documents environment variables
- ✅ `.gitignore` - Prevents committing secrets
- ✅ Comprehensive guides (4 markdown files)

### Next Steps:
1. Read **GITHUB_DEPLOYMENT_GUIDE.md** (10 min read)
2. Push your code to GitHub
3. Deploy on rabbit.com
4. Set environment variables
5. Test endpoints
6. Update Android app
7. Celebrate! 🎊

---

**Need Help?**
- Check the logs on rabbit.com dashboard
- Read GITHUB_DEPLOYMENT_GUIDE.md for detailed steps
- All guides are in your project folder

**Good luck with your deployment! 🚀**
