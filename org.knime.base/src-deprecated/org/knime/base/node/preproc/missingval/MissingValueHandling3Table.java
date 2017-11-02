/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.missingval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.base.data.statistics.Statistics3Table;
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
import org.knime.core.node.util.ConvenienceMethods;

/**
 * DataTable that replaces missing values according to ColSetting objects.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Gabor Bakos
 * @since 2.8
 * @deprecated See new Missing node that incorporates a PMML outport in package
 * org.knime.base.node.preproc.pmml.missingval
 */
@Deprecated
public final class MissingValueHandling3Table implements DataTable {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(MissingValueHandling3Table.class);

    /** We need min/max etc. */
    private final BufferedDataTable m_table;

    /** Settings for each column. */
    private final MissingValueHandling2ColSetting[] m_settings;

    /** The data table spec for this table. */
    private final DataTableSpec m_spec;

    private final MyStatisticsTable m_statisticsTable;

    /**
     * Creates new table.
     *
     * @param table table with missing values
     * @param colSetting the settings, for each column one and in correct order
     */
    private MissingValueHandling3Table(final BufferedDataTable table, final MyStatisticsTable statisticsTable,
        final MissingValueHandling2ColSetting[] colSetting) {
        m_settings = colSetting;
        m_table = table;
        m_statisticsTable = statisticsTable;
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
     *
     * @return The number of rows or -1 if not possible.
     */
    long getNrRowsInReference() {
        return m_table.size();
    }

    /** {@inheritDoc} */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /** {@inheritDoc} */
    @Override
    public RowIterator iterator() {
        return new MissingValueHandling3TableIterator(this);
    }

    /**
     * Delegating method to StatisticsTable.
     *
     * @param column the column of interest
     * @return the maximum in a column
     * @see Statistics3Table#getMaxCells()
     */
    protected DataCell getMax(final int column) {
        return getStatisticsTable().getMaxCells()[column];
    }

    /**
     * Delegating method to StatisticsTable.
     *
     * @param column the column of interest
     * @return the mean in a column (or {@link Double#NaN} for non-double columns)
     * @see Statistics3Table#getMean()
     */
    protected double getMean(final int column) {
        return getStatisticsTable().getMean(column);
    }

    /**
     * Get the most frequent value in a column.
     *
     * @param column the column of interest
     * @return Most frequent value in it.
     * @see MyStatisticsTable#getMostFrequentCell(int)
     */
    protected DataCell getMostFrequent(final int column) {
        return getStatisticsTable().getMostFrequentCell(column);
    }

    /**
     * Delegating method to StatisticsTable.
     *
     * @param column the column of interest
     * @return the min in a column
     * @see Statistics3Table#getMinCells()
     */
    protected DataCell getMin(final int column) {
        return getStatisticsTable().getMinCells()[column];
    }

    /**
     * This method is only called when we do replacement of missing values with either min, max or mean values. The
     * table as provided in the constructor must have been a StatisticsTable then.
     *
     * @return cast of m_table.
     */
    private MyStatisticsTable getStatisticsTable() {
        return m_statisticsTable;
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
     * The number of observed NaN values for each column.
     * @return number of observed NaN values
     */
    int getNumberNaNValues(final int colIdx) {
        return m_statisticsTable.getNumberNaNValues(colIdx);
    }

    /**
     * Get the DataTableSpec that is created when creating a MissingValueHandling with the settings <code>sets</code>.
     *
     * @param spec the spec of the original input table
     * @param sets the column settings to apply
     * @param warningsBuffer buffer to print warnings messages to the UI
     * @return the new DataTableSpec when handling missing values
     * @throws InvalidSettingsException if the settings don't fit to the data
     */
    static DataTableSpec createTableSpec(final DataTableSpec spec, final MissingValueHandling2ColSetting[] sets,
        final StringBuilder warningsBuffer) throws InvalidSettingsException {
        MissingValueHandling2ColSetting[] realSettings = getColSetting(spec, sets, true, warningsBuffer);
        return createTableSpecPrivate(spec, realSettings);

    }

    /* private helper that assumes the ColSetting to have the right format. */
    private static DataTableSpec createTableSpecPrivate(final DataTableSpec spec,
        final MissingValueHandling2ColSetting[] sets) {
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
                if (l != null && !l.isMissing() && (comp.compare(fixCell, l) < 0)) {
                    changed = true;
                    l = fixCell;
                }
                DataCell u = dom.getUpperBound();
                if (u != null && !u.isMissing() && (comp.compare(fixCell, u) > 0)) {
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
                    DataColumnDomain newDom = new DataColumnDomainCreator(vals, l, u).createDomain();
                    DataColumnSpecCreator c = new DataColumnSpecCreator(colSpec);
                    c.setDomain(newDom);
                    newSpec = c.createSpec();
                }
            }
            newSpecs[i] = newSpec;
        }
        return new DataTableSpec(newSpecs);
    }

