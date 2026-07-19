package dev.romerobrayan.tinto.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.common.MockData
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.UserSession
import dev.romerobrayan.tinto.core.domain.repository.AuthRepository
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.NotificationCapture
import dev.romerobrayan.tinto.core.domain.repository.SmsCapture
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val analytics: TintoAnalytics,
    private val cardRepository: CardRepository,
    private val smsCapture: SmsCapture,
    private val notificationCapture: NotificationCapture,
) : ViewModel() {

    private data class CardForm(
        val editingCardId: String? = null,
        val bank: String = "",
        val last4: String = "",
        val label: String = "",
        val submitAttempted: Boolean = false,
    )

    /** Non-null while the card bottom sheet is open. */
    private val cardForm = MutableStateFlow<CardForm?>(null)

    private data class CaptureState(
        val smsEnabled: Boolean,
        val notificationsEnabled: Boolean,
        val notificationAccessGranted: Boolean,
    )

    private val captureState = combine(
        smsCapture.enabled,
        notificationCapture.enabled,
        notificationCapture.accessGranted,
        ::CaptureState,
    )

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.session,
        cardRepository.observeCards(),
        captureState,
        cardForm,
    ) { session, cards, capture, form ->
        val formUi = form?.let {
            CardFormUiState(
                editingCardId = it.editingCardId,
                bank = it.bank,
                last4 = it.last4,
                label = it.label,
                errors = if (it.submitAttempted) validate(it) else emptySet(),
            )
        }
        when (session) {
            is UserSession.SignedIn -> ProfileUiState(
                userName = session.user.displayName ?: session.user.email.orEmpty(),
                userEmail = session.user.email.orEmpty(),
                cards = cards,
                isDemo = false,
                smsCaptureEnabled = capture.smsEnabled,
                notificationCaptureEnabled = capture.notificationsEnabled,
                notificationAccessGranted = capture.notificationAccessGranted,
                cardForm = formUi,
            )

            // Demo persona; also what Loading/SignedOut briefly render on the
            // way out of this screen.
            else -> ProfileUiState(
                userName = MockData.USER_NAME,
                userEmail = MockData.USER_EMAIL,
                cards = cards,
                isDemo = true,
                smsCaptureEnabled = capture.smsEnabled,
                notificationCaptureEnabled = capture.notificationsEnabled,
                notificationAccessGranted = capture.notificationAccessGranted,
                cardForm = formUi,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    /** The UI granted RECEIVE_SMS + READ_SMS — enable capture and backfill. */
    fun onSmsPermissionsGranted() {
        smsCapture.onPermissionsGranted()
    }

    fun onSmsCaptureDisabled() {
        smsCapture.disable()
    }

    /**
     * The screen resumed — re-read the system notification-access state so a
     * grant or revocation made in settings is reflected immediately.
     */
    fun onScreenResumed() {
        notificationCapture.refreshAccess()
    }

    /**
     * The UI returned from the notification-access settings mid-opt-in:
     * re-check access and, if it was granted, complete the opt-in.
     */
    fun onNotificationOptInReturned() {
        notificationCapture.refreshAccess()
        if (notificationCapture.accessGranted.value) {
            notificationCapture.onAccessGranted()
        }
    }

    /** Access was already granted when the user confirmed the explainer. */
    fun onNotificationAccessGranted() {
        notificationCapture.onAccessGranted()
    }

    fun onNotificationCaptureDisabled() {
        notificationCapture.disable()
    }

    fun onAddCardClick() {
        cardForm.value = CardForm()
    }

    fun onCardClick(card: Card) {
        cardForm.value = CardForm(
            editingCardId = card.id,
            bank = card.bank,
            last4 = card.last4,
            label = card.label.orEmpty(),
        )
    }

    fun onCardFormDismiss() {
        cardForm.value = null
    }

    fun onCardBankChanged(value: String) {
        cardForm.update { it?.copy(bank = value) }
    }

    fun onCardLast4Changed(value: String) {
        cardForm.update { it?.copy(last4 = value.filter(Char::isDigit).take(4)) }
    }

    fun onCardLabelChanged(value: String) {
        cardForm.update { it?.copy(label = value) }
    }

    fun onCardFormSubmit() {
        val form = cardForm.value ?: return
        if (validate(form).isNotEmpty()) {
            cardForm.update { it?.copy(submitAttempted = true) }
            return
        }
        viewModelScope.launch {
            val card = Card(
                id = form.editingCardId ?: UUID.randomUUID().toString(),
                bank = form.bank.trim(),
                last4 = form.last4,
                label = form.label.trim().ifEmpty { null },
            )
            if (form.editingCardId == null) {
                cardRepository.addCard(card)
                analytics.logAddCard()
            } else {
                cardRepository.updateCard(card)
            }
            cardForm.value = null
        }
    }

    /** Deletes the card being edited. Movements that reference it stay put. */
    fun onCardDelete() {
        val cardId = cardForm.value?.editingCardId ?: return
        viewModelScope.launch {
            cardRepository.deleteCard(cardId)
            analytics.logDeleteCard()
            cardForm.value = null
        }
    }

    /** Ends the session (or leaves the demo); TintoRoot falls back to the login gate. */
    fun onSignOut() {
        analytics.logSignOut()
        authRepository.signOut()
    }

    private fun validate(form: CardForm) = CardFormValidator.validate(
        bank = form.bank,
        last4 = form.last4,
    )
}
