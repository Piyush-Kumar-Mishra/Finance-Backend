package com.example.utils

import io.ktor.http.HttpStatusCode


open class HttpException(
    val statusCode: HttpStatusCode,
    override val message: String
) : RuntimeException(message)