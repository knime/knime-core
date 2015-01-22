package org.knime.base.data.statistics.calculation;

import static org.knime.core.node.util.CheckUtils.checkNotNull;

import org.apache.commons.math3.stat.descriptive.StorelessUnivariateStatistic;
import org.knime.base.data.statistics.Statistic;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;

/**
 * Base class for apache math {@link StorelessUnivariateStatistic}s implementations.
 *
 * @author Marcel Hanser
 * @since 2.12
 */
public class ApacheMathStatistic extends Statistic {

    private final StorelessUnivariateStatistic m_statistic;

    private StorelessUnivariateStatistic[] m_statistics;

    /**
     * @param statistic apache 3 statistic
     * @param columns the columns
     */
    ApacheMathStatistic(final StorelessUnivariateStatistic statistic, final String... columns) {
        super(DoubleValue.class, columns);
        m_statistic = checkNotNull(statistic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init(final DataTableSpec spec, final int amountOfColumns) {
        m_statistics = new StorelessUnivariateStatistic[amountOfColumns];
        m_statistic.clear();
        for (int i = 0; i < amountOfColumns; i++) {
            m_statistics[i] = m_statistic.copy();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void consumeRow(final DataRow dataRow) {
        int index = 0;
        for (int i : getIndices()) {
            DataCell cell = dataRow.getCell(i);
            if (!cell.isMissing()) {
                m_statistics[index].increment(((DoubleValue)cell).getDoubleValue());
            }
            index++;
        }
    }

    /**
     * @param columnName the column
     * @return the statistic result for that columns
     */
    public double getResult(final String columnName) {
        StorelessUnivariateStatistic stat = m_statistics[assertIndexForColumn(columnName)];
        return stat.getN() > 0 ? stat.getResult() : Double.NaN;
    }
}