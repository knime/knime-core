/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor.regression;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.Variance;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetColumnData;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeRegression;
import org.knime.base.node.mine.treeensemble2.node.predictor.AbstractRandomForestPredictor;
import org.knime.base.node.mine.treeensemble2.node.predictor.RandomForestRegressionPrediction;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class RandomForestRegressionPredictor extends AbstractRandomForestPredictor<RandomForestRegressionPrediction> {

    /**
     * @param model
     * @param modelSpec
     * @param predictSpec
     * @throws InvalidSettingsException
     */
    public RandomForestRegressionPredictor(final TreeEnsembleModel model,
        final TreeEnsembleModelPortObjectSpec modelSpec, final DataTableSpec predictSpec)
        throws InvalidSettingsException {
        super(model, modelSpec, predictSpec);
    }

    /**
     * @param model
     * @param modelSpec
     * @param predictSpec
     * @param modelRowSamples
     * @param targetColumnData
     * @throws InvalidSettingsException
     */
    public RandomForestRegressionPredictor(final TreeEnsembleModel model,
        final TreeEnsembleModelPortObjectSpec modelSpec, final DataTableSpec predictSpec,
        final RowSample[] modelRowSamples, final TreeTargetColumnData targetColumnData)
        throws InvalidSettingsException {
        super(model, modelSpec, predictSpec, modelRowSamples, targetColumnData);
    }

    /* (non-Javadoc)
     * @see org.knime.base.node.mine.treeensemble2.node.predictor.AbstractRandomForestPredictor#predictRecord(org.knime.base.node.mine.treeensemble2.data.PredictorRecord, org.knime.core.data.RowKey)
     */
    @Override
    protected RandomForestRegressionPrediction predictRecord(final PredictorRecord record, final RowKey key) {
        return new RFRegressionPrediction(record, key, hasOutOfBagFilter());
    }

    private class RFRegressionPrediction implements RandomForestRegressionPrediction {

        private final double m_mean;

        private final double m_variance;

        private final int m_modelCount;

        /**
         *
         */
        RFRegressionPrediction(final PredictorRecord record, final RowKey key, final boolean hasOutOfBagFilter) {
            Mean mean = new Mean();
            Variance variance = new Variance();
            final int nrModels = m_model.getNrModels();
            for (int i = 0; i < nrModels; i++) {
                if (hasOutOfBagFilter && isRowPartOfTrainingData(key, i)) {
                    // ignore, row was used to train the model
                } else {
                    TreeModelRegression m = m_model.getTreeModelRegression(i);
                    TreeNodeRegression match = m.findMatchingNode(record);
                    double nodeMean = match.getMean();
                    mean.increment(nodeMean);
                    variance.increment(nodeMean);
                }
            }
            m_modelCount = (int)mean.getN();
            m_variance = variance.getResult();
            m_mean = mean.getResult();
        }

        /* (non-Javadoc)
         * @see org.knime.base.node.mine.treeensemble2.node.predictor.RegressionPrediction#getPrediction()
         */
        @Override
        public double getPrediction() {
            return m_mean;
        }

        /* (non-Javadoc)
         * @see org.knime.base.node.mine.treeensemble2.node.predictor.OutOfBagPrediction#getModelCount()
         */
        @Override
        public int getModelCount() {
            return m_modelCount;
        }

        /* (non-Javadoc)
         * @see org.knime.base.node.mine.treeensemble2.node.predictor.RandomForestRegressionPrediction#getVariance()
         */
        @Override
        public double getVariance() {
            return m_variance;
        }

    }

}
