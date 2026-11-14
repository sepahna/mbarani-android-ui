package com.v2ray.ang.ui.compose

import app.rive.Result
import app.rive.Rive
import app.rive.RiveFileSource
import app.rive.rememberRiveFile
import app.rive.rememberRiveWorkerOrNull
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.R
import com.v2ray.ang.ui.main.GeoIpLocation

private val MapInk = Color(0xFF132A38)
private val MapLand = Color(0xFF294858)
private val MapCoast = Color(0xFF52798A)
private val MapMint = Color(0xFF56E0C0)
private val MapBlue = Color(0xFF69BFFF)

/**
 * Flat world-map connection dashboard. GeoIP coordinates are approximate public-IP locations,
 * never GPS positions. Connection lifecycle remains owned by MainViewModel/MainActivity.
 */
@Composable
internal fun BaranHomeStage(
    statusText: String,
    running: Boolean,
    serverName: String?,
    networkLocation: GeoIpLocation?,
    vpnLocation: GeoIpLocation?,
    isGeoIpLoading: Boolean,
    onToggle: () -> Unit,
    onChooseServer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "baran-map-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "baran-map-route-pulse",
    )
    val actionLabel = stringResource(if (running) R.string.acc_stop else R.string.acc_start)
    val stateLabel = stringResource(if (running) R.string.connection_connected else R.string.connection_not_connected)
    val unknown = stringResource(R.string.value_unknown)
    val trafficSamples = remember { mutableStateListOf<Pair<Float, Float>>() }
    LaunchedEffect(running) {
        trafficSamples.clear()
        if (running) {
            while (isActive) {
                val rates = withContext(Dispatchers.IO) {
                    runCatching {
                        CoreServiceManager.queryAllOutboundTrafficStats().fold(0L to 0L) { acc, stat ->
                            when (stat.direction.lowercase()) {
                                "uplink", "upload", "outbound" -> (acc.first + stat.value) to acc.second
                                "downlink", "download", "inbound" -> acc.first to (acc.second + stat.value)
                                else -> acc
                            }
                        }
                    }.getOrDefault(0L to 0L)
                }
                trafficSamples.add(rates.first.toFloat() to rates.second.toFloat())
                if (trafficSamples.size > 36) trafficSamples.removeAt(0)
                delay(1000)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BaranRiveAccent(Modifier.size(38.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (running) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (running) colors.primary else colors.outlineVariant),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MapInk),
            contentAlignment = Alignment.Center,
        ) {
            FlatWorldMap(
                modifier = Modifier.fillMaxWidth().height(178.dp),
                origin = networkLocation,
                destination = vpnLocation,
                running = running,
                pulse = pulse,
            )
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(if (running) MapMint else Color(0xFFDAE8ED))
                    .clickable(onClick = onToggle)
                    .semantics {
                        role = Role.Button
                        contentDescription = actionLabel
                        stateDescription = stateLabel
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (running) {
                    Text(
                        text = "ON",
                        color = Color(0xFF12313A),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_power_24dp),
                        contentDescription = null,
                        tint = Color(0xFF18313D),
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Text(
                text = if (running) stringResource(R.string.connection_connected)
                else stringResource(R.string.connection_not_connected),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                color = Color(0xFFE4F4F7),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        TrafficMiniGraph(
            samples = trafficSamples,
            active = running,
            modifier = Modifier.fillMaxWidth().height(54.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            GeoLocationCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.geoip_network),
                value = if (isGeoIpLoading && networkLocation == null) "…" else networkLocation?.country ?: unknown,
                accent = MapBlue,
            )
            GeoLocationCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.geoip_vpn_exit),
                value = if (isGeoIpLoading && vpnLocation == null) "…" else vpnLocation?.country ?: unknown,
                accent = MapMint,
            )
        }
        Text(
            text = stringResource(R.string.geoip_privacy_note),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 3.dp),
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onChooseServer),
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceVariant.copy(alpha = 0.72f),
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_speed_24dp),
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 11.dp)) {
                    Text(
                        text = serverName?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.title_file_chooser),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.title_server),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_add_24dp),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}


