# 📱 Android Web Login with Chrome Custom Tabs

Guide for integrating the web login/registration pages with your Android app using Chrome Custom Tabs.

## 🌐 Your Login URLs

**Production:**
- Login: `https://web-production-2b788.up.railway.app/auth/login`
- Register: `https://web-production-2b788.up.railway.app/auth/register`

**Local Testing:**
- Login: `http://localhost:8080/auth/login`
- Register: `http://localhost:8080/auth/register`

---

## 🚀 Android Implementation

### Step 1: Add Chrome Custom Tabs Dependency

In your `build.gradle` (app level):

```gradle
dependencies {
    implementation 'androidx.browser:browser:1.8.0'
}
```

### Step 2: Create Login Helper Class

```kotlin
package org.learn.tokenviaoauth2

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat

class WebAuthHelper(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://web-production-2b788.up.railway.app"
        const val LOGIN_URL = "$BASE_URL/auth/login"
        const val REGISTER_URL = "$BASE_URL/auth/register"
    }

    /**
     * Opens login page in Chrome Custom Tab
     */
    fun openLogin() {
        openCustomTab(LOGIN_URL)
    }

    /**
     * Opens registration page in Chrome Custom Tab
     */
    fun openRegister() {
        openCustomTab(REGISTER_URL)
    }

    /**
     * Opens a URL in Chrome Custom Tab with custom styling
     */
    private fun openCustomTab(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .build()

        customTabsIntent.launchUrl(context, Uri.parse(url))
    }

    /**
     * Opens login with pre-filled username
     */
    fun openLoginWithUsername(username: String) {
        val uri = Uri.parse(LOGIN_URL).buildUpon()
            .appendQueryParameter("username", username)
            .build()

        openCustomTab(uri.toString())
    }
}
```

### Step 3: Use in Your Activity/Fragment

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var webAuthHelper: WebAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webAuthHelper = WebAuthHelper(this)

        // Login button
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            webAuthHelper.openLogin()
        }

        // Register button
        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            webAuthHelper.openRegister()
        }
    }
}
```

---

## 🎨 Customizing Chrome Custom Tabs

### Add Theme Colors

```kotlin
private fun openCustomTab(url: String) {
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setUrlBarHidingEnabled(true)
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        // Set toolbar color to match your app
        .setToolbarColor(ContextCompat.getColor(context, R.color.purple_500))
        // Set navigation bar color
        .setNavigationBarColor(ContextCompat.getColor(context, R.color.purple_700))
        // Add custom animations
        .setStartAnimations(context, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        .setExitAnimations(context, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        // Add close button icon
        .setCloseButtonIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_close))
        .build()

    customTabsIntent.launchUrl(context, Uri.parse(url))
}
```

---

## 🔄 Handling Session After Login

### Option 1: Using Cookies (Recommended for Web View)

After successful login, the server sets a session cookie. You can access it in your app:

```kotlin
class WebAuthHelper(private val context: Context) {

    private val cookieManager = CookieManager.getInstance()

    fun getSessionCookie(): String? {
        return cookieManager.getCookie("https://web-production-2b788.up.railway.app")
    }

    fun clearSession() {
        cookieManager.removeAllCookies(null)
    }
}
```

### Option 2: OAuth Flow After Login

After web login, redirect to OAuth authorization:

```kotlin
fun openOAuthFlow() {
    val authUrl = "https://web-production-2b788.up.railway.app/oauth2/authorize?" +
        "response_type=code&" +
        "client_id=liftrr-web-client&" +
        "redirect_uri=https://web-production-2b788.up.railway.app/oauth2/callback&" +
        "scope=read write openid&" +
        "code_challenge=${generateCodeChallenge()}&" +
        "code_challenge_method=S256"

    openCustomTab(authUrl)
}
```

### Option 3: Deep Link Callback

Configure your app to receive callback:

**In AndroidManifest.xml:**
```xml
<activity android:name=".AuthCallbackActivity">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />

        <data
            android:scheme="https"
            android:host="web-production-2b788.up.railway.app"
            android:pathPrefix="/oauth2/callback" />
    </intent-filter>
</activity>
```

**Handle callback:**
```kotlin
class AuthCallbackActivity : AppCompatActivity() {
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

    private fun exchangeCodeForTokens(code: String) {
        // Use Retrofit or OkHttp to exchange code for tokens
        // POST to /oauth2/token
    }
}
```

---

## 📲 Example: Complete Login Flow

```kotlin
class LoginActivity : AppCompatActivity() {

