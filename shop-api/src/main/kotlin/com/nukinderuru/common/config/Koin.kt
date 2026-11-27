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
import com.nukinderuru.domain.event.ProductUpdateBroadcaster
import com.nukinderuru.domain.service.ClientService
import com.nukinderuru.domain.service.ImageService
import com.nukinderuru.domain.service.ProductEventService
import com.nukinderuru.domain.service.ProductService
import com.nukinderuru.domain.service.SupplierService
import com.nukinderuru.messaging.rabbitmq.ProductEventConsumerSupervisor
import com.nukinderuru.messaging.rabbitmq.RabbitMqConfig
import com.nukinderuru.messaging.rabbitmq.RabbitMqTopology
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.jetbrains.exposed.sql.Database
import org.koin.ktor.plugin.Koin
import org.koin.dsl.module
import org.koin.dsl.onClose

fun Application.configureDependencyInjection() {
    install(Koin) {
        modules(
            module {
                single { DatabaseConfig(environment.config) }
                single<HikariDataSource> {
                    get<DatabaseConfig>().createDataSource()
                } onClose { dataSource ->
                    dataSource?.close()
                }
                single<Database>(createdAtStart = true) {
                    get<DatabaseConfig>().connect(get<HikariDataSource>())
                }
                single<ManagedChannel> {
                    ManagedChannelBuilder.forAddress(
                        environment.config.property("auth.host").getString(),
                        environment.config.property("auth.port").getString().toInt()
                    ).usePlaintext().build()
                } onClose { channel ->
                    channel?.shutdown()
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
                single { ProductUpdateBroadcaster() }
                single { ProductEventService(get(), get()) }
                single { SupplierService(get()) }
                single { RabbitMqConfig.from(environment.config) }
                single { RabbitMqTopology(get()) }
                single { ProductEventConsumerSupervisor(get(), get(), get()) }
            }
        )
    }
}
