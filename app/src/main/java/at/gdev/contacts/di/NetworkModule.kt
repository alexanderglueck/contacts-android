package at.gdev.contacts.di

import at.gdev.contacts.data.network.ApiConfig
import at.gdev.contacts.data.network.AuthApi
import at.gdev.contacts.data.network.AuthInterceptor
import at.gdev.contacts.data.network.CalendarApi
import at.gdev.contacts.data.network.ContactsApi
import at.gdev.contacts.data.network.ReferenceApi
import at.gdev.contacts.data.network.TeamsApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttp(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideTeamsApi(retrofit: Retrofit): TeamsApi = retrofit.create(TeamsApi::class.java)

    @Provides
    @Singleton
    fun provideContactsApi(retrofit: Retrofit): ContactsApi = retrofit.create(ContactsApi::class.java)

    @Provides
    @Singleton
    fun provideReferenceApi(retrofit: Retrofit): ReferenceApi = retrofit.create(ReferenceApi::class.java)

    @Provides
    @Singleton
    fun provideCalendarApi(retrofit: Retrofit): CalendarApi = retrofit.create(CalendarApi::class.java)
}
