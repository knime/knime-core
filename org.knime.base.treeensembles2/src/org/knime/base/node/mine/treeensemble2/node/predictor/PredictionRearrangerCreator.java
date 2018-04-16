/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.knime.base.node.mine.treeensemble2.node.predictor.parser.DefaultPredictionParser;
import org.knime.base.node.mine.treeensemble2.node.predictor.parser.PredictionItemParser;
import org.knime.base.node.mine.treeensemble2.node.predictor.parser.PredictionParser;
import org.knime.base.node.mine.treeensemble2.node.predictor.parser.ProbabilityItemParser;
import org.knime.base.node.mine.treeensemble2.node.predictor.parser.SingleItemParsers;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class PredictionRearrangerCreator {

    private List<PredictionItemParser<? extends Prediction>> m_itemParsers = new ArrayList<>();

    private DataTableSpec m_testSpec;

    private String m_errorMsg;

    private Predictor<? extends Prediction> m_predictor;

    /**
     * Abstract constructor.
     *
     * @param predictSpec {@link DataTableSpec} of the table to predict
     * @param predictor performs the actual prediction
     */
    public PredictionRearrangerCreator(final DataTableSpec predictSpec,
        final Predictor<? extends Prediction> predictor) {
        m_testSpec = predictSpec;
        m_predictor = predictor;
    }

    /**
     * Tries to create the prediction rearranger which may not be possible if some information is missing during
     * configuration time. In this case an empty {@link Optional} is returned. Use this method during node
     * configuration.
     *
     * @return an optional prediction rearranger
     */
    public Optional<ColumnRearranger> createConfigurationRearranger() {
        if (hasErrors()) {
            return Optional.empty();
        }
        return Optional.of(createRearranger());
    }

    /**
     * Creates the prediction rearranger. Use this method for execution.
     *
     * @return the prediction rearranger
     * @throws IllegalStateException if the rearranger can't be created
     */
    public ColumnRearranger createExecutionRearranger() {
        if (hasErrors()) {
            throw new IllegalStateException("Can't create prediction rearranger: " + m_errorMsg);
        }
        return createRearranger();
    }

    private boolean hasErrors() {
        return m_errorMsg != null;
    }

    private ColumnRearranger createRearranger() {
        ColumnRearranger cr = new ColumnRearranger(m_testSpec);
        PredictionParser parser = new DefaultPredictionParser(m_testSpec, m_itemParsers);
        PredictionCellFactory pcf = new PredictionCellFactory<>(m_predictor, parser);
        cr.append(pcf);
        return cr;
    }

    /**
     * Call this method if a prediction parser can't be added because information is missing.
     *
     * @param errorMsg the error message to set
     */
    private void setErrorMsg(final String errorMsg) {
        m_errorMsg = errorMsg;
    }

    /**
     * Adds <b>itemParser</b> to the list of item parsers.
     *
     * @param itemParser
     */
    private void addPredictionItemParser(final PredictionItemParser<? extends Prediction> itemParser) {
        m_itemParsers.add(itemParser);
    }

    /**
     * Adds a column containing the class prediction.
     *
     * @param predictionColumnName the name of the appended prediction column
     */
    public void addClassPrediction(final String predictionColumnName) {
        addPredictionItemParser(SingleItemParsers.createClassPredictionItemParser(predictionColumnName));
    }

    /**
     * Adds a column with the prediction confidence i.e. the probability of the most likely class.
     *
     * @param confidenceColName the name of the class column
     */
    public void addPredictionConfidence(final String confidenceColName) {
        addPredictionItemParser(SingleItemParsers.createConfidenceItemParser(confidenceColName));
    }

    /**
     * Adds a column for each of the possible classes containing the probability that a row belongs to this class.
     *
     * @param targetValueMap the targetValueMap
     * @param classLabels the class labels in the order the model uses internally
     * @param prefix e.g. "P ("
     * @param suffix an optional suffix that is appended to the new columns' names
     * @param classColumnName the name of the class column
     */
    public void addClassProbabilities(final Map<String, DataCell> targetValueMap, final String[] classLabels,
        final String prefix, final String suffix, final String classColumnName) {
        if (targetValueMap == null) {
            setErrorMsg("No target values available.");
            return;
        }
        addPredictionItemParser(
            new ProbabilityItemParser(targetValueMap, prefix, suffix, classColumnName, classLabels));
    }

    /**
     * Adds a column containing the number of models used for the prediction. For use with random forest models.
     */
    public void addModelCount() {
        addPredictionItemParser(SingleItemParsers.createModelCountItemParser());
    }

    /**
     * Adds a column containing the regression prediction.
     *
     * @param predictionColumnName the name of the prediction column
     */
    public void addRegressionPrediction(final String predictionColumnName) {
        addPredictionItemParser(SingleItemParsers.createRegressionPredictionItemParser(predictionColumnName));
    }

    /**
     * Adds a column with the variance of the predictions of the individual models.
     *
     * @param predictionColumnName the name of the prediction column
     */
    public void addPredictionVariance(final String predictionColumnName) {
        addPredictionItemParser(SingleItemParsers.createVarianceItemParser(predictionColumnName));
    }

}
