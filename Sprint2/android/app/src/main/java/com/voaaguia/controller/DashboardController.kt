package com.voaaguia.controller
import com.voaaguia.model.User
import com.voaaguia.model.repository.ApiRepository
import com.voaaguia.model.repository.DashboardData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
class DashboardController {
    private val repository = ApiRepository()
    private val _dashboardData = MutableStateFlow(DashboardData())
    val dashboardData: StateFlow<DashboardData> = _dashboardData
    private val _ranking = MutableStateFlow<List<User>>(emptyList())
    val ranking: StateFlow<List<User>> = _ranking
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    val error = MutableStateFlow<String?>(null)
    suspend fun loadDashboard(strategyId: String? = null, projectId: String? = null) {
        _isLoading.value = true; error.value = null
        try { _dashboardData.value = repository.getDashboardData(strategyId, projectId) }
        catch (e: Exception) { error.value = e.message }
        finally { _isLoading.value = false }
    }
    suspend fun loadRanking() {
        _isLoading.value = true; error.value = null
        try { _ranking.value = repository.getRanking() }
        catch (e: Exception) { error.value = e.message }
        finally { _isLoading.value = false }
    }
}