    /* Does internal mapping of the constructor argument. */
    private static MissingValueHandling2ColSetting[]
        getColSetting(final DataTableSpec spec, final MissingValueHandling2ColSetting[] sets,
            final boolean throwExeception, final StringBuilder warningsBuffer) throws InvalidSettingsException {
        MissingValueHandling2ColSetting[] results = new MissingValueHandling2ColSetting[spec.getNumColumns()];

        // fill up the default (i.e. meta-settings for String, Double, Int,
        // and Other columns) - if they are available
        Hashtable<Integer, MissingValueHandling2ColSetting> hash =
            new Hashtable<Integer, MissingValueHandling2ColSetting>();
        // the default one
        MissingValueHandling2ColSetting untouched =
            new MissingValueHandling2ColSetting(MissingValueHandling2ColSetting.TYPE_UNKNOWN);
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

        Set<String> missingColumns = new LinkedHashSet<String>();

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
                    String error =
                        "Unable to do missing value handling for column '" + name + "', no such column in table";
                    if (throwExeception) {
                        // to be consistent with other nodes no exception is thrown here,
                        // instead a warnings is propagated. See at the end of this method.
                        missingColumns.add(name);
                    } else {
                        error = error + "; skip it.";
                        LOGGER.warn(error);
                    }
                    continue;
                }
                DataColumnSpec colSpec = spec.getColumnSpec(index);
                DataType colType = colSpec.getType();
                if (type == MissingValueHandling2ColSetting.TYPE_INT && !colType.isASuperTypeOf(IntCell.TYPE)) {
                    String error =
                        "Missing value handling for column '" + name + "' failed, incompatible types: " + colType
                            + " is not super type of int type";
                    if (throwExeception) {
                        throw new InvalidSettingsException(error);
                    } else {
                        error = error + "; skip it.";
                        LOGGER.warn(error);
                    }
                }
                if (type == MissingValueHandling2ColSetting.TYPE_DOUBLE && !colType.isASuperTypeOf(DoubleCell.TYPE)) {
                    String error =
                        "Missing value handling for column '" + name + "' failed, incompatible types: " + colType
                            + " is not super type of double type";
                    if (throwExeception) {
                        throw new InvalidSettingsException(error);
                    } else {
                        error = error + "; skip it.";
                        LOGGER.warn(error);
                    }
                }
                if (type == MissingValueHandling2ColSetting.TYPE_STRING && !colType.isASuperTypeOf(StringCell.TYPE)) {
                    String error =
                        "Missing value handling for column '" + name + "' failed, incompatible types: " + colType
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

        if (!missingColumns.isEmpty()) {
            warningsBuffer.append("Following columns are configured but no longer exist: "
                    + ConvenienceMethods.getShortStringFrom(missingColumns, 5));
        }
        return results;
    }

