package com.ozyern.evolution.repositories

import com.ozyern.evolution.models.CnRegion
import com.ozyern.evolution.models.CnRomBuild
import com.ozyern.evolution.models.CnRomDevice
import com.ozyern.evolution.models.CnRomParser
import com.ozyern.evolution.utils.AppUserAgent
import com.ozyern.evolution.utils.logInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * China (ColorOS China) firmware from the community ROM archive at [CnRomParser.BaseUrl], since our own
 * update server doesn't carry China builds.
 *
 * The archive is session-based: a build is first "resolved" (form POST, which stores it in the session),
 * then its page is read, and finally a short-lived signed link to OPPO's CDN is requested. Requests are
 * therefore serialised with [mutex], and cookies are kept in memory for the app's lifetime.
 */
@Singleton
class CnRomRepository @Inject constructor() {

    private val mutex = Mutex()

    private val client = OkHttpClient.Builder()
        .cookieJar(MemoryCookieJar())
        // Redirects are part of the protocol here (POST → 302 → result, POST → 303 → CDN), handle them manually
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", AppUserAgent).build())
        }
        .build()

    @Volatile
    private var cachedDevices: List<CnRomDevice>? = null

    /** @return devices with China builds, or `null` on failure */
    suspend fun fetchDevices(force: Boolean = false): List<CnRomDevice>? {
        if (!force) cachedDevices?.let { return it }
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    CnRomParser.parseDevices(get(CnRomParser.OtaPageUrl)).ifEmpty { null }?.also { cachedDevices = it }
                } catch (e: Exception) {
                    logInfo(TAG, "Couldn't fetch device list", e)
                    null
                }
            }
        }
    }

    /**
     * @param index position of the build in [CnRomDevice.builds]
     * @return the build's details, or `null` on failure
     */
    suspend fun resolve(device: CnRomDevice, index: Int): CnRomBuild? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                // Ensure we have a session cookie before submitting the form
                if (cachedDevices == null) get(CnRomParser.OtaPageUrl)

                val body = FormBody.Builder()
                    .add("device", device.key)
                    .add("region", CnRegion)
                    .add("version_index", index.toString())
                    .build()
                client.newCall(Request.Builder().url(CnRomParser.OtaPageUrl).post(body).build()).execute().close()

                CnRomParser.parseBuild(get(CnRomParser.OtaPageUrl))
            } catch (e: Exception) {
                logInfo(TAG, "Couldn't resolve ${device.key} #$index", e)
                null
            }
        }
    }

    /** @return a signed, short-lived URL to the full OTA zip on OPPO's servers, or `null` on failure */
    suspend fun fetchDownloadUrl(build: CnRomBuild): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val body = FormBody.Builder()
                    .add("k", build.otaKey)
                    .add("csrf", build.csrf)
                    .build()
                client.newCall(Request.Builder().url(CnRomParser.PackageUrl).post(body).build()).execute().use {
                    it.header("Location")?.takeIf { url -> it.isRedirect && url.startsWith("https://") }
                }
            } catch (e: Exception) {
                logInfo(TAG, "Couldn't fetch download URL for ${build.label}", e)
                null
            }
        }
    }

    private fun get(url: String) = client.newCall(Request.Builder().url(url).build()).execute().use {
        if (!it.isSuccessful) throw IllegalStateException("HTTP ${it.code}")
        it.body.string()
    }

    private class MemoryCookieJar : CookieJar {
        private val cookies = HashMap<String, Cookie>()

        @Synchronized
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = cookies.forEach {
            this.cookies[it.name] = it
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            this.cookies.values.removeAll { it.expiresAt < now }
            return this.cookies.values.filter { it.matches(url) }
        }
    }

    companion object {
        private const val TAG = "CnRomRepository"
    }
}
