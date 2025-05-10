package com.adyen.postaptopay.presentation.composables

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.adyen.postaptopay.data.local.TransactionRepository
import com.adyen.postaptopay.presentation.viewmodels.ReferencedRefundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TransactionsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val vm: ReferencedRefundViewModel = viewModel()
    val refundState by vm.refundState.collectAsState()
    val activity = context as? AppCompatActivity
        ?: throw IllegalStateException("TransactionsScreen must be hosted in an AppCompatActivity")

    var transactions by remember { mutableStateOf(emptyList<TransactionRepository.Transaction>()) }

    // Initial load
    LaunchedEffect(Unit) {
        transactions = TransactionRepository(context).getLastTransactions()
    }
    // Reload after a successful refund
    LaunchedEffect(refundState) {
        if (!refundState.isLoading && refundState.error == null) {
            transactions = TransactionRepository(context).getLastTransactions()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (transactions.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions yet.", fontSize = 18.sp)
                }
            } else {
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { tx ->
                        Card(
                            Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("ID: ${tx.id}", fontSize = 14.sp)
                                Text("Time: ${tx.timestamp}", fontSize = 12.sp)
                                tx.amount?.let {
                                    Text("Amt: $it ${tx.currency}", fontSize = 12.sp)
                                }
                                Spacer(Modifier.height(8.dp))

                                if (tx.isRefunded) {
                                    Text(
                                        "Status: refunded",
                                        color     = MaterialTheme.colorScheme.primary,
                                        fontStyle = FontStyle.Italic
                                    )
                                } else {
                                    Button(
                                        onClick = {
                                            vm.makeReferencedRefund(
                                                activity = activity,
                                                currency = tx.currency ?: "EUR",
                                                amount   = tx.amount   ?: 0.0
                                            )
                                        },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Refund")
                                    }
                                }
                            }
                        }
                    }
                }

                // Clear history button
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = {
                        vm.clearTransactionHistory()
                        transactions = emptyList()  // immediately reflect the clear in UI
                    }) {
                        Text("Clear History")
                    }
                }
            }
        }
    }
}
