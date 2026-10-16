package dem.dev.timeflame.feature.main.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private data class AiQuote(val symbol: String, val name: String, val price: Double,
    val change: Double, val chart: List<Double>)
private data class FxQuote(val code: String, val rub: Double, val date: String)
private data class MarketSnapshot(val tokens: List<AiQuote>, val currencies: List<FxQuote>,
    val cryptoError: Boolean, val fxError: Boolean, val fetchedAt: Long)

private val selectedCoins = listOf("bittensor", "artificial-superintelligence-alliance", "render-token", "worldcoin-wld", "akash-network")

private fun requestJson(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 6000
    connection.readTimeout = 6000
    connection.setRequestProperty("Accept", "application/json")
    connection.setRequestProperty("User-Agent", "PISTOL-GENESIS/1.0")
    try {
        if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally { connection.disconnect() }
}
private fun fetchTokens(): List<AiQuote> {
    val url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=" +
        selectedCoins.joinToString("%2C") + "&order=market_cap_desc&per_page=10&page=1&sparkline=true&price_change_percentage=24h"
    val array = JSONArray(requestJson(url))
    return (0 until array.length()).mapNotNull { i ->
        val json = array.optJSONObject(i) ?: return@mapNotNull null
        val price = json.optDouble("current_price", Double.NaN)
        val change = json.optDouble("price_change_percentage_24h", Double.NaN)
        if (!price.isFinite() || !change.isFinite()) return@mapNotNull null
        val values = json.optJSONObject("sparkline_in_7d")?.optJSONArray("price")
        AiQuote(json.optString("symbol").uppercase(Locale.US), json.optString("name"),
            price, change, if (values == null) emptyList() else (0 until values.length())
                .mapNotNull { j -> values.optDouble(j, Double.NaN).takeIf { it.isFinite() } })
    }
}
private fun fetchFx(): List<FxQuote> {
    // Frankfurter v2 aggregates national-bank sources; RUB is not supported by the old ECB-only v1.
    val rates = JSONArray(requestJson(
        "https://api.frankfurter.dev/v2/rates?base=RUB&quotes=USD,GBP,CNY"
    ))
    val byCurrency = (0 until rates.length()).mapNotNull { i ->
        rates.optJSONObject(i)?.let { it.optString("quote") to it }
    }.toMap()
    return listOf("USD", "GBP", "CNY").map { code ->
        val row = byCurrency[code] ?: error("No RUB/$code quote")
        val rate = row.getDouble("rate")
        require(rate > 0)
        FxQuote(code, 1.0 / rate, row.getString("date"))
    }
}
private var cachedMarket: MarketSnapshot? = null
private suspend fun loadMarket(force: Boolean): MarketSnapshot = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val cached = synchronized(selectedCoins) { cachedMarket }
    if (!force && cached != null && now - cached.fetchedAt < 120_000L) return@withContext cached
    val coins = runCatching { fetchTokens() }.getOrNull()
    val fx = runCatching { fetchFx() }.getOrNull()
    MarketSnapshot(
        coins?.takeIf { it.isNotEmpty() } ?: cached?.tokens.orEmpty(),
        fx?.takeIf { it.isNotEmpty() } ?: cached?.currencies.orEmpty(),
        coins == null || coins.isEmpty(), fx == null || fx.isEmpty(), now
    ).also { synchronized(selectedCoins) { cachedMarket = it } }
}
private fun number(value: Double): String = when {
    value >= 100 -> String.format(Locale.US, "%,.2f", value)
    value >= 1 -> String.format(Locale.US, "%.3f", value)
    else -> String.format(Locale.US, "%.5f", value)
}
private fun change(value: Double) = String.format(Locale.US, "%+.2f%%", value)

@Composable
private fun QuoteSparkline(values: List<Double>, changeValue: Double, modifier: Modifier = Modifier) {
    val accent = if (changeValue < 0) Color(0xFFD44335) else Color(0xFF24894C)
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.minOrNull() ?: return@Canvas
        val max = values.maxOrNull() ?: return@Canvas
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        val path = Path()
        values.forEachIndexed { i, value ->
            val x = size.width * i / (values.size - 1)
            val y = (size.height * (1.0 - (value - min) / range)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, accent, style = Stroke(width = 2.dp.toPx()))
    }
}
@Composable
private fun TokenBadge(symbol: String) {
    val dark = isSystemInDarkTheme()
    Box(Modifier.size(35.dp).background(if (dark) Color(0xFF41454B) else Color(0xFFE0E2E5),
        CircleShape), contentAlignment = Alignment.Center) {
        Text(symbol.take(3), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            color = if (dark) Color.White else Color(0xFF27292B))
    }
}

