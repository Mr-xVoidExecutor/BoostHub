package com.boosthub.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.boosthub.app.data.MetaApi
import com.boosthub.app.data.Store
import com.boosthub.app.ui.components.*
import com.boosthub.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun accountStatus(code: Int): String = when (code) {
    1 -> "Active"
    2 -> "Disabled"
    3 -> "Unsettled"
    7 -> "In review"
    9 -> "Grace period"
    100 -> "Pending closure"
    101 -> "Closed"
    else -> "Code $code"
}

@Composable
fun BillingScreen(modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val s = remember { Store(ctx) }
    val scope = rememberCoroutineScope()
    var b by remember { mutableStateOf<MetaApi.Billing?>(null) }
    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var capInput by remember { mutableStateOf("") }

    fun open(url: String) {
        try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (e: Exception) { msg = "Could not open the browser." }
    }

    fun load() {
        scope.launch {
            loading = true; msg = ""
            try { b = withContext(Dispatchers.IO) { MetaApi.billing(s) } }
            catch (e: Exception) { msg = e.message ?: "Error" }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        if (s.fbToken.isNotBlank() && s.adAccount.isNotBlank()) load()
        else msg = "Add your Facebook token and Ad Account ID in Settings first."
    }

    ScreenColumn(modifier, title = "Billing & Payments", onBack = onBack) {
        item {
            OutlineButton(
                if (loading) "Loading..." else "Refresh",
                { load() },
                Modifier.fillMaxWidth(),
                enabled = !loading,
                icon = Icons.Rounded.Refresh
            )
        }
        if (msg.isNotBlank()) item { Notice(msg) }

        val x = b
        if (x != null) {
            val cur = x.currency
            fun money(v: Long) = "${"%.2f".format(v / 100.0)} $cur"

            // 1. Account
            item {
                Panel {
                    CardHeader(Icons.Rounded.Business, BrandBlue, "Your account", "${x.name} (${x.id})")
                    StatusChip(accountStatus(x.status), if (x.status == 1) BrandGreen else BrandOrange)
                }
            }
            // 2. Outstanding balance
            item {
                Panel {
                    CardHeader(Icons.Rounded.AccountBalanceWallet, BrandRed, "Outstanding balance")
                    H(money(x.balance))
                    Spacer(Modifier.height(2.dp))
                    Muted("Lifetime spend: ${money(x.amountSpent)}")
                    Spacer(Modifier.height(12.dp))
                    BrandButton("Pay now", { open(MetaApi.billingUrl(s)) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Payments)
                }
            }
            // 3. Funds
            item {
                Panel {
                    CardHeader(Icons.Rounded.Savings, BrandGreen, "Funds")
                    H(money(x.prepay))
                    Spacer(Modifier.height(12.dp))
                    OutlineButton("Add funds", { open(MetaApi.billingUrl(s)) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
                }
            }
            // 4. Payment methods
            item {
                Panel {
                    CardHeader(Icons.Rounded.CreditCard, BrandPurple, "Payment methods")
                    Body(if (x.funding.isBlank()) "No payment method found" else "${x.funding} (Default)")
                    Spacer(Modifier.height(12.dp))
                    OutlineButton("Add payment method", { open(MetaApi.billingUrl(s)) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
                    Spacer(Modifier.height(8.dp))
                    Muted("For your security, card details are entered only on Meta's own page. This app never collects them.")
                }
            }
            // 5. Payment threshold
            item {
                Panel {
                    CardHeader(Icons.Rounded.Speed, BrandOrange, "Payment threshold")
                    Body(if (x.threshold > 0) "You are charged every time spend reaches ${money(x.threshold)}" else "No information available")
                    Spacer(Modifier.height(12.dp))
                    OutlineButton("Change", { open(MetaApi.billingUrl(s)) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Edit)
                }
            }
            // 6. Account spending limit (editable)
            item {
                Panel {
                    CardHeader(Icons.Rounded.Shield, BrandBlue, "Account spending limit")
                    Body(if (x.spendCap > 0) "Current limit: ${money(x.spendCap)}" else "No limit set")
                    Spacer(Modifier.height(12.dp))
                    Field("New limit ($cur)", capInput, { capInput = it.filter { c -> c.isDigit() } })
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrandButton(
                            "Set limit",
                            {
                                val v = capInput.toLongOrNull()
                                if (v == null || v <= 0) {
                                    msg = "Enter a valid number."
                                } else scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { MetaApi.setSpendCap(s, v * 100) }
                                        msg = "Spending limit updated."; capInput = ""; load()
                                    } catch (e: Exception) { msg = e.message ?: "Error" }
                                }
                            },
                            Modifier.weight(1f),
                            icon = Icons.Rounded.Check
                        )
                        OutlineButton(
                            "Remove limit",
                            {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { MetaApi.setSpendCap(s, 0) }
                                        msg = "Spending limit removed."; load()
                                    } catch (e: Exception) { msg = e.message ?: "Error" }
                                }
                            },
                            Modifier.weight(1f),
                            icon = Icons.Rounded.Close
                        )
                    }
                }
            }
            // 7. Payment activity
            item {
                Panel {
                    CardHeader(Icons.Rounded.Receipt, BrandPink, "Payment activity", "Spend in the last 7 days")
                    if (x.daily.isEmpty()) Muted("No spend recorded")
                    x.daily.forEach { (d, v) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Muted(d)
                            Body("${"%.2f".format(v)} $cur")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlineButton("View statements and pending charges", { open(MetaApi.billingUrl(s)) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Description)
                }
            }
            // 8. Business info
            item {
                Panel {
                    CardHeader(Icons.Rounded.Business, Color8, "Business information")
                    Body(x.bizName.ifBlank { "Name not available" })
                    if (x.bizAddress.isNotBlank()) Muted(x.bizAddress)
                    if (x.taxId.isNotBlank()) Muted("Tax ID: ${x.taxId}")
                    Spacer(Modifier.height(12.dp))
                    OutlineButton("Edit information", { open(MetaApi.billingUrl(s)) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Edit)
                }
            }
        }
        // 9. Support
        item {
            OutlineButton(
                "Get support",
                { open("https://www.facebook.com/business/help") },
                Modifier.fillMaxWidth(),
                icon = Icons.Rounded.SupportAgent
            )
        }
    }
}

private val Color8 = androidx.compose.ui.graphics.Color(0xFF455A64)
