package dev.romerobrayan.tinto.feature.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val cardRepository: CardRepository,
    categoryRepository: CategoryRepository,
    private val analytics: TintoAnalytics,
) : ViewModel() {

    private val timeZone = TimeZone.currentSystemDefault()

    private data class Form(
        val amountDigits: String = "",
        val type: TransactionType = TransactionType.EXPENSE,
        val method: PaymentMethod = PaymentMethod.CASH,
        val last4: String = "",
        val categoryId: String? = null,
        /** null = today (kept relative so the default follows the clock). */
        val date: LocalDate? = null,
        val merchant: String = "",
        val submitAttempted: Boolean = false,
    )

    private val form = MutableStateFlow(Form())

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits once after the movement is persisted; the screen then closes. */
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    val uiState: StateFlow<AddTransactionUiState> = combine(
        form,
        categoryRepository.observeCategories(),
        cardRepository.observeCards(),
    ) { currentForm, categories, cards ->
        val today = Clock.System.todayIn(timeZone)
        val date = currentForm.date ?: today
        AddTransactionUiState(
            amountDigits = currentForm.amountDigits,
            type = currentForm.type,
            method = currentForm.method,
            last4 = currentForm.last4,
            categoryId = currentForm.categoryId,
            date = date,
            isDateToday = date == today,
            merchant = currentForm.merchant,
            categories = categories,
            cards = cards,
            errors = if (currentForm.submitAttempted) validate(currentForm) else emptySet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddTransactionUiState())

    fun onAmountChanged(raw: String) {
        form.update {
            it.copy(amountDigits = raw.filter(Char::isDigit).trimStart('0').take(MAX_AMOUNT_DIGITS))
        }
    }

    fun onTypeChanged(type: TransactionType) = form.update { it.copy(type = type) }

    fun onMethodChanged(method: PaymentMethod) = form.update { it.copy(method = method) }

    fun onLast4Changed(value: String) {
        form.update { it.copy(last4 = value.filter(Char::isDigit).take(4)) }
    }

    fun onCategorySelected(categoryId: String) = form.update { it.copy(categoryId = categoryId) }

    fun onDateChanged(date: LocalDate) = form.update { it.copy(date = date) }

    fun onMerchantChanged(value: String) = form.update { it.copy(merchant = value) }

    fun onSubmit() {
        val currentForm = form.value
        if (validate(currentForm).isNotEmpty()) {
            form.update { it.copy(submitAttempted = true) }
            return
        }
        viewModelScope.launch {
            val now = Clock.System.now()
            val today = Clock.System.todayIn(timeZone)
            val date = currentForm.date ?: today
            val matchedCard = if (currentForm.method == PaymentMethod.CARD) {
                cardRepository.observeCards().first().firstOrNull { it.last4 == currentForm.last4 }
            } else {
                null
            }
            transactionRepository.addTransaction(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    type = currentForm.type,
                    amount = Money.ofPesos(currentForm.amountDigits.toLong()),
                    method = currentForm.method,
                    cardId = matchedCard?.id,
                    bank = matchedCard?.bank,
                    categoryId = requireNotNull(currentForm.categoryId),
                    merchant = currentForm.merchant.trim().ifEmpty { null },
                    occurredAt = if (date == today) now else date.atTime(12, 0).toInstant(timeZone),
                    source = TransactionSource.MANUAL,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            analytics.logAddTransaction(currentForm.type.name, currentForm.method.name)
            _saved.tryEmit(Unit)
        }
    }

    private fun validate(currentForm: Form) = AddTransactionValidator.validate(
        amountPesos = currentForm.amountDigits.toLongOrNull(),
        method = currentForm.method,
        last4 = currentForm.last4,
        categoryId = currentForm.categoryId,
    )

    private companion object {
        const val MAX_AMOUNT_DIGITS = 10
    }
}
