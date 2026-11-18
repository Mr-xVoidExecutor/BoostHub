package com.boosthub.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.boosthub.app.data.MetaApi
import com.boosthub.app.data.Store
import com.boosthub.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BillingScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val s = remember { Store(ctx) }
    val scope = rememberCoroutineScope()
    var b by remember { mutableStateOf<MetaApi.Billing?>(null) }
    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var capInput by remember { mutableStateOf("") }

    fun open(url: String) {
        try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { msg = "ব্রাউজার খোলা যায়নি" }
    }

    fun load() {
        scope.launch {
            loading = true; msg = ""
            try { b = withContext(Dispatchers.IO) { MetaApi.billing(s) } }
            catch (e: Exception) { msg = e.message ?: "এরর" }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        if (s.fbToken.isNotBlank() && s.adAccount.isNotBlank()) load()
        else msg = "আগে সেটিংসে ফেসবুক টোকেন ও অ্যাড অ্যাকাউন্ট আইডি দিন"
    }

    ScreenColumn(modifier) {
        item { H("বিলিং ও পেমেন্ট") }
        item { BrandButton(if (loading) "লোড হচ্ছে..." else "রিফ্রেশ", { load() }, enabled = !loading) }
        if (msg.isNotBlank()) item { Panel { Soft(msg) } }

        val x = b
        if (x != null) {
            val cur = x.currency
            fun money(v: Long) = "${"%.2f".format(v / 100.0)} $cur"

            // 1. Your account
            item {
                Panel {
                    White("আপনার অ্যাকাউন্ট", bold = true)
                    Soft("${x.name} (${x.id})")
                    Soft("স্ট্যাটাস: ${MetaApi.statusText(x.status)}")
                }
            }
            // 2. Outstanding balance
            item {
                Panel {
                    White("বকেয়া ব্যালেন্স", bold = true)
                    H(money(x.balance))
                    Soft("মোট খরচ (লাইফটাইম): ${money(x.amountSpent)}")
                    Spacer(Modifier.height(8.dp))
                    BrandButton("এখনই পরিশোধ করুন (Pay now)", { open(MetaApi.billingUrl(s)) })
                }
            }
            // 3. Funds
            item {
                Panel {
                    White("ফান্ড", bold = true)
                    H(money(x.prepay))
                    Spacer(Modifier.height(8.dp))
                    BrandButton("ফান্ড যোগ করুন (Add funds)", { open(MetaApi.billingUrl(s)) })
                }
            }
            // 4. Payment methods
            item {
                Panel {
                    White("পেমেন্ট মেথড", bold = true)
                    Soft(if (x.funding.isBlank()) "কোনো মেথড পাওয়া যায়নি" else "${x.funding} • Default")
                    Spacer(Modifier.height(8.dp))
                    BrandButton("পেমেন্ট মেথড যোগ করুন", { open(MetaApi.billingUrl(s)) })
                    Spacer(Modifier.height(4.dp))
                    Soft("কার্ডের তথ্য নিরাপত্তার জন্য শুধু Meta এর নিজের পেজে দিতে হয়, এই অ্যাপে নেওয়া হয় না।")
                }
            }
            // 5. Payment threshold
            item {
                Panel {
                    White("পেমেন্ট থ্রেশহোল্ড", bold = true)
                    Soft(if (x.threshold > 0) "প্রতিবার ${money(x.threshold)} খরচ হলে চার্জ হবে" else "তথ্য পাওয়া যায়নি")
                    Spacer(Modifier.height(8.dp))
                    BrandButton("পরিবর্তন করুন", { open(MetaApi.billingUrl(s)) })
                }
            }
            // 6. Account spending limit (editable)
            item {
                Panel {
                    White("অ্যাকাউন্ট স্পেন্ডিং লিমিট", bold = true)
                    Soft(if (x.spendCap > 0) "সেট করা আছে: ${money(x.spendCap)}" else "সেট করা নেই")
                    Spacer(Modifier.height(8.dp))
                    Field("নতুন লিমিট ($cur)", capInput, { capInput = it.filter { c -> c.isDigit() } })
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrandButton("লিমিট সেট করুন", {
                            val v = capInput.toLongOrNull()
                            if (v == null || v <= 0) { msg = "সঠিক সংখ্যা দিন" } else scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { MetaApi.setSpendCap(s, v * 100) }
                                    msg = "লিমিট সেট হয়েছে ✅"; capInput = ""; load()
                                } catch (e: Exception) { msg = e.message ?: "এরর" }
                            }
                        })
                        BrandButton("লিমিট তুলে দিন", {
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { MetaApi.setSpendCap(s, 0) }
                                    msg = "লিমিট তুলে দেওয়া হয়েছে ✅"; load()
                                } catch (e: Exception) { msg = e.message ?: "এরর" }
                            }
                        })
                    }
                }
            }
            // 7. Payment activity
            item {
                Panel {
                    White("পেমেন্ট অ্যাক্টিভিটি", bold = true)
                    Soft("শেষ ৭ দিনের খরচ")
                    if (x.daily.isEmpty()) Soft("কোনো খরচ নেই")
                    x.daily.forEach { (d, v) -> Soft("$d — ${"%.2f".format(v)} $cur") }
                    Spacer(Modifier.height(8.dp))
                    BrandButton("স্টেটমেন্ট ও পেন্ডিং চার্জ দেখুন", { open(MetaApi.billingUrl(s)) })
                }
            }
            // 8. Business info
            item {
                Panel {
                    White("ব্যবসার তথ্য", bold = true)
                    Soft(x.bizName.ifBlank { "নাম পাওয়া যায়নি" })
                    if (x.bizAddress.isNotBlank()) Soft(x.bizAddress)
                    if (x.taxId.isNotBlank()) Soft("ট্যাক্স আইডি: ${x.taxId}")
                    Spacer(Modifier.height(8.dp))
                    BrandButton("তথ্য এডিট করুন", { open(MetaApi.billingUrl(s)) })
                }
            }
        }
        // 9. Get support
        item {
            BrandButton("সাপোর্ট নিন (Get support)", { open("https://www.facebook.com/business/help") }, modifier = Modifier.fillMaxWidth())
        }
    }
}
