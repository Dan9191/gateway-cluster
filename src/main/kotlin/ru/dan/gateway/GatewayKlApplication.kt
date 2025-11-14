package ru.dan.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GatewayKlApplication

fun main(args: Array<String>) {
	runApplication<GatewayKlApplication>(*args)
}
