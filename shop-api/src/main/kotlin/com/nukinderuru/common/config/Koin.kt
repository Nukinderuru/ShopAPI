package com.nukinderuru.common.config

import com.nukinderuru.auth.AuthClient
import com.nukinderuru.auth.GrpcAuthClient
import com.nukinderuru.auth.grpc.AuthServiceGrpcKt
import com.nukinderuru.data.db.DatabaseConfig
import com.nukinderuru.data.repository.ClientRepository
import com.nukinderuru.data.repository.ExposedClientRepository
import com.nukinderuru.data.repository.ExposedImageRepository
import com.nukinderuru.data.repository.ExposedProductRepository
import com.nukinderuru.data.repository.ExposedSupplierRepository
import com.nukinderuru.data.repository.ProductRepository
import com.nukinderuru.data.repository.SupplierRepository
import com.nukinderuru.data.repository.ImageRepository
import com.nukinderuru.domain.service.ClientService
import com.nukinderuru.domain.service.ImageService
import com.nukinderuru.domain.service.ProductService
import com.nukinderuru.domain.service.SupplierService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.koin.ktor.plugin.Koin
import org.koin.dsl.module

fun Application.configureDependencyInjection() {
    install(Koin) {
        modules(
            module {
                single { DatabaseConfig(environment.config) }
                single { get<DatabaseConfig>().connect() }
                single<ManagedChannel> {
                    ManagedChannelBuilder.forAddress(
                        environment.config.property("auth.host").getString(),
                        environment.config.property("auth.port").getString().toInt()
                    ).usePlaintext().build()
                }
                single { AuthServiceGrpcKt.AuthServiceCoroutineStub(get<ManagedChannel>()) }
                single<AuthClient> { GrpcAuthClient(get()) }
                single<ClientRepository> { ExposedClientRepository(get()) }
                single<ProductRepository> { ExposedProductRepository(get()) }
                single<SupplierRepository> { ExposedSupplierRepository(get()) }
                single<ImageRepository> { ExposedImageRepository(get()) }
                single { ClientService(get()) }
                single { ImageService(get()) }
                single { ProductService(get()) }
                single { SupplierService(get()) }
            }
        )
    }
}
