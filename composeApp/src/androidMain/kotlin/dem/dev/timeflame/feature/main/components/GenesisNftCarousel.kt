package dem.dev.timeflame.feature.main.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import dem.dev.timeflame.R
import dem.dev.timeflame.data.repository.GenesisCoin
import dem.dev.timeflame.data.repository.GenesisNftClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.random.Random

private data class ReadyGenesisCoin(
    val index: Int,
    val coin: GenesisCoin,
    val bitmap: ImageBitmap
)

/** Loads and decodes images before exposing a card to the carousel. */
private class GenesisCoinPreloader(
    private val context: android.content.Context,
    private val client: GenesisNftClient,
    private val loader: ImageLoader,
    private val pixels: Int
) {
    private val permits = Semaphore(3)
    private val cache = object : java.util.LinkedHashMap<Int, ReadyGenesisCoin>(24, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<Int, ReadyGenesisCoin>?
        ): Boolean = size > 24
    }

    suspend fun load(index: Int): ReadyGenesisCoin {
        synchronized(cache) { cache[index] }?.let { return it }
        return permits.withPermit {
            synchronized(cache) { cache[index] }?.let { return@withPermit it }
            val coin = client.coin(index)
            val result = loader.execute(
                ImageRequest.Builder(context)
                    .data(coin.imageUri)
                    .size(pixels, pixels)
                    .allowHardware(false)
                    .build()
            )
            check(result is SuccessResult) { "Coin image unavailable" }
            val bitmap = withContext(Dispatchers.Default) {
                val drawable = result.drawable
                ((drawable as? BitmapDrawable)?.bitmap ?: drawable.toBitmap(pixels, pixels))
                    .asImageBitmap()
            }
            ReadyGenesisCoin(index, coin, bitmap).also {
                synchronized(cache) { cache[index] = it }
            }
        }
    }

    suspend fun batch(range: IntRange): List<ReadyGenesisCoin> = coroutineScope {
        range.map { index -> async { load(index) } }.awaitAll()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GenesisNftCarousel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Follow the same inverse-card palette as GENESIS membership.
    val darkApp = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardSurface = if (darkApp) Color.White else Color.Black
    val textInk = if (darkApp) Color.Black else Color.White
    val secondaryInk = if (darkApp) Color(0xFF444444) else Color(0xFFD5D5D5)
    val accent = if (darkApp) Color(0xFF8B6816) else Color(0xFFD5B65E)
    val innerSurface = if (darkApp) Color(0xFFF4F4F4) else Color(0xFF151515)
    val dividerInk = if (darkApp) Color(0xFFB8B8B8) else Color(0xFF5D5D5D)
    val client = remember { GenesisNftClient(context.applicationContext) }
    val pixels = with(LocalDensity.current) { 144.dp.roundToPx() }.coerceAtMost(432)
    val imageLoader = remember(context) {
        ImageLoader.Builder(context.applicationContext)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    val preloader = remember(client, imageLoader, pixels) {
        GenesisCoinPreloader(context.applicationContext, client, imageLoader, pixels)
    }
    DisposableEffect(client, imageLoader) {
        onDispose { client.close(); imageLoader.shutdown() }
    }

    val total = GenesisNftClient.CATALOG_SIZE
    var ready by remember { mutableStateOf(emptyList<ReadyGenesisCoin>()) }
    var initialLoading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(0) }
    var randomLoading by remember { mutableStateOf(false) }
    var randomError by remember { mutableStateOf(false) }
    // Show token #2 in the center on first launch, with #1 and #3 visible beside it.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 1)
    val scope = rememberCoroutineScope()

    LaunchedEffect(preloader) {
        try {
            ready = preloader.batch(0..2)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed = true
        } finally {
            initialLoading = false
        }
    }

    // Only ready bitmaps enter the row. Stable keys retain the visible coin when
    // batches are prepended or the far end of the bounded buffer is discarded.
    LaunchedEffect(preloader, retry) {
        if (ready.isEmpty() && !initialLoading) {
            initialLoading = true
            try {
                ready = preloader.batch(0..2)
                failed = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
            } finally {
                initialLoading = false
            }
        }
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo
            listOf(
                visible.firstOrNull()?.index ?: 0,
                visible.lastOrNull()?.index ?: 0,
                ready.firstOrNull()?.index ?: -1,
                ready.lastOrNull()?.index ?: -1,
                if (randomLoading) 1 else 0
            )
        }.collectLatest { position ->
            if (ready.isEmpty() || randomLoading || failed) return@collectLatest
            val firstVisible = position[0]
            val lastVisible = position[1]
            val first = position[2]
            val last = position[3]
            try {
                if (lastVisible >= ready.size - 5 && last < total - 1) {
                    val loaded = preloader.batch((last + 1)..minOf(last + 6, total - 1))
                    if (!randomLoading && ready.lastOrNull()?.index == last) {
                        // Drop only cards far behind the viewport, never its visible cards.
                        val combined = ready + loaded
                        val drop = minOf((combined.size - 24).coerceAtLeast(0),
                            (listState.firstVisibleItemIndex - 6).coerceAtLeast(0))
                        ready = combined.drop(drop)
                    }
                } else if (firstVisible <= 3 && first > 0) {
                    val loaded = preloader.batch(maxOf(0, first - 6)..(first - 1))
                    if (!randomLoading && ready.firstOrNull()?.index == first) {
                        val combined = loaded + ready
                        val visibleEnd = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val keep = maxOf(24, loaded.size + visibleEnd + 7)
                        ready = combined.take(keep)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true // Keep all displayed images on a network failure.
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurface)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Text("PISTOL GENESIS NFT", style = MaterialTheme.typography.titleMedium, color = textInk)
                Text(
                    stringResource(R.string.genesis_nft_carousel_count, total),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryInk
                )
            }
            if (ready.isNotEmpty()) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val sidePadding = ((maxWidth - 164.dp) / 2).coerceAtLeast(0.dp)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = sidePadding),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        flingBehavior = rememberSnapFlingBehavior(listState)
                    ) {
                        items(ready, key = { it.index }) { item ->
                            GenesisCoinCard(
                                item = item,
                                selected = selected == item.index,
                                textInk = textInk,
                                secondaryInk = secondaryInk,
                                innerSurface = innerSurface,
                                accent = accent,
                                onSelected = {
                                    selected = item.index
                                    scope.launch {
                                        val position = ready.indexOfFirst { it.index == item.index }
                                        if (position >= 0) listState.animateScrollToItem(position)
                                    }
                                }
                            )
                        }
                    }
                }
            } else if (initialLoading) {
                Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = accent)
                }
            }
            if (failed || randomError) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(stringResource(R.string.genesis_nft_load_failed),
                        style = MaterialTheme.typography.bodySmall, color = textInk)
                    TextButton(onClick = { failed = false; randomError = false; retry++ }) {
                        Text(stringResource(R.string.genesis_nft_retry), color = accent)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val current = ready.getOrNull(listState.firstVisibleItemIndex)?.index ?: selected
                        val draw = Random.nextInt(total - 1)
                        val target = if (draw >= current) draw + 1 else draw
                        randomLoading = true
                        randomError = false
                        scope.launch {
                            try {
                                // Keep the current gallery visible until the chosen image and
                                // its immediate neighbours have all finished decoding.
                                val start = maxOf(0, target - 3)
                                val loaded = preloader.batch(start..minOf(target + 3, total - 1))
                                ready = loaded
                                selected = target
                                failed = false
                                listState.scrollToItem(target - start)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                randomError = true
                            } finally {
                                randomLoading = false
                            }
                        }
                    },
                    enabled = ready.isNotEmpty() && !randomLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = cardSurface,
                        disabledContainerColor = dividerInk, disabledContentColor = secondaryInk)
                ) {
                    if (randomLoading) {
                        CircularProgressIndicator(Modifier.size(18.dp),
                            color = cardSurface, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        stringResource(if (randomLoading) R.string.genesis_nft_preparing
                            else R.string.genesis_nft_random),
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            TextButton(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GenesisNftClient.COLLECTION_URL)))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, R.string.genesis_nft_browser_unavailable, Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) { Text(stringResource(R.string.genesis_nft_open_collection), color = accent) }
        }
    }
}

@Composable
private fun GenesisCoinCard(
    item: ReadyGenesisCoin,
    selected: Boolean,
    textInk: Color,
    secondaryInk: Color,
    innerSurface: Color,
    accent: Color,
    onSelected: () -> Unit
) {
    Card(
        onClick = onSelected,
        modifier = Modifier.width(164.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else secondaryInk.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = innerSurface)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(
                bitmap = item.bitmap,
                contentDescription = item.coin.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(144.dp)
            )
            Text(
                item.coin.rarity ?: "—",
                style = MaterialTheme.typography.labelLarge,
                color = textInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stringResource(R.string.genesis_nft_token_id, item.coin.tokenId),
                style = MaterialTheme.typography.labelSmall,
                color = secondaryInk
            )
        }
    }
}
