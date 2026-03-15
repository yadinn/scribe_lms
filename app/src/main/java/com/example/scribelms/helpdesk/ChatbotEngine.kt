package com.example.scribelms.helpdesk

import com.google.firebase.firestore.FirebaseFirestore

object ChatbotEngine {

    enum class ResponseType { TEXT, QUICK_REPLIES, ESCALATE }

    data class BotResponse(
        val text: String,
        val type: ResponseType = ResponseType.TEXT,
        val quickReplies: List<String> = emptyList(),
        val imageUrl: String? = null    // optional book cover URL
    )

    private data class FaqEntry(val keywords: List<String>, val response: BotResponse)

    private val FAQ = listOf(
        FaqEntry(listOf("hi","hello","hey","hii","helo","good morning","good afternoon","good evening","start"),
            BotResponse("👋 Hello! I'm your *Library Assistant*.\n\nI can help you with:",
                ResponseType.QUICK_REPLIES,
                listOf("📚 How to borrow?","🔍 Find a book","💰 Check fines","📦 How to return?","📘 E-Books","📞 Contact library"))),

        FaqEntry(listOf("borrow","issue book","request book","how to get","take book","get a book","how to borrow","issue a book"),
            BotResponse("📚 *How to Borrow a Book*\n\n" +
                "1️⃣ Tap *Browse Books* on your home screen\n" +
                "2️⃣ Find the book → tap *Request Issue*\n" +
                "3️⃣ Wait for staff approval — you'll be notified ✅\n" +
                "4️⃣ Once approved, open *Collection QR* and show it at the counter\n" +
                "5️⃣ Staff scans it → book is issued to you!\n\n" +
                "⏳ The QR expires in 24 hrs — collect quickly.",
                ResponseType.QUICK_REPLIES,
                listOf("📦 How to return?","📋 Request status","📚 Book limit","🔍 Find a book"))),

        FaqEntry(listOf("return book","how to return","return a book","give back","returning"),
            BotResponse("📦 *How to Return a Book*\n\n" +
                "1️⃣ Tap *Issued Books* on your home screen\n" +
                "2️⃣ Tap *Return* on the book you want to return\n" +
                "3️⃣ A *Return QR* appears — show it at the counter\n" +
                "4️⃣ Staff scans it → return completed ✅\n\n" +
                "⚠️ Returning after the due date will incur a fine.",
                ResponseType.QUICK_REPLIES,
                listOf("💰 What is the fine?","📅 Check due date","📚 How to borrow?"))),

        FaqEntry(listOf("fine","penalty","late fee","overdue charge","how much fine","late return"),
            BotResponse("💰 *Library Fines*\n\n" +
                "• Fine is charged per *day past the due date*\n" +
                "• Typically ₹5–₹10 per day (set by admin)\n" +
                "• View all fines under *Fine History* on your home screen\n" +
                "• Pay at the library counter\n" +
                "• ⚠️ Unpaid fines block new book requests",
                ResponseType.QUICK_REPLIES,
                listOf("📅 Check due date","📦 How to return?","📞 Contact library"))),

        FaqEntry(listOf("due date","when to return","deadline","return date","how many days","loan period"),
            BotResponse("📅 *Due Date*\n\n" +
                "• Your due date is shown in *Issued Books*\n" +
                "• Standard loan period is set by the admin (usually 14 days)\n" +
                "• ⚠️ OVERDUE label appears when you've passed the due date\n" +
                "• Return as soon as possible to avoid fines!",
                ResponseType.QUICK_REPLIES,
                listOf("📦 How to return?","💰 What is the fine?"))),

        FaqEntry(listOf("request status","check request","pending","approved","rejected","my request","request pending"),
            BotResponse("📋 *Request Status*\n\n" +
                "Go to *My Requests* on the home screen.\n\n" +
                "🕐 *Pending* — waiting for approval\n" +
                "✅ *Approved* — collect using Collection QR!\n" +
                "📖 *Issued* — book is with you\n" +
                "📦 *Returned* — successfully returned\n" +
                "❌ *Rejected* — declined by staff\n" +
                "⏰ *Expired* — 24hr window passed",
                ResponseType.QUICK_REPLIES,
                listOf("📚 How to borrow?","📱 Collection QR"))),

        FaqEntry(listOf("collection qr","qr code","collect book","pickup","where to collect","show qr","collection card"),
            BotResponse("📱 *Collection QR*\n\n" +
                "After your request is *Approved*:\n" +
                "1️⃣ Tap *Collection QR* on the home screen\n" +
                "2️⃣ Show the QR to the librarian\n" +
                "3️⃣ They scan it → book is issued ✅\n\n" +
                "⏳ QR expires in 24 hours. Request again if expired.",
                ResponseType.QUICK_REPLIES,
                listOf("📚 How to borrow?","📋 Check status"))),

        FaqEntry(listOf("ebook","e-book","digital book","read online","pdf book","electronic book"),
            BotResponse("📘 *E-Books*\n\n" +
                "• Tap *E-Books* on your home screen\n" +
                "• Read digital books instantly — no borrowing needed!\n" +
                "• No due dates, no fines 🎉\n" +
                "• Opens in your browser or PDF viewer",
                ResponseType.QUICK_REPLIES,
                listOf("📎 Study resources","📚 Borrow physical book"))),

        FaqEntry(listOf("resource","study material","notes","pdf resource","study resources"),
            BotResponse("📎 *Study Resources*\n\n" +
                "• Tap *Resources* on your home screen\n" +
                "• Staff uploads PDFs, notes, and links here\n" +
                "• Tap any resource to open or download it",
                ResponseType.QUICK_REPLIES,
                listOf("📘 E-Books","📚 How to borrow?"))),

        FaqEntry(listOf("book limit","how many books","maximum books","books allowed","borrowing limit"),
            BotResponse("📋 *Borrowing Limit*\n\n" +
                "• Max books you can hold at once is set by the admin (typically 3)\n" +
                "• Cannot request more until you return a book\n" +
                "• Check your count in *Issued Books*",
                ResponseType.QUICK_REPLIES,
                listOf("📦 How to return?","📚 How to borrow?"))),

        FaqEntry(listOf("notification","alert","not getting","notify","bell"),
            BotResponse("🔔 *Notifications*\n\n" +
                "You get notified when:\n" +
                "• Request is *approved or rejected*\n" +
                "• Book is *issued* to you\n" +
                "• Fine is *marked as paid*\n" +
                "• New *announcements* posted",
                ResponseType.QUICK_REPLIES,
                listOf("📋 Check request status","📢 Announcements"))),

        FaqEntry(listOf("announcement","notice","news","holiday","library closed"),
            BotResponse("📢 *Announcements*\n\n" +
                "• Tap *Announcements* on your home screen\n" +
                "• Staff posts holidays, new arrivals, rule changes here\n" +
                "• 🔴 Badge appears when there are unread announcements",
                ResponseType.QUICK_REPLIES,
                listOf("📞 Contact library","📚 How to borrow?"))),

        FaqEntry(listOf("profile","edit profile","update name","change contact"),
            BotResponse("👤 *Edit Profile*\n\n" +
                "• Scroll to the bottom of the home screen\n" +
                "• Tap *Edit Profile*\n" +
                "• Update: Name, Contact Number, Branch\n" +
                "• 🔒 Email cannot be changed — contact admin",
                ResponseType.QUICK_REPLIES,
                listOf("📞 Contact library"))),

        FaqEntry(listOf("contact","phone","library number","email library","reach","address","opening time","timing","hours"),
            BotResponse("📞 *Contact the Library*\n\n" +
                "• 💬 *Live Chat* — tap the Staff button to chat with a librarian directly\n" +
                "• 🏫 Visit the library counter during working hours\n" +
                "• 📢 Check *Announcements* for notices",
                ResponseType.ESCALATE,
                listOf("👤 Talk to Staff","📢 Check announcements"))),

        FaqEntry(listOf("thank","thanks","thank you","ok thanks","great","got it","noted"),
            BotResponse("😊 You're welcome! Anything else I can help with?",
                ResponseType.QUICK_REPLIES,
                listOf("📚 How to borrow?","🔍 Find a book","👤 Talk to Staff"))),

        FaqEntry(listOf("review","rating","leave review","give feedback","rate book"),
            BotResponse("⭐ *Book Reviews*\n\n" +
                "• After returning a book, go to *Returned Books*\n" +
                "• Tap *Add Review* to rate and comment\n" +
                "• Your reviews help other students choose books!",
                ResponseType.QUICK_REPLIES,
                listOf("📦 How to return?","📚 Browse books")))
    )

