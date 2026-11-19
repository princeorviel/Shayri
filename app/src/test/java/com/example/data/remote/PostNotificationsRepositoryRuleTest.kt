package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.PostNotificationType
import com.example.data.model.ShayariCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostNotificationsRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun likeCommentShare_crossUser_deliversAndMarksReadInAuthorInbox() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val communityRepo = CommunityShayariRepository(firestore, auth)
        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            communityRepo.publishShayari(
                penName = "अमन मिर्ज़ा",
                couplet = "सितारों से आगे एक नया सवेरा खिलेगा,\nहर मेहनत करने वाले को मुकाम मिलेगा।",
                categoryId = ShayariCategory.HAUSLA.id,
                promptTitle = "हौसले की उड़ान"
            ).getOrThrow()
        }

        // Bob interacts with Alice's post via like, comment, and share
        val bobUid = signInTestUser(BOB_EMAIL)
        val notifRepoAsBob = PostNotificationsRepository(firestore, auth)

        val likeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            notifRepoAsBob.sendNotification(
                recipientUserId = aliceUid,
                actorName = "बॉब शायर",
                type = PostNotificationType.LIKE,
                postId = postId,
                postCouplet = "सितारों से आगे एक नया सवेरा खिलेगा,\nहर मेहनत करने वाले को मुकाम मिलेगा।"
            )
        }
        assertTrue(likeResult.isSuccess)
        val likeNotifId = likeResult.getOrThrow()

        val commentResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            notifRepoAsBob.sendNotification(
                recipientUserId = aliceUid,
                actorName = "बॉब शायर",
                type = PostNotificationType.COMMENT,
                postId = postId,
                postCouplet = "सितारों से आगे एक नया सवेरा खिलेगा,\nहर मेहनत करने वाले को मुकाम मिलेगा।",
                commentText = "वाह, बहुत खूबसूरत शेर!"
            )
        }
        assertTrue(commentResult.isSuccess)

        val shareResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            notifRepoAsBob.sendNotification(
                recipientUserId = aliceUid,
                actorName = "बॉब शायर",
                type = PostNotificationType.SHARE,
                postId = postId,
                postCouplet = "सितारों से आगे एक नया सवेरा खिलेगा,\nहर मेहनत करने वाले को मुकाम मिलेगा।"
            )
        }
        assertTrue(shareResult.isSuccess)

        // Bob cannot list Alice's notifications
        val bobReadException = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                notifRepoAsBob.observeMyNotifications(aliceUid).first()
            }
        }.exceptionOrNull()
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            bobReadException?.unwrapFirestoreException()?.code
        )

        // Alice receives all 3 notifications, marks one read, and deletes one
        signInTestUser(ALICE_EMAIL)
        val notifRepoAsAlice = PostNotificationsRepository(firestore, auth)
        val inbox = withTimeout(FLOW_TIMEOUT_MS) {
            notifRepoAsAlice.observeMyNotifications(aliceUid).first { list ->
                list.any { it.id == likeNotifId } && list.size >= 3
            }
        }
        val likeItem = inbox.first { it.id == likeNotifId }
        assertEquals(bobUid, likeItem.actorUserId)
        assertEquals(PostNotificationType.LIKE, likeItem.notificationType)
        assertFalse(likeItem.isRead)

        assertTrue(
            inbox.any {
                it.notificationType == PostNotificationType.COMMENT &&
                    it.commentText == "वाह, बहुत खूबसूरत शेर!"
            }
        )
        assertTrue(inbox.any { it.notificationType == PostNotificationType.SHARE })

        val markReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            notifRepoAsAlice.markAsRead(likeNotifId)
        }
        assertTrue(markReadResult.isSuccess)

        val updatedInbox = withTimeout(FLOW_TIMEOUT_MS) {
            notifRepoAsAlice.observeMyNotifications(aliceUid).first { list ->
                list.firstOrNull { it.id == likeNotifId }?.isRead == true
            }
        }
        assertTrue(updatedInbox.first { it.id == likeNotifId }.isRead)

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            notifRepoAsAlice.deleteNotification(likeNotifId)
        }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun selfNotificationAndUnauthenticatedAccess_failWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val communityRepo = CommunityShayariRepository(firestore, auth)
        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            communityRepo.publishShayari(
                penName = "अमन मिर्ज़ा",
                couplet = "दिल में जो बात है वो लफ़्ज़ों में आ जाती है,\nसच्ची शायरी अक्सर मन को भा जाती है।",
                categoryId = ShayariCategory.MOHABBAT.id,
                promptTitle = ""
            ).getOrThrow()
        }

        val notifRepo = PostNotificationsRepository(firestore, auth)
        val selfNotifResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            notifRepo.sendNotification(
                recipientUserId = aliceUid,
                actorName = "अमन मिर्ज़ा",
                type = PostNotificationType.LIKE,
                postId = postId,
                postCouplet = "दिल में जो बात है वो लफ़्ज़ों में आ जाती है।"
            )
        }
        assertTrue(selfNotifResult.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            selfNotifResult.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // Unauthenticated direct query rejected
        auth.signOut()
        val unauthException = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                notifRepo.observeMyNotifications(aliceUid).first()
            }
        }.exceptionOrNull()
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            unauthException?.unwrapFirestoreException()?.code
        )
    }

    private companion object {
        const val ALICE_EMAIL = "alice_notif@test.com"
        const val BOB_EMAIL = "bob_notif@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
