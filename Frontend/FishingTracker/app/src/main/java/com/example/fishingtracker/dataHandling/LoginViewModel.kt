import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishingtracker.API.RetrofitInstance
import com.example.fishingtracker.data.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow("")
    val loginState: StateFlow<String> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.Login(LoginRequest(username, password))
                if (response.isSuccessful) {
                    _loginState.value = response.body() ?: "Login successful"
                } else {
                    _loginState.value = "Login failed: ${response.code()}"
                }
            } catch (e: Exception) {
                _loginState.value = "Error: ${e.message}"
            }
        }
    }
}
