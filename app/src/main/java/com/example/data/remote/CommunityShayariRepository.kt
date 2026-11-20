package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.CommunityShayari
import com.example.data.model.PoetFollower
import com.example.data.model.PoetProfile
import com.example.data.model.UserProfile
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

class CommunityShayariRepository(
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

    fun observePublishedShayari(): Flow<List<CommunityShayari>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_COMMUNITY
        emitAll(
            db.collection(COLLECTION_COMMUNITY)
                .whereEqualTo("status", "published")
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CommunityShayari::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
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

    fun observePublicPoetProfiles(): Flow<List<PoetProfile>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_POET_PROFILES
        emitAll(
            db.collection(COLLECTION_POET_PROFILES)
                .whereEqualTo("isPublic", true)
                .limit(150)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PoetProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<PoetProfile> { it.followerCount }
                            .thenByDescending { it.postCount }
                            .thenByDescending { it.updatedAt ?: Timestamp(0, 0) }
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

    fun observeFollowingIds(userId: String): Flow<Set<String>> = flow {
        if (userId.isBlank()) {
            emit(emptySet())
            return@flow
        }
        val path = "users/$userId/$SUBCOLLECTION_FOLLOWING"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection(SUBCOLLECTION_FOLLOWING)
                .snapshots()
                .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
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

    fun observePoetFollowers(poetUserId: String): Flow<List<PoetFollower>> = flow {
        if (poetUserId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val uid = requireUserId()
        val path = "$COLLECTION_POET_PROFILES/$poetUserId/$SUBCOLLECTION_FOLLOWERS"
        emitAll(
            db.collection(COLLECTION_POET_PROFILES)
                .document(poetUserId)
                .collection(SUBCOLLECTION_FOLLOWERS)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PoetFollower::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
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

    fun observeUserWahIds(userId: String): Flow<Set<String>> = flow {
        val path = "users/$userId/wahs"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("wahs")
                .snapshots()
                .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
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

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users")
                .document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun publishShayari(
        penName: String,
        couplet: String,
        categoryId: String,
        promptTitle: String
    ): Result<String> = withContext(ioDispatcher) {
        var docPath = COLLECTION_COMMUNITY
        try {
            val uid = requireUserId()
            val cleanPenName = penName.trim().ifBlank { "शायर दोस्त" }.take(40)
            val cleanCouplet = couplet.trim().take(500)
            val cleanPrompt = promptTitle.trim().take(120)

            if (cleanCouplet.length < 10) {
                return@withContext Result.failure(
                    IllegalArgumentException("कृपया कम से कम 10 अक्षरों की शायरी लिखें।")
                )
            }

            val docRef = db.collection(COLLECTION_COMMUNITY).document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "penName" to cleanPenName,
                "couplet" to cleanCouplet,
                "categoryId" to categoryId,
                "promptTitle" to cleanPrompt,
                "status" to "published",
                "wahCount" to 0L,
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

    suspend fun updateShayari(
        shayariId: String,
        penName: String,
        couplet: String,
        categoryId: String,
        promptTitle: String
    ): Result<Unit> = withContext(ioDispatcher) {
        var docPath = "$COLLECTION_COMMUNITY/$shayariId"
        try {
            requireUserId()
            val cleanCouplet = couplet.trim().take(500)
            if (cleanCouplet.length < 10) {
                return@withContext Result.failure(
                    IllegalArgumentException("कृपया कम से कम 10 अक्षरों की शायरी लिखें।")
                )
            }
            val docRef = db.collection(COLLECTION_COMMUNITY).document(shayariId)
            docPath = docRef.path
            val updates = mapOf(
                "penName" to penName.trim().ifBlank { "शायर दोस्त" }.take(40),
                "couplet" to cleanCouplet,
                "categoryId" to categoryId,
                "promptTitle" to promptTitle.trim().take(120),
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

    suspend fun deleteShayari(shayariId: String): Result<Unit> = withContext(ioDispatcher) {
        var docPath = "$COLLECTION_COMMUNITY/$shayariId"
        try {
            requireUserId()
            val docRef = db.collection(COLLECTION_COMMUNITY).document(shayariId)
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

    suspend fun toggleWah(shayariId: String, currentlyWahed: Boolean): Result<Unit> {
        val uid = requireUserId()
        val postRef = db.collection(COLLECTION_COMMUNITY).document(shayariId)
        val wahRef = db.collection("users").document(uid).collection("wahs").document(shayariId)
        return try {
            db.runBatch { batch ->
                if (currentlyWahed) {
                    batch.delete(wahRef)
                    batch.update(postRef, "wahCount", FieldValue.increment(-1L))
                } else {
                    val wahPayload = mapOf(
                        "shayariId" to shayariId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(wahRef, wahPayload)
                    batch.update(postRef, "wahCount", FieldValue.increment(1L))
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, postRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(
        targetUserId: String,
        targetPenName: String,
        actorName: String,
        currentlyFollowing: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        if (targetUserId.isBlank() || targetUserId == uid) {
            return Result.failure(IllegalArgumentException("Cannot follow self or empty user."))
        }
        val cleanTargetName = targetPenName.trim().ifBlank { "शायर दोस्त" }.take(40)
        val cleanActorName = actorName.trim().ifBlank { "शायर दोस्त" }.take(40)

        val followingRef = db.collection("users").document(uid)
            .collection(SUBCOLLECTION_FOLLOWING).document(targetUserId)
        val targetProfileRef = db.collection(COLLECTION_POET_PROFILES).document(targetUserId)
        val actorProfileRef = db.collection(COLLECTION_POET_PROFILES).document(uid)
        val followerRef = targetProfileRef.collection(SUBCOLLECTION_FOLLOWERS).document(uid)

        return try {
            val targetSnap = targetProfileRef.get().await()
            val actorSnap = actorProfileRef.get().await()
            val targetExists = targetSnap.exists()
            val actorExists = actorSnap.exists()
            val targetFollowerCount = targetSnap.getLong("followerCount") ?: 0L
            val actorFollowingCount = actorSnap.getLong("followingCount") ?: 0L

            db.runBatch { batch ->
                if (currentlyFollowing) {
                    batch.delete(followingRef)
                    batch.delete(followerRef)
                    if (targetExists && targetFollowerCount > 0L) {
                        batch.update(targetProfileRef, "followerCount", FieldValue.increment(-1L))
                    }
                    if (actorExists && actorFollowingCount > 0L) {
                        batch.update(
                            actorProfileRef,
                            mapOf(
                                "followingCount" to FieldValue.increment(-1L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                } else {
                    batch.set(
                        followingRef,
                        mapOf(
                            "targetUserId" to targetUserId,
                            "targetPenName" to cleanTargetName,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.set(
                        followerRef,
                        mapOf(
                            "followerUserId" to uid,
                            "followerName" to cleanActorName,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    if (targetExists) {
                        batch.update(targetProfileRef, "followerCount", FieldValue.increment(1L))
                    }
                    if (actorExists) {
                        batch.update(
                            actorProfileRef,
                            mapOf(
                                "followingCount" to FieldValue.increment(1L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, targetProfileRef.path)
            Result.failure(e)
        }
    }

    suspend fun ensurePublicPoetProfile(
        defaultPenName: String,
        defaultBio: String = "",
        postCount: Int = 0
    ): Result<Unit> {
        val uid = requireUserId()
        val poetRef = db.collection(COLLECTION_POET_PROFILES).document(uid)
        val cleanName = defaultPenName.trim().ifBlank { "शायर दोस्त" }.take(40).let {
            if (it.length < 2) "शायर दोस्त" else it
        }
        val cleanBio = defaultBio.trim().take(160)
        val cleanPostCount = postCount.toLong().coerceAtLeast(0L)
        return try {
            val snap = poetRef.get().await()
            if (!snap.exists()) {
                poetRef.set(
                    mapOf(
                        "userId" to uid,
                        "penName" to cleanName,
                        "bio" to cleanBio,
                        "isPublic" to true,
                        "followerCount" to 0L,
                        "followingCount" to 0L,
                        "postCount" to cleanPostCount,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val existingPostCount = snap.getLong("postCount") ?: 0L
                if (existingPostCount != cleanPostCount) {
                    poetRef.update(
                        mapOf(
                            "postCount" to cleanPostCount,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, poetRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertUserProfile(
        penName: String,
        bio: String,
        postCount: Int? = null,
        socialLinks: com.example.data.model.SocialLinks? = null
    ): Result<Unit> = withContext(ioDispatcher) {
        var docPath = "users/unknown"
        try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val poetRef = db.collection(COLLECTION_POET_PROFILES).document(uid)
            val cleanPenName = penName.trim().ifBlank { "शायर दोस्त" }.take(40).let {
                if (it.length < 2) "शायर दोस्त" else it
            }
            val cleanBio = bio.trim().take(160)
            val normalizedSocials = socialLinks?.normalized()
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "penName" to cleanPenName,
                        "bio" to cleanBio,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "penName" to cleanPenName,
                        "bio" to cleanBio,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val existingPoetSnap = poetRef.get().await()
            if (existingPoetSnap.exists()) {
                val poetUpdates = mutableMapOf<String, Any>(
                    "penName" to cleanPenName,
                    "bio" to cleanBio,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (postCount != null) {
                    poetUpdates["postCount"] = postCount.toLong().coerceAtLeast(0L)
                }
                if (normalizedSocials != null) {
                    poetUpdates["facebookUrl"] = normalizedSocials.facebookUrl
                    poetUpdates["instagramUrl"] = normalizedSocials.instagramUrl
                    poetUpdates["whatsappUrl"] = normalizedSocials.whatsappUrl
                    poetUpdates["twitterUrl"] = normalizedSocials.twitterUrl
                    poetUpdates["telegramUrl"] = normalizedSocials.telegramUrl
                    poetUpdates["youtubeUrl"] = normalizedSocials.youtubeUrl
                }
                poetRef.update(poetUpdates).await()
            } else {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "penName" to cleanPenName,
                    "bio" to cleanBio,
                    "isPublic" to true,
                    "followerCount" to 0L,
                    "followingCount" to 0L,
                    "postCount" to (postCount?.toLong()?.coerceAtLeast(0L) ?: 0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (normalizedSocials != null) {
                    createPayload["facebookUrl"] = normalizedSocials.facebookUrl
                    createPayload["instagramUrl"] = normalizedSocials.instagramUrl
                    createPayload["whatsappUrl"] = normalizedSocials.whatsappUrl
                    createPayload["twitterUrl"] = normalizedSocials.twitterUrl
                    createPayload["telegramUrl"] = normalizedSocials.telegramUrl
                    createPayload["youtubeUrl"] = normalizedSocials.youtubeUrl
                }
                poetRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_COMMUNITY = "community_shayari"
        const val COLLECTION_POET_PROFILES = "poet_profiles"
        const val SUBCOLLECTION_FOLLOWING = "following"
        const val SUBCOLLECTION_FOLLOWERS = "followers"
    }
}
