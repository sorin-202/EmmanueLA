        val repository=ConfigurationRepository(target)
        val original=repository.data.first()
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=true,searchActions=setOf("app:$id")))),policies=mapOf("com.android.settings" to AppPolicy(blocked=true))))
            ActivityScenario.launch(MainActivity::class.java).use {
                click("All Apps")
                val field=find("Search field",match={it.className?.toString()=="android.widget.EditText"})
                assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Settings")}))
                click("Action: Settings")
                find("App unavailable in EmmanueLA")
                assertEquals(target.packageName,instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString())
            }
        }finally{repository.restore(original)}
    }
    @Test fun typedIntentionGatesLaunchAndIsNotPersisted()=runBlocking {
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=false))),
                focusGroups=listOf(FocusGroup(id="intent",name="Intent",packages=setOf("com.android.settings"),dailyMinutes=0,pause=true,pauseSeconds=0,requireIntention=true))))
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                click("All Apps")
                val search=find("Search field",match={it.className?.toString()=="android.widget.EditText"})
                assertTrue(search.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Settings")}))
                click("Settings app",match={it.contentDescription?.toString()?.contains(", com.android.settings")==true})
                find("Your intention")
                scenario.onActivity { activity ->
                    val model=androidx.lifecycle.ViewModelProvider(activity)[LauncherViewModel::class.java]
                    model.confirmPause("")
                    assertNotNull(model.pendingPause.value)
                }
                val intention=find("Intention field",match={it.className?.toString()=="android.widget.EditText"})
                assertTrue(intention.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Check device settings")}))
                click("Open app")
                find("Android settings window",match={it.packageName?.toString()=="com.android.settings"})
                assertFalse(ConfigurationCodec.encode(repository.data.first()).contains("Check device settings"))
            }
        }finally{repository.restore(original)}
    }
    @Test fun fixedNavigationControlsStayAboveSystemNavigation()=runBlocking<Unit> {
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=false)))))
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                find("All Apps")
                var safeBottom=0
                scenario.onActivity { activity ->
                    val decor=activity.window.decorView
                    val insets=androidx.core.view.ViewCompat.getRootWindowInsets(decor)
                    assertNotNull("Platform window insets available",insets)
                    val origin=IntArray(2);decor.getLocationOnScreen(origin)
                    safeBottom=origin[1]+decor.height-insets!!.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom
                }
                fun assertSafeControl(label:String) {
                    var node=find(label)
                    while(!node.isClickable&&node.parent!=null)node=node.parent
                    val bounds=android.graphics.Rect();node.getBoundsInScreen(bounds)
                    assertTrue(
    			"$label bounds $bounds must end above system navigation at $safeBottom",
    			bounds.bottom <= safeBottom + 1
)
                }
                click("All Apps")
                assertSafeControl("Hidden Apps")
                assertSafeControl("Organized Folders →")
                click("Organized Folders →")
                assertSafeControl("All Apps →")
                click("All Apps →")
                find("All Apps")
            }
        }finally{repository.restore(original)}
    }
    @Test fun appListWebIntentAndControlledBreakWorkflowRender()=runBlocking {
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        val observed=kotlinx.coroutines.flow.MutableStateFlow<LauncherData?>(null)
        var observer:kotlinx.coroutines.Job?=null
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=false))),
                focusGroups=listOf(FocusGroup(id="flow",name="Test Focus",packages=setOf("com.android.camera2"),dailyMinutes=0,pause=false,blockAlways=true))))
            // Observe before the click, as in the widget/editor tests: a cold
            // DataStore 1.1.7 subscription racing a write can miss its emission.
            observer=launch(kotlinx.coroutines.Dispatchers.IO){repository.data.collect{observed.value=it}}
            kotlinx.coroutines.withTimeout(15_000){observed.filterNotNull().first()}
            ActivityScenario.launch(MainActivity::class.java).use {
                click("All Apps")
                find("All Apps")
                val field=find("Search field",match={it.className?.toString()=="android.widget.EditText"})
                assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"/example query")}))
                find("Search the web: example query")
                click("Home")
                click("Launcher settings")
                click("Live the Moment",scroll=true)
                click("Test Focus",scroll=true)
                click("Take a Break",scroll=true)
                click("Start 10-minute Break")
                kotlinx.coroutines.withTimeout(15_000){observed.filterNotNull().first{it.focusGroups.single().breakUntil>System.currentTimeMillis()}}
                find("End Break",scroll=true)
                assertTrue(repository.data.first().focusGroups.single().breakUntil>System.currentTimeMillis())
                click("End Break")
                find("Take a Break")
                kotlinx.coroutines.withTimeout(15_000){observed.filterNotNull().first{it.focusGroups.single().breakUntil==0L}}
                assertEquals(0L,repository.data.first().focusGroups.single().breakUntil)
            }
        }finally{observer?.cancel();repository.restore(original)}
    }
}
