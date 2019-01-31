package ai.fritz.heartbeat.utils;

import android.content.Context;
import android.content.Intent;

import ai.fritz.heartbeat.DetectorActivity;
import ai.fritz.heartbeat.ImageSegmentationActivity;
import ai.fritz.heartbeat.LiveVideoActivity;
import ai.fritz.heartbeat.StyleActivity;

/**
 * Navigation is a helper class for common links throughout the app.
 */
public class Navigation {

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

    public static void goToImageSegmentation(Context context) {
        Intent imgSegActivity = new Intent(context, ImageSegmentationActivity.class);
        context.startActivity(imgSegActivity);
    }
}
