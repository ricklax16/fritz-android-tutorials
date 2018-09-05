package ai.fritz.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
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

import ai.fritz.app.ui.OverlayView;
import ai.fritz.fritzvisionstylemodel.ArtisticStyle;
import ai.fritz.fritzvisionstylemodel.FritzStyleResolution;
import ai.fritz.fritzvisionstylemodel.FritzVisionStylePredictor;
import ai.fritz.fritzvisionstylemodel.FritzVisionStylePredictorOptions;
import ai.fritz.fritzvisionstylemodel.FritzVisionStyleTransfer;
import ai.fritz.vision.inputs.FritzVisionImage;
import ai.fritz.vision.inputs.FritzVisionOrientation;


public class StyleActivity extends BaseCameraActivity implements OnImageAvailableListener {
    private static final String TAG = DetectorCameraBaseActivity.class.getSimpleName();

    private static final Size DESIRED_PREVIEW_SIZE = new Size(1280, 960);

    private AtomicBoolean computing = new AtomicBoolean(false);

    private Bitmap styledBitmap = null;

    private FritzVisionStylePredictor predictor;
    private FritzVisionOrientation orientation;
    private FritzVisionStylePredictorOptions options;

    private Size cameraViewSize;

    private OverlayView overlayView;
    private int activeStyleIndex = 0;

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
        ArtisticStyle[] styles = ArtisticStyle.values();
        predictor = FritzVisionStyleTransfer.getPredictor(this, styles[activeStyleIndex], options);

        orientation = FritzVisionOrientation.getImageOrientationFromCamera(this, cameraId);
        this.cameraViewSize = cameraViewSize;

        overlayView = findViewById(R.id.debug_overlay);

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (styledBitmap != null) {
                            Matrix matrix = new Matrix();
                            canvas.drawBitmap(styledBitmap, matrix, new Paint());
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

    private void getNextPredictor() {
        ArtisticStyle[] styles = ArtisticStyle.values();
        activeStyleIndex = ++activeStyleIndex % styles.length;

        Toast.makeText(this,
                styles[activeStyleIndex].name() + " Style Shown", Toast.LENGTH_LONG).show();
        predictor = FritzVisionStyleTransfer.getPredictor(this, styles[activeStyleIndex], options);
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

        final FritzVisionImage fritzImage = FritzVisionImage.fromMediaImage(image);
        fritzImage.setOrientation(orientation);
        image.close();


        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();

                        FritzVisionImage styledImage = predictor.predict(fritzImage);
                        styledImage.scale(cameraViewSize.getWidth(), cameraViewSize.getHeight());
                        styledBitmap = styledImage.getBitmap();

                        Log.d(TAG, "INFERENCE TIME:" + (SystemClock.uptimeMillis() - startTime));

                        requestRender();
                        computing.set(false);
                    }
                });
    }
}

