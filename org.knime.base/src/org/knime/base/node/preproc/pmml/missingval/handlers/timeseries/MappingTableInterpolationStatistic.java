/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   02.02.2015 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval.handlers.timeseries;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;

/**
 *
 * @author Alexander Fillbrunn
 */
public abstract class MappingTableInterpolationStatistic extends MappingStatistic {

    private DataCell m_previous;
    private DataContainer m_nextCells;
    private DataTable m_result;

    private String m_columnName;
    private int m_index = -1;
    private int m_counter = 0;
    private int m_numMissing = 0;

    /**
     * @return the index of the column for which the statistics are calculated.
     */
    protected int getColumnIndex() {
        return m_index;
    }

    /**
     * @return the last encountered valid, non-missing value or a missing value, if none exists.
     */
    protected DataCell getPrevious() {
        return m_previous;
    }

    protected void incMissing() {
        m_numMissing++;
    }

    protected void resetMissing(final DataCell prev) {
        m_previous = prev;
        m_numMissing = 0;
    }

    protected int getNumMissing() {
        return m_numMissing;
    }

    /**
     * Adds a mapping of a row key to a replacement value.
     * @param val the replacement value
     */
    protected void addMapping(final DataCell val) {
        m_nextCells.addRowToTable(new DefaultRow(new RowKey(Integer.toString(m_counter++)), val));
    }

    /**
     * @param clazz the class of the value for which this statistic can be calculated
     * @param column the column for which the column is calculated
     */
    public MappingTableInterpolationStatistic(final Class<? extends DataValue> clazz, final String column) {
        super(clazz, column);
        m_columnName = column;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String afterEvaluation() {
        // All remaining enqueued cells have no next value, so we return the previous one
        for (int i = 0; i < getNumMissing(); i++) {
            addMapping(m_previous);
        }

        m_nextCells.close();
        m_result = m_nextCells.getTable();
        return super.afterEvaluation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<DataCell> iterator() {
        return new MappingTableIterator(m_result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init(final DataTableSpec spec, final int amountOfColumns) {
        m_index = spec.findColumnIndex(m_columnName);
        m_nextCells = new DataContainer(new DataTableSpec(
                new DataColumnSpecCreator("value", spec.getColumnSpec(m_index).getType()).createSpec()));
        m_previous = DataType.getMissingCell();
    }

    public static final class MappingTableIterator implements Iterator<DataCell> {

        private RowIterator m_iter;

        public MappingTableIterator(final DataTable table) {
            m_iter = table.iterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return m_iter.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell next() {
            return m_iter.next().getCell(0);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
