package ai.fritz.heartbeat;

import android.graphics.Canvas;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;

import java.util.concurrent.atomic.AtomicBoolean;

import ai.fritz.core.FritzOnDeviceModel;
import ai.fritz.fritzvisionobjectmodel.ObjectDetectionOnDeviceModel;
import ai.fritz.heartbeat.ui.OverlayView;
import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionOrientation;
import ai.fritz.vision.objectdetection.FritzVisionObjectPredictor;
import ai.fritz.vision.objectdetection.FritzVisionObjectPredictorOptions;
import ai.fritz.vision.objectdetection.FritzVisionObjectResult;

/**
 * Detects different objects in the image.
 */
public class DetectorActivity extends BaseCameraActivity implements OnImageAvailableListener {

    private static final String TAG = DetectorActivity.class.getSimpleName();

    private static final Size DESIRED_PREVIEW_SIZE = new Size(960, 1280);

    private AtomicBoolean computing = new AtomicBoolean(false);

    private FritzVisionObjectPredictor objectPredictor;

    private int imageRotation;

    private FritzVisionImage fritzVisionImage;
    private FritzVisionObjectResult objectResult;

    @Override
    public void onPreviewSizeChosen(final Size size, final Size cameraSize, final int rotation) {
        imageRotation = FritzVisionOrientation.getImageRotationFromCamera(this, cameraId);
        FritzVisionObjectPredictorOptions options = new FritzVisionObjectPredictorOptions.Builder()
                .confidenceThreshold(.6f).build();
        FritzOnDeviceModel onDeviceModel = new ObjectDetectionOnDeviceModel();
        objectPredictor = FritzVision.ObjectDetection.getPredictor(onDeviceModel, options);

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (objectResult != null) {
                            objectResult.drawBoundingBoxes(canvas, cameraSize);
                        }
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

        fritzVisionImage = FritzVisionImage.fromMediaImage(image, imageRotation);
        image.close();

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();
                        objectResult = objectPredictor.predict(fritzVisionImage);
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
}
