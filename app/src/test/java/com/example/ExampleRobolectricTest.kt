package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.AvisoTaskParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aviso Tasks", appName)
  }

  @Test
  fun `test Aviso task parser json`() {
    val sampleJson = """
      {
        "totalTasks": 15,
        "subscribeCount": 3,
        "watchCount": 12,
        "likesCount": 0,
        "isLoggedIn": true,
        "username": "tasinjami8",
        "tasks": [
          {"id": "t1", "title": "Watch video", "category": "Watch", "reward": "0.026 ₽"}
        ]
      }
    """.trimIndent()
    val result = AvisoTaskParser.parseJsonScanResult(sampleJson)
    assertEquals(15, result.totalTasks)
    assertEquals(3, result.subscribeCount)
    assertEquals(12, result.watchCount)
    assertTrue(result.isLoggedIn)
    assertEquals("tasinjami8", result.username)
    assertEquals(1, result.tasks.size)
  }

  @Test
  fun `test AvisoPermissionHelper methods do not crash`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val hasOverlay = com.example.util.AvisoPermissionHelper.canDrawOverlays(context)
    val hasBattery = com.example.util.AvisoPermissionHelper.isIgnoringBatteryOptimizations(context)
    val hasNotif = com.example.util.AvisoPermissionHelper.isNotificationPermissionGranted(context)
    assertNotNull(hasOverlay)
    assertNotNull(hasBattery)
    assertNotNull(hasNotif)
  }
}
