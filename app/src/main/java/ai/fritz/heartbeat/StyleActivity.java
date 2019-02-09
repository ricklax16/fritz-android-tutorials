package ai.fritz.heartbeat;

import android.graphics.Canvas;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicBoolean;

import ai.fritz.fritzvisionstylepaintings.PaintingStyles;
import ai.fritz.heartbeat.ui.OverlayView;
import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionOrientation;
import ai.fritz.vision.models.StyleOnDeviceModel;
import ai.fritz.vision.options.FritzStyleResolution;
import ai.fritz.vision.options.FritzVisionStylePredictorOptions;
import ai.fritz.vision.outputs.FritzVisionStyleResult;
import ai.fritz.vision.predictors.FritzVisionStylePredictor;


public class StyleActivity extends BaseCameraActivity implements OnImageAvailableListener {
    private static final String TAG = StyleActivity.class.getSimpleName();

    private static final Size DESIRED_PREVIEW_SIZE = new Size(1280, 960);

    private AtomicBoolean computing = new AtomicBoolean(false);

    private FritzVisionStylePredictor predictor;
    private FritzVisionStyleResult styledImageResult;
    private FritzVisionStylePredictorOptions options;

    private OverlayView overlayView;
    private int activeStyleIndex = 0;

    private int imageRotationFromCamera;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_stylize;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onPreviewSizeChosen(final Size previewSize, final Size cameraViewSize, final int rotation) {
        options = new FritzVisionStylePredictorOptions.Builder()
                .imageResolution(FritzStyleResolution.NORMAL)
                .build();
        assignPredictor();

        imageRotationFromCamera = FritzVisionOrientation.getImageRotationFromCamera(this, cameraId);
        overlayView = findViewById(R.id.debug_overlay);

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (styledImageResult != null) {
                            styledImageResult.drawToCanvas(canvas, cameraViewSize);
                        }
                    }
                });

        overlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getNextPredictor();
            }
        });
    }

    private void assignPredictor() {
        StyleOnDeviceModel[] styles = PaintingStyles.getAll();
        predictor = FritzVision.StyleTransfer.getPredictor(styles[activeStyleIndex], options);
    }

    private void getNextPredictor() {
        StyleOnDeviceModel[] styles = PaintingStyles.getAll();
        activeStyleIndex = ++activeStyleIndex % styles.length;

        Toast.makeText(this,
                styles[activeStyleIndex].getName() + " Style Shown", Toast.LENGTH_LONG).show();
        predictor = FritzVision.StyleTransfer.getPredictor(styles[activeStyleIndex], options);
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

        final FritzVisionImage fritzImage = FritzVisionImage.fromMediaImage(image, imageRotationFromCamera);
        image.close();


        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();
                        styledImageResult = predictor.predict(fritzImage);
                        Log.d(TAG, "INFERENCE TIME:" + (SystemClock.uptimeMillis() - startTime));
                        requestRender();
                        computing.set(false);
                    }
                });
    }
}

