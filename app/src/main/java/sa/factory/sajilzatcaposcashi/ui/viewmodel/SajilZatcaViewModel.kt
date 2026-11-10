package sa.factory.sajilzatcaposcashi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.sajilzatcaposcashi.domain.usecase.*
import sa.factory.sajilzatcaposcashi.model.SajilZatcaItem
import sa.factory.sajilzatcaposcashi.model.SajilZatcaOrder
import sa.factory.sajilzatcaposcashi.model.SajilZatcaTelemetry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SajilZatcaUiState(
    val catalog: List<SajilZatcaItem> = emptyList(),
    val filteredCatalog: List<SajilZatcaItem> = emptyList(),
    val selectedCategory: String = "الكل",
    val cart: List<SajilZatcaItem> = emptyList(),
    val telemetry: SajilZatcaTelemetry? = null,
    val lastOrder: SajilZatcaOrder? = null,
    val isCheckingOut: Boolean = false,
    val activeTab: String = "home"
)

class SajilZatcaViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val manageCartUseCase: ManageCartUseCase,
    private val processCheckoutUseCase: ProcessCheckoutUseCase,
    private val observeHardwareTelemetryUseCase: ObserveHardwareTelemetryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SajilZatcaUiState())
    val uiState: StateFlow<SajilZatcaUiState> = _uiState.asStateFlow()

    init {
        // Observe Catalog
        getCatalogUseCase().onEach { list ->
            _uiState.update { it.copy(catalog = list, filteredCatalog = list) }
        }.launchIn(viewModelScope)

        // Observe Cart
        manageCartUseCase.cart.onEach { cartItems ->
            _uiState.update { it.copy(cart = cartItems) }
        }.launchIn(viewModelScope)

        // Observe Telemetry
        observeHardwareTelemetryUseCase().onEach { telem ->
            _uiState.update { it.copy(telemetry = telem) }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = if (category == "الكل") state.catalog else state.catalog.filter { it.category == category }
            state.copy(selectedCategory = category, filteredCatalog = filtered)
        }
    }

    fun switchTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addToCart(item: SajilZatcaItem) = manageCartUseCase.add(item)
    fun removeFromCart(item: SajilZatcaItem) = manageCartUseCase.remove(item)

    fun checkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true) }
            val order = processCheckoutUseCase()
            _uiState.update { it.copy(isCheckingOut = false, lastOrder = order, activeTab = "checkout") }
        }
    }
}
