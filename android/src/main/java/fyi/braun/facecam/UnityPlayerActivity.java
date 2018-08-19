package fyi.braun.facecam;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.unity3d.player.UnityPlayer;

import fyi.braun.facecam.CameraPoseOuterClass.CameraPose;

public class UnityPlayerActivity extends Activity {
  private static final String TAG = UnityPlayerActivity.class.getSimpleName();

  private static final String UNITY_CAMERA_OBJECT_NAME = "Camera";
  private static final String UNITY_CAMERA_SCRIPT_METHOD_NAME = "Move";

  // Don't change the name of this variable. It's referenced from native code.
  protected UnityPlayer mUnityPlayer;

  private FaceCamera faceCamera;
  private final FaceCamera.Callback faceCameraCallback = new FaceCamera.Callback() {
    @Override
    public void onUpdate(Vector3 position, Quaternion rotation) {
      // Convert to right-handed Unity coordinate system.
      position.z = -position.z;
      rotation.z = -rotation.z;
      rotation.w = -rotation.w;

      // Build and serialize the proto.
      CameraPose cameraPose = CameraPose.newBuilder()
          .setPositionX(position.x)
          .setPositionY(position.y)
          .setPositionZ(position.z)
          .setRotationX(rotation.x)
          .setRotationY(rotation.y)
          .setRotationZ(rotation.z)
          .setRotationW(rotation.w)
          .build();
      String cameraPoseMessage = Base64.encodeToString(cameraPose.toByteArray(), Base64.DEFAULT);

      // Send the proto to the Unity script.
      runOnUiThread(() -> {
        UnityPlayer.UnitySendMessage(UNITY_CAMERA_OBJECT_NAME, UNITY_CAMERA_SCRIPT_METHOD_NAME,
            cameraPoseMessage);
        mUnityPlayer.setVisibility(View.VISIBLE);
      });
    }

    @Override
    public void onGone() {
      runOnUiThread(() -> mUnityPlayer.setVisibility(View.INVISIBLE));
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    super.onCreate(savedInstanceState);

    faceCamera = new FaceCamera(this);
    faceCamera.setCallback(faceCameraCallback);

    mUnityPlayer = new UnityPlayer(this);
    setContentView(mUnityPlayer);
    mUnityPlayer.setKeepScreenOn(true);
    mUnityPlayer.requestFocus();
    mUnityPlayer.setVisibility(View.INVISIBLE);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    setIntent(intent);
  }

  @Override
  protected void onDestroy() {
    mUnityPlayer.quit();
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    super.onPause();
    faceCamera.stop();
    mUnityPlayer.pause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    faceCamera.start();
    mUnityPlayer.resume();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mUnityPlayer.start();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mUnityPlayer.stop();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mUnityPlayer.lowMemory();
  }

  @Override
  public void onTrimMemory(int level) {
    super.onTrimMemory(level);
    if (level == TRIM_MEMORY_RUNNING_CRITICAL) {
      mUnityPlayer.lowMemory();
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    mUnityPlayer.configurationChanged(newConfig);
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    mUnityPlayer.windowFocusChanged(hasFocus);
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_MULTIPLE) {
      return mUnityPlayer.injectEvent(event);
    }
    return super.dispatchKeyEvent(event);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    return mUnityPlayer.injectEvent(event);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return mUnityPlayer.injectEvent(event);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return mUnityPlayer.injectEvent(event);
  }

  @Override
  public boolean onGenericMotionEvent(MotionEvent event) {
    return mUnityPlayer.injectEvent(event);
  }
}
