package com.nukinderuru.common.config

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
import org.koin.ktor.plugin.Koin
import org.koin.dsl.module

fun Application.configureDependencyInjection() {
    install(Koin) {
        modules(
            module {
                single { DatabaseConfig(environment.config) }
                single { get<DatabaseConfig>().connect() }
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
