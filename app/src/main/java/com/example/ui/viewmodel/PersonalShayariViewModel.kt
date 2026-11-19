package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.FirebaseShayariAiClient
import com.example.ai.ShayariAiClient
import com.example.ai.ShayariAiUiState
import com.example.ai.ShayariAssistMode
import com.example.ai.ShayariDraftEditorHelper
import com.example.data.model.PersonalShayari
import com.example.data.remote.PersonalShayariStore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PersonalCollectionState {
    data object Loading : PersonalCollectionState
    data class Success(val items: List<PersonalShayari>) : PersonalCollectionState
    data class Error(val message: String) : PersonalCollectionState
}

data class PersonalShayariUiState(
    val collectionState: PersonalCollectionState = PersonalCollectionState.Loading,
    val allItems: List<PersonalShayari> = emptyList(),
    val visibleItems: List<PersonalShayari> = emptyList(),
    val availableThemeTags: List<String> = emptyList(),
    val selectedFilterTag: String? = null,
    val searchQuery: String = "",
    val editingItem: PersonalShayari? = null,
    val titleInput: String = "",
    val themeTagInput: String = "",
    val coupletInput: String = "",
    val externalDraftVersion: Int = 0,
    val isSaving: Boolean = false,
    val aiUiState: ShayariAiUiState = ShayariAiUiState.Idle
)

