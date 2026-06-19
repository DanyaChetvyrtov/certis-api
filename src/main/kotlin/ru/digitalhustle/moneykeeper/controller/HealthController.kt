package ru.digitalhustle.moneykeeper.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HealthController {

    @GetMapping("/health")
    fun health(): HealthResponse = HealthResponse(
        status = "UP",
        service = "money-keeper",
    )
}

data class HealthResponse(
    val status: String,
    val service: String,
)
