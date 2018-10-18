package io.mbarker.micrometer.stackdriver.demo

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.Metric
import com.google.api.MonitoredResource
import com.google.api.gax.core.CredentialsProvider
import com.google.cloud.monitoring.v3.MetricServiceClient
import com.google.monitoring.v3.CreateTimeSeriesRequest
import com.google.monitoring.v3.Point
import com.google.monitoring.v3.ProjectName
import com.google.monitoring.v3.TimeInterval
import com.google.monitoring.v3.TimeSeries
import com.google.monitoring.v3.TypedValue
import com.google.protobuf.util.Timestamps
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gcp.core.GcpProjectIdProvider
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

@SpringBootApplication
class MicrometerStackdriverDemo

data class LogEntry(val message: String)

@RestController
class FooController(private val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(FooController::class.java)
    private val random = ThreadLocalRandom.current()

    @Bean
    fun startup(
        gcpProjectIdProvider: GcpProjectIdProvider,
        credentialsProvider: CredentialsProvider
    ) = CommandLineRunner {
        println("project ID: ${gcpProjectIdProvider.projectId}")
        println("authentication type: ${credentialsProvider.credentials.authenticationType}")
    }

    @GetMapping
    fun foo(): Mono<String> {
        return "foo".toMono()
            .delayElement(Duration.ofMillis(random.nextLong(100, 170)))
            .map { next ->
                if (System.currentTimeMillis() % 2L == 0L) {
                    log.info(objectMapper.writeValueAsString(LogEntry("success")))
                    next
                } else {
                    log.error(objectMapper.writeValueAsString(LogEntry("error")))
                    throw RuntimeException("Uh oh!")
                }
            }
    }

    fun bar() {
        val projectId = System.getProperty("projectId")
        val client = MetricServiceClient.create()
        val name = ProjectName.of(projectId)
//        val metricType = "custom.googleapis.com/custom-gauge"

//        val descriptor = MetricDescriptor.newBuilder()
//            .setType(metricType)
//            .setDescription("This is a simple example of a custom metric.")
//            .setMetricKind(MetricDescriptor.MetricKind.GAUGE)
//            .setValueType(MetricDescriptor.ValueType.DOUBLE)
//            .build()
//
//        val request = CreateMetricDescriptorRequest.newBuilder()
//            .setName(name.toString())
//            .setMetricDescriptor(descriptor)
//            .build()
//
//        client.createMetricDescriptor(request)

        // Prepares an individual data point
        val points = listOf(
            Point.newBuilder()
                .setInterval(
                    TimeInterval.newBuilder()
                        .setEndTime(Timestamps.fromMillis(System.currentTimeMillis()))
                        .build()
                )
                .setValue(
                    TypedValue.newBuilder()
                        .setDoubleValue(123.45)
                        .build()
                )
                .build()
        )

        // Prepares the metric descriptor
        val metric = Metric.newBuilder()
            .setType("custom.googleapis.com/my_metric")
            .putAllLabels(emptyMap())
            .build()

        // Prepares the monitored resource descriptor
        val resourceLabels = mapOf(
            "instance_id" to "1234567890123456789",
            "zone" to "us-central1-f"
        )

        val resource = MonitoredResource.newBuilder()
            .setType("global")
            .putAllLabels(resourceLabels)
            .build()

        // Prepares the time series request
        val timeSeriesList = listOf(
            TimeSeries.newBuilder()
                .setMetric(metric)
                .setResource(resource)
                .addAllPoints(points)
                .build()
        )

        val request1 = CreateTimeSeriesRequest.newBuilder()
            .setName(name.toString())
            .addAllTimeSeries(timeSeriesList)
            .build()

        // Writes time series data
        client.createTimeSeries(request1)
        println("Done writing time series value.")
    }
}

fun main(args: Array<String>) {
    runApplication<MicrometerStackdriverDemo>(*args)
}
