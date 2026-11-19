package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.ShayariComment
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ShayariCommentsRepository(
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
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeCommentsForShayari(shayariKey: String): Flow<List<ShayariComment>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_COMMENTS
        emitAll(
            db.collection(COLLECTION_COMMENTS)
                .whereEqualTo("status", "published")
                .whereEqualTo("shayariKey", shayariKey)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ShayariComment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .flowOn(Dispatchers.Default)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCommentCounts(): Flow<Map<String, Int>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_COMMENTS
        emitAll(
            db.collection(COLLECTION_COMMENTS)
                .whereEqualTo("status", "published")
                .limit(500)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents
                        .mapNotNull { it.getString("shayariKey") }
                        .groupingBy { it }
                        .eachCount()
                }
                .flowOn(Dispatchers.Default)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addComment(
        shayariKey: String,
        authorName: String,
        text: String
    ): Result<String> {
        val uid = requireUserId()
        val cleanText = text.trim().take(300)
        if (cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("Comment text cannot be empty."))
        }
        val cleanAuthorName = authorName.trim().take(40).let {
            if (it.length >= 2) it else "शायर दोस्त"
        }
        val docRef = db.collection(COLLECTION_COMMENTS).document()
        val payload = mapOf(
            "shayariKey" to shayariKey,
            "userId" to uid,
            "authorName" to cleanAuthorName,
            "text" to cleanText,
            "status" to "published",
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

    suspend fun deleteComment(commentId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_COMMENTS).document(commentId)
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

    companion object {
        const val COLLECTION_COMMENTS = "shayari_comments"
    }
}
