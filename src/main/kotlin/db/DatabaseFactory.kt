package com.example.db

import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

object DatabaseFactory{
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private val dotenv = dotenv()

    fun init() {
        val dbUrl = dotenv["DB_URL"]
        val dbUser = dotenv["DB_USER"]
        val dbPassword = dotenv["DB_PASSWORD"]
        val dbDriver= dotenv["DB_DRIVER"]

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
