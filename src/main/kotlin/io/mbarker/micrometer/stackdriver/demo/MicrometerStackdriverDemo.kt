package io.mbarker.micrometer.stackdriver.demo

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono

@SpringBootApplication
class MicrometerStackdriverDemo

@RestController
class FooController {

    private val log = LoggerFactory.getLogger(FooController::class.java)

    @GetMapping
    fun foo(): Mono<String> {
        return "foo".toMono()
            .map { next ->
                if (System.currentTimeMillis() % 2 == 0L) {
                    log.info("success")
                    next
                } else {
                    log.error("error")
                    throw RuntimeException("Uh oh!")
                }
            }
    }
}

fun main(args: Array<String>) {
    runApplication<MicrometerStackdriverDemo>(*args)
}
