package com.example
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*


class TokenCreation(
    private val issuer: String,
    private val audience: String,
    private val secret: String
)
{
    private val algorithm = Algorithm.HMAC256(secret)

    fun generateToken(userId: Int): String{
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withExpiresAt(Date(System.currentTimeMillis() + 900_000))
            .withClaim("id",userId)
            .sign(algorithm)
    }
}