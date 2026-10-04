package dali.hamza.echangecurrencyapp.ui

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.updatePriority

/**
 * Play In-App Updates wrapper — covers the users Play's auto-update
 * never reaches (auto-update disabled, offline at check time, dismissed
 * store notification). Play itself serves the consent + download flow;
 * this class only decides *when* to ask and shows the restart nudge
 * once the new version is staged.
 *
 * Modes, driven by the release's `inAppUpdatePriority` (0–5, set in the
 * release workflow):
 * - priority below the immediate threshold: FLEXIBLE — an in-app prompt
 *   starts Play's background download; the user keeps using the app until
 *   the "restart to finish" dialog appears.
 * - priority at/above the threshold: IMMEDIATE — Play's full-screen
 *   mandatory update, for critical releases.
 *
 * Activity-scoped: create in [android.app.Activity.onCreate], call
 * [refresh] from onResume, [destroy] from onDestroy.
 */
class AppUpdateChecker(activity: Activity) {

    /** What the UI should render right now. */
    sealed interface UiState {
        data object Idle : UiState
        /** An update exists and the user hasn't declined this session. */
        data class Available(val versionCode: Int) : UiState
        /** Flexible download finished — restart installs the new version. */
        data object Downloaded : UiState
    }

    var uiState: UiState by mutableStateOf(UiState.Idle)
        private set

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private val activity = activity

    /** Users decline at most once per process — no nagging on every resume. */
    private var declinedThisSession = false

    /** Release priority at/above this switches to the mandatory full-screen flow. */
    private val immediateThreshold = 4

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            uiState = UiState.Downloaded
        }
    }

    /** Most recent info from [refresh] — reused when the user taps Update. */
    private var latestInfo: AppUpdateInfo? = null

    init {
        appUpdateManager.registerListener(installListener)
    }

    /** Ask Play whether an update is available; also re-nudges a finished
     *  download on warm resume (recommended by the Play docs). */
    fun refresh() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            latestInfo = info
            when {
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ->
                    handleAvailable(info)
                // a flexible update downloaded in an earlier session
                info.installStatus() == InstallStatus.DOWNLOADED && !declinedThisSession ->
                    uiState = UiState.Downloaded
            }
        }
    }

    private fun handleAvailable(info: AppUpdateInfo) {
        if (declinedThisSession) return
        if (info.updatePriority() >= immediateThreshold) {
            // critical release — full-screen, no in-app prompt first
            if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startFlow(info, AppUpdateType.IMMEDIATE)
            }
            return
        }
        if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            uiState = UiState.Available(info.availableVersionCode())
        }
    }

    /** "Update" button — starts Play's flexible consent/download flow. */
    fun startFlexibleUpdate() {
        val info = latestInfo ?: return
        if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            startFlow(info, AppUpdateType.FLEXIBLE)
        }
    }

    private fun startFlow(info: AppUpdateInfo, @AppUpdateType type: Int) {
        runCatching {
            appUpdateManager.startUpdateFlowForResult(info, type, activity, REQUEST_CODE)
        }
    }

    /** "Restart" button — installs the staged update (app restarts). */
    fun completeUpdate() {
        runCatching { appUpdateManager.completeUpdate() }
        uiState = UiState.Idle
    }

    fun decline() {
        declinedThisSession = true
        uiState = UiState.Idle
    }

    fun destroy() {
        appUpdateManager.unregisterListener(installListener)
    }

    private companion object {
        const val REQUEST_CODE = 4711
    }
}
