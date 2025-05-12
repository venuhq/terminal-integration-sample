package com.venuhq.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VenuCardRequest (
    @SerialName(value = "card")
    val card: Card,

    @SerialName(value = "amount")
    val amount: Amount,

    @SerialName(value = "external_id")
    val externalId: String?,
)

@Serializable
data class Card (
    @SerialName(value = "token")
    val token: String,

    @SerialName(value = "bin")
    val bin: String?,

    @SerialName(value = "last4")
    val last4: String?,

    @SerialName(value = "presentation_method")
    val presentationMethod: PresentationMethod
)

enum class PresentationMethod {
    ContactlessCard, ContactlessPhone, Chip, MagStripe, Other
}

@Serializable
data class Amount (
    @SerialName(value = "total")
    val total: String,

    @SerialName(value = "cashout")
    val cashout: String?,

    @SerialName(value = "surcharge")
    val surcharge: String?,

    @SerialName(value = "gratuity")
    val gratuity: String?,
)

@Serializable
data class VenuResponse (
    @SerialName("action")
    val action: Action,

    @SerialName("intent")
    val intent: String?,
)

enum class Action {
    NONE, LAUNCH_INTENT
}

@Serializable
data class VenuInitialiseRequest (
    @SerialName("metadata")
    val metadata: Map<String, String>,
)

enum class VenuMessage(val value: Int) {
    REQUEST_INITIALISE(1),
    REQUEST_CARD_PRESENTED(2),
    REQUEST_TRANSACTION_ACCEPTED(3),

    RESPONSE_JSON(4),
    RESPONSE_QUIT(5),
}

@Serializable
data class VenuCardPresentedResult(
    @SerialName("discount_amount")
    val discountAmount: String
)