package ai.fritz.heartbeat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.concurrent.atomic.AtomicBoolean;

import ai.fritz.core.utils.BitmapUtils;
import ai.fritz.heartbeat.ui.OverlayView;
import ai.fritz.peoplesegmentation.PeopleSegmentOnDeviceModel;
import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionOrientation;
import ai.fritz.vision.models.SegmentOnDeviceModel;
import ai.fritz.vision.outputs.FritzVisionSegmentResult;
import ai.fritz.vision.predictors.FritzVisionSegmentPredictor;
import butterknife.ButterKnife;


public class ImageSegmentationActivity extends BaseCameraActivity implements ImageReader.OnImageAvailableListener {
    private static final String TAG = LiveVideoActivity.class.getSimpleName();

    /**
     * Requests for the size of the preview depending on the camera results. We will try to match the closest
     * in terms of size and aspect ratio.
     */
    private static final Size DESIRED_PREVIEW_SIZE = new Size(960, 1280);

    private AtomicBoolean computing = new AtomicBoolean(false);

    private FritzVisionSegmentPredictor predictor;
    private int imgRotation;

    private FritzVisionSegmentResult segmentResult;
    private FritzVisionImage visionImage;

    Button snapshotButton;
    RelativeLayout previewLayout;
    RelativeLayout snapshotLayout;
    OverlayView snapshotOverlay;
    ProgressBar spinner;

    Button closeButton;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        setTitle(R.string.fritz_vision_img_seg_title);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_snapshot;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final Size cameraSize, final int rotation) {
        imgRotation = FritzVisionOrientation.getImageRotationFromCamera(this, cameraId);
        SegmentOnDeviceModel onDeviceModel = new PeopleSegmentOnDeviceModel();
        predictor = FritzVision.ImageSegmentation.getPredictor(onDeviceModel);
        snapshotButton = findViewById(R.id.take_picture_btn);
        previewLayout = findViewById(R.id.preview_frame);
        snapshotLayout = findViewById(R.id.snapshot_frame);
        snapshotOverlay = findViewById(R.id.snapshot_view);
        closeButton = findViewById(R.id.close_btn);
        spinner = findViewById(R.id.spinner);

        if (snapshotOverlay != null) {
            snapshotOverlay.addCallback(new OverlayView.DrawCallback() {
                @Override
                public void drawCallback(final Canvas canvas) {
                    if (segmentResult != null) {
                        segmentResult.drawVisionImage(canvas, cameraSize);
                        segmentResult.drawAllMasks(canvas, 100, cameraSize);
                    }
                }
            });
        }

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (visionImage != null) {
                            Bitmap scaledBitmap = BitmapUtils.scale(visionImage.getBitmap(), cameraSize.getWidth(), cameraSize.getHeight());
                            canvas.drawBitmap(scaledBitmap, new Matrix(), new Paint());
                        }
                    }
                });

        snapshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!computing.compareAndSet(false, true)) {
                    return;
                }

                runInBackground(
                        new Runnable() {
                            @Override
                            public void run() {
                                showSpinner();
                                segmentResult = predictor.predict(visionImage);
                                showSnapshotLayout();
                                hideSpinner();
                                snapshotOverlay.postInvalidate();
                                computing.set(false);
                            }
                        });
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPreviewLayout();
            }
        });
    }

    private void showSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setVisibility(View.GONE);
            }
        });
    }

    private void showSnapshotLayout() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                previewLayout.setVisibility(View.GONE);
                snapshotLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showPreviewLayout() {
        previewLayout.setVisibility(View.VISIBLE);
        snapshotLayout.setVisibility(View.GONE);
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = reader.acquireLatestImage();

        if (image == null) {
            return;
        }

        if (computing.get()) {
            image.close();
            requestRender();
            return;
        }

        visionImage = FritzVisionImage.fromMediaImage(image, imgRotation);
        image.close();

        requestRender();
    }
}