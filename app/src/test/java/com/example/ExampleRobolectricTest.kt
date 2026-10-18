package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ClientStorageRepository
import com.example.model.BedrockVersionTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read app_name and verify multi-version 1_19 to 1_26_plus repository engine`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Flarial Client", appName)

        val repo = ClientStorageRepository(context)
        val modules = repo.defaultModules()
        assertTrue("Expected extended modules including Time Changer and Game Speed", modules.size >= 9)
        assertTrue(modules.any { it.id == "time_changer" })
        assertTrue(modules.any { it.id == "game_speed_fov" })

        val addons = repo.defaultAddons()
        val patched119 = repo.patchAllAddonsToVersionTier(addons, BedrockVersionTier.V1_19)
        assertTrue(patched119.all { it.minEngineVersion == "1.19.0" })

        val patched126 = repo.patchAllAddonsToVersionTier(addons, BedrockVersionTier.V1_26_PLUS)
        assertTrue(patched126.all { it.minEngineVersion == "1.26.0" })
    }
}