    /**
     * Does missing value handling to the argument table given the col settings in an array and also reports progress.
     *
     * @param table the table to do missing value handling on
     * @param colSettings the settings
     * @param exec for progress/cancel and to create the buffered data table
     * @param warningBuffer To which potential warning messages are added.
     * @return a cache table, cleaned up
     * @throws CanceledExecutionException if canceled
     * @since 2.8
     * @deprecated use {@link #createMissingValueHandlingTable(BufferedDataTable, MissingValueHandling2ColSetting[],
     *              ExecutionContext, StringBuilder)} instead
     */
    @Deprecated
    public static BufferedDataTable createMissingValueHandlingTable(final BufferedDataTable table,
        final MissingValueHandling2ColSetting[] colSettings, final ExecutionContext exec,
        final StringBuffer warningBuffer) throws CanceledExecutionException {
        StringBuilder temp = new StringBuilder();
        BufferedDataTable outTable = createMissingValueHandlingTable(table, colSettings, exec, temp);
        warningBuffer.append(temp);
        return outTable;
    }

    /**
     * Does missing value handling to the argument table given the col settings in an array and also reports progress.
     *
     * @param table the table to do missing value handling on
     * @param colSettings the settings
     * @param exec for progress/cancel and to create the buffered data table
     * @param warningBuffer To which potential warning messages are added.
     * @return a cache table, cleaned up
     * @throws CanceledExecutionException if canceled
     * @since 2.10
     */
    public static BufferedDataTable createMissingValueHandlingTable(final BufferedDataTable table,
        final MissingValueHandling2ColSetting[] colSettings, final ExecutionContext exec,
        final StringBuilder warningBuffer) throws CanceledExecutionException {
        MissingValueHandling2ColSetting[] colSetting;
        try {
            colSetting = getColSetting(table.getDataTableSpec(), colSettings, false, warningBuffer);
        } catch (InvalidSettingsException ise) {
            LOGGER.coding("getColSetting method is not supposed to throw an exception, ignoring settings", ise);
            DataTableSpec s = table.getDataTableSpec();
            colSetting = new MissingValueHandling2ColSetting[s.getNumColumns()];
            for (int i = 0; i < s.getNumColumns(); i++) {
                colSetting[i] = new MissingValueHandling2ColSetting(s.getColumnSpec(i));
                colSetting[i].setMethod(MissingValueHandling2ColSetting.METHOD_NO_HANDLING);
            }
        }
        boolean needStatistics = false;

        final Set<Integer> mostFrequentColumns = new HashSet<Integer>();
        for (int i = 0; i < colSetting.length; i++) {
            MissingValueHandling2ColSetting c = colSetting[i];
            switch (c.getMethod()) {
                case MissingValueHandling2ColSetting.METHOD_MOST_FREQUENT:
                	mostFrequentColumns.add(i);
                case MissingValueHandling2ColSetting.METHOD_MAX:
                case MissingValueHandling2ColSetting.METHOD_MIN:
                case MissingValueHandling2ColSetting.METHOD_MEAN:
                    needStatistics = true;
                    break;
                default:
            }
        }

        MyStatisticsTable myT;
        ExecutionMonitor e;
        if (needStatistics) {
            // for creating statistics table
            ExecutionContext subExec = exec.createSubExecutionContext(0.5);
            myT = new MyStatisticsTable(table, subExec) {
                //do not try to get this Iterable in the constructor, it will not work, as long as
                //Statistics3Table does the statistical computation in the constructor.
                @Override
                protected Iterable<Integer> getMostFrequentColumns() {
                    return mostFrequentColumns;
                }
            };
            if (myT.m_warningMessage != null) {
                if (warningBuffer.length() > 0) {
                    warningBuffer.append('\n');
                }
                warningBuffer.append(myT.m_warningMessage);
            }
            // for the iterator
            e = exec.createSubProgress(0.5);
        } else {
            myT = null;
            e = exec;
        }
        MissingValueHandling3Table mvht = new MissingValueHandling3Table(table, myT, colSetting);
        BufferedDataContainer container = exec.createDataContainer(mvht.getDataTableSpec());
        e.setMessage("Adding rows...");
        int count = 0;
        try {
            MissingValueHandling3TableIterator it = new MissingValueHandling3TableIterator(mvht, e);
            while (it.hasNext()) {
                DataRow next;
                next = it.next();
                e.setMessage("Adding row " + (count + 1) + " (\"" + next.getKey() + "\")");
                container.addRowToTable(next);
                count++;
            }
        } catch (MissingValueHandling3TableIterator.RuntimeCanceledExecutionException rcee) {
            throw rcee.getCause();
        } finally {
            container.close();
        }
        return container.getTable();
    }

