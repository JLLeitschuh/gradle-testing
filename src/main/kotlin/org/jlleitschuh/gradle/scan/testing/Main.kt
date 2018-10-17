package org.jlleitschuh.gradle.scan.testing

import com.beust.klaxon.JsonBase
import com.beust.klaxon.Parser
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.config
import io.ktor.client.features.HttpPlainText
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.streams.asSequence

private const val knownWorking = "5bwlw6qumc4ve"

fun createClient() = HttpClient(Apache.config {
    socketTimeout = TimeUnit.SECONDS.toMillis(60).toInt()
}) {
    install(JsonFeature)
    install(HttpPlainText) {
        defaultCharset = Charsets.UTF_8
    }
}

private const val source = "abcdefghijklmnopqrstuvwxyz1234567890"
private val random = Random()

fun randomScanId(): String {
    return random
            .ints(13, 0, source.length)
            .asSequence()
            .map(source::get)
            .joinToString("")
}

fun HttpClient.generateRequest() = run {
    val id = randomScanId()
    id to async {
        get<HttpResponse>("https://scans.gradle.com/scan-data/$id")
    }
}

fun main(args: Array<String>) {
    val client = createClient()

    runBlocking {

        val deferredResponses = (1..50_000).map {
            client.generateRequest()
        }

        deferredResponses
                .mapIndexed { index, (id, deferredResponse) ->
                    println("Index: $index ID: $id")
                    val response = deferredResponse.await()
                    if (response.status != HttpStatusCode.NotFound) {
                        val text = StringBuilder(response.readText())
                        val status = response.status
                        val body = Parser().parse(text) as JsonBase
                        println("Status $status")
                        val jsonText = body.toJsonString(prettyPrint = true)
                        println(jsonText)
                        jsonText
                    } else {
                        null
                    }
                }
                .filterNotNull()
                .forEach {
                    println("Success! $it")
                }
    }
}