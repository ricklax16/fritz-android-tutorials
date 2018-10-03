package ai.fritz.heartbeat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ai.fritz.heartbeat.ui.OverlayView;
import ai.fritz.fritzvisionsegmentation.FritzVisionSegment;
import ai.fritz.fritzvisionsegmentation.FritzVisionSegmentPredictor;
import ai.fritz.peoplesegmentation.FritzVisionPeopleSegmentPredictor;
import ai.fritz.vision.inputs.FritzVisionImage;
import ai.fritz.vision.inputs.FritzVisionOrientation;
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

    List<FritzVisionSegment> segments = new ArrayList<>();
    Bitmap copiedBitmap;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        setTitle(R.string.fritz_vision_img_seg_title);
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
    public void onPreviewSizeChosen(final Size size, final Size cameraSize, final int rotation) {
        imgRotation = FritzVisionOrientation.getImageRotationFromCamera(this, cameraId);
        predictor = new FritzVisionPeopleSegmentPredictor(this);

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (segments.size() > 0) {
                            Bitmap scaledBitmap = FritzVisionImage.scale(copiedBitmap, cameraSize.getWidth(), cameraSize.getHeight());
                            canvas.drawBitmap(scaledBitmap, new Matrix(), new Paint());

                            float scaledHeight = ((float) scaledBitmap.getHeight()) / copiedBitmap.getHeight();
                            float scaledWidth = ((float) scaledBitmap.getWidth()) / copiedBitmap.getWidth();

                            for (FritzVisionSegment segment : segments) {
                                RectF boxScaled = new RectF(segment.getScaledBox().left * scaledWidth, segment.getScaledBox().top * scaledHeight, segment.getScaledBox().right * scaledWidth, segment.getScaledBox().bottom * scaledHeight);
                                Paint paint = new Paint();
                                paint.setColor(segment.getSegmentationClass().getColorIdentifier());
                                paint.setAlpha(100);
                                paint.setStyle(Paint.Style.FILL);

                                canvas.drawRect(boxScaled, paint);
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

        final FritzVisionImage visionImage = FritzVisionImage.fromMediaImage(image, imgRotation);
        image.close();

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        copiedBitmap = visionImage.getBitmap();
                        segments = predictor.predict(visionImage);
                        requestRender();
                        computing.set(false);
                    }
                });
    }
}
