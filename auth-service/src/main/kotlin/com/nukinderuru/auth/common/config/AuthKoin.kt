package com.nukinderuru.auth.common.config

import com.nukinderuru.auth.data.repository.ExposedUserRepository
import com.nukinderuru.auth.data.repository.UserRepository
import com.nukinderuru.auth.domain.AuthService
import com.nukinderuru.auth.domain.DefaultAuthService
import com.nukinderuru.auth.domain.Hs256JwtTokenService
import com.nukinderuru.auth.domain.JwtTokenService
import com.nukinderuru.auth.domain.PasswordHasher
import com.nukinderuru.auth.domain.Pbkdf2PasswordHasher
import com.nukinderuru.auth.grpc.AuthGrpcService
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun startAuthKoin() = startKoin {
    modules(
        module {
            single { AuthDatabaseConfig() }
            single { get<AuthDatabaseConfig>().connect() }
            single<UserRepository> { ExposedUserRepository(get()) }
            single<PasswordHasher> { Pbkdf2PasswordHasher() }
            single<JwtTokenService> {
                Hs256JwtTokenService(
                    secret = requiredEnv("AUTH_JWT_SECRET"),
                    ttlSeconds = env("AUTH_JWT_TTL_SECONDS", "86400").toLong()
                )
            }
            single<AuthService> { DefaultAuthService(get(), get(), get()) }
            single { AuthGrpcService(get()) }
        }
    )
}
