package ai.fritz.heartbeat;

import android.graphics.Canvas;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicBoolean;

import ai.fritz.core.FritzOnDeviceModel;
import ai.fritz.fritzvisionstylepaintings.PaintingStyles;
import ai.fritz.heartbeat.ui.OverlayView;
import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionOrientation;
import ai.fritz.vision.styletransfer.FritzStyleResolution;
import ai.fritz.vision.styletransfer.FritzVisionStylePredictor;
import ai.fritz.vision.styletransfer.FritzVisionStylePredictorOptions;
import ai.fritz.vision.styletransfer.FritzVisionStyleResult;


public class StyleActivity extends BaseCameraActivity implements OnImageAvailableListener {
    private static final String TAG = StyleActivity.class.getSimpleName();

    // Should this activity show the custom style transfer model (cycles through included models if false)
    private static final Size DESIRED_PREVIEW_SIZE = new Size(1280, 960);
    private static final String[] STYLE_NAMES = {
            "BICENTENNIAL_PRINT_MANAGED_MODEL",
            "FEMMES_MANAGED_MODEL",
            "HEAD_OF_CLOWN_MANAGED_MODEL",
            "HORSES_ON_SEASHORE_MANAGED_MODEL",
            "KALEIDOSCOPE_MANAGED_MODEL",
            "PINK_BLUE_RHOMBUS_MANAGED_MODEL",
            "POPPY_FIELD_MANAGED_MODEL",
            "RITMO_PLASTICO_MANAGED_MODEL",
            "STARRY_NIGHT_MANAGED_MODEL",
            "THE_SCREAM_MANAGED_MODEL",
            "THE_TRAIL_MANAGED_MODEL"
    };

    private AtomicBoolean computing = new AtomicBoolean(false);

    private FritzVisionImage styledImage;
    private FritzVisionStylePredictor predictor;
    private FritzVisionStylePredictorOptions options;

    private Size cameraViewSize;

    private OverlayView overlayView;
    private int activeStyleIndex = 0;

    private int imageRotationFromCamera;

    private FritzVisionStyleResult styleResult;

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
                .imageResolution(FritzStyleResolution.HIGH)
                .build();
        assignPredictor();

        imageRotationFromCamera = FritzVisionOrientation.getImageRotationFromCamera(this, cameraId);
        this.cameraViewSize = cameraViewSize;

        overlayView = findViewById(R.id.debug_overlay);

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (styleResult != null) {
                            styleResult.drawToCanvas(canvas, cameraViewSize);
                        }
                    }
                });

        // Don't add a click handler if the activity is showing a custom style transfer model.
        overlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getNextPredictor();
            }
        });
    }

    private void getNextPredictor() {
        FritzOnDeviceModel[] styles = PaintingStyles.getAll();
        activeStyleIndex = ++activeStyleIndex % styles.length;

        Toast.makeText(this,
                STYLE_NAMES[activeStyleIndex] + " Style Shown", Toast.LENGTH_LONG).show();
        FritzOnDeviceModel onDeviceModel = styles[activeStyleIndex];
        predictor = FritzVision.StyleTransfer.getPredictor(onDeviceModel, options);
    }

    private void assignPredictor() {
        FritzOnDeviceModel[] styles = PaintingStyles.getAll();
        FritzOnDeviceModel onDeviceModel = styles[activeStyleIndex];
        predictor = FritzVision.StyleTransfer.getPredictor(onDeviceModel, options);
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
                        styleResult = predictor.predict(fritzImage);
                        requestRender();
                        computing.set(false);
                    }
                });
    }
}

