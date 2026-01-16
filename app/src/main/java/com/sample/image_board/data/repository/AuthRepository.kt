package com.sample.image_board.data.repository

import com.sample.image_board.data.model.Profile
import com.sample.image_board.data.model.Result
import com.sample.image_board.utils.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.postgrest.from
import kotlin.random.Random
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AuthRepository {

    // Ambil modul Auth dari SupabaseClient yang udah kita bikin
    private val authClient = SupabaseClient.client.auth
    private val supabase = SupabaseClient.client

    /**
     * Generate username otomatis dengan format anon-XXXXXXXX X = karakter acak (huruf kecil +
     * angka)
     */
    private fun generateAnonUsername(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val randomString = (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "anon-$randomString"
    }

    /** Cek apakah user sudah login sebelumnya (Auto Login) Return: UserSession atau null */
    fun getCurrentSession(): UserSession? {
        return authClient.currentSessionOrNull()
    }

    /** Check apakah email user sudah diverifikasi */
    fun isEmailVerified(): Boolean {
        val user = authClient.currentSessionOrNull()?.user
        return user?.emailConfirmedAt != null
    }

    /** Get Access Token (untuk keperluan API calls) */
    fun getAccessToken(): String? {
        return authClient.currentAccessTokenOrNull()
    }

    /**
     * Refresh session token secara manual Supabase SDK otomatis refresh, tapi ini bisa dipanggil
     * manual jika perlu
     */
    suspend fun refreshSession(): Result<UserSession> {
        return try {
            authClient.refreshCurrentSession()
            val session = authClient.currentSessionOrNull()
            if (session != null) {
                Result.Success(session)
            } else {
                Result.Error(Exception("Failed to refresh session."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Register User Baru Username OTOMATIS dibuat sistem dengan format anon-XXXXXXXX Kalo sukses,
     * otomatis bikin data di tabel 'profiles' (karena trigger SQL kita)
     */
    suspend fun signUp(emailInput: String, passwordInput: String): Result<Unit> {
        return try {
            // Generate username otomatis
            val autoUsername = generateAnonUsername()

            authClient.signUpWith(Email) {
                email = emailInput
                password = passwordInput
                // Metadata ini buat ngisi tabel profiles via Trigger SQL
                data = buildJsonObject {
                    put("username", JsonPrimitive(autoUsername))
                    put("full_name", JsonPrimitive(autoUsername)) // Full name = username
                }
            }

            // Pastikan profile dibuat (fallback jika trigger SQL belum ada atau gagal)
            val userId = getCurrentUserId()
            if (userId != null) {
                try {
                    // Cek apakah profile sudah ada
                    val existingProfile = supabase.from("profiles")
                            .select { filter { eq("id", userId) } }
                            .decodeSingleOrNull<Profile>()

                    if (existingProfile == null) {
                        // Profile belum ada, buat manual
                        supabase.from("profiles")
                                .insert(
                                        mapOf(
                                                "id" to userId,
                                                "username" to autoUsername,
                                                "full_name" to autoUsername,
                                                "role" to "member"
                                        )
                                )
                        println("✅ Profile manually created: $autoUsername")
                    }
                } catch (e: Exception) {
                    println("⚠️ Profile creation failed: ${e.message}")
                    // Continue anyway, trigger mungkin sudah membuat profile
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /** Login User Lama */
    suspend fun signIn(emailInput: String, passwordInput: String): Result<Unit> {
        return try {
            authClient.signInWith(Email) {
                email = emailInput
                password = passwordInput
            }

            // Cek email verification SETELAH login berhasil
            if (!isEmailVerified()) {
                // Sign out user yang belum verified
                authClient.signOut()
                return Result.Error(Exception("Email not confirmed"))
            }

            // Pastikan profile user ada di database (fix untuk user lama yang mungkin belum punya profile)
            val userId = getCurrentUserId()
            if (userId != null) {
                try {
                    val existingProfile = supabase.from("profiles")
                            .select { filter { eq("id", userId) } }
                            .decodeSingleOrNull<Profile>()

                    if (existingProfile == null) {
                        // Profile belum ada, buat otomatis
                        val autoUsername = emailInput.substringBefore("@").replace(".", "_")

                        supabase.from("profiles")
                                .insert(
                                        mapOf(
                                                "id" to userId,
                                                "username" to autoUsername,
                                                "full_name" to autoUsername,
                                                "role" to "member"
                                        )
                                )
                        println("✅ Profile auto-created for existing user: $autoUsername")
                    }
                } catch (e: Exception) {
                    println("⚠️ Profile check/creation failed: ${e.message}")
                    // Continue anyway, user sudah login
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /** Logout - Hapus session dari server & lokal */
    suspend fun signOut(): Result<Unit> {
        return try {
            authClient.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /** Get current user ID */
    fun getCurrentUserId(): String? {
        return authClient.currentSessionOrNull()?.user?.id
    }

    /**
     * Ensure profile exists for current user
     * Membuat profile otomatis jika belum ada (fix untuk user lama atau signup yang gagal)
     *
     * @return true jika profile ada atau berhasil dibuat, false jika gagal
     */
    suspend fun ensureProfileExists(): Boolean {
        val currentUser = authClient.currentSessionOrNull()?.user ?: return false
        val userId = currentUser.id

        return try {
            // Cek apakah profile sudah ada
            val existingProfile = supabase.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<Profile>()

            if (existingProfile == null) {
                // Profile belum ada, buat otomatis
                val email = currentUser.email ?: "user"
                val autoUsername = email.substringBefore("@").replace(".", "_")

                supabase.from("profiles")
                        .insert(
                                mapOf(
                                        "id" to userId,
                                        "username" to autoUsername,
                                        "full_name" to autoUsername,
                                        "role" to "member"
                                )
                        )
                println("✅ Profile auto-created: $autoUsername")
                true
            } else {
                // Profile sudah ada
                true
            }
        } catch (e: Exception) {
            println("⚠️ ensureProfileExists failed: ${e.message}")
            false
        }
    }

    /**
     * Get user role dari profiles table Return: "member", "admin", "moderator", atau null jika
     * belum login
     */
    suspend fun getUserRole(): Result<String?> {
        val userId = getCurrentUserId() ?: return Result.Success(null)

        return try {
            val profile =
                    supabase.from("profiles")
                            .select { filter { eq("id", userId) } }
                            .decodeSingleOrNull<Profile>()

            Result.Success(profile?.role)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /** Check apakah user adalah admin */
    suspend fun isAdmin(): Boolean {
        return when (val result = getUserRole()) {
            is Result.Success -> result.data == "admin"
            is Result.Error -> false
        }
    }

    /** Get current user's username from profiles table */
    suspend fun getCurrentUsername(): String? {
        val userId = getCurrentUserId() ?: return null

        return try {
            val profile =
                    supabase.from("profiles")
                            .select { filter { eq("id", userId) } }
                            .decodeSingleOrNull<Profile>()

            profile?.username
        } catch (e: Exception) {
            null
        }
    }

    /** Send password reset email User akan menerima email dengan link untuk reset password */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            authClient.resetPasswordForEmail(email)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /** Get current user's email from session */
    fun getCurrentUserEmail(): String? {
        return authClient.currentSessionOrNull()?.user?.email
    }
}