    fun processInput(input: String, onResult: (BotResponse) -> Unit) {
        val lower = input.trim().lowercase()

        if (wantsStaff(lower)) {
            onResult(BotResponse(
                "🤝 *Connecting to Staff*\n\n" +
                "I'll switch you to *Live Staff Chat*.\n" +
                "A librarian will reply during library hours.",
                ResponseType.ESCALATE,
                listOf("👤 Yes, connect me","🤖 Stay with bot")))
            return
        }

        val bookQuery = extractBookQuery(lower)
        if (bookQuery != null) {
            searchBooks(bookQuery, onResult)
            return
        }

        for (entry in FAQ) {
            if (entry.keywords.any { lower.contains(it) }) {
                onResult(entry.response)
                return
            }
        }

        onResult(BotResponse(
            "🤔 I'm not sure about that. Here's what I can help with:",
            ResponseType.QUICK_REPLIES,
            listOf("📚 How to borrow?","🔍 Find a book","💰 Check fines","📦 How to return?","👤 Talk to Staff")))
    }

    private fun wantsStaff(lower: String): Boolean {
        return listOf("talk to staff","chat with staff","connect staff","real person",
            "speak to staff","live chat","need staff","connect me to staff",
            "talk to librarian","staff member","yes, connect","👤 talk").any { lower.contains(it) }
    }

