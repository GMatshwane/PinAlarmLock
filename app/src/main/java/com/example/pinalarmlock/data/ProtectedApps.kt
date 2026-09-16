package com.example.pinalarmlock.data

class ProtectedAppsLogic(
    private val readPackages: suspend () -> Set<String>,
    private val writePackages: suspend (Set<String>) -> Unit,
) {
    suspend fun list(): Set<String> = readPackages()

    suspend fun contains(packageName: String): Boolean = packageName in readPackages()

    suspend fun add(packageName: String) {
        writePackages(readPackages() + packageName)
    }

    suspend fun remove(packageName: String) {
        writePackages(readPackages() - packageName)
    }
}
