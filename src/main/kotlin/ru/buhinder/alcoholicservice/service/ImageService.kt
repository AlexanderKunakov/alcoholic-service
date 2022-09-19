package ru.buhinder.alcoholicservice.service

import io.minio.GetObjectArgs
import io.minio.MinioAsyncClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import java.io.SequenceInputStream
import java.util.UUID
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.MediaType.IMAGE_JPEG_VALUE
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import ru.buhinder.alcoholicservice.config.properties.MinioProperties
import ru.buhinder.alcoholicservice.util.toUUID


@Service
class ImageService(
    private val minioAsyncClient: MinioAsyncClient,
    private val minioProperties: MinioProperties,
) {

    fun saveImage(image: FilePart): Mono<UUID> {
        return image.toMono()
            .flatMapMany { it.content() }
            .map { it.asInputStream(true) }
            .reduce(::SequenceInputStream)
            .map {
                PutObjectArgs.builder()
                    .`object`("${UUID.randomUUID()}")
                    .bucket(minioProperties.bucket)
                    .contentType(IMAGE_JPEG_VALUE)
                    .userMetadata(mapOf(minioProperties.file.name to image.filename()))
                    .stream(it, -1, minioProperties.file.part)
                    .build()
            }
            .flatMap { Mono.fromFuture(minioAsyncClient.putObject(it)) }
            .map { it.`object`().toUUID() }
    }

    fun updateImage(imageId: UUID, image: FilePart): Mono<UUID> {
        return image.toMono()
            .flatMapMany { it.content() }
            .map { it.asInputStream(true) }
            .reduce(::SequenceInputStream)
            .map {
                PutObjectArgs.builder()
                    .`object`("$imageId")
                    .bucket(minioProperties.bucket)
                    .contentType(IMAGE_JPEG_VALUE)
                    .userMetadata(mapOf(minioProperties.file.name to image.filename()))
                    .stream(it, -1, minioProperties.file.part)
                    .build()
            }
            .flatMap { Mono.fromFuture(minioAsyncClient.putObject(it)) }
            .map { it.`object`().toUUID() }
    }

    fun getImage(imageId: UUID): Flux<DataBuffer> {
        return Mono.fromFuture(
            minioAsyncClient.getObject(
                GetObjectArgs.builder()
                    .bucket(minioProperties.bucket)
                    .`object`("$imageId")
                    .build()
            )
        )
            .flatMapMany {
                DataBufferUtils.readInputStream(
                    { it },
                    DefaultDataBufferFactory(),
                    minioProperties.file.part.toInt()
                )
            }
    }

    fun deleteImage(imageId: UUID): Mono<Void> {
        return Mono.fromFuture(
            minioAsyncClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(minioProperties.bucket)
                    .`object`("$imageId")
                    .build()
            )
        ).then()
    }
}
