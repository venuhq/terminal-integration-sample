package com.venuhq.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a card payment.
 * @property card The card details used in the transaction
 * @property amount The transaction amount details
 * @property externalId Optional identifier for the transaction from the POS
 */
@Serializable
data class VenuCardRequest (
    @SerialName(value = "card")
    val card: Card,

    @SerialName(value = "amount")
    val amount: Amount,

    @SerialName(value = "external_id")
    val externalId: String?,
)

/**
 * Contains the payment card details.
 * @property token Unique identifier for the card
 * @property bin First 6 digits of the card number
 * @property last4 Last 4 digits of the card number
 */
@Serializable
data class Card (
    @SerialName(value = "token")
    val token: String,

    @SerialName(value = "bin")
    val bin: String?,

    @SerialName(value = "last4")
    val last4: String?,
)

/**
 * Represents the monetary amounts involved in a transaction.
 * @property total The total transaction amount
 * @property cashout Optional cash out amount requested
 * @property surcharge Optional surcharge amount applied
 * @property gratuity Optional tip/gratuity amount
 */
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

/**
 * Response from the Venu service.
 * @property action The action to take based on the response
 * @property intent Optional Android intent string to launch
 */
@Serializable
data class VenuResponse (
    @SerialName("action")
    val action: Action,

    @SerialName("intent")
    val intent: String?,
)

/**
 * Possible actions that can be returned in a VenuResponse.
 * NONE: No action required
 * LAUNCH_INTENT: Launch the provided Android intent
 */
enum class Action {
    NONE, LAUNCH_INTENT
}

/**
 * Request to initialize the Venu service.
 * @property metadata Key-value pairs containing terminal identification information
 */
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

/**
 * Result returned when a card is presented to the terminal.
 * @property discountAmount Optional discount amount to apply to the transaction
 */
@Serializable
data class VenuCardPresentedResult(
    @SerialName("discount_amount")
    val discountAmount: String?
)