package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.TaskScanResult
import com.example.ui.AvisoSidebar
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AvisoUiState
import com.example.viewmodel.AvisoViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        AvisoSidebar(
          uiState = AvisoUiState(
            zoomPercent = 120,
            scanResult = TaskScanResult(
              totalTasks = 1125,
              watchCount = 1119,
              subscribeCount = 6,
              isLoggedIn = true,
              username = "tasinjami8"
            )
          ),
          viewModel = AvisoViewModel(),
          onClose = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
