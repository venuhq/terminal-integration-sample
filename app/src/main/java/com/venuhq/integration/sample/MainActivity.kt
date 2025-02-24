package com.venuhq.integration.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.venuhq.integration.sample.ui.theme.MyApplicationTheme
import com.venuhq.sdk.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var venuClient: VenuClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToFullScreen()

        venuClient = VenuClient(this)
        venuClient.connect()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VenuMockUI(venuClient)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        venuClient.disconnect()
    }

    private fun setToFullScreen() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onResume() {
        super.onResume()
        setToFullScreen()
    }
}

data class CardOption(
    val name: String,
    val token: String,
    val bin: String,
    val last4: String
)

@Composable
fun VenuMockUI(venuClient: VenuClient) {
    val cards = listOf(
        CardOption("Card 1", "token1", "400000", "0001"),
        CardOption("Card 2", "token2", "510000", "0002"),
        CardOption("Card 3", "token3", "340000", "0003"),
        CardOption("Card 4", "token4", "601100", "0004")
    )
    
    var selectedCard by remember { mutableStateOf(cards[0]) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Select Card:",
            style = MaterialTheme.typography.titleMedium
        )

        cards.forEach { card ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedCard == card,
                    onClick = { selectedCard = card }
                )
                Text(
                    text = "${card.name} (**** **** **** ${card.last4})",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        venuClient.initialise(VenuInitialiseRequest(mapOf("id" to "mock-terminal")))
                        snackbarHostState.showSnackbar("Initialised")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Initialise")
        }

        Button(
            onClick = {
                scope.launch {
                    try {
                        val request = VenuCardRequest(
                            card = Card(
                                token = selectedCard.token,
                                bin = selectedCard.bin,
                                last4 = selectedCard.last4
                            ),
                            amount = Amount(
                                total = "15",
                                cashout = null,
                                surcharge = null,
                                gratuity = null
                            ),
                            externalId = "mock-transaction"
                        )
                        
                        val result = venuClient.cardPresented(request)
                        snackbarHostState.showSnackbar("Card presented processed. Discount: ${result.discountAmount}")

                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Card Presented")
        }

        Button(
            onClick = {
                scope.launch {
                    try {
                        val request = VenuCardRequest(
                            card = Card(
                                token = selectedCard.token,
                                bin = selectedCard.bin,
                                last4 = selectedCard.last4
                            ),
                            amount = Amount(
                                total = "15",
                                cashout = null,
                                surcharge = null,
                                gratuity = null
                            ),
                            externalId = "mock-transaction"
                        )
                        
                        venuClient.transactionAccepted(request)
                        snackbarHostState.showSnackbar("Transaction accepted processed")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Transaction Accepted")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }
}