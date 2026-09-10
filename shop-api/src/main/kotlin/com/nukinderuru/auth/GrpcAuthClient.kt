package com.nukinderuru.auth

import com.nukinderuru.auth.grpc.AuthServiceGrpcKt
import com.nukinderuru.auth.grpc.authRequest
import com.nukinderuru.auth.grpc.registerRequest
import com.nukinderuru.auth.grpc.resetPasswordRequest
import com.nukinderuru.auth.grpc.validateTokenRequest

class GrpcAuthClient(
    private val stub: AuthServiceGrpcKt.AuthServiceCoroutineStub
) : AuthClient {
    override suspend fun register(request: RegisterHttpRequest): String = stub.register(
        registerRequest {
            email = request.email
            firstName = request.firstName
            lastName = request.lastName
            phone = request.phone
            password = request.password
        }
    ).token

    override suspend fun authenticate(request: AuthHttpRequest): String = stub.authenticate(
        authRequest {
            email = request.email
            password = request.password
        }
    ).token

    override suspend fun resetPassword(request: ResetPasswordHttpRequest): Boolean = stub.resetPassword(
        resetPasswordRequest { email = request.email }
    ).success

    override suspend fun validateToken(token: String): Boolean = stub.validateToken(
        validateTokenRequest { this.token = token }
    ).valid
}
