package com.nukinderuru.auth

import com.nukinderuru.auth.common.config.env
import com.nukinderuru.auth.common.config.startAuthKoin
import com.nukinderuru.auth.grpc.AuthGrpcService
import io.grpc.ServerBuilder
import org.koin.java.KoinJavaComponent.get
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("AuthApplication")

fun main() {
    startAuthKoin()
    val port = env("AUTH_GRPC_PORT", "9090").toInt()
    val server = ServerBuilder.forPort(port)
        .addService(get<AuthGrpcService>(AuthGrpcService::class.java))
        .build()
        .start()

    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info("Shutting down Auth gRPC service")
            server.shutdown()

            if (!server.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("gRPC server did not terminate gracefully, forcing shutdown")
                server.shutdownNow()
            }
        })

    logger.info("Auth gRPC service started on port $port")
    server.awaitTermination()
}
