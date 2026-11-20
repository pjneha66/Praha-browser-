/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.settings.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.lib.state.ext.observeAsComposableState
import mozilla.components.lib.state.helpers.StoreProvider.Companion.storeProvider
import mozilla.telemetry.glean.private.NoExtras
import org.praha.praha.GleanMetrics.Addresses
import org.praha.praha.R
import org.praha.praha.e2e.SystemInsetsPaddedFragment
import org.praha.praha.ext.components
import org.praha.praha.ext.showToolbar
import org.praha.praha.settings.address.controller.DefaultAddressManagementController
import org.praha.praha.settings.address.interactor.AddressManagementInteractor
import org.praha.praha.settings.address.interactor.DefaultAddressManagementInteractor
import org.praha.praha.settings.address.ui.list.AddressList
import org.praha.praha.settings.autofill.AutofillAction
import org.praha.praha.settings.autofill.AutofillFragmentState
import org.praha.praha.settings.autofill.AutofillFragmentStore
import org.praha.praha.theme.PrahaTheme

/** Displays a list of saved addresses. */
class AddressManagementFragment : Fragment(), SystemInsetsPaddedFragment {

    private lateinit var store: AutofillFragmentStore
    private lateinit var interactor: AddressManagementInteractor

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        store = storeProvider.get { restoredState ->
            AutofillFragmentStore(restoredState ?: AutofillFragmentState())
        }

        interactor =
            DefaultAddressManagementInteractor(
                controller = DefaultAddressManagementController(navController = findNavController())
            )

        loadAddresses()

        return content {
            PrahaTheme {
                val addresses = store.observeAsComposableState { state -> state.addresses }

                AddressList(
                    addresses = addresses.value,
                    onAddressClick = {
                        interactor.onSelectAddress(it)
                        Addresses.managementAddressTapped.record(NoExtras())
                    },
                    onAddAddressButtonClick = {
                        interactor.onAddAddressButtonClick()
                        Addresses.managementAddTapped.record(NoExtras())
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumeFrom(store) { state ->
            if (!state.isLoading && state.addresses.isEmpty()) {
                findNavController().popBackStack()
                return@consumeFrom
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.addresses_manage_addresses))
    }

    /**
     * Fetches all the addresses from the autofill storage and updates the [AutofillFragmentStore] with the list of
     * addresses.
     */
    private fun loadAddresses() {
        lifecycleScope.launch {
            val addresses = requireContext().components.core.autofillStorage.getAllAddresses()

            lifecycleScope.launch(Dispatchers.Main) {
                store.dispatch(AutofillAction.UpdateAddresses(addresses))
            }
        }
    }
}