    private lateinit var webAuthHelper: WebAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        webAuthHelper = WebAuthHelper(this)

        setupUI()
    }

    private fun setupUI() {
        // Web Login Button
        findViewById<Button>(R.id.btnWebLogin).setOnClickListener {
            webAuthHelper.openLogin()
        }

        // Web Register Button
        findViewById<Button>(R.id.btnWebRegister).setOnClickListener {
            webAuthHelper.openRegister()
        }

        // OAuth Login Button
        findViewById<Button>(R.id.btnOAuthLogin).setOnClickListener {
            startOAuthFlow()
        }
    }

    private fun startOAuthFlow() {
        // Generate PKCE code verifier and challenge
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)

        // Save code verifier for later
        getSharedPreferences("oauth", MODE_PRIVATE)
            .edit()
            .putString("code_verifier", codeVerifier)
            .apply()

        // Build authorization URL
        val authUrl = "https://web-production-2b788.up.railway.app/oauth2/authorize?" +
            "response_type=code&" +
            "client_id=liftrr-web-client&" +
            "redirect_uri=https://web-production-2b788.up.railway.app/oauth2/callback&" +
            "scope=read write openid&" +
            "code_challenge=$codeChallenge&" +
            "code_challenge_method=S256"

        webAuthHelper.openCustomTab(authUrl)
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
```

---

## 🧪 Testing Locally

### Test with your computer's IP:

```kotlin
companion object {
    // For local testing, use your computer's IP address
    private const val BASE_URL = "http://192.168.1.100:8080"  // Replace with your IP
    const val LOGIN_URL = "$BASE_URL/auth/login"
    const val REGISTER_URL = "$BASE_URL/auth/register"
}
```

Find your IP:
```bash
# On Mac/Linux
ifconfig | grep "inet "

# On Windows
ipconfig
```

---

## ✅ Testing Checklist

- [ ] Chrome Custom Tabs dependency added
- [ ] WebAuthHelper class created
- [ ] Login button opens Chrome Custom Tab
- [ ] Register button opens Chrome Custom Tab
- [ ] Web pages are mobile-responsive
- [ ] Colors match your app theme
- [ ] Can successfully register a user
- [ ] Can successfully login
- [ ] Success page redirects back to app
- [ ] Session is maintained (if using cookies)
- [ ] OAuth flow works after login

---

## 🎨 UI/UX Tips

### 1. Add Loading State

```kotlin
findViewById<Button>(R.id.btnLogin).setOnClickListener { btn ->
    btn.isEnabled = false
    btn.text = "Opening Login..."

    webAuthHelper.openLogin()

    // Re-enable after delay
    btn.postDelayed({
        btn.isEnabled = true
        btn.text = "Login"
    }, 2000)
}
```

### 2. Show Error if Chrome Not Installed

```kotlin
private fun openCustomTab(url: String) {
    try {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        // Fallback to regular browser
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
```

### 3. Pre-warm Custom Tabs for Faster Loading

```kotlin
class WebAuthHelper(private val context: Context) {

    private var customTabsClient: CustomTabsClient? = null
    private var customTabsSession: CustomTabsSession? = null

    fun warmup() {
        CustomTabsClient.bindCustomTabsService(
            context,
            "com.android.chrome",
            object : CustomTabsServiceConnection() {
                override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                    customTabsClient = client
                    customTabsClient?.warmup(0L)
                    customTabsSession = customTabsClient?.newSession(null)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    customTabsClient = null
                }
            }
        )
    }
}

// In your Application class or MainActivity onCreate:
webAuthHelper.warmup()
```

---

## 📞 Common Issues

### Issue: Custom Tab doesn't close after login

**Solution:** The success page has a 3-second countdown and auto-closes. If it doesn't work:
1. Check if Chrome Custom Tabs supports `window.close()`
2. Use a deep link redirect instead

### Issue: Session not maintained

**Solution:**
1. Make sure cookies are enabled
2. Use OAuth flow instead of session cookies
3. Check HTTPS is enabled (cookies don't work with HTTP)

### Issue: Page not mobile-responsive

**Solution:** The pages are already mobile-optimized. Check viewport meta tag is present.

---

## 🚀 Next Steps

1. Test login flow in Chrome Custom Tab
2. Implement token exchange for OAuth
3. Store tokens securely in Android Keystore
4. Add token refresh logic
5. Handle token expiration

**Your web login pages are ready to use! 🎉**
