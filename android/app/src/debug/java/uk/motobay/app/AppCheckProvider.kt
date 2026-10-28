package uk.motobay.app

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds can't pass Play Integrity, so they use a debug token. It is printed
 * to logcat on first run ("Enter this debug secret into the allow list...") and must
 * be added in Firebase console > App Check > Apps > MotoBays > Manage debug tokens.
 */
fun appCheckProviderFactory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
