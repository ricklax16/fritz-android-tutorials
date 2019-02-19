package ai.fritz.heartbeat;

import android.content.Context;
import android.content.Intent;

import ai.fritz.heartbeat.activities.custommodel.CustomTFLiteActivity;
import ai.fritz.heartbeat.activities.custommodel.CustomTFMobileActivity;
import ai.fritz.heartbeat.activities.vision.ObjectDetectionActivity;
import ai.fritz.heartbeat.activities.vision.PoseEstimationActivity;
import ai.fritz.heartbeat.activities.vision.ImageSegmentationActivity;
import ai.fritz.heartbeat.activities.vision.ImageLabelingActivity;
import ai.fritz.heartbeat.PredictorType;
import ai.fritz.heartbeat.activities.vision.StyleTransferActivity;

/**
 * Navigation is a helper class for common links throughout the app.
 */
public class Navigation {

    public static final String PREDICTOR_TYPE_KEY = "PredictorType";

    public static void goToTFMobile(Context context) {
        Intent tfMobile = new Intent(context, CustomTFMobileActivity.class);
        context.startActivity(tfMobile);
    }

    public static void goToTFLite(Context context) {
        Intent tflite = new Intent(context, CustomTFLiteActivity.class);
        context.startActivity(tflite);
    }

    public static void goToLiveVideoFritzLabel(Context context) {
        Intent liveVideoActivity = new Intent(context, ImageLabelingActivity.class);
        context.startActivity(liveVideoActivity);
    }

    public static void goToObjectDetection(Context context) {
        Intent objectDetection = new Intent(context, ObjectDetectionActivity.class);
        context.startActivity(objectDetection);
    }

    public static void goToStyleTransfer(Context context) {
        Intent styleActivity = new Intent(context, StyleTransferActivity.class);
        context.startActivity(styleActivity);
    }

    public static void goToImageSegmentation(Context context, PredictorType predictorType) {
        Intent imgSegActivity = new Intent(context, ImageSegmentationActivity.class);
        imgSegActivity.putExtra(PREDICTOR_TYPE_KEY, predictorType.name());
        context.startActivity(imgSegActivity);
    }

    public static void startPoseEstimation(Context context) {
        Intent fullCameraActivity = new Intent(context, PoseEstimationActivity.class);
        fullCameraActivity.putExtra(PREDICTOR_TYPE_KEY, PredictorType.POSE_ESTIMATION.name());
        context.startActivity(fullCameraActivity);
    }
}
