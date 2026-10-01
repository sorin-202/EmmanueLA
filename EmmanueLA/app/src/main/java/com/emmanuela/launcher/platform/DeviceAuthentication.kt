package com.emmanuela.launcher.platform

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun Context.fragmentActivity():FragmentActivity?=when(this){is FragmentActivity->this;is ContextWrapper->baseContext.fragmentActivity();else->null}
/** Android owns credentials. Unsupported biometric hardware falls back to the system device lock. */
@Composable
fun rememberDeviceAuthentication(method:String="Biometric + device credential",onActiveChange:(Boolean)->Unit={}):(String,()->Unit)->Unit {
    val context=LocalContext.current
    val activeChange by rememberUpdatedState(onActiveChange)
    var success by remember{mutableStateOf<(() -> Unit)?>(null)}
    var title by remember{mutableStateOf("Private Space")}
    fun explain(message:String){android.widget.Toast.makeText(context,message,android.widget.Toast.LENGTH_LONG).show()}
    val fallback=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val callback=success;success=null;activeChange(false)
        if(result.resultCode==Activity.RESULT_OK)callback?.invoke()
    }
    val credential by rememberUpdatedState<(()->Unit)>({
        @Suppress("DEPRECATION")
        val intent=context.getSystemService(KeyguardManager::class.java).createConfirmDeviceCredentialIntent(title,"Confirm your Android device lock")
        if(intent!=null)try{fallback.launch(intent)}catch(_:android.content.ActivityNotFoundException){success=null;activeChange(false);explain("Device authentication is unavailable.")}
        else {success=null;activeChange(false);explain("Set an Android device PIN/password first.")}
    })
    val prompt=remember(context){context.fragmentActivity()?.let{activity->
        BiometricPrompt(activity,ContextCompat.getMainExecutor(context),object:BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){val callback=success;success=null;activeChange(false);callback?.invoke()}
            override fun onAuthenticationError(code:Int,message:CharSequence){
                if(success==null)return
                if(code in listOf(BiometricPrompt.ERROR_HW_UNAVAILABLE,BiometricPrompt.ERROR_NO_BIOMETRICS,BiometricPrompt.ERROR_LOCKOUT,BiometricPrompt.ERROR_LOCKOUT_PERMANENT))credential()
                else {success=null;activeChange(false);if(code!=BiometricPrompt.ERROR_USER_CANCELED&&code!=BiometricPrompt.ERROR_CANCELED&&code!=BiometricPrompt.ERROR_NEGATIVE_BUTTON)explain(message.toString())}
            }
        })
    }}
    DisposableEffect(prompt){onDispose{if(success!=null)activeChange(false);success=null;prompt?.cancelAuthentication()}}
    return {requestedTitle,callback->
        val manager=context.getSystemService(KeyguardManager::class.java)
        if(manager.isDeviceSecure){
            activeChange(true);success=callback;title=requestedTitle
            val authenticators=BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            val available=BiometricManager.from(context).canAuthenticate(authenticators)==BiometricManager.BIOMETRIC_SUCCESS
            if(method!="Device credential"&&Build.VERSION.SDK_INT>=30&&prompt!=null&&available){
                try{prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(requestedTitle).setAllowedAuthenticators(authenticators).build())}
                catch(_:IllegalArgumentException){credential()}
                catch(_:IllegalStateException){success=null;activeChange(false);explain("Try authentication again when the launcher is visible.")}
            }else credential()
        }else explain("Set an Android device PIN/password first to access hidden/private apps.")
    }
}
