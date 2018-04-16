/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor.parser;

import java.util.List;

import org.knime.base.node.mine.treeensemble2.node.predictor.Prediction;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.util.UniqueNameGenerator;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <P> the type of prediction this parser requires
 */
public interface PredictionItemParser <P extends Prediction> {

    /**
     * Appends the specs for the columns this parser adds to the table.
     * @param nameGenerator used to ensure unique column names
     * @param specs the list of specs to append to
     */
    void appendSpecs(UniqueNameGenerator nameGenerator, List<DataColumnSpec> specs);

    /**
     * Appends the cells extracted from <b>prediction</b>.
     * @param cells the list of cells to append to
     * @param prediction the prediction
     */
    void appendCells(List<DataCell> cells, P prediction);

}
