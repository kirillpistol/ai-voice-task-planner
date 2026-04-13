package dem.dev.timeflame.feature.task.di

import dem.dev.timeflame.domain.repository.TaskRepository
import dem.dev.timeflame.feature.task.usecase.CreateTaskUseCase
import dem.dev.timeflame.feature.task.usecase.DeleteTaskUseCase
import dem.dev.timeflame.feature.task.usecase.UpdateTaskUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module

fun taskFeatureModule() = listOf(useCaseModule)

private val useCaseModule = module {
    single<CreateTaskUseCase> { CreateTaskUseCase(get<TaskRepository>()) }
    single<DeleteTaskUseCase> { DeleteTaskUseCase(get<TaskRepository>()) }
    single<UpdateTaskUseCase> { UpdateTaskUseCase(get<TaskRepository>()) }
}
