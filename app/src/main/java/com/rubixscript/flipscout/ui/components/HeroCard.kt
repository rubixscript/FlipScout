package com.rubixscript.flipscout.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.domain.PortfolioStats
import com.rubixscript.flipscout.ui.theme.Dimens
import com.rubixscript.flipscout.util.money

/**
 * Big plain total (label above, huge number) with the secondary stats tucked into one soft card,
 * like a wallet balance. The number counts up whenever it changes.
 */
@Composable
fun HeroCard(stats: PortfolioStats, modifier: Modifier = Modifier) {
    val profit = countUp(stats.profit.toFloat())
    val inStock = countUp(stats.inStock.toFloat())
    val roi = countUp(stats.avgRoi.toFloat())
    val sold = countUp(stats.soldCount.toFloat())
    Column(modifier.fillMaxWidth()) {
        Text(stringResource(R.string.hero_total_profit), color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(money(profit.toDouble()), fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 20.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.cardRadius))
                .background(MaterialTheme.colorScheme.surface).padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            HeroStat(stringResource(R.string.hero_in_stock), money(inStock.toDouble()), Modifier.weight(1f))
            HeroStat(stringResource(R.string.hero_items_sold), "${sold.toInt()}", Modifier.weight(1f))
            HeroStat(stringResource(R.string.hero_avg_roi), "%.0f%%".format(roi), Modifier.weight(1f))
        }
    }
}

/** Animates a number from its previous value to [target] for a count-up effect. */
@Composable
private fun countUp(target: Float): Float =
    animateFloatAsState(target, tween(900), label = "countUp").value

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
    }
}
