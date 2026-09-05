package com.dsu.extended.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Gives non-injected entry points (Glance widget, workers) access to the
 * singleton preferences DataStore. A second DataStore instance on the same
 * file throws IllegalStateException, so the widget must reuse this one.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDataStoreEntryPoint {
    fun preferencesDataStore(): DataStore<Preferences>
}
