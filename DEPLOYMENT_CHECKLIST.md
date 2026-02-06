# ✅ Deployment Checklist

Print this and check off each item as you complete it!

---

## Before Deployment

- [ ] Tested app locally with `./gradlew bootRun`
- [ ] Verified assetlinks.json accessible at http://localhost:8080/.well-known/assetlinks.json
- [ ] Have GitHub account ready
- [ ] Have rabbit.com account ready

---

## GitHub Setup

- [ ] Created GitHub repository (e.g., `oauth-server`)
- [ ] Initialized git in project: `git init`
- [ ] Added all files: `git add .`
- [ ] Created first commit: `git commit -m "Initial commit"`
- [ ] Linked to GitHub: `git remote add origin <your-repo-url>`
- [ ] Pushed to GitHub: `git push -u origin main`
- [ ] Verified code is visible on GitHub.com

---

## rabbit.com Configuration

### Database Setup
- [ ] Added PostgreSQL database to project
- [ ] Copied database URL
- [ ] Copied database username
- [ ] Copied database password

### Environment Variables
- [ ] Set `SPRING_PROFILES_ACTIVE` = `prod`
- [ ] Set `SPRING_DATASOURCE_URL` = (your database URL)
- [ ] Set `SPRING_DATASOURCE_USERNAME` = (your database username)
- [ ] Set `SPRING_DATASOURCE_PASSWORD` = (your database password)
- [ ] Set `SPRING_JPA_HIBERNATE_DDL_AUTO` = `update`
- [ ] Saved all environment variables

### Deployment
- [ ] Connected GitHub repository to rabbit.com
- [ ] Selected correct repository
- [ ] Selected correct branch (usually `main`)
- [ ] Clicked "Deploy" button
- [ ] Waited for build to complete
- [ ] Build succeeded (no errors)

---

## Verify Deployment

### Test Endpoints
- [ ] Health check works:
  ```bash
  curl https://your-app.rabbit.com/actuator/health
  ```
  Response: `{"status":"UP"}`

- [ ] assetlinks.json accessible:
  ```bash
  curl https://your-app.rabbit.com/.well-known/assetlinks.json
  ```
  Response shows your package name

- [ ] OAuth token endpoint works:
  ```bash
  curl -X POST https://your-app.rabbit.com/oauth2/token \
    -u "liftrr-service-client:service-secret" \
    -d "grant_type=client_credentials&scope=service.read"
  ```
  Response includes `access_token`

- [ ] User registration works:
  ```bash
  curl -X POST https://your-app.rabbit.com/api/auth/register \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test123","email":"test@test.com"}'
  ```
  Response shows new user created

### Verify with Google
- [ ] Google's asset links tool validates your domain:
  ```bash
  curl "https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://your-app.rabbit.com&relation=delegate_permission/common.handle_all_urls"
  ```

### Check Logs
- [ ] Viewed application logs on rabbit.com dashboard
- [ ] Saw "Started LiftrrApplicationKt" message
- [ ] Saw "Roles seeded successfully"
- [ ] Saw "Users seeded successfully"
- [ ] Saw "OAuth2 clients seeded successfully"
- [ ] No errors in logs

---

## Post-Deployment Security

- [ ] Changed `SPRING_JPA_HIBERNATE_DDL_AUTO` to `validate`
- [ ] Redeployed after changing DDL mode
- [ ] Changed default admin password (optional but recommended)
- [ ] Verified HTTPS is working (URL starts with `https://`)

---

## Android App Update

- [ ] Updated OAuth authorization URL to use your deployed domain
- [ ] Updated redirect URI in AndroidManifest.xml
- [ ] Updated `android:host` to your deployed domain
- [ ] Tested app link verification:
  ```bash
  adb shell pm get-app-links org.learn.tokenviaoauth2
  ```
- [ ] Verified app opens when clicking links from your domain
- [ ] OAuth flow works end-to-end

---

## Final Verification

- [ ] OAuth authorization flow works in Android app
- [ ] Can obtain access tokens
- [ ] Can refresh tokens (if using refresh grant)
- [ ] Protected API endpoints require valid JWT
- [ ] Invalid tokens are rejected
- [ ] App links open the Android app (not browser)

---

## Backup & Documentation

- [ ] Documented deployed URL somewhere safe
- [ ] Saved database credentials securely
- [ ] Noted OAuth client secrets
- [ ] Saved GitHub repository URL
- [ ] Bookmarked rabbit.com dashboard

---

## Troubleshooting (If Needed)

If something doesn't work:

### Build Failed
- [ ] Read build logs on rabbit.com
- [ ] Check `./gradlew build` works locally
- [ ] Verify Java 21 is specified in `system.properties`
- [ ] Check all files are committed to GitHub

### App Crashes on Startup
- [ ] Check application logs
- [ ] Verify database credentials are correct
- [ ] Ensure `SPRING_DATASOURCE_URL` starts with `jdbc:postgresql://`
- [ ] Check all environment variables are set

### Database Connection Failed
- [ ] Verify PostgreSQL database is created
- [ ] Check database URL format
- [ ] Verify username and password
- [ ] Test database connection from rabbit.com dashboard

### assetlinks.json Not Found
- [ ] Verify file exists at `src/main/resources/static/.well-known/assetlinks.json`
- [ ] Check file is in git: `git ls-files | grep assetlinks`
- [ ] Rebuild and redeploy
- [ ] Check Security config allows `/.well-known/**`

### OAuth Endpoints Error
- [ ] Check database has seeded data
- [ ] Verify client credentials are correct
- [ ] Check issuer URL matches your deployed domain
- [ ] Review application logs

---

## Success! 🎉

When all items are checked:

✅ Your OAuth 2.0 Authorization Server is live!
✅ Android App Links are working!
✅ Your Android app can authenticate users!

**Deployed URL:** _________________________

**Next Steps:**
- Monitor application logs regularly
- Set up monitoring/alerts
- Consider adding rate limiting
- Plan for database backups

---

**Date Deployed:** _______________
**Deployed By:** _________________
**GitHub Repo:** _________________
**rabbit.com URL:** ______________
