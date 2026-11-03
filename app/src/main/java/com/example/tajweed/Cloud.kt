package com.example.tajweed

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { c ->
    addOnSuccessListener { c.resume(it) }
    addOnFailureListener { c.resumeWithException(it) }
}

// تسجيل الدخول بحساب Google وحفظ التقدم في Firestore (users/{uid})
object Cloud {
    fun ready(ctx: Context): Boolean = try { FirebaseApp.getApps(ctx).isNotEmpty() } catch (e: Exception) { false }

    fun email(): String? = try { FirebaseAuth.getInstance().currentUser?.email } catch (e: Exception) { null }

    suspend fun signIn(ctx: Context, st: Store): String {
        if (!ready(ctx)) return "الربط غير مفعّل: ملف google-services.json غير مضاف للمشروع"
        val id = ctx.resources.getIdentifier("default_web_client_id", "string", ctx.packageName)
        if (id == 0) return "ملف google-services.json لا يحتوي معرّف العميل. فعّل الدخول بـ Google وأضف بصمة SHA-1 ثم نزّل الملف من جديد"
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(ctx.getString(id))
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val cred = CredentialManager.create(ctx).getCredential(ctx, request).credential
            if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                st.changeAccount(email())
                sync(ctx, st)
            } else "نوع بيانات الدخول غير مدعوم"
        } catch (e: GetCredentialCancellationException) {
            "أُلغي تسجيل الدخول"
        } catch (e: Exception) {
            "تعذّر تسجيل الدخول: ${e.javaClass.simpleName} ${e.message?.take(80) ?: ""}"
        }
    }

    // يقارن التقدم المحلي بالمحفوظ في الحساب ويأخذ الأعلى نقاطًا
    suspend fun sync(ctx: Context, st: Store): String {
        val u = try { FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null } ?: return "لم تسجّل الدخول بعد"
        return try {
            val ref = FirebaseFirestore.getInstance().collection("users").document(u.uid)
            val snap = ref.get().await()
            val code = snap.getString("code")
            val remoteXp = snap.getLong("xp") ?: -1L
            if (code != null && remoteXp > st.xp) {
                if (st.importCode(code)) {
                    (ctx as? Activity)?.recreate()
                    "تمت استعادة تقدمك من حسابك"
                } else "تعذّرت استعادة التقدم"
            } else {
                push(st)
                "تم حفظ تقدمك في حسابك"
            }
        } catch (e: Exception) {
            "تعذّرت المزامنة: ${e.javaClass.simpleName} ${e.message?.take(80) ?: ""}"
        }
    }

    // يرفع التقدم الحالي إن كان المستخدم مسجّلًا (يُحفظ محليًا ويُرسل عند توفر الإنترنت)
    fun push(st: Store) {
        try {
            val u = FirebaseAuth.getInstance().currentUser ?: return
            FirebaseFirestore.getInstance().collection("users").document(u.uid).set(
                mapOf("code" to st.exportCode(), "xp" to st.xp.toLong(), "updated" to System.currentTimeMillis())
            )
        } catch (e: Exception) { }
    }

    suspend fun signOut(ctx: Context, st: Store) {
        try { FirebaseAuth.getInstance().signOut() } catch (e: Exception) { }
        try { CredentialManager.create(ctx).clearCredentialState(ClearCredentialStateRequest()) } catch (e: Exception) { }
        st.changeAccount(null)
    }
}
