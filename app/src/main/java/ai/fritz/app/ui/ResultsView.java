package ai.fritz.app.ui;

import java.util.List;

import ai.fritz.app.ml.Classifier.Recognition;
import ai.fritz.vision.FritzVisionLabel;

public interface ResultsView {
    void setResults(final List<Recognition> results);

    void setResult(final List<FritzVisionLabel> labels);
}