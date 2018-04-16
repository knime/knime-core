/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

import java.util.function.Function;

import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.core.data.DataRow;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <P> the type of prediction
 */
public abstract class AbstractPredictor <P extends Prediction> implements Predictor<P> {

    private final Function<DataRow, PredictorRecord> m_rowConverter;

    /**
     * Constructor for AbstractPredictor.
     *
     * @param rowConverter converts input {@link DataRow rows} into {@link PredictorRecord records}
     */
    public AbstractPredictor(final Function<DataRow, PredictorRecord> rowConverter) {
        m_rowConverter = rowConverter;
    }

    /* (non-Javadoc)
     * @see org.knime.base.node.mine.treeensemble2.node.predictor.Predictor#predict(org.knime.core.data.DataRow)
     */
    @Override
    public P predict(final DataRow row) {
        return predictRecord(m_rowConverter.apply(row));
    }

    /**
     * @param record the record to predict
     * @return prediction
     */
    protected abstract P predictRecord(PredictorRecord record);

}
