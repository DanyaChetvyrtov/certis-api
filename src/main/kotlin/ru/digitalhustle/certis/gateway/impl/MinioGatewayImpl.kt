package ru.digitalhustle.certis.gateway.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.config.properties.AppMinioProperties
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PhotoConstants
import ru.digitalhustle.certis.exception.custom.PhotoProcessingException
import ru.digitalhustle.certis.gateway.MinioGateway

@Component
class MinioGatewayImpl(
    private val minioClient: MinioClient,
    private val appMinioProperties: AppMinioProperties,
) : MinioGateway {

    private val log = KotlinLogging.logger {}

    override fun getPhoto(objectName: String): ByteArray {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(appMinioProperties.bucketName)
                    .`object`(objectName)
                    .build(),
            ).use { response ->
                response.readAllBytes()
            }
        } catch (exception: Exception) {
            log.error(exception) {
                "Failed to download photo from MinIO"
            }
            throw PhotoProcessingException(ErrorMessages.PHOTO_DOWNLOAD_FAILED, exception)
        }
    }

    override fun savePhoto(
        objectName: String,
        photo: MultipartFile,
        contentType: String,
    ) {
        try {
            photo.inputStream.use { dataStream ->
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(appMinioProperties.bucketName)
                        .`object`(objectName)
                        .stream(
                            dataStream,
                            photo.size,
                            PhotoConstants.USE_DEFAULT_PART_SIZE,
                        )
                        .contentType(contentType)
                        .build(),
                )
            }
        } catch (exception: Exception) {
            log.error(exception) {
                "Failed to upload photo to MinIO"
            }
            throw PhotoProcessingException(ErrorMessages.PHOTO_UPLOAD_FAILED, exception)
        }
    }

    override fun deletePhoto(objectName: String) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(appMinioProperties.bucketName)
                    .`object`(objectName)
                    .build(),
            )
        } catch (exception: Exception) {
            log.error(exception) {
                "Failed to delete photo from MinIO"
            }
            throw PhotoProcessingException(ErrorMessages.PHOTO_DELETE_FAILED, exception)
        }
    }
}
