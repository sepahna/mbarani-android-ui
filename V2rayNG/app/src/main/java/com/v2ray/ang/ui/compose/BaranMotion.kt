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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
                    BaranLottieLoader(Modifier.size(66.dp))
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

@Composable
private fun FlatWorldMap(
    origin: GeoIpLocation?,
    destination: GeoIpLocation?,
    running: Boolean,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val land = MapLand.copy(alpha = 0.92f)
        val coast = MapCoast.copy(alpha = 0.42f)

        // Subtle latitude/longitude grid.
        for (index in 1..3) {
            val y = h * index / 4f
            drawLine(coast, Offset(0f, y), Offset(w, y), strokeWidth = 0.7.dp.toPx())
        }
        for (index in 1..5) {
            val x = w * index / 6f
            drawLine(coast, Offset(x, 0f), Offset(x, h), strokeWidth = 0.7.dp.toPx())
        }

        fun continent(points: List<Pair<Float, Float>>) {
            val path = Path().apply {
                moveTo(points.first().first * w, points.first().second * h)
                points.drop(1).forEach { lineTo(it.first * w, it.second * h) }
                close()
            }
            drawPath(path, land)
            drawPath(path, coast, style = Stroke(width = 0.9.dp.toPx()))
        }

        // Stylised flat continent silhouettes (decorative, not a political boundary dataset).
        continent(listOf(0.08f to 0.22f, 0.13f to 0.13f, 0.22f to 0.12f, 0.29f to 0.20f, 0.27f to 0.31f, 0.23f to 0.34f, 0.21f to 0.48f, 0.17f to 0.52f, 0.14f to 0.43f, 0.10f to 0.39f))
        continent(listOf(0.23f to 0.52f, 0.28f to 0.55f, 0.30f to 0.67f, 0.27f to 0.88f, 0.23f to 0.78f, 0.21f to 0.64f))
        continent(listOf(0.43f to 0.20f, 0.49f to 0.13f, 0.59f to 0.16f, 0.63f to 0.23f, 0.60f to 0.30f, 0.55f to 0.31f, 0.52f to 0.42f, 0.47f to 0.39f, 0.44f to 0.31f))
        continent(listOf(0.49f to 0.42f, 0.56f to 0.43f, 0.59f to 0.57f, 0.56f to 0.77f, 0.52f to 0.86f, 0.49f to 0.68f, 0.46f to 0.54f))
        continent(listOf(0.61f to 0.20f, 0.70f to 0.13f, 0.84f to 0.17f, 0.91f to 0.27f, 0.88f to 0.39f, 0.80f to 0.42f, 0.75f to 0.35f, 0.68f to 0.39f, 0.62f to 0.31f))
        continent(listOf(0.78f to 0.48f, 0.86f to 0.48f, 0.88f to 0.58f, 0.84f to 0.67f, 0.79f to 0.62f))
        continent(listOf(0.90f to 0.72f, 0.94f to 0.76f, 0.92f to 0.82f, 0.88f to 0.79f))

        fun project(location: GeoIpLocation): Offset = Offset(
            x = ((location.longitude + 180.0) / 360.0 * w).toFloat(),
            y = ((90.0 - location.latitude) / 180.0 * h).toFloat(),
        )

        val from = origin?.let(::project)
        val to = if (running) destination?.let(::project) else null
        if (from != null && to != null) {
            val route = Path().apply {
                moveTo(from.x, from.y)
                cubicTo(
                    (from.x + to.x) / 2f,
                    minOf(from.y, to.y) - h * 0.22f,
                    (from.x + to.x) / 2f,
                    minOf(from.y, to.y) - h * 0.22f,
                    to.x,
                    to.y,
                )
            }
            drawPath(
                route,
                MapMint.copy(alpha = 0.22f + pulse * 0.28f),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                route,
                MapMint,
                style = Stroke(
                    width = 1.7.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
                ),
            )
        }
        from?.let { point ->
            drawCircle(MapBlue.copy(alpha = 0.18f), radius = 10.dp.toPx(), center = point)
            drawCircle(MapBlue, radius = 4.dp.toPx(), center = point)
        }
        to?.let { point ->
            drawCircle(MapMint.copy(alpha = 0.20f), radius = 12.dp.toPx(), center = point)
            drawCircle(MapMint, radius = 4.5.dp.toPx(), center = point)
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