@Composable
private fun TrafficMiniGraph(
    samples: List<Pair<Float, Float>>,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("↑ UPLOAD", style = MaterialTheme.typography.labelSmall, color = MapBlue)
                Text("↓ DOWNLOAD", style = MaterialTheme.typography.labelSmall, color = MapMint)
            }
            Canvas(Modifier.fillMaxWidth().weight(1f)) {
                val baseY = size.height * 0.52f
                drawLine(
                    color = MapCoast.copy(alpha = 0.55f),
                    start = Offset(0f, baseY),
                    end = Offset(size.width, baseY),
                    strokeWidth = 1.dp.toPx(),
                )
                if (!active || samples.isEmpty()) return@Canvas
                val maxValue = samples.maxOf { maxOf(it.first, it.second) }.coerceAtLeast(1f)
                fun drawSeries(selector: (Pair<Float, Float>) -> Float, color: Color) {
                    val path = Path()
                    samples.forEachIndexed { index, sample ->
                        val x = if (samples.size <= 1) size.width else size.width * index / (samples.size - 1)
                        val y = baseY - (selector(sample) / maxValue) * (size.height * 0.42f)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
                }
                drawSeries({ it.first }, MapBlue)
                drawSeries({ it.second }, MapMint)
            }
        }
    }
}

@Composable
private fun GeoLocationCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class WorldCountry(
    val name: String,
    val iso: String,
    val polygons: List<List<List<Pair<Double, Double>>>>
)

private fun parseWorldCountries(json: String): List<WorldCountry> {
    val features = JSONObject(json).getJSONArray("features")
    return buildList {
        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val properties = feature.getJSONObject("properties")
            val geometry = feature.getJSONObject("geometry")
            val raw = geometry.getJSONArray("coordinates")
            val polygonArrays = if (geometry.getString("type") == "Polygon") listOf(raw) else
                (0 until raw.length()).map { raw.getJSONArray(it) }
            val polygons = polygonArrays.map { polygon ->
                (0 until polygon.length()).map { ringIndex ->
                    val ring = polygon.getJSONArray(ringIndex)
                    (0 until ring.length()).mapNotNull { pointIndex ->
                        val point = ring.getJSONArray(pointIndex)
                        if (point.length() >= 2) point.getDouble(0) to point.getDouble(1) else null
                    }
                }
            }
            add(WorldCountry(
                name = properties.optString("name"),
                iso = properties.optString("iso"),
                polygons = polygons
            ))
        }
    }
}

private fun ringContains(ring: List<Pair<Double, Double>>, longitude: Double, latitude: Double): Boolean {
    var inside = false
    var j = ring.lastIndex
    for (i in ring.indices) {
        val (xi, yi) = ring[i]
        val (xj, yj) = ring[j]
        if ((yi > latitude) != (yj > latitude) &&
            longitude < (xj - xi) * (latitude - yi) / ((yj - yi).takeIf { it != 0.0 } ?: 1e-12) + xi
        ) inside = !inside
        j = i
    }
    return inside
}

private fun WorldCountry.contains(location: GeoIpLocation?): Boolean {
    if (location == null) return false
    return polygons.any { polygon ->
        polygon.firstOrNull()?.let { ringContains(it, location.longitude, location.latitude) } == true
    }
}

