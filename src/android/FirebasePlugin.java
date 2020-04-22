package org.apache.cordova.firebase;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.net.Uri;
//import androidx.annotation.NonNull;
//import androidx.core.app.NotificationManagerCompat;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigInfo;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import me.leolin.shortcutbadger.ShortcutBadger;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Firebase PhoneAuth
import java.util.concurrent.TimeUnit;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

// Crashlytics
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;



public class FirebasePlugin extends CordovaPlugin {

  private FirebaseAnalytics mFirebaseAnalytics;
  private static final String TAG = "FirebasePlugin";
  protected static final String KEY = "badge";

  private static boolean inBackground = true;
  private static ArrayList<Bundle> notificationStack = null;
  private static CallbackContext notificationCallbackContext;
  private static CallbackContext tokenRefreshCallbackContext;
  private static CallbackContext dynamicLinkCallback;

  @Override
  protected void pluginInitialize() {
    final Context context = this.cordova.getActivity().getApplicationContext();
    final Bundle extras = this.cordova.getActivity().getIntent().getExtras();
    this.cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        Log.d(TAG, "Starting Firebase plugin");
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        mFirebaseAnalytics.setAnalyticsCollectionEnabled(true);
        if (extras != null && extras.size() > 1) {
          if (FirebasePlugin.notificationStack == null) {
            FirebasePlugin.notificationStack = new ArrayList<Bundle>();
          }
          if (extras.containsKey("google.message_id")) {
            extras.putBoolean("tap", true);
            notificationStack.add(extras);
          }
        }
      }
    });
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
	if (action.equals("logEvent")) {
      this.logEvent(callbackContext, args.getString(0), args.getJSONObject(1));
      return true;
    } else if (action.equals("logError")) {
      this.logError(callbackContext, args.getString(0));
      return true;
    } else if (action.equals("setCrashlyticsUserId")) {
      this.setCrashlyticsUserId(callbackContext, args.getString(0));
      return true;
    } else if (action.equals("setScreenName")) {
      this.setScreenName(callbackContext, args.getString(0));
      return true;
    } else if (action.equals("setUserId")) {
      this.setUserId(callbackContext, args.getString(0));
      return true;
    } else if (action.equals("setUserProperty")) {
      this.setUserProperty(callbackContext, args.getString(0), args.getString(1));
      return true;
    }  else if (action.equals("startTrace")) {
      this.startTrace(callbackContext, args.getString(0));
      return true;
    } else if (action.equals("incrementCounter")) {
      this.incrementCounter(callbackContext, args.getString(0), args.getString(1));
      return true;
    } else if (action.equals("stopTrace")) {
      this.stopTrace(callbackContext, args.getString(0));
      return true;
    } else if (action.equals("addTraceAttribute")) {
      this.addTraceAttribute(callbackContext, args.getString(0), args.getString(1), args.getString(2));
      return true;  
    } else if (action.equals("forceCrashlytics")) {
      this.forceCrashlytics(callbackContext);
      return true;
    } else if (action.equals("setPerformanceCollectionEnabled")) {
      this.setPerformanceCollectionEnabled(callbackContext, args.getBoolean(0));
      return true;
    } else if (action.equals("setAnalyticsCollectionEnabled")) {
      this.setAnalyticsCollectionEnabled(callbackContext, args.getBoolean(0));
      return true;
    } 

    return false;
  }

  @Override
  public void onPause(boolean multitasking) {
    FirebasePlugin.inBackground = true;
  }

  @Override
  public void onResume(boolean multitasking) {
    FirebasePlugin.inBackground = false;
  }

  @Override
  public void onReset() {
    FirebasePlugin.notificationCallbackContext = null;
    FirebasePlugin.tokenRefreshCallbackContext = null;
  }

  @Override
  public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    final Bundle data = intent.getExtras();
   
    if (data != null && data.containsKey("google.message_id")) {
      data.putBoolean("tap", true);
      //FirebasePlugin.sendNotification(data, this.cordova.getActivity().getApplicationContext());
    }
  }

  public static boolean inBackground() {
    return FirebasePlugin.inBackground;
  }

  public static boolean hasNotificationsCallback() {
    return FirebasePlugin.notificationCallbackContext != null;
  }



  // 
  // Analytics
  //
  private void logEvent(final CallbackContext callbackContext, final String name, final JSONObject params) throws JSONException {
    Log.d(TAG, "logEvent called. name: " + name);
    final Bundle bundle = new Bundle();
    Iterator iter = params.keys();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      Object value = params.get(key);

      if (value instanceof Integer || value instanceof Double) {
        bundle.putFloat(key, ((Number) value).floatValue());
      } else {
        bundle.putString(key, value.toString());
      }
    }

    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        try {
          mFirebaseAnalytics.logEvent(name, bundle);
          callbackContext.success();
          Log.d(TAG, "logEvent success");
        } catch (Exception e) {
          Crashlytics.logException(e);
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

  private void setScreenName(final CallbackContext callbackContext, final String name) {
    Log.d(TAG, "setScreenName called. name: " + name);
    cordova.getActivity().runOnUiThread(new Runnable() {
      public void run() {
        try {
          mFirebaseAnalytics.setCurrentScreen(cordova.getActivity(), name, null);
          callbackContext.success();
          Log.d(TAG, "setScreenName success");
        } catch (Exception e) {
          Crashlytics.logException(e);
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

  private void setUserId(final CallbackContext callbackContext, final String id) {
    Log.d(TAG, "setUserId called. id: " + id);
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        try {
          mFirebaseAnalytics.setUserId(id);
          callbackContext.success();
          Log.d(TAG, "setUserId success");
        } catch (Exception e) {
          Crashlytics.logException(e);
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

  private void setUserProperty(final CallbackContext callbackContext, final String name, final String value) {
    Log.d(TAG, "setUserProperty called. name: " + name + " value: " + value);
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        try {
          mFirebaseAnalytics.setUserProperty(name, value);
          callbackContext.success();
          Log.d(TAG, "setUserProperty success");
        } catch (Exception e) {
          Crashlytics.logException(e);
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

  private void setAnalyticsCollectionEnabled(final CallbackContext callbackContext, final boolean enabled) {
    Log.d(TAG, "setAnalyticsCollectionEnabled called. enabled: " + (enabled ? "true" : "false"));
    final FirebasePlugin self = this;
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        try {
          mFirebaseAnalytics.setAnalyticsCollectionEnabled(enabled);
          callbackContext.success();
          Log.d(TAG, "setAnalyticsCollectionEnabled success");
        } catch (Exception e) {
          Crashlytics.log(e.getMessage());
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

  // 
  // Performance monitoring
  //
  private HashMap<String,Trace> traces = new HashMap<String,Trace>();

  private void startTrace(final CallbackContext callbackContext, final String name) {
    Log.d(TAG, "startTrace called. name: " + name);
    final FirebasePlugin self = this;
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        try {
          Trace myTrace = null;
          if (self.traces.containsKey(name)) {
            myTrace = self.traces.get(name);
          }
          if (myTrace == null) {
            myTrace = FirebasePerformance.getInstance().newTrace(name);
            myTrace.start();
            self.traces.put(name, myTrace);
          }
          callbackContext.success();
          Log.d(TAG, "startTrace success");
        } catch (Exception e) {
          Crashlytics.logException(e);
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

  private void incrementCounter(final CallbackContext callbackContext, final String name, final String counterNamed) {
    Log.d(TAG, "incrementCounter called. name: " + name + " counterNamed: " + counterNamed);
    final FirebasePlugin self = this;
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        try {
          Trace myTrace = null;
          if (self.traces.containsKey(name)) {
            myTrace = self.traces.get(name);
          }
          if (myTrace != null && myTrace instanceof Trace) {
           // myTrace.incrementMetric(counterNamed, 1);
            callbackContext.success();
            Log.d(TAG, "incrementCounter success");
          } else {
            callbackContext.error("Trace not found");
            Log.d(TAG, "incrementCounter trace not found");
          }
        } catch (Exception e) {
          Crashlytics.logException(e);
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

  private void stopTrace(final CallbackContext callbackContext, final String name) {
    Log.d(TAG, "stopTrace called. name: " + name);
    final FirebasePlugin self = this;
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        try {
          Trace myTrace = null;
          if (self.traces.containsKey(name)) {
            myTrace = self.traces.get(name);
          }
          if (myTrace != null && myTrace instanceof Trace) {
            myTrace.stop();
            self.traces.remove(name);
            callbackContext.success();
            Log.d(TAG, "stopTrace success");
          } else {
            callbackContext.error("Trace not found");
            Log.d(TAG, "stopTrace trace not found");
          }
        } catch (Exception e) {
          Crashlytics.logException(e);
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

  private void addTraceAttribute(final CallbackContext callbackContext, final String traceName, final String attribute, final String value) {
    Log.d(TAG, "addTraceAttribute called. traceName: " + traceName + " attribute: " + attribute + " value: " + value);
    final FirebasePlugin self = this;
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        try {
          Trace myTrace = null;
          if (self.traces.containsKey(traceName)) {
            myTrace = self.traces.get(traceName);
          }
          if (myTrace != null && myTrace instanceof Trace) {
            myTrace.putAttribute(attribute, value);
            callbackContext.success();
            Log.d(TAG, "addTraceAttribute success");
          } else {
            callbackContext.error("Trace not found");
            Log.d(TAG, "addTraceAttribute trace not found");
          }
        } catch (Exception e) {
          Crashlytics.log(e.getMessage());
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

  private void setPerformanceCollectionEnabled(final CallbackContext callbackContext, final boolean enabled) {
    Log.d(TAG, "setPerformanceCollectionEnabled called. enabled: " + (enabled ? "true" : "false"));
    final FirebasePlugin self = this;
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        try {
          FirebasePerformance.getInstance().setPerformanceCollectionEnabled(enabled);
          callbackContext.success();
          Log.d(TAG, "setPerformanceCollectionEnabled success");
        } catch (Exception e) {
          Crashlytics.log(e.getMessage());
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

  // 
  // Crashlytics
  //
  private void forceCrashlytics(final CallbackContext callbackContext) {
    Log.d(TAG, "forceCrashlytics called");
    final FirebasePlugin self = this;
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        Crashlytics.getInstance().crash();
      }
    });
  }
  
  private void logError(final CallbackContext callbackContext, final String message) throws JSONException {
    Log.d(TAG, "logError called. message: " + message);
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        try {
          Crashlytics.logException(new Exception(message));
          callbackContext.success(1);
          Log.d(TAG, "logError success");
        } catch (Exception e) {
          Crashlytics.log(e.getMessage());
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

  private void setCrashlyticsUserId(final CallbackContext callbackContext, final String userId) {
    Log.d(TAG, "setCrashlyticsUserId called. userId: " + userId);
    cordova.getActivity().runOnUiThread(new Runnable() {
      public void run() {
        try {
          Crashlytics.setUserIdentifier(userId);
          callbackContext.success();
          Log.d(TAG, "setCrashlyticsUserId success");
        } catch (Exception e) {
          Crashlytics.logException(e);
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

 
  
}
