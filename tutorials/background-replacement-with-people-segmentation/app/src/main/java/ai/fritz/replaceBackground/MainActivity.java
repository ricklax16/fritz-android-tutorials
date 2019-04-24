package ai.fritz.replaceBackground;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import ai.fritz.core.Fritz;
import ai.fritz.core.utils.BitmapUtils;
import ai.fritz.peoplesegmentation.PeopleSegmentOnDeviceModel;
import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionOrientation;
import ai.fritz.vision.imagesegmentation.FritzVisionSegmentPredictor;
import ai.fritz.vision.imagesegmentation.FritzVisionSegmentPredictorOptions;
import ai.fritz.vision.imagesegmentation.FritzVisionSegmentResult;
import ai.fritz.vision.imagesegmentation.MaskType;


public class MainActivity extends BaseCameraActivity implements ImageReader.OnImageAvailableListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int SELECT_IMAGE = 1;

    private AtomicBoolean shouldSample = new AtomicBoolean(true);
    private FritzVisionSegmentPredictor predictor;
    private int imgRotation;

    private FritzVisionSegmentResult segmentResult;
    private FritzVisionImage visionImage;

    Button snapshotButton;
    Button selectBackgroundBtn;
    RelativeLayout previewLayout;
    RelativeLayout snapshotLayout;
    OverlayView snapshotOverlay;
    ProgressBar snapshotProcessingSpinner;
    Button closeButton;

    private Bitmap backgroundBitmap;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fritz.configure(getApplicationContext(), "<Your API key>");

        PeopleSegmentOnDeviceModel onDeviceModel = new PeopleSegmentOnDeviceModel();
        FritzVisionSegmentPredictorOptions options = new FritzVisionSegmentPredictorOptions.Builder()
                .targetConfidenceThreshold(.4f)
                .build();
        predictor = FritzVision.ImageSegmentation.getPredictor(onDeviceModel, options);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode != SELECT_IMAGE) {
            return;
        }
        if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return;
            }
            try {
                Uri selectedPicture = data.getData();
                Log.d(TAG, "IMAGE CHOSEN: " + selectedPicture);

                InputStream inputStream = getContentResolver().openInputStream(selectedPicture);
                ExifInterface exif = new ExifInterface(inputStream);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                backgroundBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedPicture);

                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        backgroundBitmap = BitmapUtils.rotate(backgroundBitmap, 0);
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        backgroundBitmap = BitmapUtils.rotate(backgroundBitmap, 270);
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        backgroundBitmap = BitmapUtils.rotate(backgroundBitmap, 180);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_background_replace;
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final Size cameraSize, final int rotation) {
        imgRotation = FritzVisionOrientation.getImageRotationFromCamera(this, cameraId);

        snapshotButton = findViewById(R.id.take_picture_btn);
        previewLayout = findViewById(R.id.preview_frame);
        snapshotLayout = findViewById(R.id.snapshot_frame);
        snapshotOverlay = findViewById(R.id.snapshot_view);
        closeButton = findViewById(R.id.close_btn);
        snapshotProcessingSpinner = findViewById(R.id.snapshotProcessingSpinner);
        selectBackgroundBtn = findViewById(R.id.selectBackgroundBtn);

        snapshotOverlay.setCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(final Canvas canvas) {

                if (segmentResult == null || backgroundBitmap == null) {
                    if (segmentResult != null) {
                        Bitmap predictionResult = segmentResult.getResultBitmap(cameraSize);
                        canvas.drawBitmap(predictionResult, new Matrix(), new Paint());
                    } else if (visionImage != null) {
                        Bitmap scaledBitmap = BitmapUtils.resize(visionImage.rotateBitmap(), cameraSize.getWidth(), cameraSize.getHeight());
                        canvas.drawBitmap(scaledBitmap, new Matrix(), new Paint());
                    }

                    return;
                }

                // Show the background replacement
                Bitmap scaledBackgroundBitmap = BitmapUtils.resize(backgroundBitmap, cameraSize.getWidth(), cameraSize.getHeight());
                long startTime = System.currentTimeMillis();
                Bitmap maskBitmap = segmentResult.createMaskedBitmap(MaskType.PERSON);
                Log.d(TAG, "Masked bitmap took " + (System.currentTimeMillis() - startTime) + "ms to create.");

                // Scale the result
                float scaleWidth = ((float) cameraSize.getWidth()) / maskBitmap.getWidth();
                float scaleHeight = ((float) cameraSize.getWidth()) / maskBitmap.getHeight();

                final Matrix matrix = new Matrix();
                float scale = Math.min(scaleWidth, scaleHeight);
                matrix.postScale(scale, scale);

                Bitmap scaledMaskBitmap = Bitmap.createBitmap(maskBitmap, 0, 0, maskBitmap.getWidth(), maskBitmap.getHeight(), matrix, false);

                // Print the background bitmap with the masked bitmap
                canvas.drawBitmap(scaledBackgroundBitmap, new Matrix(), new Paint());
                // Center the masked bitmap at the bottom of the image.
                canvas.drawBitmap(scaledMaskBitmap, (cameraSize.getWidth() - scaledMaskBitmap.getWidth()) / 2, cameraSize.getHeight() - scaledMaskBitmap.getHeight(), new Paint());
            }
        });

        snapshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!shouldSample.compareAndSet(true, false)) {
                    return;
                }

                snapshotOverlay.postInvalidate();

                runInBackground(
                        new Runnable() {
                            @Override
                            public void run() {
                                showSpinner();
                                segmentResult = predictor.predict(visionImage);
                                showSnapshotLayout();
                                hideSpinner();
                                snapshotOverlay.postInvalidate();
                            }
                        });
            }
        });


        selectBackgroundBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMAGE);
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPreviewLayout();
                shouldSample.set(true);
                backgroundBitmap = null;
            }
        });
    }

    private void showSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                snapshotProcessingSpinner.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                snapshotProcessingSpinner.setVisibility(View.GONE);
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

        if (!shouldSample.get()) {
            image.close();
            return;
        }

        visionImage = FritzVisionImage.fromMediaImage(image, imgRotation);
        image.close();
    }
}