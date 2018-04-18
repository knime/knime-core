/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetColumnData;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeEnsembleModel;
import org.knime.base.node.mine.treeensemble2.model.MultiClassGradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.node.predictor.classification.HardVotingFactory;
import org.knime.base.node.mine.treeensemble2.node.predictor.classification.RandomForestClassificationPredictor;
import org.knime.base.node.mine.treeensemble2.node.predictor.classification.SoftVotingFactory;
import org.knime.base.node.mine.treeensemble2.node.predictor.classification.VotingFactory;
import org.knime.base.node.mine.treeensemble2.node.predictor.regression.RandomForestRegressionPredictor;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class TreeEnsemblePredictionUtil {

    private static final String CONFIDENCE_SUFFIX = " (Confidence)";

    private TreeEnsemblePredictionUtil() {
        // utility class
    }

    /**
     * Creates a row converter for random forest and gradient boosted trees models.
     *
     * @param modelSpec the spec of the model
     * @param model the actual model (may be null)
     * @param tableSpec the table on which to predict
     * @return a row converter
     * @throws InvalidSettingsException if columns required by <b>modelSpec</b> are not present in <b>tableSpec</b>
     */
    public static Function<DataRow, PredictorRecord> createRowConverter(final TreeEnsembleModelPortObjectSpec modelSpec,
        final AbstractTreeEnsembleModel model, final DataTableSpec tableSpec) throws InvalidSettingsException {
        int[] filterIndices = modelSpec.calculateFilterIndices(tableSpec);
        DataTableSpec learnSpec = modelSpec.getLearnTableSpec();
        return r -> model.createPredictorRecord(new FilterColumnRow(r, filterIndices), learnSpec);
    }

    /**
     * Setups the PredictionRearrangerCreator for classification gbts.
     *
     * @param pre36 indicates if the model was build prior to version 3.6.0
     * @param crc the creator
     * @param modelSpec the spec of the model
     * @param model the gbt
     * @param config the predictor configuration
     * @throws InvalidSettingsException if something goes wrong
     */
    public static void setupRearrangerCreatorGBT(final boolean pre36, final PredictionRearrangerCreator crc,
        final TreeEnsembleModelPortObjectSpec modelSpec, final MultiClassGradientBoostedTreesModel model,
        final TreeEnsemblePredictorConfiguration config) throws InvalidSettingsException {
        if (pre36) {
            crc.addClassPrediction(config.getPredictionColumnName());
            if (config.isAppendPredictionConfidence()) {
                crc.addPredictionConfidence("Confidence");
            }
            if (config.isAppendClassConfidences()) {
                crc.addClassProbabilities(modelSpec.getTargetColumnPossibleValueMap(),
                    model == null ? null : model.getClassLabels(), "P(", config.getSuffixForClassProbabilities(),
                    modelSpec.getTargetColumn().getName());
            }
        } else {
            if (config.isAppendClassConfidences()) {
                addClassProbabilites(crc, "P (", modelSpec, model, config);
            }
            crc.addClassPrediction(config.getPredictionColumnName());
            if (config.isAppendPredictionConfidence()) {
                crc.addPredictionConfidence(config.getPredictionColumnName() + CONFIDENCE_SUFFIX);
            }
        }
    }

    private static void addClassProbabilites(final PredictionRearrangerCreator crc, final String prefix,
        final TreeEnsembleModelPortObjectSpec modelSpec, final MultiClassGradientBoostedTreesModel model,
        final TreeEnsemblePredictorConfiguration config) throws InvalidSettingsException {
        crc.addClassProbabilities(modelSpec.getTargetColumnPossibleValueMap(),
            model == null ? null : model.getClassLabels(), prefix, config.getSuffixForClassProbabilities(),
            modelSpec.getTargetColumn().getName());
    }

    /**
     * Creates a {@link PredictionRearrangerCreator} for creation of a {@link ColumnRearranger}
     * that can be used to predict with a classification random forest.
     *
     * @param dataSpec the spec of the table to predict
     * @param modelSpec the spec of the (classification) random forest
     * @param model the (classification) random forest
     * @param modelRowSamples row samples used to train the individual trees (may be null)
     * @param targetColumnData the target column (may be null)
     * @param config for the prediction
     * @param pre36 flag that indicates if the node was created prior to version 3.6.0
     * @return a creator that allows to create a rearranger for prediction with a random forest
     * @throws InvalidSettingsException if <b>dataSpec</b> is missing some columns the model needs
     */
    public static PredictionRearrangerCreator createPRCForClassificationRF(final DataTableSpec dataSpec,
        final TreeEnsembleModelPortObjectSpec modelSpec, final TreeEnsembleModel model,
        final RowSample[] modelRowSamples, final TreeTargetColumnData targetColumnData,
        final TreeEnsemblePredictorConfiguration config, final boolean pre36) throws InvalidSettingsException {

        Map<String, DataCell> targetValueMap = modelSpec.getTargetColumnPossibleValueMap();
        Map<String, Integer> targetVal2Idx = createTargetValueToIndexMap(targetValueMap);
        VotingFactory votingFactory =
            config.isUseSoftVoting() ? new SoftVotingFactory(targetVal2Idx) : new HardVotingFactory(targetVal2Idx);
        RandomForestClassificationPredictor predictor =
            modelRowSamples == null ? new RandomForestClassificationPredictor(model, modelSpec, dataSpec, votingFactory)
                : new RandomForestClassificationPredictor(model, modelSpec, dataSpec, modelRowSamples, targetColumnData,
                    votingFactory);
        PredictionRearrangerCreator prc = new PredictionRearrangerCreator(dataSpec, predictor);

        if (pre36) {
            prc.addClassPrediction(config.getPredictionColumnName());
            if (config.isAppendPredictionConfidence()) {
                prc.addPredictionConfidence(config.getPredictionColumnName() + CONFIDENCE_SUFFIX);
            }
            if (config.isAppendClassConfidences()) {
                prc.addClassProbabilities(targetValueMap,
                    targetValueMap.keySet().stream().map(o -> o).toArray(i -> new String[i]), "P(",
                    config.getSuffixForClassProbabilities(), modelSpec.getTargetColumn().getName());
            }
        } else {
            if (config.isAppendClassConfidences()) {
                prc.addClassProbabilities(targetValueMap,
                    targetValueMap.keySet().stream().map(o -> o).toArray(i -> new String[i]), "P (",
                    config.getSuffixForClassProbabilities(), modelSpec.getTargetColumn().getName());
            }
            prc.addClassPrediction(config.getPredictionColumnName());
            if (config.isAppendPredictionConfidence()) {
                prc.addPredictionConfidence(config.getPredictionColumnName() + CONFIDENCE_SUFFIX);
            }
        }
        if (config.isAppendModelCount()) {
            prc.addModelCount();
        }

        return prc;
    }

    private static Map<String, Integer> createTargetValueToIndexMap(final Map<String, DataCell> targetValueMap) {
        final Map<String, Integer> targetValueToIndexMap = new HashMap<>(targetValueMap.size());
        Iterator<String> targetValIterator = targetValueMap.keySet().iterator();
        for (int i = 0; i < targetValueMap.size(); i++) {
            targetValueToIndexMap.put(targetValIterator.next(), i);
        }
        return targetValueToIndexMap;
    }

    /**
     * Creates a {@link PredictionRearrangerCreator} for creation of a {@link ColumnRearranger}
     * that can be used to predict with a regression random forest.
     *
     * @param dataSpec the spec of the table to predict
     * @param modelSpec the spec of the (regression) random forest
     * @param model the (regression) random forest
     * @param modelRowSamples row samples used to train the individual trees (may be null)
     * @param targetColumnData the target column (may be null)
     * @param config for the prediction
     * @return a creator that allows to create a rearranger for prediction with a random forest
     * @throws InvalidSettingsException if <b>dataSpec</b> is missing some columns the model needs
     */
    public static PredictionRearrangerCreator createPRCForRegressionRF(final DataTableSpec dataSpec,
        final TreeEnsembleModelPortObjectSpec modelSpec, final TreeEnsembleModel model,
        final RowSample[] modelRowSamples, final TreeTargetColumnData targetColumnData,
        final TreeEnsemblePredictorConfiguration config) throws InvalidSettingsException {
        RandomForestRegressionPredictor predictor =
            modelRowSamples == null ? new RandomForestRegressionPredictor(model, modelSpec, dataSpec)
                : new RandomForestRegressionPredictor(model, modelSpec, dataSpec, modelRowSamples, targetColumnData);
        PredictionRearrangerCreator prc = new PredictionRearrangerCreator(dataSpec, predictor);
        prc.addRegressionPrediction(config.getPredictionColumnName());
        prc.addPredictionVariance(config.getPredictionColumnName());
        if (config.isAppendModelCount()) {
            prc.addModelCount();
        }
        return prc;
    }
}
