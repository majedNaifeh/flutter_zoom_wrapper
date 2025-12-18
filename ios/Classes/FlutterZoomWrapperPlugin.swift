import Flutter
import UIKit
import MobileRTC

public class FlutterZoomWrapperPlugin: NSObject, FlutterPlugin,MobileRTCAuthDelegate {
    var resultHolder: FlutterResult?


  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "flutter_zoom_wrapper", binaryMessenger: registrar.messenger())
    let instance = FlutterZoomWrapperPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

   public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
         switch call.method {
         case "getPlatformVersion":
             result("iOS " + UIDevice.current.systemVersion)
         case "initZoom":
             if let args = call.arguments as? [String: Any],
                let jwtToken = args["jwt"] as? String {
                 initializeZoom(jwt: jwtToken, result: result)
             } else {
                 result(FlutterError(code: "INVALID_ARGUMENT", message: "JWT token is missing", details: nil))
             }
         case "joinMeeting":
             if let args = call.arguments as? [String: Any],
                let meetingId = args["meetingId"] as? String,
                let password = args["meetingPassword"] as? String,
                let displayName = args["displayName"] as? String {
                 joinMeeting(meetingId: meetingId, password: password, displayName: displayName, result: result)
             } else {
                 result(FlutterError(code: "INVALID_ARGUMENT", message: "Missing meeting parameters", details: nil))
             }
         default:
             result(FlutterMethodNotImplemented)
         }
     }

    private func initializeZoom(jwt: String, result: @escaping FlutterResult) {
        resultHolder = result

        // Initialize Zoom SDK
        let context = MobileRTCSDKInitContext()
        context.domain = "zoom.us"
        context.enableLog = true

        guard MobileRTC.shared().initialize(context) else {
            result(FlutterError(code: "INIT_FAILED", message: "Zoom SDK initialization failed", details: nil))
            return
        }

        guard let authService = MobileRTC.shared().getAuthService() else {
            result(FlutterError(code: "AUTH_SERVICE_NULL", message: "Zoom AuthService is nil", details: nil))
            return
        }

        authService.delegate = self
        authService.jwtToken = jwt
        authService.sdkAuth()
    }


         public func onMobileRTCAuthReturn(_ returnValue: MobileRTCAuthError) {
                 if returnValue == .success {
                     resultHolder?(true)
                 } else {
                     resultHolder?(FlutterError(code: "AUTH_FAILED", message: "Zoom SDK auth failed with code: \(returnValue.rawValue)", details: nil))
                 }
                 resultHolder = nil
             }

    private func joinMeeting(meetingId: String, password: String, displayName: String, result: @escaping FlutterResult) {
        guard let meetingService = MobileRTC.shared().getMeetingService() else {
            result(FlutterError(code: "NO_MEETING_SERVICE", message: "Meeting service is not available", details: nil))
            return
        }

        // Configure meeting settings before joining
        if let meetingSettings = MobileRTC.shared().getMeetingSettings() {
            meetingSettings.meetingInviteHidden = true
            meetingSettings.meetingInviteUrlHidden = true
            meetingSettings.meetingPasswordHidden = true
            meetingSettings.meetingParticipantHidden = true
                       meetingSettings.enableVideoCallPicture(inPicture: true)



        }
   

        let joinParams = MobileRTCMeetingJoinParam()
        joinParams.userName = displayName
        joinParams.meetingNumber = meetingId
        joinParams.password = password

        let response = meetingService.joinMeeting(with: joinParams)
        result(response == MobileRTCMeetError.success)
    }
}

