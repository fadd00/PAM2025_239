package com.sample.image_board.utils

import android.content.Context
import com.sample.image_board.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {

    private var _context: Context? = null

    fun init(context: Context) {
        _context = context.applicationContext
    }

    val client by lazy {
        if (_context == null) {
            throw IllegalStateException(
                "SupabaseClient not initialized. Call init(context) first."
            )
        }

        createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            // 1. Module Auth (Login/Register)
            install(Auth)

            // 2. Module Database (Postgrest)
            install(Postgrest)

            // 3. Module Storage (Upload Gambar)
            install(Storage)
        }
    }
}
