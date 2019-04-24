package ai.fritz.heartbeat.activities.vision;

import android.graphics.Bitmap;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import ai.fritz.core.FritzOnDeviceModel;
import ai.fritz.core.utils.FritzModelManager;
import ai.fritz.core.utils.FritzOptional;
import ai.fritz.heartbeat.activities.BaseRecordingActivity;
import ai.fritz.heartbeat.R;
import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.PredictorStatusListener;
import ai.fritz.vision.imagesegmentation.FritzVisionSegmentPredictor;
import ai.fritz.vision.imagesegmentation.FritzVisionSegmentResult;
import ai.fritz.vision.imagesegmentation.LivingRoomSegmentManagedModel;
import ai.fritz.vision.imagesegmentation.OutdoorSegmentManagedModel;
import ai.fritz.vision.imagesegmentation.PeopleSegmentManagedModel;
import ai.fritz.vision.imagesegmentation.SegmentManagedModel;
import ai.fritz.vision.imagesegmentation.SegmentOnDeviceModel;


public class ImageSegmentationActivity extends BaseRecordingActivity implements OnImageAvailableListener {

    private static final String TAG = ImageSegmentationActivity.class.getSimpleName();
    private FritzVisionSegmentPredictor predictor;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getModelOptionsTextId() {
        return R.array.img_seg_model_options;
    }

    @Override
    protected Bitmap runPrediction(FritzVisionImage visionImage, Size cameraViewSize) {
        FritzVisionSegmentResult segmentResult = predictor.predict(visionImage);
        return segmentResult.toBitmap();
    }

    @Override
    protected void loadPredictor(int choice) {
        SegmentManagedModel managedModel = getManagedModel(choice);
        FritzOptional<FritzOnDeviceModel> onDeviceModelOpt = FritzModelManager.getActiveOnDeviceModel(managedModel.getModelId());
        if (onDeviceModelOpt.isPresent()) {
            showPredictorReadyViews();
            FritzOnDeviceModel onDeviceModel = onDeviceModelOpt.get();
            SegmentOnDeviceModel segmentOnDeviceModel = SegmentOnDeviceModel.mergeFromManagedModel(
                    onDeviceModel,
                    managedModel);
            predictor = FritzVision.ImageSegmentation.getPredictor(segmentOnDeviceModel);
        } else {
            showPredictorNotReadyViews();
            FritzVision.ImageSegmentation.loadPredictor(managedModel, new PredictorStatusListener<FritzVisionSegmentPredictor>() {
                @Override
                public void onPredictorReady(FritzVisionSegmentPredictor segmentPredictor) {
                    Log.d(TAG, "Segmentation predictor is ready");
                    predictor = segmentPredictor;
                    showPredictorReadyViews();
                }
            });
        }
    }

    private SegmentManagedModel getManagedModel(int choice) {

        switch (choice) {
            case (1):
                return new LivingRoomSegmentManagedModel();
            case (2):
                return new OutdoorSegmentManagedModel();
            default:
                return new PeopleSegmentManagedModel();
        }
    }
}

