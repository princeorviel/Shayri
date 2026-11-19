package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
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

data class AdminCategoryStatsSnapshot(
    val countsByCategory: Map<String, Int> = emptyMap(),
    val totalShayariCount: Int = 0,
    val totalWahCount: Long = 0L,
    val activeCategoryCount: Int = 0
)

interface AdminStatsDataSource {
    fun observeCategoryStats(): Flow<AdminCategoryStatsSnapshot>
    suspend fun fetchCategoryStats(): Result<AdminCategoryStatsSnapshot>
}

class AdminDashboardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : AdminStatsDataSource {

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

    override fun observeCategoryStats(): Flow<AdminCategoryStatsSnapshot> = flow {
        val uid = requireUserId()
        val path = COLLECTION_COMMUNITY
        emitAll(
            db.collection(COLLECTION_COMMUNITY)
                .whereEqualTo("status", "published")
                .snapshots()
                .map { snapshot ->
                    val counts = LinkedHashMap<String, Int>()
                    var totalWah = 0L
                    for (doc in snapshot.documents) {
                        val rawCategoryId = doc.getString("categoryId")
                            ?.trim()
                            ?.lowercase()
                            ?.takeIf { it.isNotBlank() }
                            ?: "mohabbat"
                        counts[rawCategoryId] = (counts[rawCategoryId] ?: 0) + 1
                        totalWah += (doc.get("wahCount") as? Number)?.toLong() ?: 0L
                    }
                    AdminCategoryStatsSnapshot(
                        countsByCategory = counts,
                        totalShayariCount = snapshot.size(),
                        totalWahCount = totalWah,
                        activeCategoryCount = counts.count { it.value > 0 }
                    )
                }
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

    override suspend fun fetchCategoryStats(): Result<AdminCategoryStatsSnapshot> {
        val path = COLLECTION_COMMUNITY
        return try {
            requireUserId()
            val snapshot = db.collection(COLLECTION_COMMUNITY)
                .whereEqualTo("status", "published")
                .get()
                .await()

            val counts = LinkedHashMap<String, Int>()
            var totalWah = 0L
            for (doc in snapshot.documents) {
                val rawCategoryId = doc.getString("categoryId")
                    ?.trim()
                    ?.lowercase()
                    ?.takeIf { it.isNotBlank() }
                    ?: "mohabbat"
                counts[rawCategoryId] = (counts[rawCategoryId] ?: 0) + 1
                totalWah += (doc.get("wahCount") as? Number)?.toLong() ?: 0L
            }
            Result.success(
                AdminCategoryStatsSnapshot(
                    countsByCategory = counts,
                    totalShayariCount = snapshot.size(),
                    totalWahCount = totalWah,
                    activeCategoryCount = counts.count { it.value > 0 }
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_COMMUNITY = "community_shayari"
    }
}
