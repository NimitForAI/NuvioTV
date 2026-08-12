package com.nuvio.tv.di

import com.nuvio.tv.data.remote.stremio.StremioAuthApi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt wiring for Stremio account auth.
 *
 * Uses a SEPARATE Retrofit instance pinned to Stremio's public API
 * (https://api.strem.io/) with a plain OkHttp client — no Nuvio auth
 * interceptors. Stremio auth needs no client keys, so this works on sideloaded
 * builds.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StremioRetrofit

@Module
@InstallIn(SingletonComponent::class)
object StremioAuthModule {

    // IMPORTANT: If the project already provides a Moshi @Singleton in another
    // Hilt module, DELETE this provideStremioMoshi function and change
    // provideStremioRetrofit's parameter to inject the existing Moshi
    // (i.e. `moshi: Moshi`). Two unqualified Moshi @Provides will collide.
    @Provides
    @Singleton
    @StremioRetrofit
    fun provideStremioMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    @StremioRetrofit
    fun provideStremioRetrofit(@StremioRetrofit moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.strem.io/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideStremioAuthApi(@StremioRetrofit retrofit: Retrofit): StremioAuthApi =
        retrofit.create(StremioAuthApi::class.java)
}
