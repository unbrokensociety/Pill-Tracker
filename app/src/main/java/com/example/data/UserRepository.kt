package com.example.data

import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    val allUsers: Flow<List<UserAccount>> = userDao.getAllUsers()

    suspend fun getUserByEmail(email: String): UserAccount? {
        return userDao.getUserByEmail(email)
    }

    suspend fun saveUser(user: UserAccount) {
        userDao.insertOrUpdateUser(user)
    }

    suspend fun updateLastLogin(email: String) {
        userDao.updateLastLogin(email, System.currentTimeMillis())
    }

    suspend fun deleteUser(email: String) {
        userDao.deleteUser(email)
    }
}
