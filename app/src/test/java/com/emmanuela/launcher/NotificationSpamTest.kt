package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.junit.Assert.*
import org.junit.Test

class NotificationSpamTest {
    private val conservative=NotificationRule(spamMode=SpamMode.CONSERVATIVE)
    private val strong=conservative.copy(spamMode=SpamMode.STRONG)
    @Test fun offAndAppAllowlistNeverFilter(){
        val classifier=NotificationSpamClassifier()
        assertNull(classifier.classify("shop","a","sale discount",0,NotificationRule()))
        assertNull(classifier.classify("shop","b","sale discount",1,strong.copy(spamAllowed=true)))
    }
    @Test fun conservativeRequiresRepeatedPromotionAndIgnoresConfigurationReplays(){
        val classifier=NotificationSpamClassifier()
        assertNull(classifier.classify("shop","a","Sale 10 today",0,conservative))
        repeat(5){assertNull(classifier.classify("shop","a","Sale 10 today",0,conservative))}
        assertNull(classifier.classify("shop","b","Sale 20 today",1000,conservative))
        assertEquals("Repeated promotional notification (3 in 5 minutes)",classifier.classify("shop","c","Sale 30 today",2000,conservative))
    }
    @Test fun conservativePreservesFrequentPersonalMessages(){
        val classifier=NotificationSpamClassifier()
        repeat(20){assertNull(classifier.classify("chat","id$it","Hello from a friend",it*1000L,conservative))}
    }
    @Test fun strongerModeUsesSimilarityAndMultiplePromotionalSignals(){
        val classifier=NotificationSpamClassifier()
        assertEquals("Multiple promotional patterns",classifier.classify("shop","a","Sale discount today",0,strong))
        assertNull(classifier.classify("chat","b","Repeated text",0,strong))
        assertNull(classifier.classify("chat","c","Repeated text",1,strong))
        assertEquals("Repeated similar notification (3 in 5 minutes)",classifier.classify("chat","d","Repeated text",2,strong))
    }
    @Test fun windowsExpireAndAppSignalsStayIndependent(){
        val classifier=NotificationSpamClassifier()
        assertNull(classifier.classify("one","a","Sale today",0,conservative))
        assertNull(classifier.classify("one","b","Sale today",1,conservative))
        assertNull(classifier.classify("two","c","Sale today",2,conservative))
        assertNull(classifier.classify("one","d","Sale today",300_002,conservative))
    }
    @Test fun oldActiveNotificationsDoNotBecomeANewSpamBurstAtReconnect(){
        val classifier=NotificationSpamClassifier()
        repeat(8){assertNull(classifier.classify("shop","old$it","Sale today",600_000,conservative,postedAt=0))}
    }
    @Test fun strongerModeRecognizesHighFrequencyPromotionsFromTheSameSource(){
        val classifier=NotificationSpamClassifier()
        repeat(7){assertNull(classifier.classify("shop","$it","Sale variant${'a'+it}",it*1000L,strong))}
        assertEquals("High frequency promotional notifications",classifier.classify("shop","8","Sale variantz",8_000,strong))
    }
    @Test fun explicitKeywordsHaveAllowlistAndEssentialNotificationGuards(){
        val classifier=NotificationSpamClassifier()
        val rule=strong.copy(spamKeywords=setOf("offer"),spamAllowWords=setOf("doctor"))
        assertNotNull(classifier.classify("one","a","An offer for today",0,rule))
        assertNull(classifier.classify("one","b","Doctor appointment offer",1,rule))
        assertNull(classifier.classify("one","c","Security alert: offer account verification code",2,rule))
        assertNull(classifier.classify("one","d","OTP promo code sale discount",3,rule))
        assertNull(classifier.classify("one","e","123456 sale discount offer",4,rule))
        assertNull(classifier.classify("one","f","验证码 sale discount offer",5,rule))
    }
    @Test fun spamRulesRoundTripAndOldRulesDefaultOff(){
        val rule=strong.copy(spamKeywords=setOf("promotion"),spamAllowWords=setOf("doctor"),spamAllowed=true)
        assertEquals(rule,NotificationRules.decode(NotificationRules.encode(rule)))
        val old=NotificationRules.encode(NotificationRule())
        listOf("spamMode","spamKeywords","spamAllowWords","spamAllowed").forEach(old::remove)
        assertEquals(NotificationRule(),NotificationRules.decode(old))
    }
    @Test(expected=IllegalArgumentException::class) fun invalidSpamKeywordCannotEnterPersistence(){
        NotificationRules.decode(NotificationRules.encode(strong).put("spamKeywords",org.json.JSONArray(listOf(" "))))
    }
    @Test fun globalDefaultPersistsAndExistingConfigurationsDefaultOff(){
        val prefs=ExperiencePreferences(spamMode=SpamMode.CONSERVATIVE)
        assertEquals(prefs,ExperienceCodec.decode(ExperienceCodec.encode(prefs)))
        assertEquals(SpamMode.OFF,ExperienceCodec.decode(org.json.JSONObject()).spamMode)
    }
}
