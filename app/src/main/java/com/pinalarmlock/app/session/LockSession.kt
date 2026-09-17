package com.pinalarmlock.app.session

class LockSession(
    private val ownPackage: String,
) {
    @Volatile
    var isUnlocked: Boolean = false
        private set

    fun unlock() {
        isUnlocked = true
    }

    fun lock() {
        isUnlocked = false
    }

    fun shouldGate(
        packageName: String,
        enrolled: Set<String>,
    ): Boolean {
        if (isUnlocked) return false
        if (packageName == ownPackage) return false
        return packageName in enrolled
    }
}
