package dem.dev.timeflame.data.repository

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigInteger
import java.util.LinkedHashMap

data class GenesisCoin(val tokenId: Int, val name: String, val imageUri: String, val rarity: String? = null)

/** Read-only collection browsing. No wallet, signing, payment or mint calls. */
class GenesisNftClient(context: Context? = null) {
    private val saved = context?.getSharedPreferences("genesis_nft_server_metadata_v2", Context.MODE_PRIVATE)
    companion object {
        const val CONTRACT = "0x7A5eD4eCe6A085c532F52517957aFf220B48609B"
        const val COLLECTION_URL = "https://thirdweb.com/polygon/$CONTRACT"
        const val CATALOG_SIZE = 10000
        const val ASSET_BASE = "https://api.pistolgenesis.ru/nft"
        internal fun catalogId(index: Int): Int {
            require(index in 0 until CATALOG_SIZE)
            return index + 1
        }

        internal fun serverImage(uri: String): String {
            val file = uri.substringAfterLast('/')
            require(Regex("[0-9]+\\.svg").matches(file)) { "Unexpected collection image" }
            return "$ASSET_BASE/images/$file"
        }

        val IPFS_GATEWAYS = listOf(
            "https://gateway.pinata.cloud/ipfs/",
            "https://ipfs.io/ipfs/",
            "https://dweb.link/ipfs/"
        )

        fun mediaUrl(uri: String, gatewayIndex: Int = 0): String {
            if (uri.startsWith("ipfs://")) {
                val path = uri.removePrefix("ipfs://").removePrefix("ipfs/")
                require(path.isNotBlank())
                return IPFS_GATEWAYS[gatewayIndex.coerceIn(IPFS_GATEWAYS.indices)] + path
            }
            require(uri.startsWith("https://")) { "Unsupported NFT media URL" }
            return uri
        }

        internal fun rarity(metadata: JsonObject): String? =
            runCatching {
                metadata["attributes"]?.jsonArray?.firstOrNull {
                    it.jsonObject["trait_type"]?.jsonPrimitive?.contentOrNull
                        ?.equals("Rarity", ignoreCase = true) == true
                }?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
                    ?.trim()?.takeIf { it.isNotEmpty() }
            }.getOrNull()

        /** Decode the offset, byte length and UTF-8 bytes of a Solidity string. */
        internal fun decodeString(encoded: String): String {
            val hex = encoded.removePrefix("0x")
            require(hex.length >= 128 && hex.length % 2 == 0)
            val byteOffset = BigInteger(hex.take(64), 16).intValueExact()
            require(byteOffset >= 32 && byteOffset <= (hex.length - 64) / 2)
            val offset = byteOffset * 2
            val byteLength = BigInteger(hex.substring(offset, offset + 64), 16).intValueExact()
            require(byteLength in 1..8192)
            val start = offset + 64
            require(byteLength <= (hex.length - start) / 2)
            return ByteArray(byteLength) { i ->
                hex.substring(start + i * 2, start + i * 2 + 2).toInt(16).toByte()
            }.toString(Charsets.UTF_8)
        }
    }

    private val http = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 12000
            connectTimeoutMillis = 8000
            socketTimeoutMillis = 12000
        }
    }
    private val permits = Semaphore(3)
    private val cache = object : LinkedHashMap<Int, GenesisCoin>(96, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, GenesisCoin>?): Boolean = size > 96
    }

    fun close() = http.close()

    // Owner-hosted catalog contains files numbered 1 through 10000.
    suspend fun collectionSize(): Int = CATALOG_SIZE

    suspend fun coin(tokenId: Int): GenesisCoin {
        synchronized(cache) { cache[tokenId] }?.let { return it }
        return permits.withPermit {
            synchronized(cache) { cache[tokenId] }?.let { return@withPermit it }
            val id = catalogId(tokenId)
            val key = "coin_$id"
            val cachedMetadata = saved?.getString(key, null)?.let {
                runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull()
            }
            val metadata = cachedMetadata ?: run {
                metadata("$ASSET_BASE/metadata/$id.json")
            }
            val image = metadata["image"]?.jsonPrimitive?.contentOrNull
                ?: metadata["image_url"]?.jsonPrimitive?.contentOrNull
                ?: error("NFT image is missing")
            val hostedImage = serverImage(image)
            GenesisCoin(
                id,
                metadata["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: "PISTOL #$id",
                hostedImage,
                rarity(metadata)
            ).also {
                synchronized(cache) { cache[tokenId] = it }
                if (cachedMetadata == null) saved?.edit()?.putString(key, metadata.toString())?.apply()
            }
        }
    }

    private suspend fun metadata(uri: String): JsonObject {
        var failure: Exception? = null
        val attempts = if (uri.startsWith("ipfs://")) IPFS_GATEWAYS.size else 1
        repeat(attempts) { gateway ->
            try {
                val response = http.get(mediaUrl(uri, gateway))
                check(response.status.value == 200)
                val text = response.bodyAsText()
                require(text.length <= 256000)
                return Json.parseToJsonElement(text).jsonObject
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = e
            }
        }
        throw failure ?: IllegalStateException("NFT metadata unavailable")
    }
}
