package dem.dev.timeflame.util

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import org.koin.compose.currentKoinScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dem.dev.timeflame.util.viewModel.BaseViewModel

@Composable
inline fun <reified T: BaseViewModel> koinViewModel(): T {
    val scope = currentKoinScope()
    return viewModel {
        scope.get<T>()
    }
}
@Composable
inline fun <reified T: ViewModel> androidKoinViewModel(): T {
    val scope = currentKoinScope()
    return viewModel {
        scope.get<T>()
    }
}

