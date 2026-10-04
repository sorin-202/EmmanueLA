package com.emmanuela.launcher

import com.emmanuela.launcher.data.AppFolder
import com.emmanuela.launcher.data.AppMetadata
import com.emmanuela.launcher.data.AppNaming
import com.emmanuela.launcher.data.AppPolicy
import com.emmanuela.launcher.data.AppSearch
import com.emmanuela.launcher.data.AppSearchIndex
import com.emmanuela.launcher.data.ConfigurationRepository
import com.emmanuela.launcher.data.ExperiencePreferences
import com.emmanuela.launcher.data.Favorite
import com.emmanuela.launcher.data.FocusWindows
import com.emmanuela.launcher.data.FocusCodec
import com.emmanuela.launcher.data.FocusGroup
import com.emmanuela.launcher.data.FolderPassword
import com.emmanuela.launcher.data.FolderPlacement
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.data.NotificationMode
import com.emmanuela.launcher.data.PendingPause
import com.emmanuela.launcher.data.PolicyRules
import com.emmanuela.launcher.data.Preferences
import com.emmanuela.launcher.data.UiPreferences
import com.emmanuela.launcher.data.V2Preferences
import com.emmanuela.launcher.platform.AppRepository
import com.emmanuela.launcher.platform.DigestRepository
import com.emmanuela.launcher.platform.NotificationBadges
import com.emmanuela.launcher.platform.WallpaperCache
import com.emmanuela.launcher.platform.recentGroupSession
import com.emmanuela.launcher.platform.scheduleSessionReminder
import com.emmanuela.launcher.platform.appUsageToday
import com.emmanuela.launcher.platform.hasUsageAccess
import com.emmanuela.launcher.platform.lockReady
import com.emmanuela.launcher.platform.screenTimeToday
import com.emmanuela.launcher.platform.updateDigestWork


