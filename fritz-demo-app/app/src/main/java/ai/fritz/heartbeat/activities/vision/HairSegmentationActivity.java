package ai.fritz.heartbeat.activities.vision;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.util.Size;

import ai.fritz.fritzvisionhairsegmentationmodel.HairSegmentationOnDeviceModel;
import ai.fritz.heartbeat.activities.BaseLiveVideoActivity;
import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.imagesegmentation.BlendMode;
import ai.fritz.vision.imagesegmentation.BlendModeType;
import ai.fritz.vision.imagesegmentation.FritzVisionSegmentPredictorOptions;
import ai.fritz.vision.imagesegmentation.FritzVisionSegmentResult;
import ai.fritz.vision.imagesegmentation.FritzVisionSegmentTFLPredictor;
import ai.fritz.vision.imagesegmentation.MaskType;
import ai.fritz.vision.imagesegmentation.SegmentOnDeviceModel;

public class HairSegmentationActivity extends BaseLiveVideoActivity {
    private static final int maskColor = Color.RED;

    private static final BlendMode blendMode = BlendModeType.HUE.create();
    private FritzVisionSegmentTFLPredictor hairPredictor;
    private FritzVisionSegmentResult hairResult;

    @Override
    protected void onCameraSetup(final Size cameraSize) {
        SegmentOnDeviceModel onDeviceModel = new HairSegmentationOnDeviceModel();
        MaskType.HAIR.color = maskColor;
        hairPredictor = FritzVision.ImageSegmentation.getPredictorTFL(onDeviceModel, new FritzVisionSegmentPredictorOptions.Builder()
                .targetConfidenceThreshold(.2f)
                .build());
    }

    @Override
    protected void handleDrawingResult(Canvas canvas, Size cameraSize) {
        if (hairResult != null) {
            FritzVisionImage originalImage = hairResult.getOriginalImage();
            Bitmap maskBitmap = hairResult.createMaskOverlayBitmap(blendMode.getAlpha());
            Bitmap blendedBitmap = originalImage.blend(maskBitmap, blendMode);
            canvas.drawBitmap(blendedBitmap, null, new RectF(0, 0, cameraSize.getWidth(), cameraSize.getHeight()), null);
        }
    }

    @Override
    protected void runInference(FritzVisionImage fritzVisionImage) {
        hairResult = hairPredictor.predict(fritzVisionImage);
    }
}
