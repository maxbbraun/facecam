package fyi.braun.facecam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.util.Log;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.opencv_calib3d;
import org.bytedeco.javacpp.opencv_core.Mat;

import java.io.IOException;

import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_core.CV_32FC2;
import static org.bytedeco.javacpp.opencv_core.CV_32FC3;

public class FaceCamera {
  private static final String TAG = FaceCamera.class.getSimpleName();

  // Preview camera parameters.
  private static final int PREVIEW_WIDTH_PX = 640;
  private static final int PREVIEW_HEIGHT_PX = 480;
  private static final int PREVIEW_FPS = 30;

  // TODO: Use camera calibration.
  // Intrinsic physical camera parameters.
  private static final float FOCAL_LENGTH_MM = 1.87f;
  private static final float SENSOR_WIDTH_MM = 2.4f;
  private static final float SENSOR_HEIGHT_MM = 1.8f;
  private static final Mat CAMERA_MATRIX = new Mat(3, 3, CV_32FC1, new FloatPointer(
      FOCAL_LENGTH_MM, 0.0f, SENSOR_WIDTH_MM / 2,
      0.0f, FOCAL_LENGTH_MM, SENSOR_HEIGHT_MM / 2,
      0.0f, 0.0f, 1.0f
  ));

  // Physical face parameters.
  private static final float FACE_WIDTH_MM = 14.0f;
  private static final float FACE_HEIGHT_MM = 20.0f;
  private static final float FACE_DISTANCE_MM = 50.0f;
  private static final Mat OBJECT_POINTS = new Mat(1, 4, CV_32FC3, new FloatPointer(
      0.0f, 0.0f, FACE_DISTANCE_MM,  // Top left
      FACE_WIDTH_MM, 0.0f, FACE_DISTANCE_MM,  // Top right
      FACE_WIDTH_MM, -FACE_HEIGHT_MM, FACE_DISTANCE_MM,  // Bottom right
      0.0f, -FACE_HEIGHT_MM, FACE_DISTANCE_MM  // Bottom left
  ));

  // RANSAC parameters.
  private static final int RANSAC_ITERATIONS = 100;
  private static final float RANSAC_REPROJECTION_ERROR = 8.0f;
  private static final double RANSAC_CONFIDENCE = 0.99;
  private static final int RANSAC_METHOD = opencv_calib3d.SOLVEPNP_ITERATIVE;

  // Virtual camera parameters.
  private static final float CAMERA_DISTANCE_M = 10.0f;

  public interface Callback {
    void onUpdate(Vector3 position, Quaternion rotation);
    void onGone();
  }

  private final Tracker<Face> faceTracker = new Tracker<Face>() {
    private final Mat rotation = new Mat();
    private final Mat translation = new Mat();
    private final Mat distCoeffs = new Mat();
    private final Mat inliers = new Mat();

    @Override
    public void onUpdate(Detector.Detections<Face> detections, Face face) {
      if (callback == null) {
        return;
      }

      // Create object point correspondences from the detected face.
      PointF position = face.getPosition();
      float width = face.getWidth();
      float height = face.getHeight();
      Mat imagePoints = new Mat(
          1, 4, CV_32FC2, new FloatPointer(
          position.x, PREVIEW_HEIGHT_PX - position.y,  // Top left
          position.x + width, PREVIEW_HEIGHT_PX - position.y,  // Top right
          position.x + width, PREVIEW_HEIGHT_PX - (position.y + height),  // Bottom right
          position.x, PREVIEW_HEIGHT_PX - (position.y + height)  // Bottom left
      ));

      // If there is a previous pose, use it to seed the algorithm.
      boolean useExtrinsicGuess = !rotation.empty() && !translation.empty();

      // Run RANSAC.
      if (!opencv_calib3d.solvePnPRansac(
          OBJECT_POINTS,
          imagePoints,
          CAMERA_MATRIX,
          distCoeffs,  // empty
          rotation,
          translation,
          useExtrinsicGuess,
          RANSAC_ITERATIONS,
          RANSAC_REPROJECTION_ERROR,
          RANSAC_CONFIDENCE,
          inliers,  // empty
          RANSAC_METHOD)) {
        Log.e(TAG, "Failed to solve PnP.");
        callback.onGone();
        return;
      }

      // Calculate the virtual camera position and rotation based on the detected position and
      // assuming a fixed distance looking toward the origin.
      Vector3 cameraPosition = new Vector3(
          (float) translation.ptr(0, 0).getDouble(),
          (float) translation.ptr(1, 0).getDouble(),
          (float) translation.ptr(2, 0).getDouble()
      ).normalized().scaled(CAMERA_DISTANCE_M);
      Vector3 cameraDirection = cameraPosition.normalized().negated();
      Quaternion cameraRotation = Quaternion.lookRotation(cameraDirection, Vector3.up());

      callback.onUpdate(cameraPosition, cameraRotation);
    }

    @Override
    public void onMissing(Detector.Detections<Face> detections) {
      if (callback != null) {
        callback.onGone();
      }
    }

    @Override
    public void onDone() {
      if (callback != null) {
        callback.onGone();
      }
    }
  };

  private final Context context;

  private final FaceDetector faceDetector;
  private final CameraSource cameraSource;

  private Callback callback;

  public FaceCamera(Context context) {
    this.context = context.getApplicationContext();

    faceDetector = new FaceDetector.Builder(this.context)
        .setMode(FaceDetector.ACCURATE_MODE)
        .setLandmarkType(FaceDetector.ALL_LANDMARKS)
        .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
        .setMinFaceSize(0.1f)
        .setTrackingEnabled(true)
        .setProminentFaceOnly(true)
        .build();
    faceDetector.setProcessor(
        new LargestFaceFocusingProcessor.Builder(faceDetector, faceTracker).build());

    cameraSource = new CameraSource.Builder(this.context, faceDetector)
        .setFacing(CameraSource.CAMERA_FACING_FRONT)
        .setRequestedFps(PREVIEW_FPS)
        .setRequestedPreviewSize(PREVIEW_WIDTH_PX, PREVIEW_HEIGHT_PX)
        .build();
  }

  public void setCallback(Callback callback) {
    this.callback = callback;
  }

  @SuppressLint("MissingPermission")
  public void start() {
    Log.d(TAG, "Starting.");
    try {
      cameraSource.start();
    } catch (IOException e) {
      Log.e(TAG, "Failed to start camera.", e);
    }
  }

  public void stop() {
    Log.d(TAG, "Stopping.");
    cameraSource.stop();
  }
}
