package com.emmanuela.launcher

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

/** Separate APK package, so background enforcement cannot mistake it for launcher admissions. */
class FocusFixtureActivity:Activity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            addView(TextView(this@FocusFixtureActivity).apply { text="External focus fixture";textSize=24f })
            addView(TextView(this@FocusFixtureActivity).apply {
                id=resources.getIdentifier("fixture_address","id",packageName)
                text="";contentDescription="Address bar, https://example.com/path"
                importantForAccessibility=android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
                minHeight=100
            })
        })
    }
}
