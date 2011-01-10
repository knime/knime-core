/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
package org.knime.base.data.append.row;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowIterator;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;


/**
 * {@link DataTable} implementation that concatenates a set of {@link DataTable}
 * objects. All tables should have the same columns (identified by their column
 * name). Missing columns in tables are tried to be filled with missing cells
 * (where it is possible - so far for string, double, int). Non matching column
 * types are adjusted such that they match (if not possible, the column's type
 * is {@link DataCell}).
 *
 * <p>
 * The order of colums of this table is determined by the order of the top table
 * (the first array argument in the constructor)
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedRowsTable implements DataTable {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(AppendedRowsTable.class);

    /** How to deal with duplicate row ids. */
    public enum DuplicatePolicy {
        /** Skip duplicate occurrence. */
        Skip,
        /** Append a suffix to unify IDs. */
        AppendSuffix,
        /** Fail . */
        Fail
    }

    /**
     * Tables that make up this table. The first entry contains the the top part
     * and the second, third and subsequent table the rest. The order of columns
     * is defined by the top table.
     */
    private final DataTable[] m_tables;

    /**
     * The DataTableSpec for this table (defined mainly be the order of the top
     * table (apart from column that only occur in other tables).
     */
    private final DataTableSpec m_spec;

    /**
     * Suffix to append or null to skip subsequent occurences of a row key.
     */
    private final String m_suffix;

    /** Duplicate unifying policy. */
    private final DuplicatePolicy m_duplPolicy;

    /**
     * Concatenates a set of tables. Duplicate entries are skipped.
     *
     * @param tables all tables to be appended
     * @throws NullPointerException if argument is <code>null</code> or
     *             contains <code>null</code> elements.
     */
    public AppendedRowsTable(final DataTable... tables) {
        this(null, tables);
    } // AppendedRowsTable(DataTable[])

    /**
     * Concatenates a set of tables. Duplicate keys may be skipped
     * (<code>suffix</code> argument is <code>null</code>) or may be avoided
     * by appending a fixed suffix to any subsequent occurrence of a duplicate.
     *
     * @param tables all tables to be appended
     * @param suffix suffix to append to duplicate keys or <code>null</code>
     *            to skip them
     * @throws NullPointerException if argument is <code>null</code> or
     *             contains <code>null</code> elements
     * @throws IllegalArgumentException if suffix is empty (<code>null</code>
     *             is ok)
     */
    public AppendedRowsTable(final String suffix, final DataTable... tables) {
        this(suffix == null ? DuplicatePolicy.Skip
                : DuplicatePolicy.AppendSuffix, suffix, tables);
    }

    /**
     * Concatenates a set of tables. Duplicates are handled according to the
     * policy argument.
     *
     * @param duplPolicy How to deal with duplicate keys.
     * @param tables all tables to be appended
     * @param suffix suffix to append to duplicate keys (must not be null
     *         if policy is {@link DuplicatePolicy#AppendSuffix})
     */
    public AppendedRowsTable(final DuplicatePolicy duplPolicy,
            final String suffix, final DataTable... tables) {
        m_spec = generateDataTableSpec(tables);
        switch (duplPolicy) {
        case AppendSuffix:
            if (suffix == null || suffix.equals("")) {
                throw new IllegalArgumentException(
                "Suffix must not be an empty string.");
            }
            break;
        default:
        }
        m_duplPolicy = duplPolicy;
        m_suffix = suffix;
        m_tables = new DataTable[tables.length];
        System.arraycopy(tables, 0, m_tables, 0, m_tables.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RowIterator iterator() {
        return iterator(null, -1);
    }

    /**
     * Get an iterator whose {@link RowIterator#next()} method may throw an
     * exception if the execution is canceled. In particular if you try to
     * concatenate a table to itself and do not allow duplicates, the
     * {@link RowIterator#next()} method runs <b>very</b> long (scanning the
     * entire table to just figure out that there are only duplicates).
     *
     * @param exec the execution monitor for cancel / progress
     * @param totalRowCount the total number rows or negative if unknown
     * @return an iterator which reacts on cancel events
     * @see AppendedRowsIterator.RuntimeCanceledExecutionException
     */
    public AppendedRowsIterator iterator(final ExecutionMonitor exec,
            final int totalRowCount) {
        return new AppendedRowsIterator(this, exec, totalRowCount);
    }

    /** @return the tables */
    DataTable[] getTables() {
        return m_tables;
    }

    /** @return the suffix */
    String getSuffix() {
        return m_suffix;
    }

    /** @return the duplPolicy */
    DuplicatePolicy getDuplPolicy() {
        return m_duplPolicy;
    }

    /**
     * Factory method that determines the final {@link DataTableSpec} given the
     * tables.
     *
     * @param tableSpecs the table specs as in the constructor
     * @return the outcoming {qlink DataTableSpec}
     * @see #AppendedRowsTable(DataTable[])
     */
    public static final DataTableSpec generateDataTableSpec(
            final DataTableSpec... tableSpecs) {
        // memorize the first column spec in the argument array for
        // each column name, we use it later on to initialize the column
        // spec creator.
        LinkedHashMap<String, DataColumnSpec> columnSet =
            new LinkedHashMap<String, DataColumnSpec>();
        LinkedHashMap<String, DataType> typeSet =
            new LinkedHashMap<String, DataType>();
        LinkedHashMap<String, DataColumnDomain> domainSet =
            new LinkedHashMap<String, DataColumnDomain>();

        // create final data table spec
        for (int i = 0; i < tableSpecs.length; i++) {
            DataTableSpec cur = tableSpecs[i];
            for (int c = 0; c < cur.getNumColumns(); c++) {
                DataColumnSpec colSpec = cur.getColumnSpec(c);
                String colName = colSpec.getName();
                // set the spec for this column if not yet done
                if (!columnSet.containsKey(colName)) {
                    columnSet.put(colName, colSpec);
                }
                DataType colType = colSpec.getType();
                DataColumnDomain colDomain = colSpec.getDomain();

                // duplicates are welcome - but only if they match the type
                if (typeSet.containsKey(colName)) {
                    DataType oldType = typeSet.get(colName);
                    DataColumnDomain oldDomain = domainSet.get(colName);
                    // the base type they share
                    DataType type = DataType.getCommonSuperType(oldType,
                            colType);
                    assert type.isASuperTypeOf(oldType);
                    assert type.isASuperTypeOf(colType);
                    // that shouldn't happen though, eh: shit happens.
                    if (!oldType.equals(type)) {
                        LOGGER.info("Confusing data types for column \""
                                + colName + "\": " + oldType.toString()
                                + " vs. " + colType.toString() + "\n"
                                + "Using common base type " + type.toString());
                        // that must not change the order.
                        typeSet.put(colName, type);
                    }
                    DataColumnDomain newDomain = merge(oldDomain, colDomain,
                            type.getComparator());
                    domainSet.put(colName, newDomain);
                } else { // doesn't contain the key
                    typeSet.put(colName, colType);
                    domainSet.put(colName, colDomain);
                }
            } // for all columns in the current table spec
        } // for all tables

        DataColumnSpec[] colSpecs = new DataColumnSpec[typeSet.size()];
        int i = 0;
        for (Map.Entry<String, DataType> entry : typeSet.entrySet()) {
            String name = entry.getKey();
            DataType type = entry.getValue();
            // domain is null, if we did not remember it (e.g. "keepDomain" was
            // false)
            DataColumnDomain domain = domainSet.get(name);
            DataColumnSpec initSpec = columnSet.get(name);
            DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
                    initSpec);
            specCreator.setDomain(domain);
            specCreator.setType(type);
            colSpecs[i++] = specCreator.createSpec();
        }
        return new DataTableSpec(colSpecs);
    }

    /*
     * Convenience method that extracts the table specs and calls
     * generateDataTableSpec(DataTableSpec[]).
     */
    private static final DataTableSpec generateDataTableSpec(
            final DataTable... tables) {
        DataTableSpec[] tableSpecs = new DataTableSpec[tables.length];
        for (int i = 0; i < tableSpecs.length; i++) {
            tableSpecs[i] = tables[i].getDataTableSpec();
        }
        return generateDataTableSpec(tableSpecs);
    }

    /*
     * Merges two domains of the same column, i.e. emerging from different
     * tables, min max will be updated (if possible) and the possible value set.
     */
    private static final DataColumnDomain merge(final DataColumnDomain d1,
            final DataColumnDomain d2, final DataValueComparator comp) {
        final DataCell d1Min = d1.getLowerBound();
        final DataCell d1Max = d1.getUpperBound();
        final DataCell d2Min = d2.getLowerBound();
        final DataCell d2Max = d2.getUpperBound();
        final Set<DataCell> d1Poss = d1.getValues();
        final Set<DataCell> d2Poss = d2.getValues();
        final DataCell newMin;
        if (d1Min == null || d2Min == null) {
            newMin = null;
        } else {
            newMin = comp.compare(d1Min, d2Min) < 0 ? d1Min : d2Min;
        }
        final DataCell newMax;
        if (d1Max == null || d2Max == null) {
            newMax = null;
        } else {
            newMax = comp.compare(d1Max, d2Max) < 0 ? d1Max : d2Max;
        }
        final Set<DataCell> newPoss;
        if (d1Poss == null || d2Poss == null) {
            newPoss = null;
        } else {
            newPoss = new LinkedHashSet<DataCell>(d1Poss);
            newPoss.addAll(d2Poss);
        }
        return new DataColumnDomainCreator(newPoss, newMin, newMax)
                .createDomain();
    }
}
