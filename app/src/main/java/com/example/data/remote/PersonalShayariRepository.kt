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
        val uid = requireUserId()
        val cleanTitle = title.trim().take(80)
        val cleanThemeTag = themeTag.trim().take(40)
        val cleanCouplet = couplet.trim().take(600)

        require(cleanTitle.isNotEmpty() || cleanThemeTag.isNotEmpty()) {
            "Title or theme tag is required."
        }
        require(cleanCouplet.length >= 10) {
            "Couplet must be at least 10 characters."
        }

        val docRef = db.collection("users")
            .document(uid)
            .collection(SUBCOLLECTION_PERSONAL)
            .document()

        val payload = mapOf(
            "userId" to uid,
            "title" to cleanTitle,
            "themeTag" to cleanThemeTag,
            "couplet" to cleanCouplet,
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

    override suspend fun updatePersonalShayari(
        shayariId: String,
        title: String,
        themeTag: String,
        couplet: String
    ): Result<Unit> {
        val uid = requireUserId()
        require(shayariId.isNotBlank()) { "Shayari ID must not be blank for update." }
        val cleanTitle = title.trim().take(80)
        val cleanThemeTag = themeTag.trim().take(40)
        val cleanCouplet = couplet.trim().take(600)

        require(cleanTitle.isNotEmpty() || cleanThemeTag.isNotEmpty()) {
            "Title or theme tag is required."
        }
        require(cleanCouplet.length >= 10) {
            "Couplet must be at least 10 characters."
        }

        val docRef = db.collection("users")
            .document(uid)
            .collection(SUBCOLLECTION_PERSONAL)
            .document(shayariId)

        val updates = mapOf(
            "title" to cleanTitle,
            "themeTag" to cleanThemeTag,
            "couplet" to cleanCouplet,
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

    override suspend fun deletePersonalShayari(shayariId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection(SUBCOLLECTION_PERSONAL)
            .document(shayariId)
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
        const val SUBCOLLECTION_PERSONAL = "personal_shayari"
    }
}
