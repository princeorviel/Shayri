package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.CommunityReel
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CommunityReelsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observePublishedReels(): Flow<List<CommunityReel>> = flow {
        requireUserId()
        val path = COLLECTION_COMMUNITY_REELS
        emitAll(
            db.collection(COLLECTION_COMMUNITY_REELS)
                .whereEqualTo("status", "published")
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CommunityReel::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError != null) {
                        handleFirestoreError(firestoreError, OperationType.LIST, path)
                    } else if (error is CancellationException) {
                        throw error
                    } else {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeUserLikedReelIds(): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/$SUBCOLLECTION_REEL_LIKES"
        emitAll(
            db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_REEL_LIKES)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapTo(HashSet()) { it.id }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError != null) {
                        handleFirestoreError(firestoreError, OperationType.LIST, path)
                    } else if (error is CancellationException) {
                        throw error
                    } else {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    emit(emptySet())
                }
        )
    }

    suspend fun publishReel(
        reelId: String,
        authorName: String,
        caption: String,
        tags: List<String>,
        mediaType: String,
        backgroundPreset: String,
        thumbnailBase64: String,
        voicePersonaId: String,
        bgmTrackId: String
    ): Result<String> = withContext(ioDispatcher) {
        val cleanDocId = reelId.trim().replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(100)
            .ifBlank { "reel_${System.currentTimeMillis()}" }
        val path = "$COLLECTION_COMMUNITY_REELS/$cleanDocId"
        try {
            val uid = requireUserId()
            val cleanAuthor = authorName.trim().take(40).ifBlank { "शायर दोस्त" }
            val cleanCaption = caption.trim().take(400)
            val cleanTags = tags
                .map { it.trim().removePrefix("#").take(30) }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .take(10)
            val normalizedMediaType = when (mediaType) {
                "video", "photo", "text" -> mediaType
                else -> "text"
            }
            val cleanBackdrop = backgroundPreset.trim().take(40)
            val cleanThumb = thumbnailBase64.take(640_000)
            val cleanVoice = voicePersonaId.trim().take(40)
            val cleanBgm = bgmTrackId.trim().take(40)

            if (cleanCaption.isEmpty() && cleanThumb.isEmpty() && normalizedMediaType == "text") {
                return@withContext Result.failure(
                    IllegalArgumentException("Reel में कुछ कैप्शन या मीडिया होना ज़रूरी है")
                )
            }

            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "authorName" to cleanAuthor,
                "caption" to cleanCaption,
                "tags" to cleanTags,
                "mediaType" to normalizedMediaType,
                "backgroundPreset" to cleanBackdrop,
                "thumbnailBase64" to cleanThumb,
                "voicePersonaId" to cleanVoice,
                "bgmTrackId" to cleanBgm,
                "likeCount" to 0,
                "status" to "published",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection(COLLECTION_COMMUNITY_REELS)
                .document(cleanDocId)
                .set(payload)
                .await()
            Result.success(cleanDocId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteReel(reelId: String): Result<Unit> = withContext(ioDispatcher) {
        val cleanId = reelId.trim()
        if (cleanId.isEmpty()) return@withContext Result.success(Unit)
        val path = "$COLLECTION_COMMUNITY_REELS/$cleanId"
        try {
            requireUserId()
            db.collection(COLLECTION_COMMUNITY_REELS)
                .document(cleanId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleLike(reelId: String, currentlyLiked: Boolean): Result<Boolean> = withContext(ioDispatcher) {
        val cleanId = reelId.trim()
        if (cleanId.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Empty reelId"))
        val path = "$COLLECTION_COMMUNITY_REELS/$cleanId"
        try {
            val uid = requireUserId()
            val reelRef = db.collection(COLLECTION_COMMUNITY_REELS).document(cleanId)
            val likeRef = db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_REEL_LIKES)
                .document(cleanId)

            val batch = db.batch()
            if (currentlyLiked) {
                batch.delete(likeRef)
                batch.update(reelRef, mapOf("likeCount" to FieldValue.increment(-1L)))
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "reelId" to cleanId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(reelRef, mapOf("likeCount" to FieldValue.increment(1L)))
            }
            batch.commit().await()
            Result.success(!currentlyLiked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_COMMUNITY_REELS = "community_reels"
        const val COLLECTION_USERS = "users"
        const val SUBCOLLECTION_REEL_LIKES = "reel_likes"
    }
}
