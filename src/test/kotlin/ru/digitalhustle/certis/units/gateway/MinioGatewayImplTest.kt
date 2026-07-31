package ru.digitalhustle.certis.units.gateway

import io.minio.GetObjectArgs
import io.minio.GetObjectResponse
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.config.properties.AppMinioProperties
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.PhotoProcessingException
import ru.digitalhustle.certis.gateway.impl.MinioGatewayImpl
import java.io.ByteArrayInputStream

class MinioGatewayImplTest {

    private val minioClient = mock(MinioClient::class.java)
    private val appMinioProperties = AppMinioProperties(
        endpoint = ENDPOINT,
        accessKey = "access-key",
        secretKey = "secret-key",
        bucketName = BUCKET_NAME,
    )

    private val minioGateway = MinioGatewayImpl(
        minioClient = minioClient,
        appMinioProperties = appMinioProperties,
    )

    private companion object {
        private const val ENDPOINT = "http://localhost:9000"
        private const val BUCKET_NAME = "test-bucket"
        private const val OBJECT_NAME = "photo.jpg"
        private const val CONTENT_TYPE = "image/jpeg"
        private val PHOTO_BYTES = "photo".toByteArray()
    }

    @Test
    fun `should save photo`() {
        // given
        val photo = createPhoto()

        // when
        minioGateway.savePhoto(OBJECT_NAME, photo, CONTENT_TYPE)

        // then
        val putObjectArgsCaptor = ArgumentCaptor.forClass(PutObjectArgs::class.java)
        verify(minioClient)
            .putObject(putObjectArgsCaptor.capture())

        assertThat(putObjectArgsCaptor.value.bucket()).isEqualTo(BUCKET_NAME)
        assertThat(putObjectArgsCaptor.value.`object`()).isEqualTo(OBJECT_NAME)
        assertThat(putObjectArgsCaptor.value.contentType()).isEqualTo(CONTENT_TYPE)
    }

    @Test
    fun `should throw photo processing exception when saving photo fails`() {
        // given
        val photo = createPhoto()

        doThrow(RuntimeException("MinIO is unavailable"))
            .`when`(minioClient)
            .putObject(any(PutObjectArgs::class.java))

        // when, then
        assertThatThrownBy {
            minioGateway.savePhoto(OBJECT_NAME, photo, CONTENT_TYPE)
        }.isInstanceOf(PhotoProcessingException::class.java)
            .hasMessage(ErrorMessages.PHOTO_UPLOAD_FAILED)
            .hasCauseInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `should get photo`() {
        // given
        val response = mock(GetObjectResponse::class.java)

        `when`(minioClient.getObject(any(GetObjectArgs::class.java)))
            .thenReturn(response)

        `when`(response.readAllBytes())
            .thenReturn(PHOTO_BYTES)

        // when
        val photo = minioGateway.getPhoto(OBJECT_NAME)

        // then
        assertThat(photo).isEqualTo(PHOTO_BYTES)

        val getObjectArgsCaptor = ArgumentCaptor.forClass(GetObjectArgs::class.java)
        verify(minioClient)
            .getObject(getObjectArgsCaptor.capture())

        assertThat(getObjectArgsCaptor.value.bucket()).isEqualTo(BUCKET_NAME)
        assertThat(getObjectArgsCaptor.value.`object`()).isEqualTo(OBJECT_NAME)
    }

    @Test
    fun `should throw photo processing exception when getting photo fails`() {
        // given
        doThrow(RuntimeException("MinIO is unavailable"))
            .`when`(minioClient)
            .getObject(any(GetObjectArgs::class.java))

        // when, then
        assertThatThrownBy {
            minioGateway.getPhoto(OBJECT_NAME)
        }.isInstanceOf(PhotoProcessingException::class.java)
            .hasMessage(ErrorMessages.PHOTO_DOWNLOAD_FAILED)
            .hasCauseInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `should delete photo`() {
        // when
        minioGateway.deletePhoto(OBJECT_NAME)

        // then
        val removeObjectArgsCaptor = ArgumentCaptor.forClass(RemoveObjectArgs::class.java)
        verify(minioClient)
            .removeObject(removeObjectArgsCaptor.capture())

        assertThat(removeObjectArgsCaptor.value.bucket()).isEqualTo(BUCKET_NAME)
        assertThat(removeObjectArgsCaptor.value.`object`()).isEqualTo(OBJECT_NAME)
    }

    @Test
    fun `should throw photo processing exception when deleting photo fails`() {
        // given
        doThrow(RuntimeException("MinIO is unavailable"))
            .`when`(minioClient)
            .removeObject(any(RemoveObjectArgs::class.java))

        // when, then
        assertThatThrownBy {
            minioGateway.deletePhoto(OBJECT_NAME)
        }.isInstanceOf(PhotoProcessingException::class.java)
            .hasMessage(ErrorMessages.PHOTO_DELETE_FAILED)
            .hasCauseInstanceOf(RuntimeException::class.java)
    }

    private fun createPhoto(): MultipartFile =
        mock(MultipartFile::class.java).also { photo ->
            `when`(photo.inputStream)
                .thenReturn(ByteArrayInputStream("photo".toByteArray()))

            `when`(photo.size)
                .thenReturn("photo".toByteArray().size.toLong())
        }
}
