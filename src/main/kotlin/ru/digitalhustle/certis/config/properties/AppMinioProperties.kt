package ru.digitalhustle.certis.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "digital-hustle.certis.minio")
data class AppMinioProperties(

    val endpoint: String,

    val accessKey: String,

    val secretKey: String,

    val bucketName: String,
)
