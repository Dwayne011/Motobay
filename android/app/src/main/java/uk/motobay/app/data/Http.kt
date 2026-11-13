package uk.motobay.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import uk.motobay.app.BuildConfig
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * What every request identifies itself as: "uk.motobays.app/1.0.0 (Android; +repo)".
 * Photon, postcodes.io and OpenFreeMap ask for a real User-Agent and may throttle the
 * stock OkHttp one.
 */
const val USER_AGENT = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} (Android; +https://github.com/Dwayne011/Motobay)"

/** Shared HTTP client for bay data, Photon and postcodes.io; the map's tile requests use it too (see BayMap). */
val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
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
