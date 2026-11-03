package com.example.salesforcemn

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager

class ProdutosFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_produtos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trackScreenView("Produtos Tela", "Fatura Tela")

        setupListeners(view)
    }

    private fun setupListeners(view: View) {
        view.findViewById<MaterialButton>(R.id.btnInvestRendaFixa)?.setOnClickListener {
            trackProdutoAction("Investimentos_RendaFixa")
            Snackbar.make(view, "Simulação de Renda Fixa iniciada!", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialButton>(R.id.btnInvestCaixinhas)?.setOnClickListener {
            trackProdutoAction("Investimentos_Caixinhas")
            Snackbar.make(view, "Criação de Caixinha iniciada!", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialButton>(R.id.btnSeguroCelular)?.setOnClickListener {
            trackProdutoAction("Seguros_Celular")
            Snackbar.make(view, "Cotação de Seguro Celular aberta!", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialButton>(R.id.btnEmprestimoPessoal)?.setOnClickListener {
            trackProdutoAction("Credito_EmprestimoPessoal")
            Snackbar.make(view, "Simulação de Empréstimo Pessoal iniciada!", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialButton>(R.id.btnShoppingCashback)?.setOnClickListener {
            trackProdutoAction("Shopping_Cashback")
            Snackbar.make(view, "Direcionando para o Shopping Parceiro!", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun trackProdutoAction(productName: String) {
        EventManager.customEvent(
            name = "custom",
            attributes = mapOf(
                "interactionName" to "ProdutoAction_$productName",
                "interactionType" to "userAction",
                "screenName" to "Produtos Tela"
            )
        )?.track()
        Log.d("ProdutosFragment", "Produto Action tracked: $productName")
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
        Log.d("ProdutosFragment", "Custom Event tracked: $screenName")
    }
}