    // Detect: "book python", "find python", "search calculus", "is there a hamlet" etc.
    private fun extractBookQuery(lower: String): String? {
        // Direct "book <title>" pattern — simplest and most common
        val bookMatch = Regex("""(?:^|\s)book\s+(.{2,50})""").find(lower)
        if (bookMatch != null) return bookMatch.groupValues[1].trim()

        val patterns = listOf(
            "find book ","search book ","find a book ","search for a book ",
            "is there a ","do you have ","looking for ","find ","search "
        )
        for (p in patterns) {
            val idx = lower.indexOf(p)
            if (idx >= 0) {
                val after = lower.substring(idx + p.length).trim()
                // Exclude generic words
                if (after.length >= 2 && after !in listOf("book","books","a book","the book")) return after
            }
        }
        if (lower.contains("available") && lower.length > 12) {
            val cleaned = lower.replace("available","").replace("is ","").replace("  "," ").trim()
            if (cleaned.length >= 3) return cleaned
        }
        return null
    }

    private fun searchBooks(query: String, onResult: (BotResponse) -> Unit) {
        val q = query.lowercase().trim()
        FirebaseFirestore.getInstance().collection("books").get()
            .addOnSuccessListener { snap ->
                val matches = snap.documents.filter { doc ->
                    val title  = doc.getString("title")?.lowercase()    ?: ""
                    val author = doc.getString("author")?.lowercase()   ?: ""
                    val cat    = doc.getString("category")?.lowercase() ?: ""
                    title.contains(q) || author.contains(q) || cat.contains(q)
                }

                if (matches.isEmpty()) {
                    onResult(BotResponse(
                        "📚 No books found matching *\"$query\"*.\n\n" +
                        "💡 Tip: Type *book* followed by the title or author name.\n" +
                        "Example: *book python* or *book john*",
                        ResponseType.QUICK_REPLIES,
                        listOf("🔍 Search again","👤 Ask Staff","📚 Browse books")))
                } else {
                    val first = matches.first()
                    val sb = StringBuilder()
                    sb.appendLine("📚 Found *${matches.size}* result(s) for *\"$query\"*:\n")
                    matches.take(4).forEach { doc ->
                        val title  = doc.getString("title")  ?: "Unknown"
                        val author = doc.getString("author") ?: "-"
                        val avail  = doc.getLong("availableCopies") ?: 0
                        val total  = doc.getLong("totalCopies") ?: avail
                        val shelf  = doc.getString("shelfLocation")
                        val status = if (avail > 0) "✅ Available ($avail/$total copies)" else "❌ All copies issued"
                        sb.appendLine("📖 *$title*")
                        sb.appendLine("   ✍️ $author")
                        sb.appendLine("   $status")
                        if (!shelf.isNullOrBlank()) sb.appendLine("   📍 Shelf: $shelf")
                        sb.appendLine()
                    }
                    if (matches.size > 4) sb.appendLine("...and *${matches.size - 4}* more.")

                    // Try to get cover image from the first match
                    val imageUrl = first.getString("imageUrl")
                        ?: first.getString("coverUrl")
                        ?: first.getString("thumbnail")

                    onResult(BotResponse(
                        sb.toString().trimEnd(),
                        ResponseType.QUICK_REPLIES,
                        listOf("📚 How to borrow?","🔍 Search another","👤 Ask Staff"),
                        imageUrl = imageUrl
                    ))
                }
            }
            .addOnFailureListener {
                onResult(BotResponse(
                    "⚠️ Couldn't search books right now. Please try again shortly.",
                    ResponseType.QUICK_REPLIES,
                    listOf("🔍 Try again","👤 Talk to Staff")))
            }
    }
}
