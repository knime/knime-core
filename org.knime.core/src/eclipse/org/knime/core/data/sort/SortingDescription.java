package org.knime.core.data.sort;

import static org.knime.core.node.util.CheckUtils.checkArgument;
import static org.knime.core.node.util.CheckUtils.checkSetting;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;

/**
 * The configuration class for the AbstractColumnTableSorter. It defines a set of columns which are
 * split from the original table and sorted according to the compare method.
 *
 * @author Marcel Hanser
 * @since 2.11
 */
public abstract class SortingDescription implements Comparator<DataRow> {

    private String[] m_toSort;

    private int[] m_indexes;

    /**
     * @param columns the columns to sort
     * @throws IllegalArgumentException if columns is null, empty or contains null values
     */
    public SortingDescription(final String... columns) {
        checkArgument(ArrayUtils.isNotEmpty(columns), "Column name array cannot be empty.");
        checkArgument(!ArrayUtils.contains(columns, null), "Null values are not permitted.");
        this.m_toSort = columns;
    }

    /**
     * finds and sets the indices of the set columns.
     *
     * @param spec the spec
     * @throws InvalidSettingsException if the columns are not contained in the spec
     */
    final void init(final DataTableSpec spec) throws InvalidSettingsException {
        m_indexes = new int[spec.getNumColumns()];
        Arrays.fill(m_indexes, -1);
        for (int i = 0; i < m_toSort.length; i++) {
            int colIndex = spec.findColumnIndex(m_toSort[i]);
            checkSetting(colIndex >= 0, "Column '%s' does not exist in input table.", m_toSort[i]);
            m_indexes[i] = colIndex;
        }
    }

    /**
     * @return the column names
     */
    protected String[] getToSort() {
        return m_toSort.clone();
    }

    /**
     * @param spec original spec
     * @return a data table spec describing the subset of columns which are configured in this description
     */
    public DataTableSpec createDataTableSpec(final DataTableSpec spec) {
        DataTableSpecCreator specCreator = new DataTableSpecCreator();
        for (int i = 0; i < m_indexes.length; i++) {
            int orgIndex = m_indexes[i];
            if (orgIndex >= 0) {
                specCreator.addColumns(spec.getColumnSpec(orgIndex));
            }
        }
        return specCreator.createSpec();
    }

    /**
     * @param originalRow the original row
     * @return a row just comprising the cells of the set columns
     */
    final DataRow createSubRow(final DataRow originalRow) {
        return new MappedDataRow(originalRow, m_indexes);
    }

    /**
     * Wrapper for a DataRow which maps a certain subset of indices according to the given map.
     *
     * @author Marcel Hanser
     */
    final class MappedDataRow implements DataRow {
        private final int[] m_indexMap;

        private final DataRow m_delegate;

        private final int m_count;

        /**
         * @param delegate the underlying row
         * @param indexMap index map
         */
        MappedDataRow(final DataRow delegate, final int... indexMap) {
            m_indexMap = indexMap;
            m_delegate = delegate;
            int count = 0;
            for (int i : indexMap) {
                if (i >= 0) {
                    count++;
                }
            }
            m_count = count;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getNumCells() {
            return m_count;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RowKey getKey() {
            return m_delegate.getKey();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final int index) {
            int i = m_indexMap[index];
            checkArgument(i >= 0, "The index '%d' is not mapped in the current row.", index);
            return m_delegate.getCell(i);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<DataCell> iterator() {

            return new Iterator<DataCell>() {
                private int m_currIndex = m_indexMap[0] >= 0 ? 0 : getNextSetIndex(0);

                @Override
                public boolean hasNext() {
                    return m_currIndex >= 0;
                }

                @Override
                public DataCell next() {
                    if (m_currIndex < 0) {
                        throw new NoSuchElementException();
                    }
                    DataCell cell = m_delegate.getCell(m_indexMap[m_currIndex]);
                    m_currIndex = getNextSetIndex(m_currIndex);
                    return cell;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        private int getNextSetIndex(final int current) {
            for (int i = current + 1; i < m_indexMap.length; i++) {
                if (m_indexMap[i] >= 0) {
                    return i;
                }
            }
            return -1;
        }
    }
}
