package ai.fritz.heartbeat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.concurrent.atomic.AtomicBoolean;

import ai.fritz.heartbeat.ui.OverlayView;
import ai.fritz.heartbeat.utils.Navigation;
import ai.fritz.core.utils.BitmapUtils;
import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionOrientation;
import ai.fritz.vision.PredictorStatusListener;
import ai.fritz.vision.imagesegmentation.FritzVisionSegmentResult;
import ai.fritz.vision.imagesegmentation.FritzVisionSegmentPredictor;
import ai.fritz.vision.imagesegmentation.LivingRoomSegmentManagedModel;
import ai.fritz.vision.imagesegmentation.OutdoorSegmentManagedModel;
import ai.fritz.vision.imagesegmentation.PeopleSegmentManagedModel;
import ai.fritz.vision.imagesegmentation.SegmentManagedModel;
import butterknife.ButterKnife;


public class ImageSegmentationActivity extends BaseCameraActivity implements ImageReader.OnImageAvailableListener {
    private static final String TAG = ImageSegmentationActivity.class.getSimpleName();

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

    private PredictorType predictorType;

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

        Intent callingIntent = getIntent();
        predictorType = PredictorType.valueOf(callingIntent.getStringExtra(Navigation.PREDICTOR_TYPE_KEY));
        Log.d(TAG, predictorType.name());
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

        snapshotButton = findViewById(R.id.take_picture_btn);
        snapshotButton.setVisibility(View.GONE);
        previewLayout = findViewById(R.id.preview_frame);
        snapshotLayout = findViewById(R.id.snapshot_frame);
        snapshotOverlay = findViewById(R.id.snapshot_view);
        closeButton = findViewById(R.id.close_btn);
        spinner = findViewById(R.id.spinner);
        SegmentManagedModel managedModel = getManagedModelType();
        FritzVision.ImageSegmentation.loadPredictor(managedModel, new PredictorStatusListener<FritzVisionSegmentPredictor>() {
            @Override
            public void onPredictorReady(FritzVisionSegmentPredictor segmentPredictor) {
                Log.d(TAG, "Segmentation predictor is ready");
                predictor = segmentPredictor;
                // Uncomment to test out the crop and scale option.
                // predictor.setOptions(new FritzVisionSegmentPredictorOptions.Builder().cropAndScaleOption(FritzVisionCropAndScale.CENTER_CROP).build());
                snapshotButton.setVisibility(View.VISIBLE);
            }
        });

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
                            Bitmap scaledBitmap = BitmapUtils.scale(visionImage.rotateBitmap(), cameraSize.getWidth(), cameraSize.getHeight());
                            canvas.drawBitmap(scaledBitmap, new Matrix(), new Paint());
                        }
                    }
                });

        snapshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (predictor == null) {
                    Log.d(TAG, "Predictor is not ready");
                    return;
                }
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

    private SegmentManagedModel getManagedModelType() {
        if(predictorType == PredictorType.LIVING_ROOM_SEGMENTATION) {
            return new LivingRoomSegmentManagedModel();
        }

        if(predictorType == PredictorType.OUTDOOR_SEGMENTATION) {
            return new OutdoorSegmentManagedModel();
        }

        return new PeopleSegmentManagedModel();
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