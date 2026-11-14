package com.v2ray.ang.ui.main

import com.google.gson.JsonParser
import com.v2ray.ang.handler.SettingsManager
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.HttpURLConnection
import java.net.URL

/**
 * Approximate public-IP location. Coordinates identify the network exit, not a device GPS fix.
 */
data class GeoIpLocation(
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
)

internal object GeoIpLookup {
    private const val ENDPOINT =
        "https://ipwho.is/?fields=success,country,country_code,latitude,longitude"

    /**
     * When the VPN is active, send this lookup through Xray's local SOCKS inbound.
     * The app package is intentionally excluded from Android's VPN, so a plain URL
     * connection would otherwise report the underlying ISP address instead of VPN egress.
     */
    fun fetch(viaVpnProxy: Boolean = false): GeoIpLocation? {
        val proxy = if (viaVpnProxy) {
            check(SettingsManager.getSocksUsername().isNullOrBlank() &&
                SettingsManager.getSocksPassword().isNullOrBlank()) {
                "GeoIP lookup through authenticated SOCKS is not supported"
            }
            Proxy(
                Proxy.Type.SOCKS,
                InetSocketAddress("127.0.0.1", SettingsManager.getSocksPort())
            )
        } else {
            Proxy.NO_PROXY
        }
        val connection = (URL(ENDPOINT).openConnection(proxy) as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4_500
            readTimeout = 4_500
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                parse(reader.readText())
            }
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(json: String): GeoIpLocation? = runCatching {
        val root = JsonParser.parseString(json).asJsonObject
        if (!root.get("success")?.asBoolean.orFalse()) return null
        val country = root.get("country")?.asString?.trim().orEmpty()
        val code = root.get("country_code")?.asString?.trim().orEmpty()
        val latitude = root.get("latitude")?.asDouble ?: return null
        val longitude = root.get("longitude")?.asDouble ?: return null
        if (country.isBlank() || !latitude.isFinite() || !longitude.isFinite()) return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        GeoIpLocation(country, code, latitude, longitude)
    }.getOrNull()

    private fun Boolean?.orFalse(): Boolean = this == true
}
