package ai.fritz.heartbeat;

import android.content.Intent;
import android.graphics.Canvas;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;

import java.util.concurrent.atomic.AtomicBoolean;

import ai.fritz.heartbeat.ui.OverlayView;
import ai.fritz.heartbeat.utils.Navigation;
import ai.fritz.core.FritzOnDeviceModel;
import ai.fritz.fritzvisionobjectmodel.ObjectDetectionOnDeviceModel;
import ai.fritz.poseestimationmodel.PoseEstimationOnDeviceModel;
import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionOrientation;
import ai.fritz.vision.objectdetection.FritzVisionObjectResult;
import ai.fritz.vision.poseestimation.FritzVisionPosePredictor;
import ai.fritz.vision.poseestimation.FritzVisionPoseResult;
import ai.fritz.vision.objectdetection.FritzVisionObjectPredictor;

public class FullCameraActivity extends BaseCameraActivity implements ImageReader.OnImageAvailableListener {

    private static final String TAG = FullCameraActivity.class.getSimpleName();

    private static final Size DESIRED_PREVIEW_SIZE = new Size(960, 1360);

    private AtomicBoolean computing = new AtomicBoolean(false);

    private PredictorType predictorType;

    private FritzVisionObjectPredictor objectPredictor;
    private FritzVisionObjectResult objectResult;

    private FritzVisionPosePredictor posePredictor;
    private FritzVisionPoseResult poseResult;

    private int imageRotation;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent callingIntent = getIntent();
        predictorType = PredictorType.valueOf(callingIntent.getStringExtra(Navigation.PREDICTOR_TYPE_KEY));
        Log.d(TAG, predictorType.name());
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final Size cameraSize, final int rotation) {
        imageRotation = FritzVisionOrientation.getImageRotationFromCamera(this, cameraId);
        createPredictor();

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        handleDrawingResult(canvas, cameraSize);
                    }
                });
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = reader.acquireLatestImage();

        if (image == null) {
            return;
        }

        if (!computing.compareAndSet(false, true)) {
            image.close();
            return;
        }
        final long startTime = SystemClock.uptimeMillis();
        final FritzVisionImage fritzVisionImage = FritzVisionImage.fromMediaImage(image, imageRotation);
        Log.d(TAG, "Image Creation:" + (SystemClock.uptimeMillis() - startTime));

        image.close();

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Start background task:" + (SystemClock.uptimeMillis() - startTime));
                        runInference(fritzVisionImage);
                        Log.d(TAG, "INFERENCE TIME:" + (SystemClock.uptimeMillis() - startTime));

                        requestRender();
                        computing.set(false);
                    }
                });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onSetDebug(final boolean debug) {

    }

    private void createPredictor() {
        if (predictorType == PredictorType.POSE_DETECTION) {
            FritzOnDeviceModel onDeviceModel = new PoseEstimationOnDeviceModel();
            posePredictor = FritzVision.PoseEstimation.getPredictor(onDeviceModel);
        }

        if (predictorType == PredictorType.OBJECT_DETECTION) {
            FritzOnDeviceModel onDeviceModel = new ObjectDetectionOnDeviceModel();
            objectPredictor = FritzVision.ObjectDetection.getPredictor(onDeviceModel);
        }
    }

    private void handleDrawingResult(Canvas canvas, Size cameraSize) {
        if (predictorType == PredictorType.POSE_DETECTION && poseResult != null) {
            poseResult.drawPoses(canvas, cameraSize);
        }

        if (predictorType == PredictorType.OBJECT_DETECTION && objectResult != null) {
            objectResult.drawBoundingBoxes(canvas, cameraSize);
        }
    }

    private void runInference(FritzVisionImage fritzVisionImage) {
        if (predictorType == PredictorType.POSE_DETECTION) {
            poseResult = posePredictor.predict(fritzVisionImage);
        }

        if (predictorType == PredictorType.OBJECT_DETECTION) {
            objectResult = objectPredictor.predict(fritzVisionImage);
        }
    }
}
