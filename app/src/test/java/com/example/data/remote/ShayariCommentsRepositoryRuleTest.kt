package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
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

class ShayariCommentsRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addAndObserveComments_authenticatedUser_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = ShayariCommentsRepository(firestore, auth)
        val shayariKey = "seed_test_${UUID.randomUUID().toString().take(8)}"

        val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addComment(
                shayariKey = shayariKey,
                authorName = "अमन",
                text = "बहुत ही खूबसूरत शेर है!"
            )
        }
        assertTrue(addResult.isSuccess)
        val commentId = addResult.getOrThrow()

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCommentsForShayari(shayariKey).first { list ->
                list.any { it.id == commentId }
            }
        }
        val created = observed.first { it.id == commentId }
        assertEquals(uid, created.userId)
        assertEquals(shayariKey, created.shayariKey)
        assertEquals("अमन", created.authorName)
        assertEquals("बहुत ही खूबसूरत शेर है!", created.text)
    }

    @Test
    fun observeCommentCounts_authenticatedUser_aggregatesByKey() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = ShayariCommentsRepository(firestore, auth)
        val keyA = "seed_count_a_${UUID.randomUUID().toString().take(8)}"
        val keyB = "seed_count_b_${UUID.randomUUID().toString().take(8)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addComment(keyA, "अमन", "पहला कमेंट").getOrThrow()
            repo.addComment(keyA, "अमन", "दूसरा कमेंट").getOrThrow()
            repo.addComment(keyB, "अमन", "लाजवाब").getOrThrow()
        }

        val counts = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCommentCounts().first { map ->
                (map[keyA] ?: 0) >= 2 && (map[keyB] ?: 0) >= 1
            }
        }
        assertEquals(2, counts[keyA])
        assertEquals(1, counts[keyB])
    }

    @Test
    fun deleteComment_crossUser_failsWithPermissionDenied_andOwnerSucceeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = ShayariCommentsRepository(firestore, auth)
        val shayariKey = "seed_del_${UUID.randomUUID().toString().take(8)}"
        val commentId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addComment(shayariKey, "अमन", "दिल को छू लेने वाली लाइनें").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = ShayariCommentsRepository(firestore, auth)
        val crossDeleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.deleteComment(commentId)
        }
        assertTrue(crossDeleteResult.isFailure)
        val firestoreException = crossDeleteResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)

        signInTestUser(ALICE_EMAIL)
        val ownerDeleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.deleteComment(commentId)
        }
        assertTrue(ownerDeleteResult.isSuccess)

        val remaining = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeCommentsForShayari(shayariKey).first { list ->
                list.none { it.id == commentId }
            }
        }
        assertFalse(remaining.any { it.id == commentId })
    }

    @Test
    fun observeComments_unauthenticated_failsFastWithoutSession() = runBlocking {
        auth.signOut()
        val repo = ShayariCommentsRepository(firestore, auth)
        var thrown: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeCommentsForShayari("seed_1").first()
            }
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown is IllegalStateException)
    }

    @Test
    fun directUnauthenticatedCommentsQuery_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        var thrown: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection(ShayariCommentsRepository.COLLECTION_COMMENTS)
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
        const val ALICE_EMAIL = "alice_commenter@test.com"
        const val BOB_EMAIL = "bob_commenter@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
