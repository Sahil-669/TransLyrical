package com.example.translyrical.di

import com.example.translyrical.BuildConfig
import com.example.translyrical.data.repository.CloudSongRepository
import com.example.translyrical.data.repository.CloudSongRepositoryImpl
import com.example.translyrical.data.repository.SpotifyRepository
import com.example.translyrical.domain.LyricTranslator
import com.example.translyrical.network.LrcLibApi
import com.example.translyrical.network.SpotifyAuthApi
import com.example.translyrical.network.SpotifySearchApi
import com.example.translyrical.network.TranslationApi
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
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
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

    single {
        Retrofit.Builder()
            .baseUrl("https://translate.googleapis.com/")
            .client(get<OkHttpClient>())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single { get<Retrofit>().create(TranslationApi::class.java) }
    factory { LyricTranslator(api = get()) }

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
    single<CloudSongRepository> { CloudSongRepositoryImpl(get()) }
    viewModel { CloudSongViewModel(get()) }
}