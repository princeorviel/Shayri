package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.PersonalShayari
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

interface PersonalShayariStore {
    fun observePersonalShayari(userId: String): Flow<List<PersonalShayari>>

    suspend fun savePersonalShayari(
        title: String,
        themeTag: String,
        couplet: String
    ): Result<String>

    suspend fun updatePersonalShayari(
        shayariId: String,
        title: String,
        themeTag: String,
        couplet: String
    ): Result<Unit>

    suspend fun deletePersonalShayari(shayariId: String): Result<Unit>
}

class PersonalShayariRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : PersonalShayariStore {
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

    override fun observePersonalShayari(userId: String): Flow<List<PersonalShayari>> = flow {
        val currentUid = requireUserId()
        require(currentUid == userId) { "Authenticated user mismatch for personal collection." }
        val path = "users/$userId/$SUBCOLLECTION_PERSONAL"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection(SUBCOLLECTION_PERSONAL)
                .limit(200)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PersonalShayari::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending {
                        it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)
                    }
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

    override suspend fun savePersonalShayari(
        title: String,
        themeTag: String,
        couplet: String
    ): Result<String> {
        var docPath = "users/unknown/$SUBCOLLECTION_PERSONAL"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(80)
            val rawThemeTag = themeTag.trim().take(40)
            val effectiveThemeTag = if (cleanTitle.isEmpty() && rawThemeTag.isEmpty()) {
                DEFAULT_THEME_TAG
            } else {
                rawThemeTag
            }
            val cleanCouplet = couplet.trim().take(600)

            if (cleanCouplet.length < MIN_CLOUD_COUPLET_LENGTH) {
                return Result.failure(
                    IllegalArgumentException("कृपया कम से कम $MIN_CLOUD_COUPLET_LENGTH अक्षरों की शायरी लिखें।")
                )
            }

            val docRef = db.collection("users")
                .document(uid)
                .collection(SUBCOLLECTION_PERSONAL)
                .document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "themeTag" to effectiveThemeTag,
                "couplet" to cleanCouplet,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun updatePersonalShayari(
        shayariId: String,
        title: String,
        themeTag: String,
        couplet: String
    ): Result<Unit> {
        var docPath = "users/unknown/$SUBCOLLECTION_PERSONAL/$shayariId"
        return try {
            val uid = requireUserId()
            if (shayariId.isBlank()) {
                return Result.failure(IllegalArgumentException("शायरी अपडेट करने के लिए सही ID नहीं मिली।"))
            }
            val cleanTitle = title.trim().take(80)
            val rawThemeTag = themeTag.trim().take(40)
            val effectiveThemeTag = if (cleanTitle.isEmpty() && rawThemeTag.isEmpty()) {
                DEFAULT_THEME_TAG
            } else {
                rawThemeTag
            }
            val cleanCouplet = couplet.trim().take(600)

            if (cleanCouplet.length < MIN_CLOUD_COUPLET_LENGTH) {
                return Result.failure(
                    IllegalArgumentException("कृपया कम से कम $MIN_CLOUD_COUPLET_LENGTH अक्षरों की शायरी लिखें।")
                )
            }

            val docRef = db.collection("users")
                .document(uid)
                .collection(SUBCOLLECTION_PERSONAL)
                .document(shayariId)
            docPath = docRef.path

            val updates = mapOf(
                "title" to cleanTitle,
                "themeTag" to effectiveThemeTag,
                "couplet" to cleanCouplet,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deletePersonalShayari(shayariId: String): Result<Unit> {
        var docPath = "users/unknown/$SUBCOLLECTION_PERSONAL/$shayariId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users")
                .document(uid)
                .collection(SUBCOLLECTION_PERSONAL)
                .document(shayariId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    companion object {
        const val SUBCOLLECTION_PERSONAL = "personal_shayari"
        const val DEFAULT_THEME_TAG = "मेरी शायरी"
        const val MIN_CLOUD_COUPLET_LENGTH = 10
    }
}
