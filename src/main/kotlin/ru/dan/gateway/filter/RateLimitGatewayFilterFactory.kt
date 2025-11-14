package ru.dan.gateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap


@Component
class RateLimitGatewayFilterFactory :
    AbstractGatewayFilterFactory<RateLimitGatewayFilterFactory.Config>(Config::class.java) {

    class Config

    override fun apply(config: Config): GatewayFilter {
        val requests = ConcurrentHashMap<String, MutableList<Long>>()
        val WINDOW = Duration.ofSeconds(5).toMillis()
        val MAX_REQUESTS = 20

        return GatewayFilter { exchange, chain ->
            val ip = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
            val now = Instant.now().toEpochMilli()
            val history = requests.computeIfAbsent(ip) { mutableListOf() }

            synchronized(history) {
                history.removeIf { it < now - WINDOW }
                history.add(now)
                if (history.size > MAX_REQUESTS) {
                    exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
                    return@GatewayFilter exchange.response.setComplete()
                }
            }

            chain.filter(exchange)
        }
    }
}