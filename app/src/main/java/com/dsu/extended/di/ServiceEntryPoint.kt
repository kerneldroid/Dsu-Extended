package com.dsu.extended.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.dsu.extended.model.Session
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Gives the install foreground service access to the process singletons
 * without parceling live state through intents.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ServiceEntryPoint {
    fun session(): Session
    fun preferencesDataStore(): DataStore<Preferences>
}
