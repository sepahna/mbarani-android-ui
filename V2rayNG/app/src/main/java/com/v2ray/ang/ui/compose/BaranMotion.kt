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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
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
import io.github.sceneview.RenderQuality
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.geometries.Sphere
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.SphereNode
import io.github.sceneview.node.TorusNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader

/**
 * Mint connection dashboard. Connection lifecycle and permission handling remain owned by
 * MainActivity. Lottie, Rive and Filament are visual layers only and never control VPN state.
 */
@Composable
internal fun BaranHomeStage(
    statusText: String,
    running: Boolean,
    serverName: String?,
    onToggle: () -> Unit,
    onChooseServer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "baran-connection-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "baran-pulse-scale",
    )
    val actionLabel = stringResource(if (running) R.string.acc_stop else R.string.acc_start)
    val stateLabel = stringResource(if (running) R.string.connection_connected else R.string.connection_not_connected)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BaranRiveAccent(Modifier.size(42.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (running) colors.primary else colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(
            modifier = Modifier.size(244.dp).padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(236.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.07f)),
            )
            Box(
                Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.10f)),
            )
            BaranEarthScene(Modifier.size(178.dp))
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .clickable(onClick = onToggle)
                    .semantics {
                        role = Role.Button
                        contentDescription = actionLabel
                        stateDescription = stateLabel
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (running) {
                    BaranLottieLoader(Modifier.size(82.dp))
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_power_24dp),
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(46.dp),
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onChooseServer),
            shape = RoundedCornerShape(22.dp),
            color = colors.surface.copy(alpha = 0.94f),
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_speed_24dp),
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                ) {
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
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
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
            is Result.Success -> Rive(
                file = riveFile.value,
                modifier = modifier,
            )
            is Result.Loading, is Result.Error -> Box(modifier)
        }
    } else {
        Box(modifier)
    }
}

/**
 * A lightweight procedural 3D globe built with SceneView/Filament.
 * It is deliberately decorative: server and user locations are not inferred here.
 */
@Composable
private fun BaranEarthScene(modifier: Modifier = Modifier) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val rotation by rememberInfiniteTransition(label = "baran-earth-rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 42_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "baran-earth-yaw",
    )

    SceneView(
        modifier = modifier,
        engine = engine,
        surfaceType = SurfaceType.TextureSurface,
        isOpaque = false,
        renderQuality = RenderQuality.Performance,
    ) {
        val globeMaterial = remember(materialLoader) {
            materialLoader.createColorInstance(
                Color(0xFF2F9D87),
                metallic = 0.12f,
                roughness = 0.62f,
            )
        }
        val orbitMaterial = remember(materialLoader) {
            materialLoader.createColorInstance(
                Color(0xFF9BE9D1),
                metallic = 0.25f,
                roughness = 0.38f,
            )
        }
        SphereNode(
            radius = 0.72f,
            stacks = 32,
            slices = 48,
            materialInstance = globeMaterial,
            rotation = Rotation(y = rotation),
        )
        TorusNode(
            majorRadius = 0.94f,
            minorRadius = 0.012f,
            majorSegments = 48,
            minorSegments = 8,
            materialInstance = orbitMaterial,
            rotation = Rotation(x = 66f, z = 18f),
        )
    }
}

/** Compact brand mark reused by the navigation drawer. */
@Composable
internal fun BaranOrbitMark(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 138.dp, height = 68.dp)
                .graphicsLayer { rotationZ = -28f }
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.10f)),
        )
        Box(
            Modifier
                .size(width = 112.dp, height = 58.dp)
                .graphicsLayer { rotationZ = 28f }
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
