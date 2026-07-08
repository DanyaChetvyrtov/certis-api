package ru.digitalhustle.certis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CertisApplication

fun main(args: Array<String>) {
    runApplication<CertisApplication>(*args)
}
