# Environment Variables for rabbit.com Deployment

Copy and paste these into your rabbit.com dashboard's environment variables section.

## 📋 Required Variables

### 1. Spring Profile
```
SPRING_PROFILES_ACTIVE=prod
```
**What it does:** Tells the app to run in production mode

---

### 2. Database Connection URL
```
SPRING_DATASOURCE_URL=<your-database-url-here>
```
**Example:**
```
SPRING_DATASOURCE_URL=jdbc:postgresql://dpg-abc123.rabbit.com:5432/oauth_db_xyz
```
**Where to get it:** Usually provided automatically when you add a PostgreSQL database to your app on rabbit.com

---

### 3. Database Username
```
SPRING_DATASOURCE_USERNAME=<your-db-username>
```
**Example:**
```
SPRING_DATASOURCE_USERNAME=oauth_user_abc
```
**Where to get it:** Provided when you create the database

---

### 4. Database Password
```
SPRING_DATASOURCE_PASSWORD=<your-db-password>
```
**Example:**
```
SPRING_DATASOURCE_PASSWORD=randomly-generated-secure-password-here
```
**Where to get it:** Provided when you create the database

---

### 5. Hibernate DDL Mode
```
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```
**What it does:** Creates database tables automatically on first run
**⚠️ Important:** After your first successful deployment, change this to `validate`

---

## 🎯 Complete Example

Here's what your environment variables should look like on rabbit.com:

```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://dpg-abc123.rabbit.com:5432/oauth_db
SPRING_DATASOURCE_USERNAME=oauth_user
SPRING_DATASOURCE_PASSWORD=Xg9mK2pLq8sR4nT7vY5zA
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

## 📝 Step-by-Step Instructions

### On rabbit.com Dashboard:

1. **Find Environment Variables Section:**
   - Look for "Settings", "Configuration", "Environment Variables", or "Config Vars"
   - Different platforms use different names

2. **Add Each Variable:**
   - Click "Add Variable" or "New Variable"
   - Enter the **Key** (e.g., `SPRING_PROFILES_ACTIVE`)
   - Enter the **Value** (e.g., `prod`)
   - Click "Save" or "Add"

3. **Database Variables:**
   - If rabbit.com automatically created a database, they might provide:
     - `DATABASE_URL` - Use this for `SPRING_DATASOURCE_URL`
     - `DB_USER` or `PGUSER` - Use this for `SPRING_DATASOURCE_USERNAME`
     - `DB_PASSWORD` or `PGPASSWORD` - Use this for `SPRING_DATASOURCE_PASSWORD`

4. **Save and Redeploy:**
   - After adding all variables, save
   - Trigger a new deployment (or it may auto-redeploy)

## ⚠️ Common Mistakes

### ❌ Wrong Database URL Format

**Incorrect:**
```
SPRING_DATASOURCE_URL=postgres://user:pass@host:5432/db
```

**Correct:**
```
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/db
```
**Note:** Must start with `jdbc:postgresql://` not just `postgres://`

### ❌ Missing Password

Don't forget to set the password! Even if it's auto-generated.

### ❌ Using H2 in Production

The app is configured for PostgreSQL in production. Make sure rabbit.com provides a PostgreSQL database.

## 🔄 After First Deployment

Once your app successfully starts for the first time:

1. Go back to environment variables
2. Change `SPRING_JPA_HIBERNATE_DDL_AUTO` from `update` to `validate`
3. Save and redeploy

**Why?**
- `update` = automatically creates/updates tables (good for first run)
- `validate` = only validates schema, doesn't change it (safer for production)

## 🧪 Testing

After setting variables and deploying, test:

```bash
# Replace YOUR_APP_URL with your actual URL
curl https://YOUR_APP_URL/actuator/health

# Should return:
# {"status":"UP"}
```

If you get errors, check the application logs on rabbit.com dashboard.

## 🆘 If Database Connection Fails

### Check These:

1. **Is PostgreSQL database created?**
   - Most platforms require you to manually add a database
   - Look for "+ Add Database" or "Resources" section

2. **Is the URL correct?**
   - Must start with `jdbc:postgresql://`
   - Check port (usually 5432)
   - Check database name at the end

3. **Are credentials correct?**
   - Username and password must match what the platform provided
   - Check for typos

4. **Check Logs:**
   - Look for: "Failed to configure a DataSource"
   - This means database connection is wrong

## 📞 Platform-Specific Help

### If you see these environment variables:

**`DATABASE_URL`** → Convert to `SPRING_DATASOURCE_URL`:
```
# If they give you:
DATABASE_URL=postgres://user:pass@host:5432/db

# Convert to:
SPRING_DATASOURCE_URL=jdbc:postgresql://user:pass@host:5432/db
```

**`PORT`** → No action needed, Spring Boot handles this automatically

**`JAVA_TOOL_OPTIONS`** → Platform sets this, you don't need to

## ✅ Final Checklist

Before deploying, make sure you have:

- [ ] `SPRING_PROFILES_ACTIVE` = `prod`
- [ ] `SPRING_DATASOURCE_URL` = Your PostgreSQL connection URL
- [ ] `SPRING_DATASOURCE_USERNAME` = Your database username
- [ ] `SPRING_DATASOURCE_PASSWORD` = Your database password
- [ ] `SPRING_JPA_HIBERNATE_DDL_AUTO` = `update` (for first deploy)

After first successful deploy:

- [ ] Changed `SPRING_JPA_HIBERNATE_DDL_AUTO` to `validate`
- [ ] Tested all OAuth endpoints work
- [ ] Verified `assetlinks.json` is accessible

---

**Need help?** Check the logs on your rabbit.com dashboard for specific error messages.
