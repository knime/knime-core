/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.gradientboosting.predictor;

import java.util.function.Function;

import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.model.GradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.node.predictor.AbstractPredictor;
import org.knime.base.node.mine.treeensemble2.node.predictor.RegressionPrediction;
import org.knime.core.data.DataRow;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class GBTRegressionPredictor extends AbstractPredictor<RegressionPrediction> {

    private final GradientBoostedTreesModel m_model;

    /**
     * Constructor for a {@link GBTRegressionPredictor}.
     *
     * @param model the gbt model
     * @param rowConverter converts input {@link DataRow rows} into {@link PredictorRecord records}
     */
    public GBTRegressionPredictor(final GradientBoostedTreesModel model,
        final Function<DataRow, PredictorRecord> rowConverter) {
        super(rowConverter);
        m_model = model;
    }

    /* (non-Javadoc)
     * @see org.knime.base.node.mine.treeensemble2.node.predictor.Predictor#predict(org.knime.base.node.mine.treeensemble2.data.PredictorRecord)
     */
    @Override
    public RegressionPrediction predictRecord(final PredictorRecord record) {
        double prediction = m_model.predict(record);
        return () -> prediction;
    }

}
