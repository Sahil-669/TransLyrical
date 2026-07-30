package com.example.translyrical.di

import androidx.room.Room
import com.example.translyrical.BuildConfig
import com.example.translyrical.data.local.SlangDatabase
import com.example.translyrical.data.repository.CloudSongRepository
import com.example.translyrical.data.repository.CloudSongRepositoryImpl
import com.example.translyrical.data.repository.DictionaryRepository
import com.example.translyrical.data.repository.SpotifyRepository
import com.example.translyrical.domain.LyricTranslator
import com.example.translyrical.network.LrcLibApi
import com.example.translyrical.network.SpotifyAuthApi
import com.example.translyrical.network.SpotifySearchApi
import com.example.translyrical.network.TranslationApi
import com.example.translyrical.ui.CloudSongViewModel
import com.google.firebase.firestore.FirebaseFirestore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {

    single {
        Room.databaseBuilder(
                androidContext(),
                SlangDatabase::class.java,
                "translyrical_db"
            ).fallbackToDestructiveMigration(false)
            .build()
    }

    single<LrcLibApi> {
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LrcLibApi::class.java)
    }

    single<SpotifyAuthApi> {
        Retrofit.Builder()
            .baseUrl("https://accounts.spotify.com/")
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

    single { get<SlangDatabase>().songCacheDao }
    single { get<SlangDatabase>().slangDao }
    single { DictionaryRepository(androidContext(), get()) }
    single { SpotifyRepository(get(), get()) }

    single {
        Retrofit.Builder()
            .baseUrl("https://translate.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single { get<Retrofit>().create(TranslationApi::class.java) }
    factory { LyricTranslator(get(), get(), get()) }

    single { FirebaseFirestore.getInstance() }
    single<SupabaseClient> {
        createSupabaseClient(
            supabaseUrl = "https://ffldywbvaxusbiqlwruc.supabase.co",
            supabaseKey = BuildConfig.SUPABASE_KEY_SECRET
        ) {
            install(Storage)
            install(Auth)
        }
    }
    single<CloudSongRepository> { CloudSongRepositoryImpl(get(), get()) }
    viewModel { CloudSongViewModel(get()) }
}