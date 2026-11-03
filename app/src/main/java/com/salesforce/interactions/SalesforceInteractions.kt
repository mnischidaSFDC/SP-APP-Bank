package com.salesforce.interactions

import android.util.Log

data class PersonalizationResponse(
    val personalizations: List<PersonalizationDecision> = emptyList()
)

data class PersonalizationDecision(
    val personalizationPointName: String,
    val data: Map<String, Any?>
)

sealed class PersonalizationResult {
    data class Success(val response: PersonalizationResponse) : PersonalizationResult()
    data class Failure(val error: Throwable) : PersonalizationResult()

    inline fun onSuccess(action: (PersonalizationResponse) -> Unit): PersonalizationResult {
        if (this is Success) action(response)
        return this
    }

    inline fun onFailure(action: (Throwable) -> Unit): PersonalizationResult {
        if (this is Failure) action(error)
        return this
    }
}

object SalesforceInteractions {
    val personalization = PersonalizationManager()

    class PersonalizationManager {
        fun fetch(
            personalizationPoints: List<String>,
            callback: (PersonalizationResult) -> Unit
        ) {
            Log.d("MCN SP", "[SalesforceInteractions] Fetch chamado para os pontos: $personalizationPoints")

            val decisions = mutableListOf<PersonalizationDecision>()

            Log.d("MCN SP", "[SalesforceInteractions] Executando retorno. Total de decisões: ${decisions.size}")
            callback(PersonalizationResult.Success(PersonalizationResponse(decisions)))
        }
    }
}
