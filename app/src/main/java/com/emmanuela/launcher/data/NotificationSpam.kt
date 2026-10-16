package com.emmanuela.launcher.data

import java.security.MessageDigest
import java.util.Locale

/** Local, bounded signal history. Notification text never leaves this method or persists. */
class NotificationSpamClassifier {
    private data class Signal(val identity:String,val at:Long,val fingerprint:String)
    private val history=linkedMapOf<String,MutableList<Signal>>()
    @Synchronized
    fun classify(packageName:String,identity:String,text:String,now:Long,rule:NotificationRule,postedAt:Long=now):String? {
        if(rule.spamMode==null||rule.spamMode==SpamMode.OFF||rule.spamAllowed)return null
        if(rule.spamAllowWords.any{text.contains(it,true)})return null
        if(essentialText(text))return null
        val normalized=text.lowercase(Locale.ROOT).replace(urls,"<url>")
            .replace(numbers,"#").replace(spaces," ").trim()
        if(normalized.isEmpty())return null
        val fingerprint=MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
            .joinToString(""){"%02x".format(it)}
        val signals=history.getOrPut(packageName){mutableListOf()}
        signals.removeAll{now-it.at>5*60_000L||now<it.at}
        // Configuration refreshes and reposts of the same notification are not new arrivals.
        val at=postedAt.coerceAtMost(now)
        if(now-at<=5*60_000L&&signals.none{it.identity==identity})signals+=Signal(identity,at,fingerprint)
        while(signals.size>64)signals.removeAt(0)
        while(history.size>500)history.remove(history.keys.first())
        if(rule.spamKeywords.any{text.contains(it,true)})return "Matched your spam keyword"
        val copies=signals.count{it.fingerprint==fingerprint}
        val promotion=promotions.count{text.contains(it,true)}
        return when {
            rule.spamMode==SpamMode.CONSERVATIVE&&promotion>0&&copies>=3 -> "Repeated promotional notification (3 in 5 minutes)"
            rule.spamMode==SpamMode.STRONG&&promotion>=2 -> "Multiple promotional patterns"
            rule.spamMode==SpamMode.STRONG&&copies>=3 -> "Repeated similar notification (3 in 5 minutes)"
            rule.spamMode==SpamMode.STRONG&&promotion>0&&signals.count{now-it.at<=60_000}>=8 -> "High frequency promotional notifications"
            else -> null
        }
    }
    companion object {
        private val urls=Regex("https?://\\S+")
        private val numbers=Regex("\\d+")
        private val spaces=Regex("\\s+")
        private val possibleCode=Regex("(?<!\\d)\\d{4,8}(?!\\d)")
        private val promotions=listOf("sale","discount","limited time","special offer","buy now","promo code","deal ends","shop now","exclusive offer")
        private val essential=listOf("verification","security code","one-time","password","passcode","authentication","authenticator","security alert","sign-in","sign in","login code","emergency","otp",
            "missed call","incoming call","alarm","cod de verificare","autentificare","código de","codice di","code de sécurité","bestätigungscode","验证码","認証","인증","رمز","код")
        fun essentialText(text:String)=possibleCode.containsMatchIn(text)||essential.any{text.contains(it,true)}
    }
}