import android.app.Application
import android.app.KeyguardManager
import android.content.Intent
import java.time.ZonedDateTime
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val systemActions=com.emmanuela.launcher.platform.SystemActionController(application)
    override fun onCleared(){systemActions.close();super.onCleared()}
    fun toggleTorch(onPermissionNeeded:()->Unit){viewModelScope.launch{try{if(!systemActions.toggleTorch())error.value="This device has no available rear flashlight."}catch(_:SecurityException){onPermissionNeeded()}catch(_:android.hardware.camera2.CameraAccessException){error.value="Flashlight is temporarily unavailable while the camera is in use."}catch(_:IllegalArgumentException){error.value="Flashlight is unavailable on this device."}}}
    fun toggleDnd(onPermissionNeeded:()->Unit){viewModelScope.launch{try{if(!systemActions.toggleDnd())onPermissionNeeded()}catch(_:SecurityException){onPermissionNeeded()}}}
    val appRepository = AppRepository(application)
    private val repository = ConfigurationRepository(application)
    val homeEpoch = MutableStateFlow(0)
    val searchEpoch=MutableStateFlow(0)
    private val _apps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    private val _data = MutableStateFlow(LauncherData())
    val data = _data.asStateFlow()
    val apps = combine(_apps, _data.map { it.appMetadata }.distinctUntilChanged()) { catalog, metadata -> AppNaming.decorate(catalog, metadata) }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val drawerIndex=combine(apps,_data.map{state->Triple(
        state.policies.filterValues{it.hidden}.keys,
        state.policies.filterValues{it.privateApp}.keys,
        state.settings.ui.experience.hideFromSearch to state.settings.ui.experience.hidePrivateSearch
    )}.distinctUntilChanged()){catalog,state->
        val (hidden,privateApps,visibility)=state
        val searchable=if(visibility.second)catalog.filter{it.packageName !in privateApps}else catalog
        AppSearchIndex(searchable,hidden,visibility.first)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope,SharingStarted.Eagerly,AppSearchIndex(emptyList()))

    suspend fun bulkTags(packages:Set<String>,input:String):Boolean = write {
        val tags=AppNaming.metadata("",input).tags
        val ids=apps.value.filter{it.packageName in packages}.map{it.id}
        repository.update{state->
            require(packages.none{state.policies[it]?.hidden==true}||hiddenUnlocked.value)
            val metadata=state.appMetadata.toMutableMap()
            ids.forEach{id->val old=metadata[id]?:AppMetadata();metadata[id]=AppNaming.metadata(old.alias,(old.tags+tags).joinToString(" "))}
            state.copy(appMetadata=metadata)
        }
    }
    suspend fun bulkFolder(packages:Set<String>,folderId:String):Boolean = write {
        val ids=apps.value.filter{it.packageName in packages}.map{it.id}
        repository.update{state->
            require(packages.none{state.policies[it]?.hidden==true}||hiddenUnlocked.value)
            val folder=state.folders.first{it.id==folderId}
            require(!folder.isProtected||folderId in unlockedFolders.value)
            state.copy(folders=state.folders.map{if(it.id==folderId)it.copy(apps=(it.apps+ids).distinct())else it})
        }
    }
    fun experienceSettings(transform:(ExperiencePreferences)->ExperiencePreferences)=uiSettings{it.copy(experience=transform(it.experience))}
    fun v2Settings(transform: (V2Preferences) -> V2Preferences) = uiSettings { it.copy(v2 = transform(it.v2)) }
    fun rememberTags(query:String,app:LaunchableApp) {
        if(!AppSearch.isTagMode(query))return
        val words=runCatching{AppNaming.metadata("",query).tags}.getOrDefault(emptyList())
        val used=words.flatMap{word->app.tags.filter{it.startsWith(word)}}
        if(used.isNotEmpty())v2Settings{it.copy(recentTags=(used+it.recentTags).distinct().take(3))}
    }
    fun uiSettings(transform: (UiPreferences) -> UiPreferences) = settings { it.copy(ui = transform(it.ui)) }
    val ready = MutableStateFlow(false)
    val loading = MutableStateFlow(true)
    val error = MutableStateFlow<String?>(null)
    val iconGeneration = MutableStateFlow(0)
    val screenTime = MutableStateFlow<Long?>(null)
    val usageAllowed = MutableStateFlow(false)
    val adminAllowed = MutableStateFlow(false)
    val unlockedFolders = MutableStateFlow<Set<String>>(emptySet())
    private val folderFailures = mutableMapOf<String, Pair<Int, Long>>()
    val hiddenUnlocked = MutableStateFlow(false)
    private var securityTimeout:Job?=null
    fun relockPrivateSpace(){securityTimeout?.cancel();hiddenUnlocked.value=false;unlockedFolders.value=emptySet();strictAuthorizations.clear();if(pendingPrivate)cancelPause()}
    private var authenticationPending=false
    private var authenticationReturnUntil=0L
    fun authenticationChanged(active:Boolean){authenticationPending=active;if(!active)authenticationReturnUntil=android.os.SystemClock.elapsedRealtime()+1000L}
    fun isAuthenticationReturn()=authenticationPending || android.os.SystemClock.elapsedRealtime()<authenticationReturnUntil
    fun backgroundSecurity(){if(isAuthenticationReturn())return;when(data.value.settings.ui.experience.autoLock){
        "30 seconds"->{securityTimeout?.cancel();securityTimeout=viewModelScope.launch{kotlinx.coroutines.delay(30_000);relockPrivateSpace()}}
        "Screen off"->Unit
        else->relockPrivateSpace()
    }}
    var usageVisible = false
    val appUsage = MutableStateFlow<Map<String, Long>?>(null)
    val openDigest = MutableStateFlow(false)
    val privateRequest=MutableStateFlow<String?>(null)
    val blockedMessage = MutableStateFlow<String?>(null)
    private var digestScheduled: Boolean? = null
    private var appUsageJob: Job? = null
    private var refreshJob: Job? = null
    private var observeJob: Job? = null
    private var usageJob: Job? = null
    init { observe(); refresh() }
    fun observe() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            ready.value = false
            try {
                repository.migrate()
                repository.data.collect {
                    _data.value = it; ready.value = true
                    NotificationBadges.enabled.value=it.settings.ui.v2.showBadges
                    NotificationBadges.muted.value=it.policies.filterValues{p->p.badgesMuted||p.hidden}.keys
                    val enabled = it.settings.ui.v2.notificationFilter && it.settings.ui.experience.digestEnabled && it.policies.values.any { p -> p.notifications == NotificationMode.DIGEST }
                    if (digestScheduled != enabled) {
                        getApplication<Application>().updateDigestWork(enabled); digestScheduled = enabled
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error.value = "Could not read configuration. Saved data has not been overwritten." }
        }
    }
    fun refresh(invalidateIcons: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (invalidateIcons) { appRepository.invalidateIcons(); iconGeneration.value++ }
            loading.value = true
            try { _apps.value = appRepository.load() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error.value = "Could not load apps. Try again." }
            finally { loading.value = false }
        }
    }
    fun refreshUsage() {
        val context = getApplication<Application>()
        usageAllowed.value = context.hasUsageAccess()
        adminAllowed.value = context.lockReady()
        if (usageVisible) refreshAppUsage()
        usageJob?.cancel()
        usageJob = viewModelScope.launch {
            try { screenTime.value = if (_data.value.settings.showScreenTime) context.screenTimeToday() else null }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { screenTime.value = null }
        }
    }
    fun refreshAppUsage() {
        appUsageJob?.cancel()
        appUsageJob = viewModelScope.launch {
            try { appUsage.value = getApplication<Application>().appUsageToday() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { appUsage.value = null }
        }
    }
    suspend fun changePolicies(packages: Set<String>, transform: (AppPolicy) -> AppPolicy): Boolean = write {
        val context = getApplication<Application>()
        repository.update { state ->
            val updated = state.policies.toMutableMap()
            packages.forEach { pkg ->
                require(pkg != context.packageName)
                val old = updated[pkg] ?: AppPolicy()
                val policy = transform(old)
                require(!policy.privateApp || context.getSystemService(KeyguardManager::class.java).isDeviceSecure) { "Set a device PIN before protecting private apps." }
                if (policy == AppPolicy()) updated.remove(pkg) else updated[pkg] = policy
            }
            state.copy(policies = updated)
        }
    }
    private suspend fun write(block: suspend () -> Unit): Boolean = try { block(); true }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { error.value = "Could not save changes. Please try again."; false }
    fun settings(transform: (Preferences) -> Preferences) {
        if (!ready.value) return
        viewModelScope.launch { write { repository.update { state->val next=transform(state.settings);state.copy(settings=next.copy(ui=next.ui.copy(v2=next.ui.v2.copy(photoStyles=next.ui.v2.photoStyles.filterKeys{it in next.wallpapers})))) } } }
    }
    fun unlockFolderWithDevice(id:String):Boolean {
        val folder=data.value.folders.find{it.id==id}?:return false
        if(!folder.biometricUnlock||!getApplication<Application>().getSystemService(android.app.KeyguardManager::class.java).isDeviceSecure)return false
        unlockedFolders.value=unlockedFolders.value+id;return true
    }
    suspend fun unlockFolder(id: String, password: String): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        val previous = folderFailures[id] ?: (0 to 0L)
        if (previous.second > now) { error.value = "Wait before trying this folder password again."; return false }
        val folder = _data.value.folders.find { it.id == id } ?: return false
        val matched = kotlinx.coroutines.withContext(Dispatchers.Default) { FolderPassword.matches(password, folder.passwordSalt, folder.passwordHash) }
        if (matched) { unlockedFolders.value += id; folderFailures.remove(id) }
        else { val count = previous.first + 1; folderFailures[id] = if (count >= 5) 0 to (now + 30_000L) else count to 0L }
        return matched
    }
    suspend fun addToFolder(id: String, appId: String): Boolean = write {
        repository.update { data ->
            val target = data.folders.first { it.id == id }
            require(!target.isProtected || id in unlockedFolders.value)
            data.copy(folders = data.folders.map { if (it.id == id) it.copy(apps = (it.apps + appId).distinct()) else it })
        }
    }
    suspend fun moveFolderApps(folderId:String,visibleOrder:List<String>):Boolean=write{
        repository.update{state->
            val folder=state.folders.first{it.id==folderId}
            require(!folder.isProtected||folderId in unlockedFolders.value)
            state.copy(folders=state.folders.map{if(it.id==folderId)it.copy(apps=com.emmanuela.launcher.data.ReorderRules.mergeVisible(it.apps,visibleOrder),manualOrder=true)else it})
        }
    }
    fun placeDenseFolder(id:String,target:Int,columns:Int){viewModelScope.launch{write{repository.update{state->
        val changed=state.folders.map{if(it.id==id)it.copy(gridCell=target)else it}
        val cells=com.emmanuela.launcher.data.FolderGridLayout.pack(changed,columns,state.settings.ui.v2.folderWidthUnits,state.settings.ui.v2.folderHeightUnits,id)
        state.copy(folders=state.folders.map{folder->val c=cells.first{it.folder.id==folder.id};folder.copy(gridCell=c.row*columns+c.column)})
    }}}}
    fun placeFolder(id: String, target: Int, freeform: Boolean) { viewModelScope.launch { write {
        repository.update { state ->
            if (freeform) {
                val cells = FolderPlacement.cells(state.folders).toMutableMap()
                val from = cells.entries.first { it.value.id == id }.key
                val source = cells.remove(from)!!
                val occupant = cells.remove(target)
                cells[target] = source
                if (occupant != null) cells[from] = occupant
                state.copy(folders = cells.toSortedMap().map { (cell, folder) -> folder.copy(gridCell = cell) })
            } else {
                val list = state.folders.toMutableList()
                val source = list.indexOfFirst { it.id == id }
                if (source >= 0 && target in list.indices) java.util.Collections.swap(list, source, target)
                state.copy(folders = list.mapIndexed { index, folder -> folder.copy(gridCell = index) })
            }
        }
    } } }
    suspend fun saveFolder(folder: AppFolder, password: String = "", removePassword: Boolean = false): Boolean = write {
        val previous = _data.value.folders.find { it.id == folder.id }
        require(previous == null || !previous.isProtected || folder.id in unlockedFolders.value)
        val secured = when {
            removePassword -> folder.copy(passwordSalt = "", passwordHash = "",deviceProtected=false)
            password.isNotEmpty() -> kotlinx.coroutines.withContext(Dispatchers.Default) {
                val (salt, hash) = FolderPassword.create(password)
                folder.copy(passwordSalt = salt, passwordHash = hash)
            }
            else -> folder
        }
        require(!secured.deviceProtected||secured.biometricUnlock&&getApplication<Application>().getSystemService(android.app.KeyguardManager::class.java).isDeviceSecure)
        require(folder.name.isNotBlank() && folder.name.length <= 40)
        repository.update { state -> state.copy(folders = if (state.folders.any { it.id == folder.id })
            state.folders.map { if (it.id == folder.id) secured else it } else state.folders + secured) }
        if (secured.isProtected) unlockedFolders.value += secured.id
    }
    suspend fun deleteFolder(id: String): Boolean = write { repository.update { state ->
        val folder = state.folders.firstOrNull { it.id == id }
        require(folder == null || !folder.isProtected || id in unlockedFolders.value)
        state.copy(folders = state.folders.filterNot { it.id == id })
    } }
    suspend fun saveFavorites(favorites: List<Favorite>): Boolean = write {
        require(favorites.size <= 8)
        repository.update { it.copy(favorites = favorites) }
    }
    suspend fun saveAppDetails(id:String,alias:String,tags:String,folderIds:Set<String>):Boolean=write {
        val metadata=AppNaming.metadata(alias,tags)
        repository.update{state->
            require(state.policies[id.substringBefore('/')]?.hidden!=true || hiddenUnlocked.value)
            require(id.contains('/') && folderIds.all{f->state.folders.any{it.id==f}})
            require(state.folders.filter{it.isProtected&&((id in it.apps)!=(it.id in folderIds))}.all{it.id in unlockedFolders.value})
            val map=state.appMetadata.toMutableMap();if(metadata==AppMetadata())map.remove(id)else map[id]=metadata
            state.copy(appMetadata=map,folders=state.folders.map{folder->folder.copy(apps=if(folder.id in folderIds)(folder.apps+id).distinct()else folder.apps.filterNot{it==id})})
        }
    }
    suspend fun saveAppMetadata(id: String, alias: String, tags: String): Boolean = write {
        require(id.contains('/'))
        val metadata = AppNaming.metadata(alias, tags)
        repository.update { state ->
            val updated = state.appMetadata.toMutableMap()
            if (metadata == AppMetadata()) updated.remove(id) else updated[id] = metadata
            state.copy(appMetadata = updated)
        }
    }
    suspend fun setAppFolders(appId: String, folderIds: Set<String>): Boolean = write {
        require(appId.contains('/'))
        repository.update { state ->
            val known = state.folders.map { it.id }.toSet()
            require(folderIds.all { it in known })
            val securedChanging = state.folders.filter { folder ->
                folder.isProtected && ((appId in folder.apps) != (folder.id in folderIds))
            }
            require(securedChanging.all { it.id in unlockedFolders.value }) { "Unlock protected folders before changing membership." }
            state.copy(folders = state.folders.map { folder ->
                val apps = if (folder.id in folderIds) (folder.apps + appId).distinct() else folder.apps.filterNot { it == appId }
                folder.copy(apps = apps)
            })
        }
    }
    fun moveFolder(id: String, direction: Int) { viewModelScope.launch { write {
        repository.update { state ->
            val list = state.folders.toMutableList(); val from = list.indexOfFirst { it.id == id }; val to = from + direction
            if (from >= 0 && to in list.indices) java.util.Collections.swap(list, from, to)
            state.copy(folders = list.mapIndexed { index, folder -> folder.copy(gridCell = index) })
        }
    } } }
    suspend fun factoryReset():Boolean = write {
        val context=getApplication<Application>()
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            repository.restore(LauncherData())
            DigestRepository(context).clear()
            context.getSharedPreferences("weather_cache",android.content.Context.MODE_PRIVATE).edit().clear().commit()
            java.io.File(context.filesDir,"custom-font.ttf").delete()
            context.contentResolver.persistedUriPermissions.forEach { permission ->
                runCatching { context.contentResolver.releasePersistableUriPermission(permission.uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }
            WallpaperCache.load(context,null)
        }
        hiddenUnlocked.value=false;unlockedFolders.value=emptySet();folderFailures.clear()
        NotificationBadges.clear();appRepository.invalidateIcons();iconGeneration.value++
        context.updateDigestWork(false)
        kotlinx.coroutines.withContext(Dispatchers.IO){context.getSharedPreferences("focus_opens",android.content.Context.MODE_PRIVATE).edit().clear().commit()}
        androidx.work.WorkManager.getInstance(context).cancelAllWorkByTag("focus-session")
        cancelPause()
    }
    suspend fun restore(data: LauncherData): Boolean = write { repository.restore(data) }.also { if (it) observe() }
    val pendingPause = MutableStateFlow<PendingPause?>(null)
    private var approvedFocus: String? = null
    private var pendingPlatformIntent:Intent?=null
    private var pendingPrivate = false
    private var pauseReadyAt=0L
    fun cancelPause() { pendingPlatformIntent=null;pendingPause.value=null;pendingPrivate=false;approvedFocus=null }
    fun confirmPause(){
        if(android.os.SystemClock.elapsedRealtime()<pauseReadyAt)return
        val pending=pendingPause.value?:return
        pendingPause.value=null;approvedFocus=pending.app.packageName
        val authorized=pendingPrivate;pendingPrivate=false
        val intent=pendingPlatformIntent;pendingPlatformIntent=null
        if(intent==null)launch(pending.app,privateAuthorized=authorized)
        else if(authorized)launchPlatformAuthorized(pending.app.packageName,intent)else launchPlatform(intent)
    }
    private val strictAuthorizations=mutableSetOf<String>()
    fun authorizeStrictGroup(id:String){strictAuthorizations+=id}
    suspend fun saveFocusGroup(group:FocusGroup):Boolean=write {
        require(group.name.isNotBlank()&&group.name.length<=40&&(group.packages.isNotEmpty()||group.websites.isNotEmpty()||group.keywords.isNotEmpty()))
        FocusCodec.decode(FocusCodec.encode(listOf(group))) // Validate before writing an unreadable configuration.
        // Strict edits are authenticated in the UI; never exported authentication tokens.
        repository.update{state->require(state.focusGroups.none{it.id==group.id&&it.strict&&it.requireAuthentication} || strictAuthorizations.remove(group.id));state.copy(focusGroups=if(state.focusGroups.any{it.id==group.id})state.focusGroups.map{if(it.id==group.id)group else it}else state.focusGroups+group)}
    }
    suspend fun deleteFocusGroup(id:String):Boolean=write {repository.update{require(it.focusGroups.none{g->g.id==id&&g.strict&&g.requireAuthentication}||strictAuthorizations.remove(id));it.copy(focusGroups=it.focusGroups.filterNot{g->g.id==id})}}
    private suspend fun mayLaunch(packageName: String, app:LaunchableApp?=null,privateAuthorized:Boolean=false): Boolean {
        val focusApproved=approvedFocus==packageName;approvedFocus=null
        val state=repository.data.first()
        val lockedFolders=state.folders.filter{f->f.isProtected && f.apps.any{it.substringBefore('/')==packageName} && f.id !in unlockedFolders.value}
        if(lockedFolders.isNotEmpty()){blockedMessage.value="Unlock the protected folder before opening this app.";return false}
        val policy = state.policies[packageName] ?: AppPolicy()
        if (policy.hidden && !hiddenUnlocked.value) {
            blockedMessage.value = "Unlock Hidden Apps to open this app."; return false
        }
        if (policy.privateApp && !privateAuthorized) {
            blockedMessage.value = "This is a Private app. Open it from EmmanueLA and authenticate first."; return false
        }
        val used = if (policy.dailyLimitMinutes != null) {
            val current = getApplication<Application>().appUsageToday()
            appUsage.value = current
            current?.let { it[packageName] ?: 0L }
        } else null
        val reason = PolicyRules.reason(policy, ZonedDateTime.now(), used)
        if (reason != null) { blockedMessage.value = reason; return false }
        val groups=state.focusGroups.filter{packageName in it.packages}
        if(groups.isNotEmpty()) {
            val now=ZonedDateTime.now()
            val usage=if(groups.any{it.dailyMinutes>0&&com.emmanuela.launcher.data.FocusWindows.limited(it,now)})getApplication<Application>().appUsageToday()else emptyMap()
            if(usage==null&&groups.any{it.dailyMinutes>0&&com.emmanuela.launcher.data.FocusWindows.limited(it,now)}){blockedMessage.value="Enable Usage Access to apply this group's daily allowance.";return false}
            val context=getApplication<Application>()
            val counts=kotlinx.coroutines.withContext(Dispatchers.IO){context.getSharedPreferences("focus_opens",android.content.Context.MODE_PRIVATE).all}
            val sessions=mutableMapOf<String,com.emmanuela.launcher.platform.GroupSession>()
            groups.forEach { g ->
                val countKey=com.emmanuela.launcher.data.FocusWindows.countKey(g,packageName,now.toLocalDate().toString())
                val opens=(counts[countKey] as? Int)?:0
                val total=if(g.perApp)usage?.get(packageName)?:0L else g.packages.sumOf{usage?.get(it)?:0L}
                val message=com.emmanuela.launcher.data.FocusWindows.reason(g,now,total,opens)
                if(message!=null){blockedMessage.value=message;return false}
                if(g.sessionMinutes>0&&com.emmanuela.launcher.data.FocusWindows.limited(g,now)){
                    val session=context.recentGroupSession(if(g.perApp)setOf(packageName)else g.packages,g.cooldownSeconds)
                    if(session==null){blockedMessage.value="Enable Usage Access to apply session limits.";return false}
                    sessions[g.id]=session
                    if(session.milliseconds>=g.sessionMinutes*60_000L && System.currentTimeMillis()-session.lastActiveAt<g.cooldownSeconds*1000L){
                        blockedMessage.value="${g.name}: session limit reached. Take a ${g.cooldownSeconds}-second break before opening again.";return false
                    }
                }
            }
            val seconds=groups.maxOfOrNull{g->if(com.emmanuela.launcher.data.FocusWindows.limited(g,now))FocusCodec.pauseDelay(g,(counts[com.emmanuela.launcher.data.FocusWindows.countKey(g,packageName,now.toLocalDate().toString())] as? Int)?:0)else 0}?:0
            if(app!=null&&seconds>0&&!focusApproved){pendingPrivate=policy.privateApp;pauseReadyAt=android.os.SystemClock.elapsedRealtime()+seconds*1000L;pendingPause.value=PendingPause(app,seconds,groups.any{it.prompt});return false}
            approvedFocus=null
            groups.filter{it.sessionMinutes>0&&FocusWindows.limited(it,now)}.forEach{g->context.scheduleSessionReminder(g.id,(g.sessionMinutes*60_000L-(sessions[g.id]?.milliseconds?:0L)).coerceAtLeast(1_000L),if(g.perApp)packageName else null)}
            kotlinx.coroutines.withContext(Dispatchers.IO){val prefs=context.getSharedPreferences("focus_opens",android.content.Context.MODE_PRIVATE)
                val editor=prefs.edit();val prefix="${now.toLocalDate()}:"
                prefs.all.keys.filterNot{it.startsWith(prefix)}.forEach{editor.remove(it)}
                groups.filter{com.emmanuela.launcher.data.FocusWindows.limited(it,now)}.forEach{g->val key=com.emmanuela.launcher.data.FocusWindows.countKey(g,packageName,now.toLocalDate().toString());editor.putInt(key,prefs.getInt(key,0)+1)};check(editor.commit())}
        }
        return true
    }
    data class ProtectedShortcut(val app:LaunchableApp,val intent:Intent)
    val protectedShortcuts=kotlinx.coroutines.flow.MutableSharedFlow<ProtectedShortcut>(extraBufferCapacity=1)
    fun launchPlatformAuthorized(packageName:String,intent:Intent){launchPlatform(intent,privateAuthorizedPackage=packageName)}
    val successfulLaunches=kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity=1)
    fun launch(app: LaunchableApp,privateAuthorized:Boolean=false) { viewModelScope.launch {
        try {
            if (mayLaunch(app.packageName, app,privateAuthorized)) {
                com.emmanuela.launcher.platform.EnforcementBridge.grant(app.packageName)
                if(appRepository.launch(app))successfulLaunches.emit(app.id)
                else {error.value = "This app is unavailable."; refresh(true)}
            }
        } catch (e: CancellationException) { throw e }
          catch (_: Exception) { blockedMessage.value = "Could not verify app rules. Try again from App management." }
    } }
    fun launchPlatform(intent: Intent,privateAuthorizedPackage:String?=null) { viewModelScope.launch {
        try {
            val context = getApplication<Application>()
            val target = kotlinx.coroutines.withContext(Dispatchers.IO) {
                context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            }
            if (target == null || target.activityInfo.packageName == "android") {
                error.value = "Choose a specific app for this shortcut in Settings."; return@launch
            }
            val packageName=target.activityInfo.packageName
            val state=repository.data.first()
            val policy=state.policies[packageName]?:AppPolicy()
            val needsFolder=state.folders.any{f->f.isProtected && f.id !in unlockedFolders.value && f.apps.any{it.substringBefore('/')==packageName}}
            if(needsFolder || (policy.hidden&&!hiddenUnlocked.value) || (policy.privateApp&&privateAuthorizedPackage!=packageName)) {
                val app=apps.value.firstOrNull{it.packageName==packageName} ?: kotlinx.coroutines.withContext(Dispatchers.IO){
                    LaunchableApp("$packageName/${target.activityInfo.name}",target.loadLabel(context.packageManager).toString(),packageName)
                }
                protectedShortcuts.emit(ProtectedShortcut(app,Intent(intent)));return@launch
            }
            if (mayLaunch(packageName, apps.value.firstOrNull{it.packageName==packageName},privateAuthorizedPackage==packageName)) {com.emmanuela.launcher.platform.EnforcementBridge.grant(packageName);context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
            else if(pendingPause.value?.app?.packageName==target.activityInfo.packageName)pendingPlatformIntent=Intent(intent)
        } catch (e: CancellationException) { throw e }
          catch (_: Exception) { error.value = "This shortcut is unavailable." }
    } }
    fun launchId(id: String) {
        apps.value.find { it.id == id }?.let{launch(it)} ?: run { error.value = "This app is unavailable. Choose another app in Settings." }
    }
    fun launchPrivateAuthorized(id: String) {
        val app = apps.value.find { it.id == id } ?: run { error.value = "This app is unavailable."; return }
        launch(app,privateAuthorized=true)
    }
}
