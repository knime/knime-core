/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.node.predictor.parser.PredictionParser;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.AbstractCellFactory;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <P> the type of prediction
 */
public class PredictionCellFactory<P extends Prediction> extends AbstractCellFactory {

    private final Predictor<P> m_predictor;

    private final PredictionParser<P> m_predictionParser;

    /**
     * Constructor for a PredictionCellFactory.
     *
     * @param predictor produces predictions from {@link PredictorRecord}s
     * @param predictionParser parses predictions produced by the <b>predictor</b> into {@link DataCell}s
     */
    public PredictionCellFactory(final Predictor<P> predictor, final PredictionParser<P> predictionParser) {
        super(predictionParser.getAppendSpecs());
        setParallelProcessing(true);
        m_predictor = predictor;
        m_predictionParser = predictionParser;
    }

    /* (non-Javadoc)
     * @see org.knime.core.data.container.CellFactory#getCells(org.knime.core.data.DataRow)
     */
    @Override
    public final DataCell[] getCells(final DataRow row) {
        P prediction = m_predictor.predict(row);
        return m_predictionParser.parse(prediction);
    }

}
