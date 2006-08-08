/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.preproc.missingval;

import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedHashSet;
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
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * DataTable that replaces missing values according to ColSetting objects.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class MissingValueHandlingTable implements DataTable {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(MissingValueHandlingTable.class);

    /** We need min/max etc. */
    private final DataTable m_table;

    /** Settings for each column. */
    private final ColSetting[] m_settings;

    /** The data table spec for this table. */
    private final DataTableSpec m_spec;

    /**
     * Creates new table.
     * 
     * @param table table with missing values
     * @param colSetting the settings, may come in arbitrary order and also does
     *            not need to have settings for all columns. ColSettings for
     *            meta columns should only be in there once.
     */
    private MissingValueHandlingTable(final DataTable table,
            final ColSetting[] colSetting) {
        ColSetting[] sets = null;
        try {
            sets = getColSetting(table.getDataTableSpec(), colSetting, false);
        } catch (InvalidSettingsException ise) {
            LOGGER.error("Unable to generate settings", ise);
        }
        m_settings = sets;
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
     * @see org.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * @see org.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {
        return new MissingValueHandlingTableIterator(this);
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
    private StatisticsTable getStatisticsTable() {
        return (StatisticsTable)m_table;
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
    ColSetting getColSetting(final int column) {
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
            final ColSetting[] sets) throws InvalidSettingsException {
        ColSetting[] realSettings = getColSetting(spec, sets, true);
        return createTableSpecPrivate(spec, realSettings);

    }

    /* private helper that assumes the ColSetting to have the right format. */
    private static DataTableSpec createTableSpecPrivate(
            final DataTableSpec spec, final ColSetting[] sets) {
        assert (spec.getNumColumns() == sets.length);
        DataColumnSpec[] newSpecs = new DataColumnSpec[sets.length];
        for (int i = 0; i < sets.length; i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);
            DataColumnSpec newSpec = colSpec;
            if (sets[i].getMethod() == ColSetting.METHOD_FIX_VAL) {
                DataColumnDomain dom = colSpec.getDomain();
                Comparator<DataCell> comp = colSpec.getType().getComparator();
                DataCell fixCell = sets[i].getFixCell();
                boolean changed = false;
                DataCell l = dom.getLowerBound();
                if (l != null && (comp.compare(fixCell, l) < 0)) {
                    changed = true;
                    l = fixCell;
                }
                DataCell u = dom.getUpperBound();
                if (u != null && (comp.compare(fixCell, u) > 0)) {
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
    private static ColSetting[] getColSetting(final DataTableSpec spec,
            final ColSetting[] sets, final boolean throwExeception)
            throws InvalidSettingsException {
        ColSetting[] results = new ColSetting[spec.getNumColumns()];

        // fill up the default (i.e. meta-settings for String, Double, Int,
        // and Other columns) - if they are available
        Hashtable<Integer, ColSetting> hash = new Hashtable<Integer, ColSetting>();
        // the default one
        ColSetting untouched = new ColSetting(ColSetting.TYPE_UNKNOWN);
        untouched.setMethod(ColSetting.METHOD_NO_HANDLING);
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
            Object hashKey;
            if (type.isASuperTypeOf(StringCell.TYPE)) {
                hashKey = new Integer(ColSetting.TYPE_STRING);
            } else if (type.isASuperTypeOf(DoubleCell.TYPE)) {
                hashKey = new Integer(ColSetting.TYPE_DOUBLE);
            } else if (type.isASuperTypeOf(IntCell.TYPE)) {
                hashKey = new Integer(ColSetting.TYPE_INT);
            } else {
                hashKey = new Integer(ColSetting.TYPE_UNKNOWN);
            }
            ColSetting setting = hash.get(hashKey);
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
            String name = sets[i].getName();
            int type = sets[i].getType();
            final int index = spec.findColumnIndex(name);
            if (index < 0) {
                String error = "Unable to do missing value handling for column"
                        + " '" + name + "', no such column in table";
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
            if (type == ColSetting.TYPE_INT
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
            if (type == ColSetting.TYPE_DOUBLE
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
            if (type == ColSetting.TYPE_STRING
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
        return results;
    } // getColSetting(DataTableSpec, ColSetting[])

    /**
     * Does missing value handling to the argument table given the col settings
     * in an array and also reports progress.
     * 
     * @param table the table to do missing value handling on
     * @param colSetting the settings
     * @param exec for progress/cancel
     * @return a cache table, cleaned up
     * @throws CanceledExecutionException if canceled
     */
    public static DataTable createMissingValueHandlingTable(
            final DataTable table, final ColSetting[] colSetting,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        boolean needStatistics = false;
        for (int i = 0; i < colSetting.length && !needStatistics; i++) {
            ColSetting c = colSetting[i];
            switch (c.getMethod()) {
            case ColSetting.METHOD_MAX:
            case ColSetting.METHOD_MIN:
            case ColSetting.METHOD_MEAN:
                needStatistics = true;
                break;
            default:
            }
        }
        DataTable t;
        ExecutionMonitor e;
        if (needStatistics && !(table instanceof StatisticsTable)) {
            // for creating statistics table
            ExecutionMonitor subExec = exec.createSubProgress(0.5);
            t = new StatisticsTable(table, subExec);
            // for the iterator
            e = exec.createSubProgress(0.5);
        } else {
            t = table;
            e = exec;
        }
        MissingValueHandlingTable mvht = new MissingValueHandlingTable(t,
                colSetting);
        DataContainer container = new DataContainer(mvht.getDataTableSpec());
        e.setMessage("Adding rows...");
        int count = 0;
        try {
            MissingValueHandlingTableIterator it = new MissingValueHandlingTableIterator(
                    mvht, e);
            while (it.hasNext()) {
                DataRow next;
                next = it.next();
                e.setMessage("Adding row " + (count + 1) + " (\""
                        + next.getKey() + "\")");
                container.addRowToTable(next);
                count++;
            }
        } catch (MissingValueHandlingTableIterator.RuntimeCanceledExecutionException rcee) {
            throw rcee.getCause();
        } finally {
            container.close();
        }
        return container.getTable();
    }
}
