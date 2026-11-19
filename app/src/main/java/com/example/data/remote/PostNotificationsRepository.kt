package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.PostNotification
import com.example.data.model.PostNotificationType
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PostNotificationsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing notifications.")
    }

    fun observeMyNotifications(userId: String): Flow<List<PostNotification>> = flow {
        val path = "users/$userId/$SUBCOLLECTION_NOTIFICATIONS"
        emitAll(
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_NOTIFICATIONS)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents
                        .mapNotNull { it.toPostNotificationOrNull() }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun sendNotification(
        recipientUserId: String,
        actorName: String,
        type: PostNotificationType,
        postId: String,
        postCouplet: String,
        commentText: String = ""
    ): Result<String> {
        val actorUid = requireUserId()
        if (recipientUserId.isBlank() || postId.isBlank()) {
            return Result.failure(IllegalArgumentException("Recipient and postId must not be blank."))
        }
        val cleanActorName = actorName.trim().ifBlank { "शायर दोस्त" }.take(40)
        val cleanExcerpt = postCouplet
            .replace('\n', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "शायरी" }
            .take(140)
        val cleanCommentText = if (type == PostNotificationType.COMMENT) {
            commentText.trim().ifBlank { "कमेंट" }.take(160)
        } else {
            ""
        }

        val docRef = db.collection(COLLECTION_USERS)
            .document(recipientUserId)
            .collection(SUBCOLLECTION_NOTIFICATIONS)
            .document()

        val payload = mapOf(
            "recipientUserId" to recipientUserId,
            "actorUserId" to actorUid,
            "actorName" to cleanActorName,
            "type" to type.value,
            "postId" to postId,
            "postExcerpt" to cleanExcerpt,
            "commentText" to cleanCommentText,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_USERS)
            .document(uid)
            .collection(SUBCOLLECTION_NOTIFICATIONS)
            .document(notificationId)

        val updates = mapOf(
            "isRead" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(unreadNotificationIds: List<String>): Result<Unit> {
        val uid = requireUserId()
        if (unreadNotificationIds.isEmpty()) return Result.success(Unit)
        val colRef = db.collection(COLLECTION_USERS)
            .document(uid)
            .collection(SUBCOLLECTION_NOTIFICATIONS)
        return try {
            unreadNotificationIds.chunked(400).forEach { chunk ->
                db.runBatch { batch ->
                    chunk.forEach { id ->
                        batch.update(
                            colRef.document(id),
                            mapOf(
                                "isRead" to true,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }.await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_USERS)
            .document(uid)
            .collection(SUBCOLLECTION_NOTIFICATIONS)
            .document(notificationId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.toPostNotificationOrNull(): PostNotification? {
        val recipient = getString("recipientUserId") ?: return null
        val actorId = getString("actorUserId") ?: return null
        val actorName = getString("actorName") ?: return null
        val type = getString("type") ?: return null
        val postId = getString("postId") ?: return null
        val postExcerpt = getString("postExcerpt") ?: ""
        val commentText = getString("commentText") ?: ""
        val isRead = getBoolean("isRead") ?: false
        val created = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        val updated = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

        return PostNotification(
            id = id,
            recipientUserId = recipient,
            actorUserId = actorId,
            actorName = actorName,
            type = type,
            postId = postId,
            postExcerpt = postExcerpt,
            commentText = commentText,
            isRead = isRead,
            createdAt = created,
            updatedAt = updated
        )
    }

    companion object {
        const val COLLECTION_USERS = "users"
        const val SUBCOLLECTION_NOTIFICATIONS = "notifications"
    }
}
