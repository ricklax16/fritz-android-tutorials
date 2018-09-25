package ai.fritz.heartbeat.ui;

import java.util.List;

import ai.fritz.heartbeat.ml.Classifier.Recognition;
import ai.fritz.vision.FritzVisionLabel;

public interface ResultsView {
    void setResults(final List<Recognition> results);

    void setResult(final List<FritzVisionLabel> labels);
}