package com.example.pinalarmlock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.pinDataStore: DataStore<Preferences> by preferencesDataStore(name = "pin_prefs")

class PinRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.pinDataStore)

    private val logic =
        PinRepositoryLogic(
            readSaltAndHash = {
                val prefs = dataStore.data.first()
                prefs[SALT_KEY] to prefs[HASH_KEY]
            },
            writeSaltAndHash = { salt, hash ->
                dataStore.edit { prefs ->
                    prefs[SALT_KEY] = salt
                    prefs[HASH_KEY] = hash
                }
            },
        )

    suspend fun hasPin(): Boolean = logic.hasPin()

    suspend fun setPin(pin: String) = logic.setPin(pin)

    suspend fun verifyPin(pin: String): Boolean = logic.verifyPin(pin)

    companion object {
        val SALT_KEY = stringPreferencesKey("pin_salt")
        val HASH_KEY = stringPreferencesKey("pin_hash")
    }
}

class PinRepositoryLogic(
    private val readSaltAndHash: suspend () -> Pair<String?, String?>,
    private val writeSaltAndHash: suspend (salt: String, hash: String) -> Unit,
    private val generateSalt: () -> ByteArray = { PinHasher.generateSalt() },
    private val hashPin: (String, ByteArray) -> String = PinHasher::hashPin,
    private val verifyHash: (pin: String, salt: String, hash: String) -> Boolean = PinHasher::verify,
) {
    suspend fun hasPin(): Boolean {
        val (salt, hash) = readSaltAndHash()
        return !salt.isNullOrBlank() && !hash.isNullOrBlank()
    }

    suspend fun setPin(pin: String) {
        require(isValidPin(pin)) { "PIN must be 4 to 6 digits" }
        val salt = generateSalt()
        writeSaltAndHash(PinHasher.encode(salt), hashPin(pin, salt))
    }

    suspend fun verifyPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val (salt, hash) = readSaltAndHash()
        if (salt.isNullOrBlank() || hash.isNullOrBlank()) return false
        return verifyHash(pin, salt, hash)
    }

    companion object {
        fun isValidPin(pin: String): Boolean = pin.length in 4..6 && pin.all { it.isDigit() }
    }
}
