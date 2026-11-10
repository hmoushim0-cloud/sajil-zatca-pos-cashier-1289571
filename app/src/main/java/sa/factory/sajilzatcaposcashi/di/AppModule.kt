package sa.factory.sajilzatcaposcashi.di

import sa.factory.sajilzatcaposcashi.data.repository.SajilZatcaRepositoryImpl
import sa.factory.sajilzatcaposcashi.domain.repository.SajilZatcaRepository
import sa.factory.sajilzatcaposcashi.domain.usecase.*
import sa.factory.sajilzatcaposcashi.hardware.HardwareSensorDriver
import sa.factory.sajilzatcaposcashi.ui.viewmodel.SajilZatcaViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: SajilZatcaRepository = SajilZatcaRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): SajilZatcaViewModel = SajilZatcaViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
