package dem.dev.timeflame.feature.calendar.di

import dem.dev.timeflame.domain.repository.TaskRepository
import dem.dev.timeflame.feature.calendar.usecase.LoadCalendarForMonthUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module

fun calendarFeatureModule() = listOf(useCaseModule)

private val useCaseModule = module {
    single<LoadCalendarForMonthUseCase> { LoadCalendarForMonthUseCase(get<TaskRepository>()) }
}
