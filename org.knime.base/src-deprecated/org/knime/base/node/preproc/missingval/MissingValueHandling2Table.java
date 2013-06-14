/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.missingval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.knime.base.data.statistics.StatisticsTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.MutableInteger;

/**
 * DataTable that replaces missing values according to ColSetting objects.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class MissingValueHandling2Table implements DataTable {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(MissingValueHandling2Table.class);

    /** We need min/max etc. */
    private final DataTable m_table;

    /** Settings for each column. */
    private final MissingValueHandling2ColSetting[] m_settings;

    /** The data table spec for this table. */
    private final DataTableSpec m_spec;

    /**
     * Creates new table.
     *
     * @param table table with missing values
     * @param colSetting the settings, for each column one and in correct order
     */
    private MissingValueHandling2Table(final DataTable table,
            final MissingValueHandling2ColSetting[] colSetting) {
        m_settings = colSetting;
        m_table = table;
        m_spec = createTableSpecPrivate(table.getDataTableSpec(), m_settings);
    }

    /**
     * Get reference to underlying table.
     *
     * @return the table being wrapped by us
     */
    DataTable getUnderlyingTable() {
        return m_table;
    }

    /**
     * Tries to retrieve the number of rows in the underlying table.
     * @return The number of rows or -1 if not possible.
     */
    int getNrRowsInReference() {
        if (m_table instanceof BufferedDataTable) {
            return ((BufferedDataTable)m_table).getRowCount();
        } else if (m_table instanceof MyStatisticsTable) {
            DataTable underlying =
                ((MyStatisticsTable)m_table).getUnderlyingTable();
            if (underlying instanceof BufferedDataTable) {
                return ((BufferedDataTable)underlying).getRowCount();
            }
        }
        return -1;
    }

    /** {@inheritDoc} */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /** {@inheritDoc} */
    @Override
    public RowIterator iterator() {
        return new MissingValueHandling2TableIterator(this);
    }

    /**
     * Delegating method to StatisticsTable.
     *
     * @param column the column of interest
     * @return the maximum in a column
     * @see StatisticsTable#getMax(int)
     */
    protected DataCell getMax(final int column) {
        return getStatisticsTable().getMax(column);
    }

    /**
     * Delegating method to StatisticsTable.
     *
     * @param column the column of interest
     * @return the mean in a column (or {@link Double#NaN} for non-double
     *         columns)
     * @see StatisticsTable#getMean(int)
     */
    protected double getMean(final int column) {
        return getStatisticsTable().getMean(column);
    }

    /**
     * Get the most frequent value in a column.
     *
     * @param column the column of interest
     * @return Most frequent value in it.
     * @see StatisticsTable#getMean(int)
     */
    protected DataCell getMostFrequent(final int column) {
        return getStatisticsTable().getMostFrequentCell(column);
    }

    /**
     * Delegating method to StatisticsTable.
     *
     * @param column the column of interest
     * @return the min in a column
     * @see StatisticsTable#getMin(int)
     */
    protected DataCell getMin(final int column) {
        return getStatisticsTable().getMin(column);
    }

    /**
     * This method is only called when we do replacement of missing values with
     * either min, max or mean values. The table as provided in the constructor
     * must have been a StatisticsTable then.
     *
     * @return cast of m_table.
     */
    private MyStatisticsTable getStatisticsTable() {
        return (MyStatisticsTable)m_table;
    }

    /**
     * Get RowIterator of underlying table.
     *
     * @return the iterator of the base table
     */
    RowIterator getInternalIterator() {
        return m_table.iterator();
    }

    /**
     * The column settings for a column.
     *
     * @param column the column of interest
     * @return the column setting to use
     */
    MissingValueHandling2ColSetting getColSetting(final int column) {
        return m_settings[column];
    }

    /**
     * Get the DataTableSpec that is created when creating a
     * MissingValueHandling with the settings <code>sets</code>.
     *
     * @param spec the spec of the original input table
     * @param sets the column settings to apply
     * @return the new DataTableSpec when handling missing values
     * @throws InvalidSettingsException if the settings don't fit to the data
     */
    static DataTableSpec createTableSpec(final DataTableSpec spec,
            final MissingValueHandling2ColSetting[] sets) throws InvalidSettingsException {
        MissingValueHandling2ColSetting[] realSettings = getColSetting(spec, sets, true);
        return createTableSpecPrivate(spec, realSettings);

    }

    /* private helper that assumes the ColSetting to have the right format. */
    private static DataTableSpec createTableSpecPrivate(
            final DataTableSpec spec, final MissingValueHandling2ColSetting[] sets) {
        assert (spec.getNumColumns() == sets.length);
        DataColumnSpec[] newSpecs = new DataColumnSpec[sets.length];
        for (int i = 0; i < sets.length; i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);
            DataColumnSpec newSpec = colSpec;
            if (sets[i].getMethod() == MissingValueHandling2ColSetting.METHOD_FIX_VAL) {
                DataColumnDomain dom = colSpec.getDomain();
                Comparator<DataCell> comp = colSpec.getType().getComparator();
                DataCell fixCell = sets[i].getFixCell();
                boolean changed = false;
                DataCell l = dom.getLowerBound();
                // lower and upper bound should not contain missing values
                // (but rather be null). It may happen anyway, we catch it here
                if (l != null && !l.isMissing()
                        && (comp.compare(fixCell, l) < 0)) {
                    changed = true;
                    l = fixCell;
                }
                DataCell u = dom.getUpperBound();
                if (u != null && !u.isMissing()
                        && (comp.compare(fixCell, u) > 0)) {
                    changed = true;
                    u = fixCell;
                }
                Set<DataCell> vals = dom.getValues();
                if (vals != null && !vals.contains(fixCell)) {
                    changed = true;
                    vals = new LinkedHashSet<DataCell>(vals);
                    vals.add(fixCell);
                }
                if (changed) {
                    DataColumnDomain newDom = new DataColumnDomainCreator(vals,
                            l, u).createDomain();
                    DataColumnSpecCreator c =
                        new DataColumnSpecCreator(colSpec);
                    c.setDomain(newDom);
                    newSpec = c.createSpec();
                }
            }
            newSpecs[i] = newSpec;
        }
        return new DataTableSpec(newSpecs);
    }

    /* Does internal mapping of the constructor argument. */
    private static MissingValueHandling2ColSetting[] getColSetting(final DataTableSpec spec,
            final MissingValueHandling2ColSetting[] sets, final boolean throwExeception)
            throws InvalidSettingsException {
        MissingValueHandling2ColSetting[] results = new MissingValueHandling2ColSetting[spec.getNumColumns()];

        // fill up the default (i.e. meta-settings for String, Double, Int,
        // and Other columns) - if they are available
        Hashtable<Integer, MissingValueHandling2ColSetting> hash =
            new Hashtable<Integer, MissingValueHandling2ColSetting>();
        // the default one
        MissingValueHandling2ColSetting untouched = new MissingValueHandling2ColSetting(MissingValueHandling2ColSetting.TYPE_UNKNOWN);
        untouched.setMethod(MissingValueHandling2ColSetting.METHOD_NO_HANDLING);
        for (int i = 0; i < sets.length; i++) {
            if (sets[i].isMetaConfig()) {
                int type = sets[i].getType();
                hash.put(type, sets[i]);
            }
        }
        // there are only double, int, string, other
        assert (hash.size() <= 4);

        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);
            DataType type = colSpec.getType();
            Integer hashKey;
            if (type.isASuperTypeOf(StringCell.TYPE)) {
                hashKey = MissingValueHandling2ColSetting.TYPE_STRING;
            } else if (type.isASuperTypeOf(DoubleCell.TYPE)) {
                hashKey = MissingValueHandling2ColSetting.TYPE_DOUBLE;
            } else if (type.isASuperTypeOf(IntCell.TYPE)) {
                hashKey = MissingValueHandling2ColSetting.TYPE_INT;
            } else {
                hashKey = MissingValueHandling2ColSetting.TYPE_UNKNOWN;
            }
            MissingValueHandling2ColSetting setting = hash.get(hashKey);
            if (setting == null) {
                setting = untouched;
            }
            // may be replaced by an individual setting below
            results[i] = setting;
        }

        for (int i = 0; i < sets.length; i++) {
            if (sets[i].isMetaConfig()) {
                continue;
            }
            String[] names = sets[i].getNames();
            for (int j = 0; j < names.length; j++) {
                String name = names[j];
                int type = sets[i].getType();
                final int index = spec.findColumnIndex(name);
                if (index < 0) {
                    String error = "Unable to do missing value handling for"
                            + " column '" + name + "', no such column in table";
                    if (throwExeception) {
                        throw new InvalidSettingsException(error);
                    } else {
                        error = error + "; skip it.";
                        LOGGER.warn(error);
                    }
                    continue;
                }
                DataColumnSpec colSpec = spec.getColumnSpec(index);
                DataType colType = colSpec.getType();
                if (type == MissingValueHandling2ColSetting.TYPE_INT
                        && !colType.isASuperTypeOf(IntCell.TYPE)) {
                    String error = "Missing value handling for column '" + name
                            + "' failed, incompatible types: " + colType
                            + " is not super type of int type";
                    if (throwExeception) {
                        throw new InvalidSettingsException(error);
                    } else {
                        error = error + "; skip it.";
                        LOGGER.warn(error);
                    }
                }
                if (type == MissingValueHandling2ColSetting.TYPE_DOUBLE
                        && !colType.isASuperTypeOf(DoubleCell.TYPE)) {
                    String error = "Missing value handling for column '" + name
                            + "' failed, incompatible types: " + colType
                            + " is not super type of double type";
                    if (throwExeception) {
                        throw new InvalidSettingsException(error);
                    } else {
                        error = error + "; skip it.";
                        LOGGER.warn(error);
                    }
                }
                if (type == MissingValueHandling2ColSetting.TYPE_STRING
                        && !colType.isASuperTypeOf(StringCell.TYPE)) {
                    String error = "Missing value handling for column '" + name
                            + "' failed, incompatible types: " + colType
                            + " is not super type of string type";
                    if (throwExeception) {
                        throw new InvalidSettingsException(error);
                    } else {
                        error = error + "; skip it.";
                        LOGGER.warn(error);
                    }
                }
                results[index] = sets[i];
            }
        }
        return results;
    } // getColSetting(DataTableSpec, ColSetting[])

    /**
     * Does missing value handling to the argument table given the col settings
     * in an array and also reports progress.
     *
     * @param table the table to do missing value handling on
     * @param colSettings the settings
     * @param exec for progress/cancel and to create the buffered data table
     * @param warningBuffer To which potential warning messages are added.
     * @return a cache table, cleaned up
     * @throws CanceledExecutionException if canceled
     */
    public static BufferedDataTable createMissingValueHandlingTable(
            final DataTable table, final MissingValueHandling2ColSetting[] colSettings,
            final ExecutionContext exec, final StringBuffer warningBuffer)
        throws CanceledExecutionException {
        MissingValueHandling2ColSetting[] colSetting;
        try {
            colSetting = getColSetting(
                    table.getDataTableSpec(), colSettings, false);
        } catch (InvalidSettingsException ise) {
            LOGGER.coding("getColSetting method is not supposed to throw "
                    + "an exception, ignoring settings", ise);
            DataTableSpec s = table.getDataTableSpec();
            colSetting = new MissingValueHandling2ColSetting[s.getNumColumns()];
            for (int i = 0; i < s.getNumColumns(); i++) {
                colSetting[i] = new MissingValueHandling2ColSetting(s.getColumnSpec(i));
                colSetting[i].setMethod(MissingValueHandling2ColSetting.METHOD_NO_HANDLING);
            }
        }
        boolean needStatistics = false;
        int mostFrequentColCount = 0;
        for (int i = 0; i < colSetting.length; i++) {
            MissingValueHandling2ColSetting c = colSetting[i];
            switch (c.getMethod()) {
            case MissingValueHandling2ColSetting.METHOD_MOST_FREQUENT:
                mostFrequentColCount++;
            case MissingValueHandling2ColSetting.METHOD_MAX:
            case MissingValueHandling2ColSetting.METHOD_MIN:
            case MissingValueHandling2ColSetting.METHOD_MEAN:
                needStatistics = true;
                break;
            default:
            }
        }
        int[] mostFrequentCols = new int[mostFrequentColCount];
        if (mostFrequentColCount > 0) {
            int index = 0;
            for (int i = 0; i < colSetting.length; i++) {
                MissingValueHandling2ColSetting c = colSetting[i];
                switch (c.getMethod()) {
                case MissingValueHandling2ColSetting.METHOD_MOST_FREQUENT:
                    mostFrequentCols[index++] = i;
                    break;
                default:
                }
            }
        }
        DataTable t;
        ExecutionMonitor e;
        if (needStatistics && !(table instanceof StatisticsTable)) {
            // for creating statistics table
            ExecutionMonitor subExec = exec.createSubProgress(0.5);
            t = new MyStatisticsTable(table, subExec, mostFrequentCols);
            if (((MyStatisticsTable)t).m_warningMessage != null) {
                warningBuffer.append(((MyStatisticsTable)t).m_warningMessage);
            }
            // for the iterator
            e = exec.createSubProgress(0.5);
        } else {
            t = table;
            e = exec;
        }
        MissingValueHandling2Table mvht =
            new MissingValueHandling2Table(t, colSetting);
        BufferedDataContainer container =
            exec.createDataContainer(mvht.getDataTableSpec());
        e.setMessage("Adding rows...");
        int count = 0;
        try {
            MissingValueHandling2TableIterator it =
                new MissingValueHandling2TableIterator(mvht, e);
            while (it.hasNext()) {
                DataRow next;
                next = it.next();
                e.setMessage("Adding row " + (count + 1) + " (\""
                        + next.getKey() + "\")");
                container.addRowToTable(next);
                count++;
            }
        } catch (MissingValueHandling2TableIterator.
                RuntimeCanceledExecutionException rcee) {
            throw rcee.getCause();
        } finally {
            container.close();
        }
        return container.getTable();
    }

    /**
     * Determines not only min, max, mean but also most frequent values
     * in a certain column set.
     */
    private static final class MyStatisticsTable extends StatisticsTable {
        private HashMap<DataCell, MutableInteger>[] m_countMaps;
        private int[] m_cols;
        private DataCell[] m_mostFrequentCells;
        private String m_warningMessage;

        @SuppressWarnings("unchecked")
        private MyStatisticsTable(final DataTable t,
                final ExecutionMonitor exec, final int[] cols)
            throws CanceledExecutionException {
            super(t);
            final int colCount = t.getDataTableSpec().getNumColumns();
            m_countMaps = new HashMap[colCount];
            for (int i = 0; i < cols.length; i++) {
                // use linked hash map to get the first most frequent value
                // when counts are equal
                m_countMaps[cols[i]] =
                    new LinkedHashMap<DataCell, MutableInteger>();
            }
            m_cols = cols;
            calculateAllMoments(exec);
            m_mostFrequentCells = new DataCell[colCount];
            ArrayList<String> errorCols = new ArrayList<String>();
            for (int i = 0; i < m_cols.length; i++) {
                HashMap<DataCell, MutableInteger> map =
                    m_countMaps[cols[i]];
                // determine most frequent item
                int bestCount = 0;
                DataCell best = null;
                for (Map.Entry<DataCell, MutableInteger> e : map.entrySet()) {
                    int count = e.getValue().intValue();
                    if (count > bestCount) {
                        bestCount = count;
                        best = e.getKey();
                    }
                }
                if (best == null) {
                    String colName =
                        t.getDataTableSpec().getColumnSpec(cols[i]).getName();
                    best = DataType.getMissingCell();
                    errorCols.add(colName);
                }
                m_mostFrequentCells[cols[i]] = best;
            }
            if (errorCols.isEmpty()) {
                m_warningMessage = null;
            } else {
                m_warningMessage = "Column(s) "
                    + Arrays.toString(errorCols.toArray())
                    + " contain(s) no valid cells.";
            }
            m_cols = null;
            m_countMaps = null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void calculateMomentInSubClass(final DataRow row) {
            for (int i = 0; i < m_cols.length; i++) {
                DataCell c = row.getCell(m_cols[i]);
                HashMap<DataCell, MutableInteger> map =
                    m_countMaps[m_cols[i]];
                if (!c.isMissing()) {
                    MutableInteger count = map.get(c);
                    if (count == null) {
                        map.put(c, new MutableInteger(1));
                    } else {
                        count.inc();
                    }
                }
            }
        }

        /**
         * @param col The column index
         * @return The most frequent cell.
         */
        public DataCell getMostFrequentCell(final int col) {
            if (m_mostFrequentCells[col] == null) {
                throw new IndexOutOfBoundsException("Didn't calculate most"
                        + " frequent value for column " + col);
            }
            return m_mostFrequentCells[col];
        }

        /**
         * Changes scope.
         * @see StatisticsTable#getUnderlyingTable()
         */
        @Override
        protected DataTable getUnderlyingTable() {
            return super.getUnderlyingTable();
        }

    }
}
