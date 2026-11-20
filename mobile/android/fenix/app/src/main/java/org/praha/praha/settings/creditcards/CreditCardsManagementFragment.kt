/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.settings.creditcards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.lib.state.helpers.StoreProvider.Companion.storeProvider
import org.praha.praha.R
import org.praha.praha.SecureFragment
import org.praha.praha.databinding.ComponentCreditCardsBinding
import org.praha.praha.e2e.SystemInsetsPaddedFragment
import org.praha.praha.ext.components
import org.praha.praha.ext.redirectToReAuth
import org.praha.praha.ext.showToolbar
import org.praha.praha.settings.autofill.AutofillAction
import org.praha.praha.settings.autofill.AutofillFragmentState
import org.praha.praha.settings.autofill.AutofillFragmentStore
import org.praha.praha.settings.creditcards.controller.DefaultCreditCardsManagementController
import org.praha.praha.settings.creditcards.interactor.CreditCardsManagementInteractor
import org.praha.praha.settings.creditcards.interactor.DefaultCreditCardsManagementInteractor
import org.praha.praha.settings.creditcards.view.CreditCardsManagementView

/** Displays a list of saved credit cards. */
class CreditCardsManagementFragment : SecureFragment(), SystemInsetsPaddedFragment {

    private lateinit var store: AutofillFragmentStore
    private lateinit var interactor: CreditCardsManagementInteractor
    private lateinit var creditCardsView: CreditCardsManagementView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(CreditCardsManagementView.LAYOUT_ID, container, false)

        store = storeProvider.get { restoredState ->
            AutofillFragmentStore(restoredState ?: AutofillFragmentState())
        }

        interactor =
            DefaultCreditCardsManagementInteractor(
                controller = DefaultCreditCardsManagementController(navController = findNavController())
            )
        val binding = ComponentCreditCardsBinding.bind(view)

        creditCardsView =
            CreditCardsManagementView(
                binding = binding,
                interactor = interactor,
                shouldShowCvvSaved = requireContext().components.settings.isCreditCardSecurityCodeEnabled,
            )

        loadCreditCards()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumeFrom(store) { state ->
            if (!state.isLoading && state.creditCards.isEmpty()) {
                findNavController().popBackStack()
                return@consumeFrom
            }

            creditCardsView.update(state)
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.credit_cards_saved_cards))
    }

    /** When the fragment is paused, navigate back to the settings page to reauthenticate. */
    override fun onPause() {
        // Don't redirect if the user is navigating to the credit card editor fragment.
        redirectToReAuth(
            listOf(R.id.creditCardEditorFragment),
            findNavController().currentDestination?.id,
            R.id.creditCardsManagementFragment,
        )

        super.onPause()
    }

    /**
     * Fetches all the credit cards from the autofill storage and updates the [AutofillFragmentStore] with the list of
     * credit cards.
     */
    private fun loadCreditCards() {
        lifecycleScope.launch(Dispatchers.IO) {
            val creditCards = requireContext().components.core.autofillStorage.getAllCreditCards()

            lifecycleScope.launch(Dispatchers.Main) {
                store.dispatch(AutofillAction.UpdateCreditCards(creditCards))
            }
        }
    }
}
