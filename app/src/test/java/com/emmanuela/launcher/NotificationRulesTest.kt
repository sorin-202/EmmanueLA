package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import java.time.ZonedDateTime
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NotificationRulesTest {
    private val now=ZonedDateTime.parse("2026-10-05T23:00:00+03:00[Europe/Bucharest]")
    private val dismiss=AppPolicy(notifications=NotificationMode.DISMISS)
    private fun action(p:AppPolicy,time:ZonedDateTime=now,groups:List<FocusGroup> = emptyList(),text:()->String={error("Content must not be accessed")})=
        NotificationRules.action(p,"app",groups,time,true,text)
    @Test fun defaultAndDisabledDigestNeverReadContent(){
        assertEquals(NotificationMode.NORMAL,action(AppPolicy()))
        assertEquals(NotificationMode.DISMISS,action(dismiss))
        assertEquals(NotificationMode.NORMAL,NotificationRules.action(dismiss.copy(notifications=NotificationMode.DIGEST),"app",emptyList(),now,false){error("No content")})
    }
    @Test fun overnightUsesStartingDayAndExclusiveEnd(){
        val p=dismiss.copy(notificationRule=NotificationRule(scope=NotificationScope.SCHEDULE,days=setOf(1)))
        assertEquals(NotificationMode.DISMISS,action(p))
        assertEquals(NotificationMode.DISMISS,action(p,now.plusHours(7)))
        assertEquals(NotificationMode.NORMAL,action(p,now.plusHours(8)))
        assertEquals(NotificationMode.NORMAL,action(p,now.plusDays(1)))
    }
    @Test fun equalScheduleTimesMeanSelectedWholeDay(){
        val p=dismiss.copy(notificationRule=NotificationRule(scope=NotificationScope.SCHEDULE,days=setOf(1),startMinute=0,endMinute=0))
        assertEquals(NotificationMode.DISMISS,action(p,now.withHour(0)))
        assertEquals(NotificationMode.NORMAL,action(p,now.plusDays(1)))
    }
    @Test fun optionalKeywordsUseCaseInsensitiveAnyMatch(){
        val p=dismiss.copy(notificationRule=NotificationRule(keywords=setOf("sale","offer")))
        assertEquals(NotificationMode.DISMISS,action(p,text={"An OFFER for today"}))
        assertEquals(NotificationMode.NORMAL,action(p,text={"Message from a friend"}))
        assertEquals(NotificationMode.NORMAL,action(p,text={""}))
    }
    @Test fun inactiveScheduleDoesNotReadText(){
        val p=dismiss.copy(notificationRule=NotificationRule(scope=NotificationScope.SCHEDULE,days=setOf(2),keywords=setOf("sale")))
        assertEquals(NotificationMode.NORMAL,action(p))
    }
    @Test fun focusRequiresTargetAppAndHonorsBreak(){
        val p=dismiss.copy(notificationRule=NotificationRule(scope=NotificationScope.FOCUS))
        val group=FocusGroup(packages=setOf("app"),blockAlways=true)
        assertEquals(NotificationMode.DISMISS,action(p,groups=listOf(group)))
        assertEquals(NotificationMode.NORMAL,action(p,groups=listOf(group.copy(packages=setOf("other")))))
        val epoch=now.toInstant().toEpochMilli()
        assertEquals(NotificationMode.NORMAL,action(p,groups=listOf(group.copy(breakStartedAt=epoch,breakUntil=epoch+60_000))))
    }
    @Test fun temporarySuppressionExpiresAndDoesNotTravelInBackups(){
        val p=AppPolicy(notificationRule=NotificationRule(suppressUntil=now.plusMinutes(30).toInstant().toEpochMilli()))
        assertEquals(NotificationMode.DISMISS,action(p))
        assertEquals(NotificationMode.NORMAL,action(p,now.plusMinutes(30)))
        assertEquals(0L,ConfigurationCodec.portable(LauncherData(policies=mapOf("app" to p))).policies.getValue("app").notificationRule.suppressUntil)
    }
    @Test fun codecRoundTripAndOldPolicyDefault(){
        val p=dismiss.copy(notificationRule=NotificationRule(scope=NotificationScope.SCHEDULE,keywords=setOf("sale")))
        val encoded=PolicyCodec.encode(mapOf("app" to p))
        assertEquals(p,PolicyCodec.decode(encoded).getValue("app"))
        encoded.getJSONObject("app").remove("notificationRule")
        assertEquals(NotificationRule(),PolicyCodec.decode(encoded).getValue("app").notificationRule)
    }
    @Test(expected=IllegalArgumentException::class) fun invalidRulesAreRejected(){
        NotificationRules.decode(NotificationRules.encode(NotificationRule()).put("start",1440))
    }
}
