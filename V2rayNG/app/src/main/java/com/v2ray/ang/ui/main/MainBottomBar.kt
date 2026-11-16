package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R

enum class MainTab { Home, Servers }

@Composable
fun MainBottomBar(
    displayText: String,
    isRunning: Boolean,
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        tonalElevation = 3.dp,
        shadowElevation = 7.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape)
                    .background(if (isRunning) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.5f)))
                Text(
                    text = displayText,
                    modifier = Modifier.padding(start = 7.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomAction(R.drawable.ic_home_24dp, stringResource(R.string.app_name),
                    selected = selectedTab == MainTab.Home, onClick = { onSelectTab(MainTab.Home) })
                BottomAction(R.drawable.ic_dns_24dp, stringResource(R.string.title_server),
                    selected = selectedTab == MainTab.Servers, onClick = { onSelectTab(MainTab.Servers) })
                BottomAction(R.drawable.ic_add_24dp, stringResource(R.string.acc_add),
                    onClick = { onAction(MainAction.AddSubscription) })
                BottomAction(R.drawable.ic_settings_24dp, stringResource(R.string.title_settings),
                    onClick = { onNavigate(MainDestination.Settings) })
            }
        }
    }
}

@Composable
private fun BottomAction(
    icon: Int,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.secondaryContainer else colors.surface)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null,
            tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
            modifier = Modifier.size(21.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
            maxLines = 1)
    }
}
