package dem.dev.timeflame.data.di

import dem.dev.timeflame.domain.manager.LocalAuthManager
import dem.dev.timeflame.domain.manager.LocalSettingsManager
import dem.dev.timeflame.feature.calendar.usecase.LoadCalendarForMonthUseCase
import dem.dev.timeflame.feature.login.usecase.LoginUseCase
import dem.dev.timeflame.feature.register.usecase.RegisterUseCase
import dem.dev.timeflame.feature.task.usecase.CreateTaskUseCase
import dem.dev.timeflame.feature.task.usecase.DeleteTaskUseCase
import dem.dev.timeflame.feature.task.usecase.UpdateTaskUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PreferencesComponent: KoinComponent {
    val localAuthManager: LocalAuthManager by inject<LocalAuthManager>()
    val localSettingsManager: LocalSettingsManager by inject<LocalSettingsManager>()
}

class UseCaseComponent: KoinComponent {
    val loadCalendarForMonthUseCase: LoadCalendarForMonthUseCase by inject<LoadCalendarForMonthUseCase>()
    val registerUseCase: RegisterUseCase by inject<RegisterUseCase>()
    val loginUseCase: LoginUseCase by inject<LoginUseCase>()
    val createTaskUseCase: CreateTaskUseCase by inject<CreateTaskUseCase>()
    val deleteTaskUseCase: DeleteTaskUseCase by inject<DeleteTaskUseCase>()
    val updateTaskUseCase: UpdateTaskUseCase by inject<UpdateTaskUseCase>()
}