/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.gradientboosting.predictor;

import java.util.Arrays;
import java.util.function.Function;

import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.model.MultiClassGradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeRegression;
import org.knime.base.node.mine.treeensemble2.node.predictor.AbstractPredictor;
import org.knime.base.node.mine.treeensemble2.node.predictor.ClassificationPrediction;
import org.knime.core.data.DataRow;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class LKGradientBoostedTreesPredictor extends AbstractPredictor<ClassificationPrediction> {

    private final MultiClassGradientBoostedTreesModel m_model;

    private final boolean m_calculateProbabilities;

    /**
     * Constructor for classification gbt predictors.
     *
     * @param model the gradient boosted trees model
     * @param calculateProbabilities indicates whether probabilities should be calculated
     * @param rowConverter converts input {@link DataRow rows} into {@link PredictorRecord records}
     */
    public LKGradientBoostedTreesPredictor(final MultiClassGradientBoostedTreesModel model,
        final boolean calculateProbabilities, final Function<DataRow, PredictorRecord> rowConverter) {
        super(rowConverter);
        m_model = model;
        m_calculateProbabilities = calculateProbabilities;
    }

    /* (non-Javadoc)
     * @see org.knime.base.node.mine.treeensemble2.node.predictor.Predictor#predict(org.knime.base.node.mine.treeensemble2.data.PredictorRecord)
     */
    @Override
    public ClassificationPrediction predictRecord(final PredictorRecord record) {
        double[] logits = calculateLogits(record);
        if (m_calculateProbabilities) {
            transformToProbabilities(logits);
            return new LKGBTPrediction(argmax(logits), logits);
        } else {
            return new LKGBTPrediction(argmax(logits));
        }
    }

    private double[] calculateLogits(final PredictorRecord record) {
        int nrClasses = m_model.getNrClasses();
        int nrLevels = m_model.getNrLevels();
        final double[] logits = new double[nrClasses];
        Arrays.fill(logits, m_model.getInitialValue());
        for (int i = 0; i < nrLevels; i++) {
            for (int j = 0; j < nrClasses; j++) {
                final TreeNodeRegression matchingNode = m_model.getModel(i, j).findMatchingNode(record);
                logits[j] += m_model.getCoefficientMap(i, j).get(matchingNode.getSignature());
            }
        }
        return logits;
    }

    private void transformToProbabilities(final double[] logits) {
        double[] probabilities = logits;
        double expSum = 0;
        for (int i = 0; i < logits.length; i++) {
            probabilities[i] = Math.exp(logits[i]);
            expSum += probabilities[i];
        }
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= expSum;
        }
    }

    private int argmax(final double[] vector) {
        int argmax = -1;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < vector.length; i++) {
            if (vector[i] > max) {
                max = vector[i];
                argmax = i;
            }
        }
        return argmax;
    }

    private class LKGBTPrediction implements ClassificationPrediction {

        private final int m_winningClassIdx;

        private final double[] m_probabilities;

        private LKGBTPrediction(final int winningClassIdx, final double[] probabilities) {
            m_winningClassIdx = winningClassIdx;
            m_probabilities = probabilities;
        }

        /**
         * Used if no probabilities are desired.
         */
        private LKGBTPrediction(final int winningClassIdx) {
            this(winningClassIdx, null);
        }

        /* (non-Javadoc)
         * @see org.knime.base.node.mine.treeensemble2.node.predictor.ClassificationPrediction#getClassPrediction()
         */
        @Override
        public String getClassPrediction() {
            return m_model.getClassLabel(m_winningClassIdx);
        }

        /* (non-Javadoc)
         * @see org.knime.base.node.mine.treeensemble2.node.predictor.ClassificationPrediction#getProbability(int)
         */
        @Override
        public double getProbability(final int classIdx) {
            assert m_probabilities != null : "No probabilities were calculated. This is likely a coding issue.";
            return m_probabilities[classIdx];
        }

        /* (non-Javadoc)
         * @see org.knime.base.node.mine.treeensemble2.node.predictor.ClassificationPrediction#getWinningClassIdx()
         */
        @Override
        public int getWinningClassIdx() {
            return m_winningClassIdx;
        }
    }

}
