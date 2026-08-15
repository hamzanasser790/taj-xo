package com.example.util

import android.util.Log

object ErrorSanitizer {

    private const val TAG = "XO_APP_ERROR"

    /**
     * Sanitizes raw error messages, network errors, Supabase JSON, or technical stacktraces
     * into clean, user-friendly, localized Arabic messages.
     * Logs raw technical details to Android Logcat for developer diagnosis.
     */
    fun sanitize(rawMessage: String?): String {
        if (rawMessage.isNullOrBlank()) return ""
        val trimmed = rawMessage.trim()

        // 1. Always log raw error for developer troubleshooting in Logcat
        Log.e(TAG, "Backend/System Error Encountered: $trimmed")

        // 2. Cloud Server / API Key / Supabase / Database errors
        if (trimmed.contains("Invalid API key", ignoreCase = true) ||
            trimmed.contains("anon", ignoreCase = true) ||
            trimmed.contains("service_role", ignoreCase = true) ||
            trimmed.contains("Supabase", ignoreCase = true) ||
            trimmed.contains("PGRST", ignoreCase = true) ||
            trimmed.contains("JWT", ignoreCase = true) ||
            trimmed.contains("apikey", ignoreCase = true) ||
            trimmed.contains("hint", ignoreCase = true) ||
            trimmed.contains("relation", ignoreCase = true) ||
            trimmed.contains("column", ignoreCase = true) ||
            trimmed.contains("syntax error", ignoreCase = true)
        ) {
            return "تعذر الاتصال بالخادم السحابي. يرجى المحاولة مرة أخرى أو التواصل مع الدعم الفني."
        }

        // 3. Internet Connectivity & Timeout issues
        if (trimmed.contains("Unable to resolve host", ignoreCase = true) ||
            trimmed.contains("ConnectException", ignoreCase = true) ||
            trimmed.contains("SocketTimeoutException", ignoreCase = true) ||
            trimmed.contains("timeout", ignoreCase = true) ||
            trimmed.contains("NoRouteToHostException", ignoreCase = true) ||
            trimmed.contains("Network is unreachable", ignoreCase = true) ||
            trimmed.contains("UnknownHostException", ignoreCase = true) ||
            trimmed.contains("Failed to connect", ignoreCase = true) ||
            trimmed.contains("SSLException", ignoreCase = true)
        ) {
            return "تعذر الاتصال بالإنترنت. يرجى التحقق من اتصالك بالشبكة وإعادة المحاولة."
        }

        // 4. Known App & Auth Business Logic Codes
        if (trimmed.contains("USER_NOT_FOUND", ignoreCase = true)) {
            return "لم يتم العثور على حساب بهذه البيانات. يرجى التأكد من البيانات أو إنشاء حساب جديد."
        }
        if (trimmed.contains("WRONG_PASSWORD", ignoreCase = true)) {
            return "كلمة المرور غير صحيحة. يرجى التأكد وإعادة المحاولة."
        }
        if (trimmed.contains("USERNAME_ALREADY_EXISTS", ignoreCase = true)) {
            return "اسم المستخدم مستخدم بالفعل، يرجى اختيار اسم آخر."
        }
        if (trimmed.contains("EMAIL_ALREADY_EXISTS", ignoreCase = true)) {
            return "البريد الإلكتروني مسجل بالفعل بحساب آخر."
        }
        if (trimmed.contains("ACCOUNT_LOCKED", ignoreCase = true) || trimmed.contains("BANNED", ignoreCase = true)) {
            return "تم تجميد هذا الحساب، يرجى التواصل مع الدعم الفني للمساعدة."
        }

        // 5. Raw JSON payloads or Java/Kotlin exception strings
        if (trimmed.startsWith("{") || trimmed.contains("\"message\":") || trimmed.contains("\"code\":") || trimmed.contains("Exception:")) {
            return "حدث خطأ أثناء معالجة الطلب، يرجى المحاولة مرة أخرى."
        }

        // 6. Clean friendly text
        return trimmed
    }
}
