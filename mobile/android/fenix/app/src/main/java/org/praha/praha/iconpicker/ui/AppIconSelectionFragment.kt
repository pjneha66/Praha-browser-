/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.iconpicker.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import mozilla.components.lib.state.helpers.StoreProvider.Companion.storeProvider
import mozilla.components.support.base.feature.UserInteractionHandler
import org.praha.praha.R
import org.praha.praha.e2e.SystemInsetsPaddedFragment
import org.praha.praha.ext.components
import org.praha.praha.ext.showToolbar
import org.praha.praha.iconpicker.AppIconMiddleware
import org.praha.praha.iconpicker.AppIconRepository
import org.praha.praha.iconpicker.AppIconState
import org.praha.praha.iconpicker.AppIconStore
import org.praha.praha.iconpicker.AppIconTelemetryMiddleware
import org.praha.praha.iconpicker.AppIconUpdater
import org.praha.praha.iconpicker.DefaultAppIconRepository
import org.praha.praha.iconpicker.DefaultPackageManagerWrapper
import org.praha.praha.iconpicker.SearchWidgetsUpdater
import org.praha.praha.theme.PrahaTheme
import org.praha.praha.utils.ShortcutManagerWrapperDefault
import org.praha.praha.utils.ShortcutsUpdaterDefault
import org.praha.praha.utils.changeAppLauncherIcon
import org.mozilla.gecko.search.SearchWidgetProvider

/** Fragment that displays a list of alternative app icons. */
class AppIconSelectionFragment : Fragment(), UserInteractionHandler, SystemInsetsPaddedFragment {

    private val appIconRepository: AppIconRepository by lazy {
        DefaultAppIconRepository(
            packageManager = DefaultPackageManagerWrapper(requireContext().packageManager),
            packageName = requireContext().packageName,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        PrahaTheme {
            AppIconSelection(
                store =
                    storeProvider.get { restoredState ->
                        AppIconStore(
                            initialState =
                                restoredState
                                    ?: AppIconState(
                                        currentAppIcon = appIconRepository.selectedAppIcon,
                                        groupedIconOptions = appIconRepository.groupedAppIcons,
                                    ),
                            middleware =
                                listOf(
                                    AppIconMiddleware(
                                        updateAppIcon = updateAppIcon(),
                                        updateSearchWidgets = updateSearchWidgets(),
                                    ),
                                    AppIconTelemetryMiddleware(),
                                ),
                        )
                    },
                shortcutRemovalWarning = { shouldWarnAboutShortcutRemoval() },
            )
        }
    }

    private fun updateAppIcon(): AppIconUpdater = AppIconUpdater { newIcon, currentIcon ->
        with(requireContext()) {
            changeAppLauncherIcon(
                packageManager = packageManager,
                shortcutManager = ShortcutManagerWrapperDefault(this),
                shortcutInfo = ShortcutsUpdaterDefault(this),
                appAlias = ComponentName(this, "$packageName.${currentIcon.aliasSuffix}"),
                newAppAlias = ComponentName(this, "$packageName.${newIcon.aliasSuffix}"),
                crashReporter = components.analytics.crashReporter,
            )
        }
    }

    private fun updateSearchWidgets(): SearchWidgetsUpdater = SearchWidgetsUpdater {
        val appWidgetManager = AppWidgetManager.getInstance(requireContext())
        SearchWidgetProvider.updateAllWidgets(requireContext(), appWidgetManager)
    }

    private fun shouldWarnAboutShortcutRemoval(): Boolean {
        // Android versions older than 10 will remove existing shortcuts when activity alias changes,
        // which is the underlying mechanics of changing the app icon on android.
        val willRemoveShortcuts = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        val hasShortcuts = ShortcutManagerWrapperDefault(requireContext()).getPinnedShortcuts().isNotEmpty()
        return willRemoveShortcuts && hasShortcuts
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_app_icon))
    }

    override fun onBackPressed() = findNavController().popBackStack()
}
