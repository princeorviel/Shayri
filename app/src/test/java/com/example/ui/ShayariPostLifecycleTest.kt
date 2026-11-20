package com.example.ui

import android.content.Context
import android.os.Looper
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ROUTE_MEHFIL
import com.example.ROUTE_WRITE_STUDIO
import com.example.data.local.LocalPersonalShayariRepository
import com.example.data.local.ShayariDatabase
import com.example.data.model.LOCAL_GUEST_USER_ID
import com.example.data.model.LocalPersonalShayariEntity
import com.example.data.model.PersonalShayari
import com.example.data.model.toPersonalShayari
import com.example.data.remote.PersonalShayariStore
import com.example.ui.studio.ShayariWriteStudioScreen
import com.example.ui.theme.ShayariTheme
import com.example.ui.viewmodel.PersonalShayariViewModel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShayariPostLifecycleTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `LocalPersonalShayariRepository saves asynchronously off main thread and handles edge timestamps`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShayariDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val ranOnMainThread = AtomicBoolean(true)
            val trackingRepo = LocalPersonalShayariRepository(
                dao = db.localPersonalShayariDao(),
                ioDispatcher = Dispatchers.IO
            )

            // Verify actual save runs off main thread when invoked from Dispatchers.Main-like caller
            val result = withContext(Dispatchers.Default) {
                val res = trackingRepo.savePersonalShayari(
                    title = "",
                    themeTag = "",
                    couplet = "रात खामोश है और चाँदनी बोलती है,\nदिल की हर बात शायरी में खुलती है।"
                )
                ranOnMainThread.set(Looper.myLooper() == Looper.getMainLooper())
                res
            }

            assertTrue("Save should succeed", result.isSuccess)
            assertFalse("Repository work must run off the main thread", ranOnMainThread.get())

            val savedList = trackingRepo.observePersonalShayari(LOCAL_GUEST_USER_ID).first()
            assertEquals(1, savedList.size)
            assertEquals(LocalPersonalShayariRepository.DEFAULT_THEME_TAG, savedList.first().themeTag)

            // Negative/zero timestamp edge case must never throw IllegalArgumentException
            val edgeEntity = LocalPersonalShayariEntity(
                id = "edge_ts",
                title = "Edge",
                themeTag = "दर्द",
                couplet = "चाँद भी सो गया आज तन्हाई में",
                createdAtMillis = -5000L,
                updatedAtMillis = 0L
            )
            val mapped = edgeEntity.toPersonalShayari()
            assertNotNull(mapped.createdAt)
            assertNotNull(mapped.updatedAt)
        } finally {
            db.close()
        }
    }

    @Test
    fun `rapid double tap on post only submits once and invokes onSaved once`() {
        val saveGate = CompletableDeferred<Unit>()
        val saveCalls = AtomicInteger(0)
        val savedItems = MutableStateFlow<List<PersonalShayari>>(emptyList())
        val slowStore = object : PersonalShayariStore {
            override fun observePersonalShayari(userId: String): Flow<List<PersonalShayari>> = savedItems
            override suspend fun savePersonalShayari(title: String, themeTag: String, couplet: String): Result<String> {
                val count = saveCalls.incrementAndGet()
                saveGate.await()
                val created = PersonalShayari(
                    id = "post_$count",
                    userId = LOCAL_GUEST_USER_ID,
                    title = title,
                    themeTag = themeTag,
                    couplet = couplet
                )
                savedItems.value = listOf(created) + savedItems.value
                return Result.success(created.id)
            }

            override suspend fun updatePersonalShayari(shayariId: String, title: String, themeTag: String, couplet: String) =
                Result.success(Unit)

            override suspend fun deletePersonalShayari(shayariId: String) = Result.success(Unit)
        }

        val viewModel = PersonalShayariViewModel(slowStore, LOCAL_GUEST_USER_ID)
        var onSavedCount = 0

        composeRule.setContent {
            ShayariTheme {
                ShayariWriteStudioScreen(
                    viewModel = viewModel,
                    authorName = "Prince",
                    onClose = {},
                    onSaved = { onSavedCount += 1 },
                    onFeedback = {}
                )
            }
        }

        composeRule.onNodeWithTag("personal_couplet_input")
            .performTextInput("दिल की गहराई से निकली है ये बात,\nतुम्हारे बिना अधूरी है हर एक रात।")
        composeRule.waitForIdle()

        // Tap quick save while async save is pending; attempt a second direct submit concurrently
        composeRule.onNodeWithTag("studio_quick_save_button").performClick()
        val secondAccepted = viewModel.submitShayari()
        assertFalse("Concurrent submission must be rejected while save is in flight", secondAccepted)

        // Release async save and wait for UI lifecycle event
        saveGate.complete(Unit)
        composeRule.waitForIdle()

        assertEquals("Repository save must only run once", 1, saveCalls.get())
        assertEquals("onSaved navigation callback must fire exactly once", 1, onSavedCount)
    }

    @Test
    fun `leaving studio mid-save does not trigger stale navigation or close newly reopened studio`() {
        val saveGate = CompletableDeferred<Unit>()
        val savedItems = MutableStateFlow<List<PersonalShayari>>(emptyList())
        val slowStore = object : PersonalShayariStore {
            override fun observePersonalShayari(userId: String): Flow<List<PersonalShayari>> = savedItems
            override suspend fun savePersonalShayari(title: String, themeTag: String, couplet: String): Result<String> {
                saveGate.await()
                val created = PersonalShayari(
                    id = "async_1",
                    userId = LOCAL_GUEST_USER_ID,
                    title = title,
                    themeTag = themeTag,
                    couplet = couplet
                )
                savedItems.value = listOf(created) + savedItems.value
                return Result.success(created.id)
            }

            override suspend fun updatePersonalShayari(shayariId: String, title: String, themeTag: String, couplet: String) =
                Result.success(Unit)

            override suspend fun deletePersonalShayari(shayariId: String) = Result.success(Unit)
        }

        val viewModel = PersonalShayariViewModel(slowStore, LOCAL_GUEST_USER_ID)
        var showStudio by mutableStateOf(true)
        var onSavedCount = 0

        composeRule.setContent {
            ShayariTheme {
                if (showStudio) {
                    ShayariWriteStudioScreen(
                        viewModel = viewModel,
                        authorName = "Prince",
                        onClose = { showStudio = false },
                        onSaved = { onSavedCount += 1 },
                        onFeedback = {}
                    )
                } else {
                    Text(text = "Home Screen", modifier = Modifier.testTag("home_placeholder"))
                }
            }
        }

        composeRule.onNodeWithTag("personal_couplet_input")
            .performTextInput("सफर लंबा सही पर हौसला अभी बाक़ी है")
        composeRule.waitForIdle()

        // Start async save, then immediately leave studio before save completes
        composeRule.onNodeWithTag("studio_quick_save_button").performClick()
        composeRule.runOnIdle { showStudio = false }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_placeholder").assertIsDisplayed()

        // Complete the in-flight save while studio is disposed
        saveGate.complete(Unit)
        composeRule.waitForIdle()

        // Save persisted cleanly without firing stale onSaved callback
        assertEquals(1, savedItems.value.size)
        assertEquals(0, onSavedCount)

        // Reopen Write Studio — buffered old event must NOT prematurely close the new studio session
        composeRule.runOnIdle { showStudio = true }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("personal_couplet_input").assertIsDisplayed()
        assertEquals(0, onSavedCount)
    }

    @Test
    fun `posting shayari inside NavHost pops write studio cleanly back to home without emptying stack`() {
        val savedItems = MutableStateFlow<List<PersonalShayari>>(emptyList())
        val store = object : PersonalShayariStore {
            override fun observePersonalShayari(userId: String): Flow<List<PersonalShayari>> = savedItems
            override suspend fun savePersonalShayari(title: String, themeTag: String, couplet: String): Result<String> {
                val created = PersonalShayari(
                    id = "nav_1",
                    userId = LOCAL_GUEST_USER_ID,
                    title = title,
                    themeTag = themeTag,
                    couplet = couplet
                )
                savedItems.value = listOf(created) + savedItems.value
                return Result.success(created.id)
            }

            override suspend fun updatePersonalShayari(shayariId: String, title: String, themeTag: String, couplet: String) =
                Result.success(Unit)

            override suspend fun deletePersonalShayari(shayariId: String) = Result.success(Unit)
        }
        val viewModel = PersonalShayariViewModel(store, LOCAL_GUEST_USER_ID)
        var feedbackMessage: String? = null
        var onSavedCalls = 0
        var routeBeforePop: String? = null
        var prevRouteBeforePop: String? = null
        var popResult: Boolean? = null
        var currentRouteAfterSave: String? = null

        composeRule.setContent {
            ShayariTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = ROUTE_MEHFIL) {
                    composable(ROUTE_MEHFIL) {
                        androidx.compose.foundation.layout.Column {
                            Text(
                                text = "Mehfil Home",
                                modifier = Modifier.testTag("mehfil_home_marker")
                            )
                            androidx.compose.material3.Button(
                                onClick = {
                                    navController.navigate(ROUTE_WRITE_STUDIO) {
                                        launchSingleTop = true
                                    }
                                },
                                modifier = Modifier.testTag("open_studio_btn")
                            ) {
                                Text("Open Studio")
                            }
                        }
                    }
                    composable(ROUTE_WRITE_STUDIO) {
                        ShayariWriteStudioScreen(
                            viewModel = viewModel,
                            authorName = "Prince",
                            onClose = {
                                if (navController.currentBackStackEntry?.destination?.route == ROUTE_WRITE_STUDIO &&
                                    navController.previousBackStackEntry != null
                                ) {
                                    navController.popBackStack(ROUTE_WRITE_STUDIO, inclusive = true)
                                }
                            },
                            onSaved = {
                                onSavedCalls += 1
                                routeBeforePop = navController.currentBackStackEntry?.destination?.route
                                prevRouteBeforePop = navController.previousBackStackEntry?.destination?.route
                                if (routeBeforePop == ROUTE_WRITE_STUDIO && prevRouteBeforePop != null) {
                                    popResult = navController.popBackStack(ROUTE_WRITE_STUDIO, inclusive = true)
                                }
                                currentRouteAfterSave = navController.currentBackStackEntry?.destination?.route
                            },
                            onFeedback = { feedbackMessage = it }
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("open_studio_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("personal_couplet_input")
            .performTextInput("चाँद सितारे गवाह हैं मेरी हर एक दुआ के")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("studio_next_design_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("studio_publish_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(1, savedItems.value.size)
        assertEquals(1, onSavedCalls)
        assertEquals(ROUTE_MEHFIL, currentRouteAfterSave)
        composeRule.onNodeWithTag("mehfil_home_marker").assertIsDisplayed()
    }
}
