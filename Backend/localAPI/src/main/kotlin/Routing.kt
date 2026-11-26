package com.example

import com.auth0.jwt.JWT
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val username: String)
data class DataRequest(val id: Int)
lateinit var creareToken: TokenCreation

fun Application.configureRouting() {

    val ktorConfig = environment.config.config("ktor")
    val conf= ktorConfig.config("security.jwt")

    val secret= conf.property("secret").getString()
    val issuer = conf.property("issuer").getString()
    val audience = conf.property("audience").getString()
    val myrealm = conf.property("myrealm").getString()

    install(Authentication) {
        jwt("auth") {
            realm = myrealm
            verifier(JWT
                .require(Algorithm.HMAC256(secret))
                .withIssuer(issuer)
                .withAudience(audience)
                .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("id").asInt()!=null) {
                    JWTPrincipal(credential.payload)
                }
                else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respondText("Invalid token",status=HttpStatusCode.Unauthorized)
            }
        }
    }

    creareToken = TokenCreation(
        issuer = conf.property("issuer").getString(),
        audience = conf.property("audience").getString(),
        secret = conf.property("secret").getString()
    )


    routing {

        authenticate("auth") {

//            post("/profile_data") {
//                val pricial= call.principal<JWTPrincipal>()
//                val userId= pricial!!.payload.getClaim("id").asInt()
//
//                if(userId!=null) {
//
//                    val prifileData = profileDataExtraction(userId)
//
//                    if (prifileData != null) {
//                        call.respond(prifileData)
//                    }
//                    else{
//                        call.respond(HttpStatusCode.Unauthorized)
//                    }
//
//                }
//
//            }

        }
        get("/") {
            val connected = Database.testConnection()
            if (connected) {
                call.respondText("Conexiunea la baza de date MySQL este OK!")
            } else {
                call.respondText("Conexiunea la baza de date MySQL a eșuat!")
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            environment.log.info("Email: '${request.email}'")
            environment.log.info("Password: '${request.password}'")

            val userId = getUserId(request.email, request.password)

            if (userId != null) {
                val token= creareToken.generateToken(userId)

                call.respond(mapOf("token" to token))

            }else{
                call.respondText("Invalid credentials", status = HttpStatusCode.Unauthorized)
            }
        }


        post("/register") {
            val request = call.receive<RegisterRequest>()
            var rol ="user"


            val alreadyExists = emailExists(request.email)
            val isValid = addUser(request.email, request.password,request.username, rol)


            if (isValid and !alreadyExists) {
                call.respondText("Register succesful!", status = HttpStatusCode.OK)
            }
            else if (isValid and alreadyExists) {
                call.respondText("User already exists!", status = HttpStatusCode.Conflict)
            }
            else if (!isValid){
                call.respondText("Register failed!", status = HttpStatusCode.BadRequest)
            }
        }

    }
}



