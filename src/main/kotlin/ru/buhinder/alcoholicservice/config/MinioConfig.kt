package ru.buhinder.alcoholicservice.config

import io.minio.MinioAsyncClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.buhinder.alcoholicservice.config.properties.MinioProperties

@Configuration
class MinioConfig(
    private val minioProperties: MinioProperties,
) {

    @Bean
    fun minioAsyncClient(): MinioAsyncClient {
        return MinioAsyncClient.builder()
            .credentials(minioProperties.user, minioProperties.password)
            .endpoint(minioProperties.url, minioProperties.port, false)
            .build()
    }

}