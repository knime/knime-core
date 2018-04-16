/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor.classification;

import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetColumnData;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.model.TreeModelClassification;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeClassification;
import org.knime.base.node.mine.treeensemble2.node.predictor.AbstractRandomForestPredictor;
import org.knime.base.node.mine.treeensemble2.node.predictor.RandomForestClassificationPrediction;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class RandomForestClassificationPredictor
    extends AbstractRandomForestPredictor<RandomForestClassificationPrediction> {

    private final VotingFactory m_votingFactory;

    /**
     * @param model
     * @param modelSpec
     * @param predictSpec
     * @param modelRowSamples
     * @param targetColumnData
     * @param votingFactory
     * @throws InvalidSettingsException
     */
    public RandomForestClassificationPredictor(final TreeEnsembleModel model,
        final TreeEnsembleModelPortObjectSpec modelSpec, final DataTableSpec predictSpec,
        final RowSample[] modelRowSamples, final TreeTargetColumnData targetColumnData,
        final VotingFactory votingFactory) throws InvalidSettingsException {
        super(model, modelSpec, predictSpec, modelRowSamples, targetColumnData);
        m_votingFactory = votingFactory;
    }

    /**
     * @param model
     * @param modelSpec
     * @param predictSpec
     * @param votingFactory
     * @throws InvalidSettingsException
     */
    public RandomForestClassificationPredictor(final TreeEnsembleModel model,
        final TreeEnsembleModelPortObjectSpec modelSpec, final DataTableSpec predictSpec,
        final VotingFactory votingFactory) throws InvalidSettingsException {
        super(model, modelSpec, predictSpec);
        m_votingFactory = votingFactory;
    }

    /* (non-Javadoc)
     * @see org.knime.base.node.mine.treeensemble2.node.predictor.AbstractRandomForestPredictor#predictRecord(org.knime.base.node.mine.treeensemble2.data.PredictorRecord, org.knime.core.data.RowKey)
     */
    @Override
    protected RandomForestClassificationPrediction predictRecord(final PredictorRecord record, final RowKey key) {
        return new RFClassificationPrediction(record, key, hasOutOfBagFilter());
    }

    private class RFClassificationPrediction implements RandomForestClassificationPrediction {

        private final Voting m_voting;

        RFClassificationPrediction(final PredictorRecord record, final RowKey key, final boolean hasOutOfBagFilter) {
            m_voting = m_votingFactory.createVoting();
            final int nrModels = m_model.getNrModels();
            int nrValidModels = 0;
            for (int i = 0; i < nrModels; i++) {
                if (hasOutOfBagFilter && isRowPartOfTrainingData(key, i)) {
                    System.out.println("Row " + key + " ignored.");
                    // ignore, row was used to train the model
                } else {
                    TreeModelClassification m = m_model.getTreeModelClassification(i);
                    TreeNodeClassification match = m.findMatchingNode(record);
                    m_voting.addVote(match);
                    nrValidModels += 1;
                }
            }
        }

        /* (non-Javadoc)
         * @see org.knime.base.node.mine.treeensemble2.node.predictor.ClassificationPrediction#getClassPrediction()
         */
        @Override
        public String getClassPrediction() {
            return m_voting.getMajorityClass();
        }

        /* (non-Javadoc)
         * @see org.knime.base.node.mine.treeensemble2.node.predictor.ClassificationPrediction#getWinningClassIdx()
         */
        @Override
        public int getWinningClassIdx() {
            return m_voting.getMajorityClassIdx();
        }

        /* (non-Javadoc)
         * @see org.knime.base.node.mine.treeensemble2.node.predictor.ClassificationPrediction#getProbability(int)
         */
        @Override
        public double getProbability(final int classIdx) {
            return m_voting.getClassProbabilityForClass(classIdx);
        }

        /* (non-Javadoc)
         * @see org.knime.base.node.mine.treeensemble2.node.predictor.OutOfBagPrediction#getModelCount()
         */
        @Override
        public int getModelCount() {
            return m_voting.getNrVotes();
        }
    }
}
