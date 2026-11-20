/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.home.pocket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.map
import org.praha.praha.components.appstate.recommendations.ContentRecommendationsState
import org.praha.praha.components.components
import org.praha.praha.e2e.SystemInsetsPaddedFragment
import org.praha.praha.ext.requireComponents
import org.praha.praha.home.pocket.controller.DefaultPocketStoriesController
import org.praha.praha.home.pocket.controller.PocketStoriesController
import org.praha.praha.home.pocket.interactor.DefaultPocketStoriesInteractor
import org.praha.praha.home.pocket.interactor.PocketStoriesInteractor
import org.praha.praha.home.pocket.ui.StoriesScreen
import org.praha.praha.theme.PrahaTheme

/** A [Fragment] displaying the stories screen. */
class StoriesFragment : Fragment(), SystemInsetsPaddedFragment {

    private lateinit var interactor: PocketStoriesInteractor
    private lateinit var controller: PocketStoriesController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controller =
            DefaultPocketStoriesController(
                navControllerRef = WeakReference(findNavController()),
                appStore = requireComponents.appStore,
                settings = requireComponents.settings,
                fenixBrowserUseCases = requireComponents.useCases.fenixBrowserUseCases,
                marsUseCases = requireComponents.useCases.marsUseCases,
                viewLifecycleScope = viewLifecycleOwner.lifecycleScope,
            )

        interactor = DefaultPocketStoriesInteractor(controller = controller)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = content {
        PrahaTheme {
            val appStore = components.appStore
            val storiesState by remember {
                appStore.stateFlow.map { state -> state.recommendationState }
            }
                .collectAsState(initial = ContentRecommendationsState())

            StoriesScreen(
                state = storiesState,
                interactor = interactor,
                onNavigationIconClick = {
                    this@StoriesFragment.findNavController().popBackStack()
                },
            )
        }
    }
}
