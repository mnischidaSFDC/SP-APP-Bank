package com.example.salesforcemn

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.salesforcemn.data.DatabaseRepository
import com.example.salesforcemn.data.Transaction
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ExtratoFragment : Fragment() {

    private var isValuesMasked = false
    private var allTransactions: List<Transaction> = emptyList()
    private val amountViews = mutableListOf<Pair<TextView, Double>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_extrato, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackScreenView("Extrato Tela", "Home Tela")
        setupFilterChips(view)
        fetchTransactions(view)
    }

    fun setValuesMasked(masked: Boolean) {
        isValuesMasked = masked
        applyMask()
    }

    private fun fetchTransactions(view: View) {
        val account = (activity as? DashboardActivity)?.selectedAccount ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            allTransactions = DatabaseRepository.getTransactions(account.id)
            renderTransactions(view, allTransactions)
        }
    }

    private fun renderTransactions(view: View, transactions: List<Transaction>) {
        val container = view.findViewById<LinearLayout>(R.id.llTransactionsList) ?: return
        val tvEmpty = view.findViewById<TextView>(R.id.tvTransactionsEmpty)
        amountViews.clear()
        container.removeAllViews()
        container.addView(tvEmpty)

        if (transactions.isEmpty()) {
            tvEmpty?.visibility = View.VISIBLE
            return
        }
        tvEmpty?.visibility = View.GONE

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val dateFmt = SimpleDateFormat("dd 'de' MMMM", Locale("pt", "BR"))
        var lastDateLabel = ""

        transactions.forEach { t ->
            val parsedDate = runCatching { inputFmt.parse(t.transactionDate) }.getOrNull()
            val dateLabel = if (parsedDate != null) dateFmt.format(parsedDate).uppercase(Locale("pt","BR")) else ""

            if (dateLabel != lastDateLabel) {
                if (lastDateLabel.isNotEmpty()) {
                    addDivider(container)
                }
                lastDateLabel = dateLabel
                addDateHeader(container, dateLabel)
            }

            val isCredit = t.transactionType == "CREDIT"
            val amountColor = if (isCredit) Color.parseColor("#10B981") else Color.parseColor("#64748B")
            val amountText = (if (isCredit) "+ " else "- ") + currencyFormat.format(Math.abs(t.amount))

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 24, 0, 24)
            }

            val textBlock = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textBlock.addView(TextView(requireContext()).apply {
                text = t.title
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1E293B"))
            })
            textBlock.addView(TextView(requireContext()).apply {
                text = t.category
                textSize = 12f
                setTextColor(Color.parseColor("#64748B"))
            })
            row.addView(textBlock)

            val tvAmount = TextView(requireContext()).apply {
                text = if (isValuesMasked) "••••••" else amountText
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(amountColor)
            }
            amountViews.add(Pair(tvAmount, t.amount))
            row.addView(tvAmount)

            container.addView(row)
        }
    }

    private fun applyMask() {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        amountViews.forEach { (tv, amount) ->
            if (isValuesMasked) {
                tv.text = "••••••"
            } else {
                val isCredit = amount > 0
                tv.text = (if (isCredit) "+ " else "- ") + currencyFormat.format(Math.abs(amount))
            }
        }
    }

    private fun addDateHeader(container: LinearLayout, label: String) {
        container.addView(TextView(requireContext()).apply {
            text = label
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#64748B"))
            val p = (8 * resources.displayMetrics.density).toInt()
            setPadding(0, p * 2, 0, p)
        })
    }

    private fun addDivider(container: LinearLayout) {
        container.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(Color.parseColor("#E2E8F0"))
        })
    }

    private fun setupFilterChips(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupExtrato)
        chipGroup?.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds.first())
                val filterName = chip?.text?.toString() ?: "Filtro"
                trackExtratoFilter(filterName)
                Snackbar.make(view, "Filtro aplicado: $filterName", Snackbar.LENGTH_SHORT).show()
                val filtered = when (filterName) {
                    "Entradas" -> allTransactions.filter { it.transactionType == "CREDIT" }
                    "Saídas"   -> allTransactions.filter { it.transactionType == "DEBIT" }
                    else       -> allTransactions
                }
                renderTransactions(view, filtered)
            }
        }
    }

    private fun trackExtratoFilter(filterName: String) {
        EventManager.customEvent(
            name = "custom",
            attributes = mapOf(
                "interactionName" to "ExtratoFiltro_$filterName",
                "interactionType" to "userAction",
                "screenName" to "Extrato Tela"
            )
        )?.track()
        Log.d("ExtratoFragment", "Extrato Filter tracked: $filterName")
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
        Log.d("ExtratoFragment", "Custom Event tracked: $screenName")
    }
}
