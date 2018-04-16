/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor.parser;

import org.knime.base.node.mine.treeensemble2.node.predictor.Prediction;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <P> the type of {@link Prediction} this parser parses
 */
public interface PredictionParser <P extends Prediction> {

    /**
     * Parses the prediction into an array of {@link DataCell}s.
     *
     * @param prediction the prediction to parse
     * @return an array of {@link DataCell}
     */
    DataCell[] parse(P prediction);

    /**
     * @return the {@link DataColumnSpec}s for the cells this parser produces
     */
    DataColumnSpec[] getAppendSpecs();
}
