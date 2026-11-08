package com.rubixscript.flipscout.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Status
import com.rubixscript.flipscout.domain.GroupProfit
import com.rubixscript.flipscout.domain.MonthProfit
import com.rubixscript.flipscout.domain.monthlyProfit
import com.rubixscript.flipscout.domain.profitByCategory
import com.rubixscript.flipscout.domain.profitByPlatform
import com.rubixscript.flipscout.ui.theme.Dimens
import com.rubixscript.flipscout.ui.theme.Palette
import com.rubixscript.flipscout.util.money

/** Profit trends over time. Takes the live item list, so it always matches the Items tab. */
@Composable
fun InsightsScreen(items: List<Item>, modifier: Modifier = Modifier) {
    val hasSales = remember(items) { items.any { it.status == Status.SOLD } }
    val months = remember(items) { items.monthlyProfit(6) }
    val uncategorised = stringResource(R.string.uncategorised)
    val noPlatform = stringResource(R.string.no_platform)
    val byCategory = remember(items, uncategorised) { items.profitByCategory(uncategorised) }
    val byPlatform = remember(items, noPlatform) { items.profitByPlatform(noPlatform) }

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0)) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = Dimens.screen, end = Dimens.screen, top = 8.dp, bottom = Dimens.bottomBarSpace),
            verticalArrangement = Arrangement.spacedBy(Dimens.listGap),
        ) {
            item {
                Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                    Text(stringResource(R.string.insights_title), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.insights_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (!hasSales) {
                item { InsightsEmptyState() }
            } else {
                item { MonthlyProfitCard(months) }
                if (byCategory.isNotEmpty()) item { BreakdownCard(stringResource(R.string.insights_by_category), byCategory) }
                if (byPlatform.isNotEmpty()) item { BreakdownCard(stringResource(R.string.insights_by_platform), byPlatform) }
            }
        }
    }
}

@Composable
private fun MonthlyProfitCard(months: List<MonthProfit>) {
    val total = months.sumOf { it.profit }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.insights_monthly), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.insights_six_months), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Text(
                    (if (total >= 0) "+" else "") + money(total),
                    color = if (total >= 0) Palette.Emerald else Palette.Loss,
                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                )
            }
            ProfitBars(months)
        }
    }
}

/** Rounded gradient bars with value and month labels, growing in on first show. */
@Composable
private fun ProfitBars(months: List<MonthProfit>) {
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val progress by animateFloatAsState(1f, tween(800), label = "barsGrow")
    val valueStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        drawProfitBars(months, measurer, labelColor, valueStyle, labelStyle, progress)
    }
}

private fun DrawScope.drawProfitBars(
    months: List<MonthProfit>,
    measurer: TextMeasurer,
    labelColor: Color,
    valueStyle: TextStyle,
    labelStyle: TextStyle,
    progress: Float,
) {
    if (months.isEmpty()) return
    val labelSpace = 30.dp.toPx() // month labels at the bottom
    val chartHeight = size.height - labelSpace
    val maxProfit = months.maxOf { it.profit }.coerceAtLeast(0.0)
    val minProfit = months.minOf { it.profit }.coerceAtMost(0.0)
    val range = (maxProfit - minProfit).takeIf { it > 0 } ?: 1.0
    val zeroY = chartHeight * (maxProfit / range).toFloat()
    val slot = size.width / months.size
    val barWidth = slot * 0.52f

    months.forEachIndexed { i, month ->
        val barLeft = slot * i + (slot - barWidth) / 2
        val fullHeight = (Math.abs(month.profit) / range * chartHeight).toFloat() * progress
        val top = if (month.profit >= 0) zeroY - fullHeight else zeroY
        if (fullHeight > 0f) {
            drawRoundRect(
                brush = if (month.profit >= 0) Brush.verticalGradient(listOf(Palette.Emerald, Palette.EmeraldDeep))
                else Brush.verticalGradient(listOf(Palette.Loss.copy(alpha = 0.7f), Palette.Loss)),
                topLeft = Offset(barLeft, top),
                size = Size(barWidth, fullHeight),
                cornerRadius = CornerRadius(7.dp.toPx()),
            )
        }
        // Value label floating above the bar (below it for losses).
        val value = measurer.measure(
            (if (month.profit >= 0) "+" else "") + money(month.profit),
            valueStyle.copy(color = if (month.profit >= 0) Palette.Emerald else Palette.Loss),
        )
        val valueY = if (month.profit >= 0) top - value.size.height - 3.dp.toPx() else top + fullHeight + 3.dp.toPx()
        drawText(
            value,
            topLeft = Offset(
                slot * i + (slot - value.size.width) / 2f,
                valueY.coerceIn(0f, chartHeight - value.size.height),
            ),
        )
    }
    // Baseline separating bars from month labels.
    drawLine(
        color = labelColor.copy(alpha = 0.25f),
        start = Offset(0f, zeroY),
        end = Offset(size.width, zeroY),
        strokeWidth = 1.dp.toPx(),
    )
    months.forEachIndexed { i, month ->
        val label = measurer.measure(month.label, labelStyle.copy(color = labelColor), maxLines = 1)
        drawText(label, topLeft = Offset(slot * i + (slot - label.size.width) / 2f, size.height - label.size.height.toFloat()))
    }
}

@Composable
private fun BreakdownCard(title: String, groups: List<GroupProfit>) {
    val maxAbs = groups.maxOf { Math.abs(it.profit) }.takeIf { it > 0 } ?: 1.0
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            groups.take(8).forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(group.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.insights_sold_count, group.count),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Text((if (group.profit >= 0) "+" else "") + money(group.profit),
                            color = if (group.profit >= 0) Palette.Emerald else Palette.Loss,
                            fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Row(
                        Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            Modifier.fillMaxWidth((Math.abs(group.profit) / maxAbs).toFloat()).height(6.dp)
                                .background(
                                    if (group.profit >= 0) Palette.Emerald else Palette.Loss,
                                    RoundedCornerShape(50),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsEmptyState() {
    Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📊", fontSize = 56.sp)
        Text(stringResource(R.string.insights_empty_title), style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp))
        Text(stringResource(R.string.insights_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp))
    }
}
