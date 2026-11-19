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

class PersonalShayariRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObservePersonalShayari_owner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = PersonalShayariRepository(firestore, auth)

        val uniqueSuffix = UUID.randomUUID().toString().take(8)
        val title = "मेरी पहली डायरी $uniqueSuffix"
        val themeTag = "प्रेरणा"
        val couplet = "हर सुबह एक नया पैग़ाम लाती है $uniqueSuffix,\nउम्मीद की किरण दिल को जगाती है।"

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.savePersonalShayari(
                title = title,
                themeTag = themeTag,
                couplet = couplet
            )
        }
        assertTrue(
            "savePersonalShayari failed: ${saveResult.exceptionOrNull()?.stackTraceToString()}",
            saveResult.isSuccess
        )
        val docId = saveResult.getOrThrow()

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePersonalShayari(uid).first { list -> list.any { it.id == docId } }
        }
        val saved = observed.first { it.id == docId }
        assertEquals(uid, saved.userId)
        assertEquals(title, saved.title)
        assertEquals(themeTag, saved.themeTag)
        assertEquals(couplet, saved.couplet)
    }

    @Test
    fun updateAndDeletePersonalShayari_owner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = PersonalShayariRepository(firestore, auth)

        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.savePersonalShayari(
                title = "बरसात की शाम",
                themeTag = "प्रकृति",
                couplet = "भीगी हवाओं में कुछ पुराने नग़्मे गूँजे,\nदिल के कोने में फिर नए ख़्वाब जागे।"
            ).getOrThrow()
        }

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updatePersonalShayari(
                shayariId = docId,
                title = "बारिश और सुकून",
                themeTag = "सुकून",
                couplet = "भीगी हवाओं में आज सुकून के रंग घुले हैं,\nआसमाँ के नीचे मन के सारे दरवाज़े खुले हैं।"
            )
        }
        assertTrue(
            "updatePersonalShayari failed: ${updateResult.exceptionOrNull()?.stackTraceToString()}",
            updateResult.isSuccess
        )

        val updatedList = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePersonalShayari(uid).first { list ->
                list.any { it.id == docId && it.title == "बारिश और सुकून" }
            }
        }
        assertEquals("सुकून", updatedList.first { it.id == docId }.themeTag)

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.deletePersonalShayari(docId)
        }
        assertTrue(deleteResult.isSuccess)

        val afterDelete = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePersonalShayari(uid).first { list -> list.none { it.id == docId } }
        }
        assertFalse(afterDelete.any { it.id == docId })
    }

    @Test
    fun personalShayari_crossUserReadWrite_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = PersonalShayariRepository(firestore, auth)
        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.savePersonalShayari(
                title = "निजी पंक्तियाँ",
                themeTag = "तन्हाई",
                couplet = "ख़ामोश रातों में सितारे भी गुनगुनाते हैं,\nअनकहे जज़्बात पन्नों पर उतर आते हैं।"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)

        var readThrown: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("users")
                    .document(aliceUid)
                    .collection(PersonalShayariRepository.SUBCOLLECTION_PERSONAL)
                    .document(docId)
                    .get()
                    .await()
            }
        } catch (t: Throwable) {
            readThrown = t
        }
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            readThrown?.unwrapFirestoreException()?.code
        )

        var writeThrown: Throwable? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users")
                    .document(aliceUid)
                    .collection(PersonalShayariRepository.SUBCOLLECTION_PERSONAL)
                    .document(docId)
                    .delete()
                    .await()
            }
        } catch (t: Throwable) {
            writeThrown = t
        }
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            writeThrown?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun observePersonalShayari_unauthenticated_failsFastWithoutSession() = runBlocking {
        auth.signOut()
        val repo = PersonalShayariRepository(firestore, auth)
        var thrown: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observePersonalShayari("any_uid").first()
            }
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown is IllegalStateException)
    }

    @Test
    fun directUnauthenticatedPersonalQuery_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        var thrown: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("users")
                    .document("some_user")
                    .collection(PersonalShayariRepository.SUBCOLLECTION_PERSONAL)
                    .get()
                    .await()
            }
        } catch (t: Throwable) {
            thrown = t
        }
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            thrown?.unwrapFirestoreException()?.code
        )
    }

    private companion object {
        const val ALICE_EMAIL = "alice_personal@test.com"
        const val BOB_EMAIL = "bob_personal@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
