package dropit.mobile.ui.main.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dropit.infrastructure.event.EventBus
import dropit.mobile.application.entity.Computer
import dropit.mobile.lib.db.SQLiteHelper
import dropit.mobile.lib.preferences.PreferencesHelper
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

class HomeViewModel(
    private val sqLiteHelper: SQLiteHelper,
    private val preferencesHelper: PreferencesHelper,
    private val eventBus: EventBus
) : ViewModel() {
    init {
        eventBus.subscribe(PreferencesHelper.CurrentComputerIdChanged::class, ::computerIdChanged)
    }

    private val _computer = MutableLiveData<Computer?>()
    val computer: LiveData<Computer?> = _computer

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    private fun computerIdChanged(id: UUID?) {
        if (id == null) {
            _computer.value = null
        } else {
            _computer.value = sqLiteHelper.getComputer(id)
        }
    }

    @Singleton
    class Factory @Inject constructor(
        private val sqLiteHelper: SQLiteHelper,
        private val preferencesHelper: PreferencesHelper,
        private val eventBus: EventBus
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return HomeViewModel(sqLiteHelper, preferencesHelper, eventBus) as T
        }
    }
}
