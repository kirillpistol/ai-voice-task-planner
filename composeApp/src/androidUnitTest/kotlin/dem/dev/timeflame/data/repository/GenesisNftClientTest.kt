package dem.dev.timeflame.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GenesisNftClientTest {
    @Test fun decodesActualContractTokenUri() {
        val encoded = "0x" + "20".padStart(64, '0') + "37".padStart(64, '0') +
            "697066733a2f2f516d644d64726632764c4c7542657a753174335a733244597456674c6e395836626445695036654b546e634275782f30" +
            "00".repeat(9)
        assertEquals("ipfs://QmdMdrf2vLLuBezu1t3Zs2DYtVgLn9X6bdEiP6eKTncBux/0", GenesisNftClient.decodeString(encoded))
    }

    @Test fun rejectsTruncatedContractResponse() {
        assertThrows(IllegalArgumentException::class.java) {
            GenesisNftClient.decodeString("0x" + "20".padStart(64, '0') + "ff".padStart(64, '0'))
        }
    }

    @Test fun rejectsUnboundedStringOffset() {
        assertThrows(ArithmeticException::class.java) {
            GenesisNftClient.decodeString("0x" + "f".repeat(64) + "00".repeat(32))
        }
    }

    @Test fun convertsIpfsAndPreservesHttps() {
        assertEquals("https://gateway.pinata.cloud/ipfs/CID/1.svg", GenesisNftClient.mediaUrl("ipfs://CID/1.svg"))
        assertEquals("https://ipfs.io/ipfs/CID/1.svg", GenesisNftClient.mediaUrl("ipfs://ipfs/CID/1.svg", 1))
        assertEquals("https://example.org/1.svg", GenesisNftClient.mediaUrl("https://example.org/1.svg"))
    }

    @Test fun rejectsUnsafeMediaSchemes() {
        assertThrows(IllegalArgumentException::class.java) { GenesisNftClient.mediaUrl("file:///etc/passwd") }
        assertThrows(IllegalArgumentException::class.java) { GenesisNftClient.mediaUrl("javascript:alert(1)") }
    }
}
