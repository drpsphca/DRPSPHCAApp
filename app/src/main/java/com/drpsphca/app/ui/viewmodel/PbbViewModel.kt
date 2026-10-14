package com.drpsphca.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drpsphca.app.data.pbb.AnnouncementPost
import com.drpsphca.app.data.pbb.BoardIndex
import com.drpsphca.app.data.pbb.PbbRepository
import com.drpsphca.app.data.pbb.SeasonPack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PbbSection {
    LIVE_UPDATES,
    ANNOUNCEMENTS
}

enum class LiveSubTab {
    OVERVIEW,
    HOUSEMATES,
    NOMINATIONS,
    WEEKLY_TASKS
}

data class SeasonDropdownItem(
    val id: String,
    val label: String
)

data class PbbUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedSection: PbbSection = PbbSection.LIVE_UPDATES,
    val selectedLiveSubTab: LiveSubTab = LiveSubTab.OVERVIEW,
    val boardIndex: BoardIndex? = null,
    val availableSeasons: List<SeasonDropdownItem> = emptyList(),
    val selectedSeasonId: String? = null,
    val announcements: List<AnnouncementPost> = emptyList()
) {
    val currentSeasonPack: SeasonPack?
        get() = if (selectedSeasonId != null && boardIndex != null) {
            boardIndex.seasons[selectedSeasonId]
        } else null
}

class PbbViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PbbRepository(application)

    private val _uiState = MutableStateFlow(PbbUiState())
    val uiState: StateFlow<PbbUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            try {
                val index = repository.getBoardIndex(forceRefresh = forceRefresh)
                val sortedKeys = repository.getSortedSeasonKeys(index.seasons)
                val dropdownItems = sortedKeys.map { key ->
                    SeasonDropdownItem(
                        id = key,
                        label = repository.getSeasonLabel(key, index.seasons[key])
                    )
                }

                val currentSelected = _uiState.value.selectedSeasonId
                val defaultSeasonId = if (currentSelected != null && index.seasons.containsKey(currentSelected)) {
                    currentSelected
                } else {
                    repository.determineDefaultSeason(index.seasons)
                }

                val mergedAnnouncements = repository.getMergedAnnouncements()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        boardIndex = index,
                        availableSeasons = dropdownItems,
                        selectedSeasonId = defaultSeasonId,
                        announcements = mergedAnnouncements
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.localizedMessage ?: "Failed to load PBB Updates."
                    )
                }
            }
        }
    }

    fun selectSection(section: PbbSection) {
        _uiState.update { it.copy(selectedSection = section) }
    }

    fun selectLiveSubTab(tab: LiveSubTab) {
        _uiState.update { it.copy(selectedLiveSubTab = tab) }
    }

    fun selectSeason(seasonId: String) {
        repository.saveSelectedSeason(seasonId)
        _uiState.update { it.copy(selectedSeasonId = seasonId) }
    }

    fun refresh() {
        loadData(forceRefresh = true)
    }

    fun getPhotoUrl(housemateId: String): String? {
        val seasonPack = uiState.value.currentSeasonPack
        val seasonId = uiState.value.selectedSeasonId ?: seasonPack?.season?.id ?: ""
        return repository.getPhotoUrl(seasonId, seasonPack, housemateId)
    }
}