class PersonalShayariViewModel(
    private val repository: PersonalShayariStore,
    private val currentUserId: String,
    private val aiClient: ShayariAiClient = FirebaseShayariAiClient()
) : ViewModel() {

    private val titleInputFlow = MutableStateFlow("")
    private val themeTagInputFlow = MutableStateFlow("")
    private val coupletInputFlow = MutableStateFlow("")
    private val externalDraftVersionFlow = MutableStateFlow(0)
    private val editingItemFlow = MutableStateFlow<PersonalShayari?>(null)
    private val selectedFilterTagFlow = MutableStateFlow<String?>(null)
    private val searchQueryFlow = MutableStateFlow("")
    private val isSavingFlow = MutableStateFlow(false)
    private val aiUiStateFlow = MutableStateFlow<ShayariAiUiState>(ShayariAiUiState.Idle)
    private var aiJob: Job? = null

    private val itemsFlow: StateFlow<PersonalCollectionState> = repository
        .observePersonalShayari(currentUserId)
        .map<List<PersonalShayari>, PersonalCollectionState> { PersonalCollectionState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing personal shayari", rootError)
            emit(
                PersonalCollectionState.Error(
                    rootError.message ?: "आपकी सेव शायरी लोड करने में दिक्कत आई"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = PersonalCollectionState.Loading
        )

    private val editorFlow = combine(
        editingItemFlow,
        titleInputFlow,
        themeTagInputFlow,
        coupletInputFlow,
        isSavingFlow
    ) { editing, title, tag, couplet, saving ->
        EditorState(editing, title, tag, couplet, saving)
    }

    private val controlsFlow = combine(
        selectedFilterTagFlow,
        searchQueryFlow,
        aiUiStateFlow,
        externalDraftVersionFlow
    ) { tag, query, aiState, draftVersion ->
        ControlsState(tag, query, aiState, draftVersion)
    }

    val uiState: StateFlow<PersonalShayariUiState> = combine(
        itemsFlow,
        editorFlow,
        controlsFlow
    ) { collectionState, editor, controls ->
        val all = (collectionState as? PersonalCollectionState.Success)?.items ?: emptyList()
        val tags = all.mapNotNull { item -> item.themeTag.trim().takeIf { it.isNotEmpty() } }
            .distinct()

        val activeFilterTag = if (controls.filterTag != null && tags.contains(controls.filterTag)) {
            controls.filterTag
        } else {
            null
        }
        val trimmedQuery = controls.searchQuery.trim().lowercase()

        val filtered = all.filter { item ->
            val matchesTag = activeFilterTag == null ||
                item.themeTag.trim().equals(activeFilterTag, ignoreCase = true)
            val matchesQuery = trimmedQuery.isEmpty() ||
                item.title.lowercase().contains(trimmedQuery) ||
                item.themeTag.lowercase().contains(trimmedQuery) ||
                item.couplet.lowercase().contains(trimmedQuery)
            matchesTag && matchesQuery
        }

        PersonalShayariUiState(
            collectionState = collectionState,
            allItems = all,
            visibleItems = filtered,
            availableThemeTags = tags,
            selectedFilterTag = activeFilterTag,
            searchQuery = controls.searchQuery,
            editingItem = editor.editingItem,
            titleInput = editor.title,
            themeTagInput = editor.themeTag,
            coupletInput = editor.couplet,
            externalDraftVersion = controls.externalDraftVersion,
            isSaving = editor.isSaving,
            aiUiState = controls.aiUiState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PersonalShayariUiState()
    )

    fun onTitleChanged(value: String) {
        if (value.length <= 80) {
            titleInputFlow.value = value
        }
    }

    fun onThemeTagChanged(value: String) {
        if (value.length <= 40) {
            themeTagInputFlow.value = value
        }
    }

    fun togglePresetThemeTag(tag: String) {
        themeTagInputFlow.value = if (themeTagInputFlow.value.trim() == tag) "" else tag
    }

    fun onCoupletChanged(value: String) {
        if (value.length <= 600) {
            coupletInputFlow.value = value
        }
    }

    fun selectFilterTag(tag: String?) {
        selectedFilterTagFlow.value = tag
    }

    fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
    }

    fun startEditing(item: PersonalShayari) {
        editingItemFlow.value = item
        titleInputFlow.value = item.title
        themeTagInputFlow.value = item.themeTag
        coupletInputFlow.value = item.couplet
        externalDraftVersionFlow.value += 1
    }

    fun cancelEditing() {
        editingItemFlow.value = null
        titleInputFlow.value = ""
        themeTagInputFlow.value = ""
        coupletInputFlow.value = ""
        aiUiStateFlow.value = ShayariAiUiState.Idle
        externalDraftVersionFlow.value += 1
    }

    fun startCollabDraft(
        sourceCouplet: String,
        sourcePoet: String,
        themeTag: String
    ) {
        val cleanPoet = sourcePoet.trim()
        val cleanCouplet = sourceCouplet.trim()
        val collabHeader = if (cleanPoet.isNotEmpty()) {
            "Collab • $cleanPoet"
        } else {
            "Collab"
        }
        val prefillText = buildString {
            append(cleanCouplet)
            if (cleanPoet.isNotEmpty()) {
                append("\n— ")
                append(cleanPoet)
            }
            append("\n\n")
        }
        editingItemFlow.value = null
        titleInputFlow.value = collabHeader.take(80)
        themeTagInputFlow.value = themeTag.trim().take(40)
        coupletInputFlow.value = prefillText.take(600)
        aiUiStateFlow.value = ShayariAiUiState.Idle
        externalDraftVersionFlow.value += 1
    }

    fun startWriteDraft(themeTag: String = "") {
        editingItemFlow.value = null
        titleInputFlow.value = ""
        themeTagInputFlow.value = themeTag.trim().take(40)
        coupletInputFlow.value = ""
        aiUiStateFlow.value = ShayariAiUiState.Idle
        externalDraftVersionFlow.value += 1
    }

    fun requestAiAssist(mode: ShayariAssistMode) {
        val draft = coupletInputFlow.value
        val themeOrTitle = listOf(themeTagInputFlow.value.trim(), titleInputFlow.value.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" • ")

        if (draft.isBlank() && themeOrTitle.isBlank()) {
            aiUiStateFlow.value = ShayariAiUiState.Error(
                mode = mode,
                message = "सुझाव पाने के लिए पहले कोई शब्द, आधी लाइन या टॉपिक लिखें।"
            )
            return
        }

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            aiUiStateFlow.value = ShayariAiUiState.Loading(mode)
            try {
                val suggestions = aiClient.suggest(
                    mode = mode,
                    draft = draft,
                    themeOrTitle = themeOrTitle
                )
                aiUiStateFlow.value = ShayariAiUiState.Success(mode, suggestions)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI suggestion timed out", e)
                aiUiStateFlow.value = ShayariAiUiState.Error(
                    mode = mode,
                    message = "जवाब आने में ज़्यादा समय लग गया (60 सेकंड)। फिर से कोशिश करें।"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI suggestion failed", e)
                aiUiStateFlow.value = ShayariAiUiState.Error(
                    mode = mode,
                    message = e.localizedMessage ?: "AI से सुझाव लाने में दिक्कत आई।"
                )
            }
        }
    }

    fun insertRhymingWord(word: String) {
        coupletInputFlow.value = ShayariDraftEditorHelper.appendRhymingWord(
            currentDraft = coupletInputFlow.value,
            word = word,
            maxLength = 600
        )
        externalDraftVersionFlow.value += 1
    }

    fun applyLineCompletion(line: String, replaceExisting: Boolean) {
        coupletInputFlow.value = if (replaceExisting) {
            line.trim().take(600)
        } else {
            ShayariDraftEditorHelper.appendLine(
                currentDraft = coupletInputFlow.value,
                line = line,
                maxLength = 600
            )
        }
        externalDraftVersionFlow.value += 1
    }

    fun dismissAiSuggestions() {
        aiJob?.cancel()
        aiUiStateFlow.value = ShayariAiUiState.Idle
    }

    fun saveShayari(
        onSuccess: (wasUpdate: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isSavingFlow.value) return
        val cleanTitle = titleInputFlow.value.trim()
        val cleanTag = themeTagInputFlow.value.trim()
        val cleanCouplet = coupletInputFlow.value.trim()

        if (cleanCouplet.length < 4) {
            onError("कृपया कम से कम कुछ शब्द लिखें")
            return
        }

        val editing = editingItemFlow.value
        viewModelScope.launch {
            isSavingFlow.value = true
            val result = if (editing == null) {
                repository.savePersonalShayari(
                    title = cleanTitle,
                    themeTag = cleanTag,
                    couplet = cleanCouplet
                ).map { Unit }
            } else {
                repository.updatePersonalShayari(
                    shayariId = editing.id,
                    title = cleanTitle,
                    themeTag = cleanTag,
                    couplet = cleanCouplet
                )
            }
            isSavingFlow.value = false
            if (result.isSuccess) {
                editingItemFlow.value = null
                titleInputFlow.value = ""
                themeTagInputFlow.value = ""
                coupletInputFlow.value = ""
                aiUiStateFlow.value = ShayariAiUiState.Idle
                externalDraftVersionFlow.value += 1
                onSuccess(editing != null)
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "शायरी सेव नहीं हो सकी")
            }
        }
    }

    fun deleteShayari(
        shayariId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (editingItemFlow.value?.id == shayariId) {
                cancelEditing()
            }
            val result = repository.deletePersonalShayari(shayariId)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "शायरी हटाई नहीं जा सकी")
            }
        }
    }

    private data class EditorState(
        val editingItem: PersonalShayari?,
        val title: String,
        val themeTag: String,
        val couplet: String,
        val isSaving: Boolean
    )

    private data class ControlsState(
        val filterTag: String?,
        val searchQuery: String,
        val aiUiState: ShayariAiUiState,
        val externalDraftVersion: Int
    )

    class Factory(
        private val repository: PersonalShayariStore,
        private val currentUserId: String,
        private val aiClient: ShayariAiClient = FirebaseShayariAiClient()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PersonalShayariViewModel(repository, currentUserId, aiClient) as T
        }
    }

    private companion object {
        const val TAG = "PersonalShayariVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
