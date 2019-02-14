package ai.fritz.heartbeat.utils;

import android.content.Context;
import android.content.Intent;

import ai.fritz.heartbeat.DetectorActivity;
import ai.fritz.heartbeat.FullCameraActivity;
import ai.fritz.heartbeat.ImageSegmentationActivity;
import ai.fritz.heartbeat.LiveVideoActivity;
import ai.fritz.heartbeat.PredictorType;
import ai.fritz.heartbeat.StyleActivity;

/**
 * Navigation is a helper class for common links throughout the app.
 */
public class Navigation {

    public static final String PREDICTOR_TYPE_KEY = "PredictorType";

    public static void goToLiveVideoFritzLabel(Context context) {
        Intent liveVideoActivity = new Intent(context, LiveVideoActivity.class);
        context.startActivity(liveVideoActivity);
    }

    public static void goToObjectDetection(Context context) {
        Intent objectDetection = new Intent(context, DetectorActivity.class);
        context.startActivity(objectDetection);
    }

    public static void goToStyleTransfer(Context context) {
        Intent styleActivity = new Intent(context, StyleActivity.class);
        context.startActivity(styleActivity);
    }

    public static void goToImageSegmentation(Context context, PredictorType predictorType) {
        Intent imgSegActivity = new Intent(context, ImageSegmentationActivity.class);
        imgSegActivity.putExtra(PREDICTOR_TYPE_KEY, predictorType.name());
        context.startActivity(imgSegActivity);
    }

    public static void startPoseEstimation(Context context) {
        Intent fullCameraActivity = new Intent(context, FullCameraActivity.class);
        fullCameraActivity.putExtra(PREDICTOR_TYPE_KEY, PredictorType.POSE_ESTIMATION.name());
        context.startActivity(fullCameraActivity);
    }
}
