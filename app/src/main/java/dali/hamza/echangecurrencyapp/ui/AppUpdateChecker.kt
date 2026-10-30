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
 * [refresh] from onResume, forward the flow result from the activity's
 * onActivityResult to [onFlowResult], and call [destroy] from onDestroy.
 */
class AppUpdateChecker(activity: Activity) {

    /** What the UI should render right now. */
    sealed interface UiState {
        data object Idle : UiState
        /** An update exists and the user hasn't declined this session. */
        data class Available(val versionCode: Int) : UiState
        /**
         * The flexible flow was handed to Play (consent sheet up or
         * download running). No UI — Play owns the surface from here.
         * Without this, refresh() keeps reporting UPDATE_AVAILABLE during
         * the background download and re-opens our dialog on every resume.
         */
        data object Updating : UiState
        /** Flexible download finished — restart installs the new version. */
        data object Downloaded : UiState
    }

    var uiState: UiState by mutableStateOf(UiState.Idle)
        private set

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private val activity = activity

    /** Users decline at most once per process — no nagging on every resume. */
    private var declinedThisSession = false

    /**
     * Play owns the update lifecycle (consent sheet open or download
     * running) until the listener reports DOWNLOADED/FAILED or the user
     * cancels the sheet. While true, [refresh] must not re-prompt — the
     * availability stays UPDATE_AVAILABLE until the new version actually
     * installs, which made the dialog re-appear mid-download.
     */
    private var flowInFlight = false

    /** Release priority at/above this switches to the mandatory full-screen flow. */
    private val immediateThreshold = 4

    private val installListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> uiState = UiState.Downloaded
            InstallStatus.FAILED, InstallStatus.CANCELED -> {
                // download died — release the flow so a later refresh()
                // can offer the update again
                flowInFlight = false
                uiState = UiState.Idle
            }
            else -> Unit
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
        if (declinedThisSession || flowInFlight) return
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

    /** "Update" button — hides the prompt immediately and hands the
     *  flow to Play's consent sheet; download then runs in background. */
    fun startFlexibleUpdate() {
        val info = latestInfo ?: return
        if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            uiState = UiState.Updating
            startFlow(info, AppUpdateType.FLEXIBLE)
        }
    }

    /**
     * Result of Play's consent sheet — forward here from the activity's
     * onActivityResult. Acceptance needs nothing (the listener takes
     * over); a cancel counts as a decline so the sheet isn't re-offered
     * on the next resume.
     */
    fun onFlowResult(requestCode: Int, resultCode: Int) {
        if (requestCode != REQUEST_CODE) return
        if (resultCode != Activity.RESULT_OK) {
            flowInFlight = false
            declinedThisSession = true
            uiState = UiState.Idle
        }
    }

    private fun startFlow(info: AppUpdateInfo, @AppUpdateType type: Int) {
        // From here Play owns the flow — keep refresh() quiet until the
        // listener reports DOWNLOADED/FAILED or the user cancels.
        flowInFlight = true
        runCatching {
            appUpdateManager.startUpdateFlowForResult(info, type, activity, REQUEST_CODE)
        }
    }

    /** "Restart" button — installs the staged update (app restarts). */
    fun completeUpdate() {
        runCatching { appUpdateManager.completeUpdate() }
        flowInFlight = false
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
