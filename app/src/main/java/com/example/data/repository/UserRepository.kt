package com.example.data.repository

import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class UserRepository(
    private val userDao: UserDao,
    private val preferences: UserPreferences
) {

    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    val activeUser: Flow<UserEntity?> = preferences.activeUserIdFlow.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(null) else userDao.getUserById(id)
    }

    suspend fun getActiveUserDirect(): UserEntity? {
        val activeId = preferences.activeUserIdFlow.firstOrNull() ?: return null
        return userDao.getUserByIdDirect(activeId)
    }

    suspend fun createProfile(displayName: String, role: String): UserEntity {
        val user = UserEntity(
            id = UUID.randomUUID().toString(),
            displayName = displayName.trim(),
            role = role.uppercase()
        )
        userDao.insertUser(user)
        preferences.setActiveUserId(user.id)
        return user
    }

    suspend fun switchProfile(userId: String) {
        val user = userDao.getUserByIdDirect(userId)
        if (user != null) {
            preferences.setActiveUserId(userId)
        }
    }

    suspend fun verifyAndAuthorizeAdmin(pin: String): Boolean {
        val storedHash = preferences.adminPinHashFlow.firstOrNull() ?: UserPreferences.hashPin(UserPreferences.DEFAULT_ADMIN_PIN)
        return preferences.verifyAdminPin(pin, storedHash)
    }

    suspend fun initializeDefaultUsersIfEmpty() {
        val existing = userDao.getAllUsersDirect()
        if (existing.isEmpty()) {
            val student = UserEntity(
                id = UUID.randomUUID().toString(),
                displayName = "ITI Student",
                role = "USER"
            )
            val admin = UserEntity(
                id = UUID.randomUUID().toString(),
                displayName = "ITI Instructor (Admin)",
                role = "ADMIN"
            )
            userDao.insertUser(student)
            userDao.insertUser(admin)
            preferences.setActiveUserId(student.id)
        }
    }
}
