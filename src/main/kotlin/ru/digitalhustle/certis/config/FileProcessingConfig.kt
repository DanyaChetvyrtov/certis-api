package ru.digitalhustle.certis.config

import org.apache.tika.Tika
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FileProcessingConfig {

    @Bean
    fun tika(): Tika = Tika()
}
