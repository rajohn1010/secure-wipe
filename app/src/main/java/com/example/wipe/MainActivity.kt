package com.example.wipe

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private lateinit var root: LinearLayout

    private var failedAttempts = 0
    private var lockedUntil = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Block screenshots / recents thumbnails
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ComponentName(this, WipeAdminReceiver::class.java)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        root.removeAllViews()
        when {
            !dpm.isAdminActive(admin) -> {
                label("Step 1: grant device admin permission so the app can erase the device.")
                button("Enable device admin") {
                    startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Needed to factory-reset the device when you request it.")
                    })
                }
            }
            !PasswordStore.isSet(this) -> {
                label("Step 2: choose a wipe password (min 6 characters).")
                val pw = passwordField("New password")
                button("Save password") {
                    val chars = CharArray(pw.length()).also { pw.text.getChars(0, pw.length(), it, 0) }
                    if (chars.size < 6) toast("Too short") else {
                        PasswordStore.set(this, chars)
                        render()
                    }
                }
            }
            else -> {
                label("Enter your password, then confirm with your fingerprint to ERASE EVERYTHING.")
                val pw = passwordField("Password")
                button("ERASE EVERYTHING") {
                    if (SystemClock.elapsedRealtime() < lockedUntil) {
                        toast("Locked. Try again later."); return@button
                    }
                    val chars = CharArray(pw.length()).also { pw.text.getChars(0, pw.length(), it, 0) }
                    if (PasswordStore.verify(this, chars)) {
                        failedAttempts = 0
                        pw.text.clear()
                        askFingerprint()
                    } else {
                        failedAttempts++
                        if (failedAttempts >= 5) {
                            lockedUntil = SystemClock.elapsedRealtime() + 60_000
                            failedAttempts = 0
                        }
                        toast("Wrong password")
                    }
                }
            }
        }
    }

    private fun askFingerprint() {
        val can = BiometricManager.from(this).canAuthenticate(BIOMETRIC_STRONG)
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            toast("No enrolled fingerprint available. Wipe cancelled."); return
        }
        val prompt = BiometricPrompt(
            this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    wipe()
                }
                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    toast("Wipe cancelled: $msg")
                }
            })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm erase")
                .setSubtitle("All data on this device will be permanently deleted")
                .setAllowedAuthenticators(BIOMETRIC_STRONG)   // fingerprint only, no PIN fallback
                .setNegativeButtonText("Cancel")
                .setConfirmationRequired(true)
                .build()
        )
    }

    private fun wipe() {
        if (!dpm.isAdminActive(admin)) { toast("Device admin not active"); return }
        dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE)
    }

    // --- tiny UI helpers ---
    private fun label(t: String) = root.addView(TextView(this).apply { text = t; textSize = 18f; setPadding(0, 0, 0, 32) })
    private fun button(t: String, onClick: () -> Unit) = root.addView(Button(this).apply { text = t; setOnClickListener { onClick() } })
    private fun passwordField(hint: String) = EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }.also { root.addView(it) }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