    /**
     * Determines not only min, max, mean but also most frequent values in a certain column set.
     */
    private abstract static class MyStatisticsTable extends Statistics3Table {
        private Map<Integer, LinkedHashMap<DataCell, Integer>> m_currentMostFrequent;

        private Map<Integer, DataCell> m_actualMostFrequent;

        private String m_warningMessage;

        private MyStatisticsTable(final BufferedDataTable t, final ExecutionContext exec)
            throws CanceledExecutionException {
            super(t, false, 0, Collections.<String> emptyList(), exec);
            ArrayList<String> errorCols = new ArrayList<String>();

            for (Integer col : getMostFrequentColumns()) {
                LinkedHashMap<DataCell, Integer> map = getCurrentMostFrequent().get(col);
                if (map != null) {
                    getActualMostFrequent().put(col, findMostFrequent(map));
                } else {
                    errorCols.add(t.getDataTableSpec().getColumnSpec(col).getName());
                }
            }
            if (errorCols.isEmpty()) {
                m_warningMessage = null;
            } else if (errorCols.size() == 1) {
                m_warningMessage = String.format("Column %s contains no valid cells.",
                    ConvenienceMethods.getShortStringFrom(errorCols, 5));
            } else {
                m_warningMessage = String.format("Columns %s contain no valid cells.",
                    ConvenienceMethods.getShortStringFrom(errorCols, 5));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onStatisticComputation(final DataRow row) {
            for (Integer i : getMostFrequentColumns()) {
                DataCell c = row.getCell(i);
                if (!c.isMissing()) {
                    LinkedHashMap<DataCell, Integer> map = getCurrentMostFrequent().get(i);
                    if (map == null) {
                        map = new LinkedHashMap<DataCell, Integer>();
                        getCurrentMostFrequent().put(i, map);
                    }
                    Integer integer = map.get(c);
                    final int toSet = integer == null ? 1 : integer + 1;
                    map.put(c, toSet);
                }
            }
        }

        /**
         * As the given map is a linked hash map we can be sure that the returned item is always the first most frequent
         * item.
         *
         * @param map map to use
         * @return the first mostly frequent used item
         */
        private DataCell findMostFrequent(final LinkedHashMap<DataCell, Integer> map) {
            int currentMax = 0;
            DataCell toReturn = null;
            for (Entry<DataCell, Integer> a : map.entrySet()) {
                if (currentMax < a.getValue()) {
                    currentMax = a.getValue();
                    toReturn = a.getKey();
                }
            }
            if (toReturn == null) {
                throw new IllegalArgumentException("Coding issue, as there must be datacell in the map");
            }
            return toReturn;
        }

        /**
         * Hacky stuff because Statistics3Table does the computation in the Constructor...
         */
        /**
         * @return the columns to get the most frequent value from
         */
        protected abstract Iterable<Integer> getMostFrequentColumns();

        /**
         * @param col The column index
         * @return The most frequent cell.
         */
        public DataCell getMostFrequentCell(final int col) {
            DataCell toReturn = getActualMostFrequent().get(col);
            if (toReturn == null) {
                return DataType.getMissingCell();
            }
            return toReturn;
        }

        /**
         * @return the currentMostFrequent
         */
        private Map<Integer, LinkedHashMap<DataCell, Integer>> getCurrentMostFrequent() {
            if (m_currentMostFrequent == null) {
                m_currentMostFrequent = new HashMap<Integer, LinkedHashMap<DataCell, Integer>>();
            }
            return m_currentMostFrequent;
        }

        /**
         * @return the actualMostFrequent
         */
        private Map<Integer, DataCell> getActualMostFrequent() {
            if (m_actualMostFrequent == null) {
                m_actualMostFrequent = new HashMap<Integer, DataCell>();
            }
            return m_actualMostFrequent;
        }
    }
}
