/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

import java.util.List;
import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.util.UniqueNameGenerator;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <P> the type of prediction
 * @param <C> the type of cell that is produced
 */
public class SingleItemParser <P extends Prediction, C extends DataCell> implements PredictionItemParser<P> {

    private final String m_spec;

    private final Function<P, C> m_parseFn;

    private final DataType m_type;

    /**
     * Constructs an instance of a {@link SingleItemParser}.
     *
     * @param name column name
     * @param type column type
     * @param parseFn function from prediction to DataCell
     */
    public SingleItemParser(final String name, final DataType type, final Function<P, C> parseFn) {
        m_spec = name;
        m_parseFn = parseFn;
        m_type = type;
    }

    /* (non-Javadoc)
     * @see org.knime.base.node.mine.treeensemble2.node.predictor.PredictionItemParser#appendSpecs(org.knime.core.util.UniqueNameGenerator, java.util.List)
     */
    @Override
    public void appendSpecs(final UniqueNameGenerator nameGenerator, final List<DataColumnSpec> specs) {
        specs.add(nameGenerator.newColumn(m_spec, m_type));
    }

    /* (non-Javadoc)
     * @see org.knime.base.node.mine.treeensemble2.node.predictor.PredictionItemParser#appendCells(java.util.List, org.knime.base.node.mine.treeensemble2.node.predictor.Prediction)
     */
    @Override
    public void appendCells(final List<DataCell> cells, final P prediction) {
        cells.add(m_parseFn.apply(prediction));
    }

}
