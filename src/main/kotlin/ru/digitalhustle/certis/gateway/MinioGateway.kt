package ru.digitalhustle.certis.gateway

import org.springframework.web.multipart.MultipartFile

interface MinioGateway {

    fun getPhoto(objectName: String): ByteArray

    fun savePhoto(objectName: String, photo: MultipartFile, contentType: String)

    fun deletePhoto(objectName: String)
}
