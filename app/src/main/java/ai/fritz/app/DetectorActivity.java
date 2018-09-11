package ai.fritz.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ai.fritz.app.ui.OverlayView;
import ai.fritz.fritzvisionobjectmodel.FritzVisionObjectPredictor;
import ai.fritz.vision.FritzVisionObject;
import ai.fritz.vision.inputs.FritzVisionImage;
import ai.fritz.vision.inputs.FritzVisionOrientation;

/**
 * Detects different objects in the image.
 */
public class DetectorActivity extends BaseCameraActivity implements OnImageAvailableListener {

    private static final String TAG = DetectorActivity.class.getSimpleName();

    private static final Size DESIRED_PREVIEW_SIZE = new Size(960, 1280);

    private AtomicBoolean computing = new AtomicBoolean(false);

    private FritzVisionObjectPredictor objectPredictor;

    private int imageRotation;

    Bitmap originalBitmap;
    private FritzVisionImage fritzVisionImage;
    private List<FritzVisionObject> objects = new ArrayList<>();

    @Override
    public void onPreviewSizeChosen(final Size size, final Size cameraSize, final int rotation) {
        imageRotation = FritzVisionOrientation.getImageRotationFromCamera(this, cameraId);
        objectPredictor = FritzVisionObjectPredictor.getInstance(this);

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (originalBitmap != null) {
                            // For displaying the preview / original bitmap that we ran prediction on (uncomment below)
//                             canvas.drawBitmap(originalBitmap, new Matrix(), new Paint());
                            float scaleFactorWidth = ((float) cameraSize.getWidth()) / originalBitmap.getWidth();
                            float scaleFactorHeight = ((float) cameraSize.getHeight()) / originalBitmap.getHeight();
                            for (FritzVisionObject object : objects) {
                                object.drawOnCanvas(getApplicationContext(), canvas, scaleFactorWidth, scaleFactorHeight);
                            }
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
                        originalBitmap = fritzVisionImage.copyBitmap();
                        final long startTime = SystemClock.uptimeMillis();
                        objects = objectPredictor.predict(fritzVisionImage);
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
