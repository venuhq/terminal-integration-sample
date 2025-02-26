# Venu Terminal Integration Sample

This sample app shows how to integrate your payment application with the Venu terminal SDK. The 
`VenuClient` and `VenuTypes` files are designed to be copied into the payment application.

![Screenshot of the sample app](screenshot.png)

## Setup

1. Ensure you have the Venu terminal app installed on your device.
2. Clone this repository
3. Open the project in Android Studio
4. Build and run the app on your device

## Architecture

The on-terminal architecture has two components:
* Venu terminal app: Responsible for communicating with the Venu backend and displaying the Venu user interface when needed.
* Venu client: Provides the interface between the payment application and the Venu terminal application. 

The client and the terminal app communicate via an Android service.

## API Reference

### Types

```kotlin
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
```

### VenuClient

VenuClient provides the interface between the payment application and the Venu terminal application. 
It abstracts the mechanics of connecting to the Venu terminal app, sending/receiving messages and launching
the terminal app when necessary.

#### disconnect()
Disconnects from the Venu service. Should be called when your activity is destroyed.

#### initialise(request: VenuInitialiseRequest)
Initialises the Venu service and links the terminal with the Venu backend. This should be called when
the payment application initialises.

In the `metadata` field of the request, provide any information to help identify the terminal—terminal ID, merchant ID, location etc.

#### cardPresented(request: VenuCardRequest): VenuCardPresentedResult
Call this method when the payment card has been presented. It will return the discount to apply to the transaction (if any).

#### transactionAccepted(request: VenuCardRequest)
Call this method when the transaction has been accepted.
