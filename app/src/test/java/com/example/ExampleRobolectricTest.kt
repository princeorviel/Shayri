package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ShayariDatabase
import com.example.data.local.ShayariRepository
import com.example.data.local.ShayariSeedData
import com.example.data.model.ShayariCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
        assertEquals("शायरी", appName)
    }

    @Test
    fun `catalog has 30000 unique shayari across all 32 categories with valid background drawables`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val items = ShayariSeedData.allShayari
        assertEquals(30_000, items.size)
        assertEquals(30_000, items.map { it.id }.distinct().size)
        assertEquals(30_000, items.map { it.couplet.trim() }.distinct().size)

        items.forEach { item ->
            assertTrue("Couplet blank for id=${item.id}", item.couplet.isNotBlank())
            assertTrue("Poet blank for id=${item.id}", item.poet.isNotBlank())
            assertTrue("Mood blank for id=${item.id}", item.moodTag.isNotBlank())
            assertTrue(
                "Legacy poet label found for id=${item.id}: ${item.poet}",
                item.poet != "लोकप्रिय शायरी" && item.poet != "अज्ञात शायर"
            )
        }

        val grouped = items.groupBy { it.categoryId }
        assertEquals(ShayariCategory.entries.map { it.id }.toSet(), grouped.keys)

        grouped.forEach { (catId, list) ->
            assertTrue(
                "Expected at least 800 shayari in $catId, got ${list.size}",
                list.size >= 800
            )
        }

        val newItems = items.filter { it.id > 10_000 }
        assertEquals(20_000, newItems.size)
        val fourOrMoreLinesCount = newItems.count { it.couplet.lines().size >= 4 }
        assertTrue(
            "Expected mostly 4+ liners in new 20k catalog, got $fourOrMoreLinesCount / 20000",
            fourOrMoreLinesCount >= 18_000
        )

        val combinedNewText = newItems.joinToString("\n") { it.couplet }
        val expectedKeywordGroups = listOf(
            listOf("कायनात"),
            listOf("खुदा", "ख़ुदा"),
            listOf("रब"),
            listOf("जिंदगी", "ज़िंदगी"),
            listOf("आंसू", "आँसू"),
            listOf("तन्हाई"),
            listOf("जनाजे", "जनाज़े"),
            listOf("इबादत"),
            listOf("साकी", "साक़ी"),
            listOf("मयखाना", "मयख़ाना", "मयखाने", "मयख़ाने"),
            listOf("तलबगार"),
            listOf("राही", "रही"),
            listOf("मुसाफिर", "मुसाफ़िर"),
            listOf("सनम"),
            listOf("कसम", "क़सम"),
            listOf("गवाह"),
            listOf("अदालत"),
            listOf("इंतजार", "इंतज़ार"),
            listOf("आँखें", "आँखों")
        )
        expectedKeywordGroups.forEach { forms ->
            assertTrue(
                "Expected keyword group $forms to appear in new 20k catalog",
                forms.any { combinedNewText.contains(it) }
            )
        }

        ShayariCategory.entries.forEach { category ->
            val primaryDrawable = context.resources.getDrawable(category.backgroundResId, null)
            assertNotNull("Missing primary drawable for ${category.id}", primaryDrawable)
            for (sampleId in 1..6) {
                val variedResId = category.backgroundFor(sampleId)
                assertNotNull(
                    "Missing pooled drawable $variedResId for ${category.id}",
                    context.resources.getDrawable(variedResId, null)
                )
            }
        }
    }

    @Test
    fun `room repository seeds all 30000 shayari cleanly preserves favorites and normalizes legacy poets`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShayariDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.shayariDao()
            val repo = ShayariRepository(dao)
            // Simulate legacy 10,000-row install with legacy poet names and a user favorite
            val legacy10k = ShayariSeedData.allShayari.take(10_000).mapIndexed { index, item ->
                when (index) {
                    4 -> item.copy(poet = "लोकप्रिय शायरी")
                    5 -> item.copy(poet = "अज्ञात शायर")
                    else -> item
                }
            }
            dao.insertAllChunked(legacy10k)
            dao.updateFavorite(id = 1, isFavorite = true)
            assertEquals(10_000, dao.getCount())

            // Upgrade seed should backfill 10001..30000 -> 30000 total, preserve favorites, and normalize poets
            repo.ensureSeeded()
            assertEquals(30_000, dao.getCount())

            val allRows = repo.allShayari.first()
            assertEquals(30_000, allRows.size)
            assertTrue("Favorite flag should be preserved on upgrade", allRows.first { it.id == 1 }.isFavorite)
            assertEquals("_ शायरी", allRows[4].poet)
            assertEquals("_ शायरी", allRows[5].poet)
            assertTrue(
                "No legacy poet labels should remain after ensureSeeded",
                allRows.none { it.poet == "लोकप्रिय शायरी" || it.poet == "अज्ञात शायर" }
            )
            assertTrue("Adultery category should be seeded in Room", allRows.any { it.categoryId == ShayariCategory.ADULTERY.id })
        } finally {
            db.close()
        }
    }

    @Test
    fun `local personal shayari repository saves edits deletes without login and migrates to cloud on sign in`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShayariDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val localRepo = com.example.data.local.LocalPersonalShayariRepository(db.localPersonalShayariDao())
            val savedId = localRepo.savePersonalShayari(
                title = "बिना लॉगिन पहली शायरी",
                themeTag = "ज़िंदगी",
                couplet = "खुद अपने कलम से लिखो अपना नया अफ़साना,\nबिना किसी इजाज़त के सजाओ अपना आशियाना।"
            ).getOrThrow()

            val afterSave = localRepo.observePersonalShayari(com.example.data.model.LOCAL_GUEST_USER_ID).first()
            assertEquals(1, afterSave.size)
            assertEquals("बिना लॉगिन पहली शायरी", afterSave.first().title)

            localRepo.updatePersonalShayari(
                shayariId = savedId,
                title = "अपडेटेड शायरी",
                themeTag = "सुकून",
                couplet = "हर शाम नई रोशनी से महकेगा यह सफ़र,\nअल्फ़ाज़ बने रहेंगे दिलों में ता-उम्र।"
            ).getOrThrow()

            val afterUpdate = localRepo.observePersonalShayari(com.example.data.model.LOCAL_GUEST_USER_ID).first()
            assertEquals("अपडेटेड शायरी", afterUpdate.first().title)

            val migratedTitles = mutableListOf<String>()
            val fakeCloudStore = object : com.example.data.remote.PersonalShayariStore {
                override fun observePersonalShayari(userId: String) = kotlinx.coroutines.flow.emptyFlow<List<com.example.data.model.PersonalShayari>>()
                override suspend fun savePersonalShayari(title: String, themeTag: String, couplet: String): Result<String> {
                    migratedTitles.add(title)
                    return Result.success("cloud-1")
                }
                override suspend fun updatePersonalShayari(shayariId: String, title: String, themeTag: String, couplet: String) = Result.success(Unit)
                override suspend fun deletePersonalShayari(shayariId: String) = Result.success(Unit)
            }

            val migratedCount = localRepo.migrateAllToCloud(fakeCloudStore)
            assertEquals(1, migratedCount)
            assertEquals(listOf("अपडेटेड शायरी"), migratedTitles)
            assertEquals(0, localRepo.observePersonalShayari(com.example.data.model.LOCAL_GUEST_USER_ID).first().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `local personal shayari saves cleanly with blank optional title and tag and short couplet`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShayariDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val localRepo = com.example.data.local.LocalPersonalShayariRepository(db.localPersonalShayariDao())

            // Reproduces exact Write Studio flow where optional title & tag are left blank
            val savedId = localRepo.savePersonalShayari(
                title = "",
                themeTag = "",
                couplet = "तेरी यादों का सफ़र कभी थमता नहीं"
            ).getOrThrow()

            // Also verify short 5-character shayari saves cleanly instead of crashing
            val shortId = localRepo.savePersonalShayari(
                title = "",
                themeTag = "",
                couplet = "इश्क़ है"
            ).getOrThrow()

            val items = localRepo.observePersonalShayari(com.example.data.model.LOCAL_GUEST_USER_ID).first()
            assertEquals(2, items.size)
            assertTrue(items.all { it.themeTag == com.example.data.local.LocalPersonalShayariRepository.DEFAULT_THEME_TAG })

            // Verify update with blank title/tag also succeeds without throwing
            val updateResult = localRepo.updatePersonalShayari(
                shayariId = savedId,
                title = "",
                themeTag = "",
                couplet = "दिल की बात लफ़्ज़ों में उतर आई"
            )
            assertTrue("Update without optional title/tag should succeed", updateResult.isSuccess)

            // Too short (< 4 chars) returns Result.failure rather than throwing an uncaught exception
            val tooShortResult = localRepo.savePersonalShayari(
                title = "",
                themeTag = "",
                couplet = "हाय"
            )
            assertTrue("Too-short couplet should return failure Result cleanly", tooShortResult.isFailure)
            assertTrue(shortId.isNotBlank())
        } finally {
            db.close()
        }
    }

    @Test
    fun `local login session persists immediately on startup across fresh repository and ViewModel instances`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShayariDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val prefsRepo1 = com.example.data.local.UserPreferencesRepository(context)
            prefsRepo1.registerLocalAccount(
                fullName = "प्रिंस शायर",
                username = "princeorviel",
                contact = "princeorviel@gmail.com",
                password = "secret123",
                bio = "दिल से लिखने वाला",
                socialLinks = com.example.data.model.SocialLinks(instagramUrl = "@princeorviel")
            )

            // Simulate closing and reopening the app with brand-new instances
            val prefsRepo2 = com.example.data.local.UserPreferencesRepository(context)
            val shayariRepo2 = ShayariRepository(db.shayariDao())
            val vm2 = com.example.ui.viewmodel.ShayariViewModel(
                repository = shayariRepo2,
                preferencesRepository = prefsRepo2,
                autoSeed = false
            )

            // Must be logged in IMMEDIATELY on frame 0 before Room even emits
            val frame0Account = vm2.uiState.value.preferences.localAccount
            assertTrue("Account should remain registered on relaunch", frame0Account.isRegistered)
            assertTrue("Account should remain logged in immediately on relaunch", frame0Account.isLoggedIn)
            assertEquals("प्रिंस शायर", frame0Account.fullName)
            assertEquals("princeorviel", frame0Account.username)
            assertEquals("princeorviel@gmail.com", frame0Account.contact)
            assertTrue(frame0Account.socialLinks.instagramUrl.contains("princeorviel"))

            // Also verify preferencesFlow stays logged in after DataStore emission
            val emittedAccount = prefsRepo2.preferencesFlow.first().localAccount
            assertTrue("DataStore flow should preserve logged-in state", emittedAccount.isLoggedIn)
            assertEquals("प्रिंस शायर", emittedAccount.fullName)

            // Explicit logout persists cleanly while keeping account registered
            prefsRepo2.logoutLocalAccount()
            val prefsRepo3 = com.example.data.local.UserPreferencesRepository(context)
            val afterLogout = prefsRepo3.readCachedPreferencesSync().localAccount
            assertTrue("Account should remain registered after logout", afterLogout.isRegistered)
            assertTrue("Account should be logged out after explicit logout", !afterLogout.isLoggedIn)

            // Logging back in persists across next restart
            val loginResult = prefsRepo3.loginLocalAccount("princeorviel", "secret123")
            assertEquals(com.example.data.local.LoginLocalResult.Success, loginResult)
            val prefsRepo4 = com.example.data.local.UserPreferencesRepository(context)
            assertTrue(
                "Account should be logged in again on next launch",
                prefsRepo4.readCachedPreferencesSync().localAccount.isLoggedIn
            )
        } finally {
            db.close()
        }
    }
}
