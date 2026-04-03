package com.example.security

import org.mindrot.jbcrypt.BCrypt

interface PasswordHasher {
    fun hash(value: String): String
    fun verify(value: String, hashedValue: String): Boolean
}

class BCryptPasswordHasher : PasswordHasher {
    override fun hash(value: String): String = BCrypt.hashpw(value, BCrypt.gensalt())
    override fun verify(value: String, hashedValue: String): Boolean = BCrypt.checkpw(value, hashedValue)
}
