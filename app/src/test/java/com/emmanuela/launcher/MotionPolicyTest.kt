package com.emmanuela.launcher

import com.emmanuela.launcher.data.ExperiencePreferences
import com.emmanuela.launcher.ui.navigation.alphabetEffect
import com.emmanuela.launcher.ui.navigation.motionDuration
import org.junit.Assert.*
import org.junit.Test

class MotionPolicyTest {
    @Test fun accessibilityDisablesControlledAlphabetEffects(){
        val reduced=ExperiencePreferences(reduceMotion=true)
        assertEquals(0,reduced.motionDuration())
        assertEquals("None",reduced.alphabetEffect("Bubble"))
        assertEquals("None",ExperiencePreferences(motion="Off").alphabetEffect("Wave"))
    }
    @Test fun speedsAreConsistent(){
        assertEquals(200,ExperiencePreferences(motionSpeed="Fast").motionDuration())
        assertEquals(280,ExperiencePreferences(motionSpeed="Normal").motionDuration())
        assertEquals(360,ExperiencePreferences(motionSpeed="Slow").motionDuration())
    }
}
