package at.gdev.contacts.di

import at.gdev.contacts.data.repository.DefaultAuthRepository
import at.gdev.contacts.data.repository.DefaultCalendarRepository
import at.gdev.contacts.data.repository.DefaultContactsRepository
import at.gdev.contacts.data.repository.DefaultReferenceRepository
import at.gdev.contacts.data.repository.DefaultTeamsRepository
import at.gdev.contacts.domain.repository.AuthRepository
import at.gdev.contacts.domain.repository.CalendarRepository
import at.gdev.contacts.domain.repository.ContactsRepository
import at.gdev.contacts.domain.repository.ReferenceRepository
import at.gdev.contacts.domain.repository.TeamsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindContactsRepository(impl: DefaultContactsRepository): ContactsRepository

    @Binds
    @Singleton
    abstract fun bindTeamsRepository(impl: DefaultTeamsRepository): TeamsRepository

    @Binds
    @Singleton
    abstract fun bindReferenceRepository(impl: DefaultReferenceRepository): ReferenceRepository

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(impl: DefaultCalendarRepository): CalendarRepository
}
