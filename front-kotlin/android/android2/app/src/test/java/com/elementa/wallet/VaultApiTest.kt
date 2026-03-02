package com.elementa.wallet

import com.elementa.wallet.data.bindings.*
import com.elementa.wallet.data.bindings.VaultApi
import org.junit.Test
import kotlin.test.assertEquals

class VaultApiTest {
    @Test
    fun vault_json_roundtrip() {
        val kdf = KdfParams("pbkdf2", byteArrayOf(1,2,3), 1024, 1000, 1)
        val cipher = CipherBlob(byteArrayOf(9,8,7), byteArrayOf(4,5,6))
        val original = VaultRecord(1, kdf, cipher, "0xdeadbeef", hd_index = 0, is_hd = true)

        val json = VaultApi.vaultToJson(original)
        val parsed = VaultApi.jsonToVault(json)

        assertEquals(original.version, parsed.version)
        assertEquals(original.public_address, parsed.public_address)
        assertEquals(original.hd_index, parsed.hd_index)
        assertEquals(original.is_hd, parsed.is_hd)
    }
}
