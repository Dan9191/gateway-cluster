package ru.dan.gateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap


@Component
class RateLimitFilter : GatewayFilter {

    private val requests = ConcurrentHashMap<String, MutableList<Long>>()

    private val WINDOW = Duration.ofSeconds(5).toMillis()  // окно 5 секунд
    private val MAX_REQUESTS = 20                          // максимум запросов с IP

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val ip = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"

        val now = Instant.now().toEpochMilli()

        val history = requests.computeIfAbsent(ip) { mutableListOf() }

        synchronized(history) {
            history.removeIf { it < now - WINDOW }
            history.add(now)

            if (history.size > MAX_REQUESTS) {
                exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
                return exchange.response.setComplete()
            }
        }

        return chain.filter(exchange)
    }
}