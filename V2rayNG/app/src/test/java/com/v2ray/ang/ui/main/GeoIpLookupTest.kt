package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GeoIpLookupTest {
    @Test
    fun parsesSuccessfulCountryAndCoordinates() {
        val result = GeoIpLookup.parse(
            """{"success":true,"country":"Finland","country_code":"FI","latitude":60.17,"longitude":24.94}"""
        )
        assertEquals(GeoIpLocation("Finland", "FI", 60.17, 24.94), result)
    }

    @Test
    fun rejectsFailedLookupAndOutOfRangeCoordinates() {
        assertNull(GeoIpLookup.parse("""{"success":false,"message":"invalid query"}"""))
        assertNull(
            GeoIpLookup.parse(
                """{"success":true,"country":"Nowhere","country_code":"NW","latitude":120,"longitude":20}"""
            )
        )
    }

    @Test
    fun rejectsMalformedOrIncompleteResponses() {
        assertNull(GeoIpLookup.parse("not-json"))
        assertNull(GeoIpLookup.parse("""{"success":true,"country":"Finland"}"""))
    }
}
