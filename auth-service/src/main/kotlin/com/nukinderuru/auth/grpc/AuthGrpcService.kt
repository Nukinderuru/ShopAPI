package com.nukinderuru.auth.grpc

import com.nukinderuru.auth.data.repository.DuplicateUserException
import com.nukinderuru.auth.domain.AuthException
import com.nukinderuru.auth.domain.AuthService
import com.nukinderuru.auth.domain.InvalidAuthInputException
import com.nukinderuru.auth.domain.InvalidCredentialsException
import com.nukinderuru.auth.domain.RegisterCommand
import io.grpc.Status

class AuthGrpcService(
    private val authService: AuthService
) : AuthServiceGrpcKt.AuthServiceCoroutineImplBase() {
    override suspend fun register(request: RegisterRequest): TokenResponse = grpcCall {
        tokenResponse { token = authService.register(request.toCommand()) }
    }

    override suspend fun authenticate(request: AuthRequest): TokenResponse = grpcCall {
        tokenResponse { token = authService.authenticate(request.email, request.password) }
    }

    override suspend fun validateToken(request: ValidateTokenRequest): ValidateTokenResponse = grpcCall {
        val principal = authService.validateToken(request.token)
        validateTokenResponse {
            valid = principal != null
            principal?.let {
                userId = it.userId.toString()
                email = it.email
            }
        }
    }

    override suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse = grpcCall {
        changePasswordResponse {
            success = authService.changePassword(request.token, request.oldPassword, request.newPassword)
        }
    }

    override suspend fun resetPassword(request: ResetPasswordRequest): ResetPasswordResponse = grpcCall {
        resetPasswordResponse { success = authService.resetPassword(request.email) }
    }

    private fun RegisterRequest.toCommand(): RegisterCommand = RegisterCommand(
        email = email,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        password = password
    )

    private suspend fun <T> grpcCall(block: suspend () -> T): T = try {
        block()
    } catch (cause: DuplicateUserException) {
        throw Status.ALREADY_EXISTS.withDescription(cause.message).asRuntimeException()
    } catch (cause: InvalidCredentialsException) {
        throw Status.UNAUTHENTICATED.withDescription(cause.message).asRuntimeException()
    } catch (cause: InvalidAuthInputException) {
        throw Status.INVALID_ARGUMENT.withDescription(cause.message).asRuntimeException()
    } catch (cause: AuthException) {
        throw Status.INVALID_ARGUMENT.withDescription(cause.message).asRuntimeException()
    }
}
