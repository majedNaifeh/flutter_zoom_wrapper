package com.mfy.flutter_zoom_wrapper

import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import us.zoom.sdk.*
import us.zoom.sdk.ZoomError


class FlutterZoomWrapperPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, ZoomSDKInitializeListener {

  private lateinit var channel: MethodChannel
  private lateinit var context: Context
  private lateinit var zoomSDK: ZoomSDK
  private var activityBinding: ActivityPluginBinding? = null
  private var pendingStatus: Boolean? = false

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_zoom_wrapper")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    zoomSDK = ZoomSDK.getInstance()
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "getPlatformVersion" -> result.success("Android ${android.os.Build.VERSION.RELEASE}")
      "initZoom" -> {
        val jwt = call.argument<String>("jwt")
        if (jwt.isNullOrEmpty()) {
          result.error("INVALID_ARGUMENT", "JWT token is missing", null)
        } else {
          initZoom(jwt, result)
        }
      }
      "joinMeeting" -> {
        val meetingId = call.argument<String>("meetingId")
        val password = call.argument<String>("meetingPassword")
        val displayName = call.argument<String>("displayName")
        joinMeeting(meetingId, password, displayName, result)
      }
      else -> result.notImplemented()
    }
  }

  private fun initZoom(jwt: String, result: MethodChannel.Result) {
    //Log.d("ZOOM_SDK", "JWT Token received: $jwt")
   // Log.d("ZOOM_SDK", "JWT Token length: ${jwt.length}")
    zoomSDK = ZoomSDK.getInstance()

    if (zoomSDK.isInitialized) {
      result.success(true)
      return
    }

    val activity = activityBinding?.activity
    if (activity == null) {
      result.error("NO_ACTIVITY", "Activity is null. Cannot initialize Zoom SDK.", null)
      return
    }

    val initParams = ZoomSDKInitParams().apply {
      jwtToken = jwt
      domain = "zoom.us"
      enableLog = true
      enableGenerateDump = true
      logSize = 5
    }

    val listener = object : ZoomSDKInitializeListener {
      override fun onZoomSDKInitializeResult(errorCode: Int, internalErrorCode: Int) {
        if (errorCode == ZoomError.ZOOM_ERROR_SUCCESS) {
          result.success(true)
        } else {
          result.error("INIT_ERROR", "Failed to initialize Zoom SDK. Error: $errorCode, internalErrorCode: $internalErrorCode", null)
        }
      }

      override fun onZoomAuthIdentityExpired() {
        Log.w("Zoom", "Auth identity expired")
      }
    }

    zoomSDK.initialize(activity, listener, initParams)
  }

  private fun initZooms(jwt: String, result: MethodChannel.Result) {
    Log.d("ZOOM_SDK", "JWT Token received: $jwt")
    Log.d("ZOOM_SDK", "JWT Token length: ${jwt.length}")
    zoomSDK = ZoomSDK.getInstance()

    try {
      // Check if SDK is already initialized
      if (zoomSDK.isInitialized()){
        Log.d("ZOOM_SDK", "Zoom SDK already initialized")
        result.success(true)
      }
      // Only initialize if not already initialized
      if (!zoomSDK.isInitialized()) {
        // Configure SDK initialization parameters
        var initParams =  ZoomSDKInitParams()
        initParams.jwtToken = jwt;
        initParams.enableLog = true;
        initParams.enableGenerateDump = true;
        initParams.logSize = 5;
        initParams.domain="zoom.us";
        initParams.videoRawDataMemoryMode = ZoomSDKRawDataMemoryMode.ZoomSDKRawDataMemoryModeStack;

        // Create listener for initialization callbacks
        val listener = object : ZoomSDKInitializeListener {
          /**
           * Callback for SDK initialization result
           * @param errorCode Error code from initialization
           * @param internalErrorCode Internal error code from initialization
           */
          override fun onZoomSDKInitializeResult(errorCode: Int, internalErrorCode: Int) {
            if (errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
              Log.d("Failed", "Failed to initialize Zoom SDK")
              Log.d(
                "Failed to initialize Zoom SDK. Error: \" + $errorCode + \"",
                "internalErrorCode=\" + $internalErrorCode"
              )
              return result.error("INIT_ERROR", "\"Failed to initialize Zoom SDK. Error: \" + $errorCode + \",\n" +
                      "\"internalErrorCode=\" + $internalErrorCode\"", null)
            } else {
              Log.d("Success", "Initialize Zoom SDK successfully.")
              return result.success(true)
            }
          }

          /**
           * Callback when Zoom auth identity expires
           */
          override fun onZoomAuthIdentityExpired() {

          }
        }

        // Initialize SDK with prepared parameters
        zoomSDK.initialize(context, listener, initParams)

      }
    } catch (e: Exception) {
      Log.e("ZOOM_SDK", "Error while initializing ZoomSDK: ${e.message}", e)
      result.error("INIT_ERROR", "Failed to initialize Zoom SDK: ${e.message}", null)
    }
  }

// private fun joinMeeting(meetingId: String?, password: String?, displayName: String?, result: Result) {
//     if (!zoomSDK.isInitialized) {
//       result.error("SDK_NOT_INITIALIZED", "Zoom SDK not initialized", null)
//       return
//     }

