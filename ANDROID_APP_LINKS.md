# Android App Links Setup Guide

This guide explains how to configure Android App Links for your OAuth 2.0 server.

## What are Android App Links?

Android App Links allow your Android app to be opened directly when users click on links from your domain (e.g., `https://yourdomain.com/oauth2/authorize`). This provides a seamless OAuth experience without the browser staying open.

## Step 1: Get Your App's SHA256 Fingerprint

### For Debug Keystore:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### For Release Keystore:

```bash
keytool -list -v -keystore /path/to/your-release-key.keystore -alias your-key-alias
```

**Look for the SHA256 fingerprint in the output:**
```
Certificate fingerprints:
         SHA256: AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99
```

### From Google Play Console:

If your app is on Google Play, you can find the SHA256 from:
1. Google Play Console → Your App → Release → Setup → App Integrity
2. Copy the **App signing key certificate** SHA-256 fingerprint

## Step 2: Update assetlinks.json

Edit `/src/main/resources/static/.well-known/assetlinks.json`:

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.liftrr.android",
      "sha256_cert_fingerprints": [
        "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99",
        "11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF"
      ]
    }
  }
]
```

**Important:** Replace `com.liftrr.android` with your actual package name!

## Step 3: Configure Your Android App

### In AndroidManifest.xml:

```xml
<activity android:name=".OAuthCallbackActivity">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />

        <!-- Handle OAuth callback URLs -->
        <data
            android:scheme="https"
            android:host="yourdomain.com"
            android:pathPrefix="/oauth2/callback" />
    </intent-filter>
</activity>
```

### Key attributes:
- `android:autoVerify="true"` - Enables automatic verification
- `android:scheme="https"` - Must use HTTPS in production
- `android:host="yourdomain.com"` - Your OAuth server domain

## Step 4: Test the Configuration

### Test assetlinks.json locally:

```bash
# Start your server
./gradlew bootRun

# Verify the file is accessible
curl http://localhost:8080/.well-known/assetlinks.json
```

### Test in production:

1. Deploy your server to a domain with HTTPS
2. Verify: `https://yourdomain.com/.well-known/assetlinks.json`
3. Use Google's verification tool:
   ```
   https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://yourdomain.com&relation=delegate_permission/common.handle_all_urls
   ```

## Step 5: OAuth Flow with App Links

### Authorization Code Flow:

```kotlin
// In your Android app
val authorizationUrl = "https://yourdomain.com/oauth2/authorize?" +
    "response_type=code&" +
    "client_id=liftrr-mobile-client&" +
    "redirect_uri=https://yourdomain.com/oauth2/callback&" +
    "scope=read write openid&" +
    "code_challenge=$codeChallenge&" +
    "code_challenge_method=S256"

// Open in Chrome Custom Tabs
val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl))
startActivity(intent)
```

### Handle the callback:

```kotlin
class OAuthCallbackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (uri != null) {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                // Exchange code for tokens
                exchangeCodeForTokens(code)
            }
        }

        finish()
    }
}
```

## Common Issues & Debugging

### 1. Links open in browser instead of app

**Causes:**
- SHA256 fingerprint mismatch
- Package name mismatch
- HTTPS not used
- `autoVerify="true"` missing

**Fix:**
```bash
# Check what Android thinks about your app links
adb shell pm get-app-links com.liftrr.android

# Reset verification
adb shell pm set-app-links --package com.liftrr.android 0 all
adb shell pm verify-app-links --re-verify com.liftrr.android
```

### 2. assetlinks.json not accessible

**Checklist:**
- ✅ File is in `/src/main/resources/static/.well-known/assetlinks.json`
- ✅ Server allows access to `/.well-known/**` (check SecurityConfig)
- ✅ HTTPS enabled in production
- ✅ No redirect from HTTP to HTTPS that changes the path

### 3. Verify current status:

```bash
# Check verification status
adb shell pm get-app-links com.liftrr.android

# Output should show:
# com.liftrr.android:
#   ID: xxxx
#   Signatures: [AA:BB:CC:...]
#   Domain verification state:
#     yourdomain.com: verified
```

## Multiple Apps (Debug + Release)

You can list multiple SHA256 fingerprints for different builds:

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.liftrr.android",
      "sha256_cert_fingerprints": [
        "AA:BB:CC:...",  // Release keystore
        "11:22:33:...",  // Debug keystore
        "44:55:66:..."   // CI/CD keystore
      ]
    }
  },
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.liftrr.android.debug",
      "sha256_cert_fingerprints": [
        "77:88:99:..."  // Debug app variant
      ]
    }
  }
]
```

## iOS Universal Links (Bonus)

For iOS, create `apple-app-site-association`:

```bash
# Create the file (no .json extension!)
cat > /Users/faizanmir/Desktop/backend/src/main/resources/static/.well-known/apple-app-site-association << 'EOF'
{
  "applinks": {
    "apps": [],
    "details": [
      {
        "appID": "TEAM_ID.com.liftrr.ios",
        "paths": ["/oauth2/callback", "/oauth2/*"]
      }
    ]
  }
}
EOF
```

## Production Checklist

Before deploying to production:

- [ ] HTTPS enabled on your domain
- [ ] `assetlinks.json` accessible at `https://yourdomain.com/.well-known/assetlinks.json`
- [ ] Correct package name in assetlinks.json
- [ ] Correct SHA256 from release keystore
- [ ] Update `authorizationserver.issuer` in application.yml to production URL
- [ ] Test with Google's verification tool
- [ ] Verify app opens links using `adb shell pm get-app-links`

## References

- [Android App Links Documentation](https://developer.android.com/training/app-links)
- [Digital Asset Links](https://developers.google.com/digital-asset-links/v1/getting-started)
- [OAuth 2.0 for Mobile Apps](https://datatracker.ietf.org/doc/html/rfc8252)

## Testing Commands Quick Reference

```bash
# Get debug keystore SHA256
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA256

# Test assetlinks.json
curl http://localhost:8080/.well-known/assetlinks.json | jq

# Verify with Google
curl "https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://yourdomain.com&relation=delegate_permission/common.handle_all_urls" | jq

# Check app link verification status
adb shell pm get-app-links com.liftrr.android

# Force re-verification
adb shell pm verify-app-links --re-verify com.liftrr.android

# Clear app data and cache
adb shell pm clear com.liftrr.android
```
