package com.example.salesforcemn

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.NumberFormat
import java.util.Locale
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager

class FaturaFragment : Fragment() {

    private var isValuesMasked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fatura, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trackScreenView("Fatura Tela", "Home Tela")

        setupListeners(view)
        updateMaskedValues(view)
    }

    fun setValuesMasked(masked: Boolean) {
        isValuesMasked = masked
        view?.let { updateMaskedValues(it) }
    }

    private fun updateMaskedValues(view: View) {
        val tvFaturaAmount = view.findViewById<TextView>(R.id.tvFaturaAmount)
        val tvFaturaLimitDetails = view.findViewById<TextView>(R.id.tvFaturaLimitDetails)
        val account = (activity as? DashboardActivity)?.selectedAccount
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        if (isValuesMasked) {
            tvFaturaAmount?.text = "••••••"
            tvFaturaLimitDetails?.text = "Limite disponível: ••••••"
        } else {
            val invoice = account?.cardInvoice ?: 0.0
            val available = account?.availableLimit ?: 0.0
            val total = available + invoice
            tvFaturaAmount?.text = currencyFormat.format(invoice)
            tvFaturaLimitDetails?.text = "Limite disponível: ${currencyFormat.format(available)} de ${currencyFormat.format(total)}"
        }
    }

    private fun setupListeners(view: View) {
        view.findViewById<MaterialButton>(R.id.btnPagarFatura)?.setOnClickListener {
            trackFaturaAction("Pagar Fatura")
            Snackbar.make(view, "Solicitação de pagamento iniciada!", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialButton>(R.id.btnParcelarFatura)?.setOnClickListener {
            trackFaturaAction("Parcelar Fatura")
            Snackbar.make(view, "Simulação de parcelamento aberta!", Snackbar.LENGTH_SHORT).show()
        }

        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupMonths)
        chipGroup?.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds.first())
                val monthName = chip?.text?.toString() ?: "Mês"
                trackFaturaAction("Seletor_Mes_$monthName")
                Snackbar.make(view, "Exibindo fatura do mês: $monthName", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun trackFaturaAction(actionName: String) {
        EventManager.customEvent(
            name = "custom",
            attributes = mapOf(
                "interactionName" to "FaturaAction_$actionName",
                "interactionType" to "userAction",
                "screenName" to "Fatura Tela"
            )
        )?.track()
        Log.d("FaturaFragment", "Fatura Action tracked: $actionName")
    }

    private fun trackScreenView(screenName: String, previousScreen: String) {
        EventManager.customEvent(
            name = "custom",
            attributes = mapOf(
                "interactionName" to screenName,
                "interactionType" to "screenView",
                "screenName" to screenName,
                "previousScreen" to previousScreen
            )
        )?.track()
        Log.d("FaturaFragment", "Custom Event tracked: $screenName")
    }
}