//     if (meetingId.isNullOrEmpty() || password.isNullOrEmpty() || displayName.isNullOrEmpty()) {
//       result.error("INVALID_ARGUMENTS", "Missing meeting details", null)
//       return
//     }

//     val joinParams = JoinMeetingParams().apply {
//       meetingNo = meetingId
//       this.password = password
//       this.displayName = displayName
//     }



//    val options = JoinMeetingOptions().apply {
//     no_titlebar = true
//     no_invite = true
//     no_share = true
//     invite_options = InviteOptions.INVITE_DISABLE_ALL
//     meeting_views_options = 
//         MeetingViewsOptions.NO_TEXT_MEETING_ID or
//         MeetingViewsOptions.NO_TEXT_PASSWORD or
//         MeetingViewsOptions.NO_BUTTON_INVITE or
//         MeetingViewsOptions.NO_BUTTON_SHARE or
//         MeetingViewsOptions.NO_BUTTON_MORE or
//         MeetingViewsOptions.NO_BUTTON_PARTICIPANTS or
//         MeetingViewsOptions.NO_TEXT_INVITE or 
//         MeetingViewsOptions.NO_BUTTON_INVITE_LINK or
//         MeetingViewsOptions.NO_BUTTON_MEETING_INFO
   
// }

//     val meetingService = zoomSDK.meetingService
//     activityBinding?.activity?.let {
//       meetingService.joinMeetingWithParams(it, joinParams, options)
//     } ?: run {
//       meetingService.joinMeetingWithParams(context, joinParams, options)
//     }

//     result.success(true)
//   }

private fun joinMeeting(meetingId: String?, password: String?, displayName: String?, result: Result) {
    if (!zoomSDK.isInitialized) {
        result.error("SDK_NOT_INITIALIZED", "Zoom SDK not initialized", null)
        return
    }

    if (meetingId.isNullOrEmpty() || password.isNullOrEmpty() || displayName.isNullOrEmpty()) {
        result.error("INVALID_ARGUMENTS", "Missing meeting details", null)
        return
    }

    // --- Correct calls to SDK helpers (must be before join) ---
    val meetingSettingsHelper = zoomSDK.getMeetingSettingsHelper()
    meetingSettingsHelper?.disableCopyMeetingUrl(true)    // disable copy URL from title-bar long-press / menu
    meetingSettingsHelper?.setNoInviteH323RoomCallInEnabled(true) // other invite H323/room settings if needed

    val uiService = zoomSDK.getZoomUIService()
    uiService?.hideMeetingInviteUrl(true)                 // hide the "Invite URL" section in the meeting info popup
    // Note: some hide/disable functions exist under ZoomUIService or MeetingSettingsHelper depending on SDK version

    // --- Join params & options (your existing code) ---
    val joinParams = JoinMeetingParams().apply {
        meetingNo = meetingId
        this.password = password
        this.displayName = displayName
    }

    val options = JoinMeetingOptions().apply {
        no_titlebar = true
        no_invite = true
        no_share = true
        invite_options = InviteOptions.INVITE_DISABLE_ALL
        meeting_views_options =
            MeetingViewsOptions.NO_TEXT_MEETING_ID or
            MeetingViewsOptions.NO_TEXT_PASSWORD or
            MeetingViewsOptions.NO_BUTTON_INVITE or
            MeetingViewsOptions.NO_BUTTON_SHARE or
            MeetingViewsOptions.NO_BUTTON_MORE or
            MeetingViewsOptions.NO_BUTTON_PARTICIPANTS or
            MeetingViewsOptions.NO_TEXT_INVITE or
            MeetingViewsOptions.NO_BUTTON_INVITE_LINK or
            MeetingViewsOptions.NO_BUTTON_MEETING_INFO
    }

    val meetingService = zoomSDK.meetingService
    activityBinding?.activity?.let {
        meetingService.joinMeetingWithParams(it, joinParams, options)
    } ?: run {
        meetingService.joinMeetingWithParams(context, joinParams, options)
    }

    result.success(true)
}

  override fun onZoomSDKInitializeResult(errorCode: Int, internalErrorCode: Int) {
    when (errorCode) {
      ZoomError.ZOOM_ERROR_SUCCESS -> {
        Log.d("Zoom", "✅ Zoom SDK initialized successfully.")
      }
      1001 -> { // Numeric value for ZOOM_ERROR_WRONG_ZOOM_DOMAIN
        Log.e("Zoom", "❌ Wrong Zoom domain configured")
      }
      else -> {
        Log.e("Zoom", "❌ Zoom SDK initialization failed. Error code: $errorCode, Internal error: $internalErrorCode")
      }
    }
  }



  override fun onZoomAuthIdentityExpired() {
    Log.w("Zoom", "Auth identity expired")
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    Log.d("ZoomPlugin", "Attached to activity: ${binding.activity}")
    activityBinding = binding
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activityBinding = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activityBinding = binding
  }

  override fun onDetachedFromActivity() {
    activityBinding = null
  }
}
