package digital.tonima.bubbleslauncher.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.bubbleslauncher.data.dao.ImpulseDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class MetricsState(
    val timeSavedTodayMinutes: Int = 0,
    val timeSavedThisMonthMinutes: Int = 0,
    val winRateToday: Float = 0f, // 0.0 to 1.0
    val lifetimeBubbles: Int = 0
) {
    val timeSavedTodayFormatted: String
        get() = formatMinutes(timeSavedTodayMinutes)

    val timeSavedThisMonthFormatted: String
        get() = formatMinutes(timeSavedThisMonthMinutes)

    private fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }
}

@HiltViewModel
class MetricsViewModel @Inject constructor(
    private val impulseDao: ImpulseDao
) : ViewModel() {

    private val timeSavedPerResistanceMinutes = 15

    private val startOfToday = getStartOfDayTimestamp()
    private val startOfMonth = getStartOfMonthTimestamp()
    private val now = System.currentTimeMillis() // Assuming 'now' doesn't strictly need real-time updates while viewing the screen

    private val resistedTodayFlow = impulseDao.getResistedCountByDateRange(startOfToday, now)
    private val totalTodayFlow = impulseDao.getTotalCountByDateRange(startOfToday, now)
    
    private val resistedThisMonthFlow = impulseDao.getResistedCountByDateRange(startOfMonth, now)
    
    private val lifetimeResistedFlow = impulseDao.getLifetimeResistedCount()

    val uiState: StateFlow<MetricsState> = combine(
        resistedTodayFlow,
        totalTodayFlow,
        resistedThisMonthFlow,
        lifetimeResistedFlow
    ) { resistedToday, totalToday, resistedThisMonth, lifetimeResisted ->
        val winRate = if (totalToday > 0) {
            resistedToday.toFloat() / totalToday.toFloat()
        } else {
            0f
        }

        MetricsState(
            timeSavedTodayMinutes = resistedToday * timeSavedPerResistanceMinutes,
            timeSavedThisMonthMinutes = resistedThisMonth * timeSavedPerResistanceMinutes,
            winRateToday = winRate,
            lifetimeBubbles = lifetimeResisted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MetricsState()
    )

    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getStartOfMonthTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
