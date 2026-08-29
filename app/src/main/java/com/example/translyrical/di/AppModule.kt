package com.example.translyrical.di

import com.example.translyrical.BuildConfig
import com.example.translyrical.data.repository.CloudSongRepository
import com.example.translyrical.data.repository.CloudSongRepositoryImpl
import com.example.translyrical.data.repository.SpotifyRepository
import com.example.translyrical.domain.LyricTranslator
import com.example.translyrical.network.GeminiApi
import com.example.translyrical.network.ITunesApi
import com.example.translyrical.network.LrcLibApi
import com.example.translyrical.network.SpotifyAuthApi
import com.example.translyrical.network.SpotifySearchApi
import com.example.translyrical.ui.CloudSongViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import okhttp3.OkHttpClient
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {

    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "TransLyrical/1.0 (kur0)")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    single<LrcLibApi> {
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(get<OkHttpClient>())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LrcLibApi::class.java)
    }

    single<SpotifyAuthApi> {
        Retrofit.Builder()
            .baseUrl("https://accounts.spotify.com/")
            .client(get<OkHttpClient>())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyAuthApi::class.java)
    }

    single<SpotifySearchApi> {
        Retrofit.Builder()
            .baseUrl("https://api.spotify.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifySearchApi::class.java)
    }

    single { SpotifyRepository(get(), get()) }

    single<SupabaseClient> {
        createSupabaseClient(
            supabaseUrl = "https://ffldywbvaxusbiqlwruc.supabase.co",
            supabaseKey = BuildConfig.SUPABASE_KEY_SECRET
        ) {
            install(Storage)
            install(Auth)
            install(Postgrest)
        }
    }

    single<GeminiApi> {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(get<OkHttpClient>())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }

    single<ITunesApi> {
        Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(get<OkHttpClient>())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ITunesApi::class.java)
    }

    single { LyricTranslator(get()) }
    single<CloudSongRepository> { CloudSongRepositoryImpl(get()) }
    viewModel { CloudSongViewModel(get()) }
}