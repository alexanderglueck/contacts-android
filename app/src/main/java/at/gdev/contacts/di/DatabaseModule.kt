package at.gdev.contacts.di

import android.content.Context
import androidx.room.Room
import at.gdev.contacts.data.local.AppDatabase
import at.gdev.contacts.data.local.CallEventsDao
import at.gdev.contacts.data.local.ContactsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "contacts.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideContactsDao(db: AppDatabase): ContactsDao = db.contactsDao()

    @Provides
    fun provideCallEventsDao(db: AppDatabase): CallEventsDao = db.callEventsDao()
}
