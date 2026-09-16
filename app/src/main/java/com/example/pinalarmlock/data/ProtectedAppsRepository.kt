package com.example.pinalarmlock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first

class ProtectedAppsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.pinDataStore)

    private val logic = ProtectedAppsLogic(
        readPackages = { dataStore.data.first()[PACKAGES_KEY] ?: emptySet() },
        writePackages = { value ->
            dataStore.edit { prefs -> prefs[PACKAGES_KEY] = value }
        },
    )

    suspend fun list(): Set<String> = logic.list()

    suspend fun contains(packageName: String): Boolean = logic.contains(packageName)

    suspend fun add(packageName: String) = logic.add(packageName)

    suspend fun remove(packageName: String) = logic.remove(packageName)

    companion object {
        val PACKAGES_KEY = stringSetPreferencesKey("protected_packages")
    }
}
