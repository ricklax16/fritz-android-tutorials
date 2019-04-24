package ai.fritz.heartbeat.activities.vision;

import android.graphics.Canvas;
import android.util.Size;

import ai.fritz.core.FritzOnDeviceModel;
import ai.fritz.fritzvisionobjectmodel.ObjectDetectionOnDeviceModel;
import ai.fritz.heartbeat.activities.BaseLiveVideoActivity;
import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.objectdetection.FritzVisionObjectPredictor;
import ai.fritz.vision.objectdetection.FritzVisionObjectResult;

public class ObjectDetectionActivity extends BaseLiveVideoActivity {

    private FritzVisionObjectPredictor objectPredictor;
    private FritzVisionObjectResult objectResult;

    @Override
    protected void onCameraSetup(final Size cameraSize) {
        FritzOnDeviceModel onDeviceModel = new ObjectDetectionOnDeviceModel();
        objectPredictor = FritzVision.ObjectDetection.getPredictor(onDeviceModel);
    }

    @Override
    protected void handleDrawingResult(Canvas canvas, Size cameraSize) {
        if (objectResult != null) {
            objectResult.drawBoundingBoxes(canvas, cameraSize);
        }
    }

    @Override
    protected void runInference(FritzVisionImage fritzVisionImage) {
        objectResult = objectPredictor.predict(fritzVisionImage);
    }
}
