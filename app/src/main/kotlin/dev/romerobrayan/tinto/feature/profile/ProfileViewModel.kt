package dev.romerobrayan.tinto.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.common.MockData
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProfileViewModel @Inject constructor(
    cardRepository: CardRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = cardRepository.observeCards()
        .map { cards ->
            ProfileUiState(
                userName = MockData.USER_NAME,
                userEmail = MockData.USER_EMAIL,
                cards = cards,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ProfileUiState(userName = MockData.USER_NAME, userEmail = MockData.USER_EMAIL),
        )
}
