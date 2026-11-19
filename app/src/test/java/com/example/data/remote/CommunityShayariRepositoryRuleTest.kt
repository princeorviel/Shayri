package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ShayariCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityShayariRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun publishAndObserveShayari_authenticatedUser_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = CommunityShayariRepository(firestore, auth)

        val uniqueSuffix = UUID.randomUUID().toString().take(8)
        val couplet = "बारिश की बूँदों में आज फिर तेरा नाम लिखा है $uniqueSuffix,\nहवाओं ने भी मेरे दिल का पैग़ाम लिखा है।"
        val publishResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.publishShayari(
                penName = "अमन मिर्ज़ा",
                couplet = couplet,
                categoryId = ShayariCategory.PRAKRITI.id,
                promptTitle = "बारिश और यादें"
            )
        }
        assertTrue(publishResult.isSuccess)
        val postId = publishResult.getOrThrow()

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePublishedShayari().first { list -> list.any { it.id == postId } }
        }
        val created = observed.first { it.id == postId }
        assertEquals(uid, created.userId)
        assertEquals(0L, created.wahCount)
    }

    @Test
    fun toggleWah_authenticatedUser_atomicallyUpdatesCountAndUserWahIds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = CommunityShayariRepository(firestore, auth)
        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.publishShayari(
                penName = "सहर",
                couplet = "चाँदनी रात में झील का पानी चमकता है,\nदिल का हर ख़्वाब उम्मीद से महकता है।",
                categoryId = ShayariCategory.PRAKRITI.id,
                promptTitle = ""
            ).getOrThrow()
        }

        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = CommunityShayariRepository(firestore, auth)

        val wahAddResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.toggleWah(shayariId = postId, currentlyWahed = false)
        }
        assertTrue(wahAddResult.isSuccess)

        val wahIdsAfterAdd = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeUserWahIds(bobUid).first { it.contains(postId) }
        }
        assertTrue(wahIdsAfterAdd.contains(postId))

        val postsAfterAdd = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observePublishedShayari().first { list ->
                list.any { it.id == postId && it.wahCount == 1L }
            }
        }
        assertEquals(1L, postsAfterAdd.first { it.id == postId }.wahCount)

        val wahRemoveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.toggleWah(shayariId = postId, currentlyWahed = true)
        }
        assertTrue(wahRemoveResult.isSuccess)

        val wahIdsAfterRemove = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeUserWahIds(bobUid).first { !it.contains(postId) }
        }
        assertFalse(wahIdsAfterRemove.contains(postId))
    }

    @Test
    fun deleteShayari_crossUser_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = CommunityShayariRepository(firestore, auth)
        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.publishShayari(
                penName = "अमन",
                couplet = "ज़िंदगी की राहों में मुस्कुराते चलो यारों,\nहर मोड़ पर नए गीत गुनगुनाते चलो यारों।",
                categoryId = ShayariCategory.ZINDAGI.id,
                promptTitle = ""
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CommunityShayariRepository(firestore, auth)
        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.deleteShayari(postId)
        }
        assertTrue(deleteResult.isFailure)
        val firestoreException = deleteResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observePublishedShayari_unauthenticated_failsFastWithoutSession() = runBlocking {
        auth.signOut()
        val repo = CommunityShayariRepository(firestore, auth)
        var thrown: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observePublishedShayari().first()
            }
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown is IllegalStateException)
    }

    @Test
    fun directUnauthenticatedFirestoreQuery_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        var thrown: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection(CommunityShayariRepository.COLLECTION_COMMUNITY)
                    .whereEqualTo("status", "published")
                    .limit(10)
                    .get()
                    .await()
            }
        } catch (t: Throwable) {
            thrown = t
        }
        val firestoreException = thrown?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_poet@test.com"
        const val BOB_EMAIL = "bob_poet@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
