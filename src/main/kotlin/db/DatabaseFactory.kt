package com.example.db

import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

object DatabaseFactory{
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private val dotenv = dotenv()

    private fun requireEnv(key: String): String =
        dotenv[key] ?: throw IllegalStateException("$key is missing from .env")

    fun init() {
        val dbUrl = requireEnv("DB_URL")
        val dbUser = requireEnv("DB_USER")
        val dbPassword = requireEnv("DB_PASSWORD")
        val dbDriver = requireEnv("DB_DRIVER")

        try {
            Database.connect(
                url = dbUrl,
                driver = dbDriver,
                user = dbUser,
                password = dbPassword
            )
            logger.info("Db connected successfully")
        }
        catch (e: Exception) {
            logger.error("Db connection failed: ${e.message}", e)
            throw e
        }
    }
}
