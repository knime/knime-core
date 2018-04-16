/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.util.UniqueNameGenerator;

import com.google.common.collect.Lists;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <P> the type of prediction this parser parses
 */
public final class DefaultPredictionParser<P extends Prediction> implements PredictionParser<P> {

    private final Collection<PredictionItemParser<P>> m_itemParsers;

    private final DataColumnSpec[] m_appendSpecs;

    /**
     * The order of the output columns will follow the order of the provided parsers
     *
     * @param testSpec {@link DataTableSpec} of the test table
     * @param itemParsers the parsers to apply to the prediction
     *
     */
    public DefaultPredictionParser(final DataTableSpec testSpec,
        final Collection<PredictionItemParser<P>> itemParsers) {
        m_itemParsers = Lists.newArrayList(itemParsers);
        m_appendSpecs = createSpecs(testSpec);
    }

    private DataColumnSpec[] createSpecs(final DataTableSpec testSpec) {
        UniqueNameGenerator nameGen = new UniqueNameGenerator(testSpec);
        List<DataColumnSpec> specs = new ArrayList<>();
        for (PredictionItemParser<P> parser : m_itemParsers) {
            parser.appendSpecs(nameGen, specs);
        }
        return specs.toArray(new DataColumnSpec[specs.size()]);
    }

    /* (non-Javadoc)
     * @see org.knime.base.node.mine.treeensemble2.node.predictor.PredictionParser#parse(org.knime.base.node.mine.treeensemble2.node.predictor.Prediction)
     */
    @Override
    public DataCell[] parse(final P prediction) {
        List<DataCell> cells = new ArrayList<>(m_appendSpecs.length);
        for (PredictionItemParser<P> parser : m_itemParsers) {
            parser.appendCells(cells, prediction);
        }
        return cells.toArray(new DataCell[cells.size()]);
    }

    /* (non-Javadoc)
     * @see org.knime.base.node.mine.treeensemble2.node.predictor.PredictionParser#getAppendSpecs()
     */
    @Override
    public DataColumnSpec[] getAppendSpecs() {
        return m_appendSpecs;
    }

}
