package ru.digitalhustle.moneykeeper

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MoneyKeeperApplication

fun main(args: Array<String>) {
    runApplication<MoneyKeeperApplication>(*args)
}
