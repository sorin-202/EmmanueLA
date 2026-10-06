package com.emmanuela.launcher

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emmanuela.launcher.data.AppSearchIndex
import com.emmanuela.launcher.data.LaunchableApp
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Diagnostic workload, not a device-independent performance acceptance threshold. */
@RunWith(AndroidJUnit4::class)
class SearchMeasurementTest {
    @Test fun measureFiveHundredAppSearch() {
        val apps=(0 until 500).map { i ->
            LaunchableApp("example.app$i/.Main","Café Companion $i","example.app$i",tags=listOf(if(i%2==0)"work" else "personal"))
        }
        val start=SystemClock.elapsedRealtimeNanos()
        val index=AppSearchIndex(apps)
        val indexMs=(SystemClock.elapsedRealtimeNanos()-start)/1_000_000.0
        val queries=listOf("cafe companion 42","comp 42","panion","missing","#wor")
        repeat(10){queries.forEach{index.search(it)}}
        val times=mutableListOf<Double>()
        var matches=0
        repeat(40){queries.forEach { query ->
            val began=SystemClock.elapsedRealtimeNanos()
            matches+=index.search(query).apps.size
            times+=(SystemClock.elapsedRealtimeNanos()-began)/1_000_000.0
        }}
        assertTrue(matches>0)
        times.sort()
        Log.i("EmaMeasurement","search apps=500 samples=${times.size} indexMs=$indexMs p50Ms=${times[times.size/2]} p95Ms=${times[(times.size*.95).toInt()]} maxMs=${times.last()} matches=$matches")
    }
}