@Composable
private fun FlatWorldMap(
    origin: GeoIpLocation?,
    destination: GeoIpLocation?,
    running: Boolean,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var countries by remember { mutableStateOf<List<WorldCountry>>(emptyList()) }
    LaunchedEffect(context) {
        countries = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("world_countries.geojson").bufferedReader().use { reader ->
                    parseWorldCountries(reader.readText())
                }
            }.getOrDefault(emptyList())
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val coast = MapCoast.copy(alpha = 0.62f)
        for (index in 1..3) {
            val y = h * index / 4f
            drawLine(coast.copy(alpha = 0.20f), Offset(0f, y), Offset(w, y), strokeWidth = 0.6.dp.toPx())
        }
        for (index in 1..5) {
            val x = w * index / 6f
            drawLine(coast.copy(alpha = 0.20f), Offset(x, 0f), Offset(x, h), strokeWidth = 0.6.dp.toPx())
        }

        fun project(longitude: Double, latitude: Double): Offset = Offset(
            x = ((longitude + 180.0) / 360.0 * w).toFloat(),
            y = ((90.0 - latitude) / 180.0 * h).toFloat(),
        )
        countries.forEach { country ->
            val isOrigin = country.contains(origin)
            val isDestination = running && country.contains(destination)
            val fill = when {
                isOrigin && isDestination -> MapMint.copy(alpha = 0.52f)
                isOrigin -> MapBlue.copy(alpha = 0.48f)
                isDestination -> MapMint.copy(alpha = 0.48f)
                else -> MapLand.copy(alpha = 0.94f)
            }
            country.polygons.forEach { polygon ->
                val path = Path()
                polygon.forEach { ring ->
                    if (ring.isNotEmpty()) {
                        val first = project(ring.first().first, ring.first().second)
                        path.moveTo(first.x, first.y)
                        ring.drop(1).forEach { coordinate ->
                            val point = project(coordinate.first, coordinate.second)
                            path.lineTo(point.x, point.y)
                        }
                        path.close()
                    }
                }
                drawPath(path, fill)
                drawPath(path, coast, style = Stroke(width = 0.55.dp.toPx()))
            }
        }

        val from = origin?.let { project(it.longitude, it.latitude) }
        val to = if (running) destination?.let { project(it.longitude, it.latitude) } else null
        if (from != null && to != null) {
            val route = Path().apply {
                moveTo(from.x, from.y)
                cubicTo(
                    (from.x + to.x) / 2f, minOf(from.y, to.y) - h * 0.18f,
                    (from.x + to.x) / 2f, minOf(from.y, to.y) - h * 0.18f,
                    to.x, to.y,
                )
            }
            drawPath(route, MapMint.copy(alpha = 0.22f + pulse * 0.28f),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
            drawPath(route, MapMint, style = Stroke(
                width = 1.6.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))
            ))
        }
        from?.let { point ->
            drawCircle(MapBlue.copy(alpha = 0.20f), radius = 9.dp.toPx(), center = point)
            drawCircle(MapBlue, radius = 3.5.dp.toPx(), center = point)
        }
        to?.let { point ->
            drawCircle(MapMint.copy(alpha = 0.22f), radius = 10.dp.toPx(), center = point)
            drawCircle(MapMint, radius = 4.dp.toPx(), center = point)
        }
    }
}

@Composable
private fun BaranLottieLoader(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.connection_loader)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true,
        speed = 1f,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
    )
}

@Composable
private fun BaranRiveAccent(modifier: Modifier = Modifier) {
    val errorState = remember { mutableStateOf<Throwable?>(null) }
    val worker = rememberRiveWorkerOrNull(errorState)
    if (worker != null) {
        when (val riveFile = rememberRiveFile(RiveFileSource.RawRes.from(R.raw.baran_orbit), worker)) {
            is Result.Success -> Rive(file = riveFile.value, modifier = modifier)
            is Result.Loading, is Result.Error -> Box(modifier)
        }
    } else {
        Box(modifier)
    }
}

/** Compact brand mark reused by the navigation drawer. */
@Composable
internal fun BaranOrbitMark(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 138.dp, height = 68.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.10f)),
        )
        Box(
            Modifier
                .size(width = 112.dp, height = 58.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.16f)),
        )
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_power_24dp),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