/** Swipeable full-width market pages. Lower movers -> summary -> FX. No fabricated quotes. */
@Composable
fun GenesisMarketCarousel(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Color(0xFF25282B) else Color(0xFFF0F1F2)
    val panel = if (dark) Color(0xFF32363A) else Color(0xFFE4E6E8)
    val ink = if (dark) Color(0xFFF5F5F5) else Color(0xFF25282A)
    val muted = if (dark) Color(0xFFB5BBC0) else Color(0xFF62676B)
    var page by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf<MarketSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(refresh) {
        loading = snapshot == null
        snapshot = loadMarket(force = refresh > 0)
        loading = false
    }
    // Refresh when visible, then every 2 minutes. FX data is published daily.
    LaunchedEffect(Unit) {
        while (true) {
            delay(120_000L)
            snapshot = loadMarket(force = true)
        }
    }
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (dark) Color(0xFF41464A) else Color(0xFFD4D7D9)),
        colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(when (page) { 0 -> "AI Crypto Movers"; 1 -> "AI Crypto Summary"; else -> "Валютные курсы" },
                    modifier = Modifier.weight(1f), color = ink,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { refresh++ }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = muted)
                }
            }
            if (loading) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(26.dp), color = muted)
                }
            } else {
                val market = snapshot
                when (page) {
                    0 -> {
                        if (market?.tokens.isNullOrEmpty()) {
                            Text("Котировки временно недоступны. Нажмите обновить.", color = muted)
                        } else market!!.tokens.forEach { coin ->
                            Row(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(10.dp))
                                .padding(9.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                TokenBadge(coin.symbol)
                                Column(Modifier.weight(1f)) {
                                    Text(coin.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        color = ink, style = MaterialTheme.typography.bodyMedium)
                                    Text(coin.symbol, color = muted, style = MaterialTheme.typography.labelSmall)
                                }
                                QuoteSparkline(coin.chart, coin.change, Modifier.width(52.dp).height(25.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(change(coin.change), color = if (coin.change < 0) Color(0xFFD44335) else Color(0xFF24894C),
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text("$" + number(coin.price), color = muted,
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    1 -> {
                        val tokens = market?.tokens.orEmpty()
                        if (tokens.isEmpty()) Text("Нет данных для сводки.", color = muted)
                        else {
                            Text("Среднее изменение за 24 часа · равный вес", color = muted,
                                style = MaterialTheme.typography.bodySmall)
                            val average = tokens.map { it.change }.average()
                            Text(change(average), style = MaterialTheme.typography.headlineLarge,
                                color = if (average < 0) Color(0xFFD44335) else Color(0xFF24894C))
                            Text("AI-токены · цены в USD", color = muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                tokens.take(4).forEach { coin ->
                                    Column(Modifier.weight(1f).background(panel, RoundedCornerShape(10.dp))
                                        .padding(vertical = 12.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally) {
                                        TokenBadge(coin.symbol)
                                        Spacer(Modifier.height(8.dp))
                                        Text(coin.symbol, maxLines = 1, color = ink,
                                            style = MaterialTheme.typography.labelSmall)
                                        Text(change(coin.change), color = if (coin.change < 0)
                                            Color(0xFFD44335) else Color(0xFF24894C),
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        val currencies = market?.currencies.orEmpty()
                        if (currencies.isEmpty()) Text("Курсы валют временно недоступны.", color = muted)
                        else {
                            Text("Стоимость 1 единицы валюты в рублях", color = muted,
                                style = MaterialTheme.typography.bodySmall)
                            currencies.forEach { fx ->
                                Row(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(10.dp))
                                    .padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(when (fx.code) {"USD" -> "$"; "GBP" -> "£"; else -> "¥"},
                                        color = ink, style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(fx.code, color = ink)
                                        Text(when (fx.code) {"USD" -> "Доллар США"; "GBP" -> "Фунт стерлингов";
                                            else -> "Китайский юань"}, color = muted,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(String.format(Locale.US, "%.2f ₽", fx.rub), color = ink,
                                        style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Text("Дата курсов: " + currencies.first().date + " · Frankfurter, справочные значения",
                                color = muted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                val failed = if (page == 2) market?.fxError == true else market?.cryptoError == true
                if (failed && market != null && (market.tokens.isNotEmpty() || market.currencies.isNotEmpty())) {
                    Text("Источник недоступен, показаны последние полученные значения.",
                        color = muted, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { page = (page + 2) % 3 }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Назад", tint = ink)
                }
                Text("${page + 1} / 3  •  " + if (page == 2) "Frankfurter" else "CoinGecko",
                    color = muted, style = MaterialTheme.typography.labelSmall)
                IconButton(onClick = { page = (page + 1) % 3 }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Вперёд", tint = ink)
                }
            }
        }
    }
}
