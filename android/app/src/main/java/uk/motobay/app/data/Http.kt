package uk.motobay.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import uk.motobay.app.BuildConfig
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/** Shared HTTP client. Open-data services ask apps to identify themselves. */
val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", "MotoBays/${BuildConfig.VERSION_NAME} (Android)")
                .build(),
        )
    }
    .build()

class HttpException(val code: Int) : IOException("HTTP $code")

/** Runs a request and returns the body, or throws [HttpException] / [IOException]. */
fun OkHttpClient.fetch(request: Request): String =
    newCall(request).execute().use { res ->
        if (!res.isSuccessful) throw HttpException(res.code)
        res.body?.string() ?: throw IOException("Empty response")
    }

/** A short reason a rider can act on, for network errors. */
fun Throwable.friendlyMessage(): String = when (this) {
    is UnknownHostException -> "No internet connection"
    is SocketTimeoutException -> "The server took too long to respond"
    is HttpException -> when (code) {
        404 -> "The data isn't available yet"
        429 -> "The server is busy. Try again in a minute"
        in 500..599 -> "The server is having problems ($code)"
        else -> "Server error ($code)"
    }
    else -> message ?: "Something went wrong"
}
