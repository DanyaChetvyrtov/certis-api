package ru.digitalhustle.certis.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.digitalhustle.certis.config.properties.AppMinioProperties
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.BucketCreationException

@Configuration
class MinioConfig(
    private val appMinioProperties: AppMinioProperties,
) {

    private val log = KotlinLogging.logger {}

    @Bean
    fun minioClient(): MinioClient =
        MinioClient.builder()
            .endpoint(appMinioProperties.endpoint)
            .credentials(
                appMinioProperties.accessKey,
                appMinioProperties.secretKey,
            )
            .build()

    @Bean
    @ConditionalOnProperty(
        prefix = "digital-hustle.certis.minio",
        name = ["bucket-initializer-enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun minioBucketInitializer(
        minioClient: MinioClient,
    ) = InitializingBean {
        try {
            createBucket(
                minioClient,
                appMinioProperties.bucketName,
            )
        } catch (exception: Exception) {
            log.error(exception) {
                "Failed to initialize MinIO bucket"
            }
            throw BucketCreationException(ErrorMessages.BUCKET_CREATION_FAILED)
        }
    }

    private fun createBucket(
        minioClient: MinioClient,
        bucketName: String,
    ) {
        if (bucketExists(minioClient, bucketName)) {
            log.info { "Bucket '$bucketName' already exists" }
            return
        }

        minioClient.makeBucket(
            MakeBucketArgs.builder()
                .bucket(bucketName)
                .build(),
        )

        log.info { "Bucket '$bucketName' successfully created" }
    }

    private fun bucketExists(
        minioClient: MinioClient,
        bucketName: String,
    ): Boolean =
        minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(bucketName)
                .build(),
        )
}
