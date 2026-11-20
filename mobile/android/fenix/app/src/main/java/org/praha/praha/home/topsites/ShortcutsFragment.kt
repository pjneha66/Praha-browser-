/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.home.topsites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import java.lang.ref.WeakReference
import mozilla.components.feature.top.sites.presenter.DefaultTopSitesPresenter
import mozilla.components.lib.state.helpers.StoreProvider.Companion.fragmentStore
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.praha.praha.e2e.SystemInsetsPaddedFragment
import org.praha.praha.ext.requireComponents
import org.praha.praha.home.topsites.controller.DefaultTopSiteController
import org.praha.praha.home.topsites.controller.TopSiteController
import org.praha.praha.home.topsites.controller.TopSitesSource
import org.praha.praha.home.topsites.interactor.DefaultTopSiteInteractor
import org.praha.praha.home.topsites.interactor.TopSiteInteractor
import org.praha.praha.home.topsites.middleware.ShortcutsMiddleware
import org.praha.praha.home.topsites.store.ShortcutsState
import org.praha.praha.home.topsites.store.ShortcutsStore
import org.praha.praha.home.topsites.ui.ShortcutsScreen
import org.praha.praha.theme.PrahaTheme

/** A [Fragment] displaying the shortcuts screen. */
class ShortcutsFragment : Fragment(), SystemInsetsPaddedFragment {

    private val topSitesBinding = ViewBoundFeatureWrapper<TopSitesBinding>()

    private lateinit var interactor: TopSiteInteractor
    private lateinit var controller: TopSiteController

    private val shortcutsStore by
        fragmentStore(initialState = ShortcutsState.INITIAL) {
            ShortcutsStore(
                initialState = it,
                middleware =
                    listOf(
                        ShortcutsMiddleware(
                            appStore = requireComponents.appStore,
                            topSitesUseCases = requireComponents.useCases.topSitesUseCase,
                            merinoManifestProvider = requireComponents.core.merinoManifestProvider,
                            scope = viewLifecycleOwner.lifecycleScope,
                        )
                    ),
            )
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controller =
            DefaultTopSiteController(
                activityRef = WeakReference(requireActivity()),
                store = requireComponents.core.store,
                appStore = requireComponents.appStore,
                navControllerRef = WeakReference(findNavController()),
                settings = requireComponents.settings,
                defaultTopSitesProvider = requireComponents.core.defaultTopSitesProvider,
                addTabUseCase = requireComponents.useCases.tabsUseCases.addTab,
                selectTabUseCase = requireComponents.useCases.tabsUseCases.selectTab,
                fenixBrowserUseCases = requireComponents.useCases.fenixBrowserUseCases,
                topSitesUseCases = requireComponents.useCases.topSitesUseCase,
                mozAdsUseCases = requireComponents.useCases.mozAdsUseCases,
                viewLifecycleScope = viewLifecycleOwner.lifecycleScope,
                source = TopSitesSource.SHORTCUTS_LIBRARY,
            )

        interactor = DefaultTopSiteInteractor(controller = controller)

        topSitesBinding.set(
            feature =
                TopSitesBinding(
                    browserStore = requireComponents.core.store,
                    presenter =
                        DefaultTopSitesPresenter(
                            view =
                                DefaultTopSitesView(
                                    appStore = requireComponents.appStore,
                                    settings = requireComponents.settings,
                                ),
                            storage = requireComponents.core.topSitesStorage,
                            config =
                                getTopSitesConfig(
                                    settings = requireComponents.settings,
                                    store = requireComponents.core.store,
                                ),
                        ),
                ),
            owner = viewLifecycleOwner,
            view = view,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = content {
        PrahaTheme {
            ShortcutsScreen(
                store = shortcutsStore,
                interactor = interactor,
                onNavigationIconClick = {
                    this@ShortcutsFragment.findNavController().popBackStack()
                },
            )
        }
    }
}
