package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY lastLoginAt DESC")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserAccount)

    @Query("UPDATE users SET lastLoginAt = :timestamp WHERE email = :email")
    suspend fun updateLastLogin(email: String, timestamp: Long)

    @Query("UPDATE users SET name = :name WHERE email = :email")
    suspend fun updateUserName(email: String, name: String)

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUser(email: String)
}
