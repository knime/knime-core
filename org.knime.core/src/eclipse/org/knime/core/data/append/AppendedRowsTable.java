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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
package org.knime.core.data.append;

import java.util.EnumSet;
import java.util.LinkedHashMap;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataColumnSpecCreator.MergeOptions;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.append.AppendedRowsIterator.PairSupplier;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;


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
 * @since 3.1
 */
public class AppendedRowsTable implements DataTable {

    /**
     * The options {@link DataColumnSpecCreator#merge(DataColumnSpec, java.util.Set)} is called with.
     */
    private static final EnumSet<MergeOptions> MERGE_OPTIONS =
        EnumSet.of(MergeOptions.ALLOW_VARYING_TYPES, MergeOptions.ALLOW_VARYING_ELEMENT_NAMES);

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(AppendedRowsTable.class);

    /** How to deal with duplicate row ids. */
    public enum DuplicatePolicy {
        /** Skip duplicate occurrence. */
        Skip(true),
        /** Append a suffix to unify IDs. */
        AppendSuffix(true),
        /** Fail . */
        Fail(false),
        /**
         * Prevent duplicates by generating completely new IDs.
         * @since 5.1
         * */
        CreateNew(false);

        private boolean m_needsDuplicateMap;

        DuplicatePolicy(final boolean needsDuplicateMap) {
            m_needsDuplicateMap = needsDuplicateMap;
        }

        boolean needsDuplicateMap() {
            return m_needsDuplicateMap;
        }
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
     * Suffix to append or null to skip subsequent occurrences of a row key.
     */
    private final String m_suffix;

    /** Duplicate unifying policy. */
    private final DuplicatePolicy m_duplPolicy;

    /**
     * If true, the duplicate map is also created for {@link DuplicatePolicy DuplicatePolicies} that don't need a
     * duplicate map.
     */
    private boolean m_fillDuplicateMap;

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
        if (duplPolicy == DuplicatePolicy.AppendSuffix) {
            CheckUtils.checkArgument(StringUtils.isNotEmpty(suffix), "Suffix must not be an empty string.");
        }
        m_duplPolicy = duplPolicy;
        m_suffix = suffix;
        m_tables = new DataTable[tables.length];
        System.arraycopy(tables, 0, m_tables, 0, m_tables.length);
    }

    /**
     * Note: The duplicate map can cause memory issues for sufficiently large tables.
     *
     * @param fillDuplicateMap whether to fill the duplicate map even if it isn't needed by the {@link DuplicatePolicy}
     * @since 5.1
     */
    public void setFillDuplicateMap(final boolean fillDuplicateMap) {
        m_fillDuplicateMap = fillDuplicateMap;
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
        PairSupplier[] iteratorSuppliers = new PairSupplier[m_tables.length];
        for (int i = 0; i < iteratorSuppliers.length; i++) {
            iteratorSuppliers[i] = new PairSupplier(new Pair<RowIterator, DataTableSpec>(
                    m_tables[i].iterator(), m_tables[i].getDataTableSpec()));
        }
        return new AppendedRowsIterator(iteratorSuppliers, m_duplPolicy, m_suffix, m_spec, exec, totalRowCount,
            m_fillDuplicateMap);
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
        final LinkedHashMap<String, DataColumnSpecCreator> colCreatorSet = new LinkedHashMap<>();
        for (DataTableSpec tableSpec : tableSpecs) {
            for (DataColumnSpec colSpec : tableSpec) {
                final String colName = colSpec.getName();
                final DataColumnSpecCreator colCreator = colCreatorSet.get(colName);
                if (colCreator == null) {
                    // new column -> initialize the creator
                    colCreatorSet.put(colName, new DataColumnSpecCreator(colSpec));
                } else {
                    // known column -> merge the specs
                    mergeSpec(colSpec, colCreator);
                }
            }
        }
        return new DataTableSpec(
            colCreatorSet.values().stream().map(DataColumnSpecCreator::createSpec).toArray(DataColumnSpec[]::new));
    }

    private static void mergeSpec(final DataColumnSpec colSpec, final DataColumnSpecCreator colCreator) {
        final DataType oldType = colCreator.getType();
        colCreator.merge(colSpec, MERGE_OPTIONS);
        // that shouldn't happen though, eh: shit happens.
        DataType type = colCreator.getType();
        if (!oldType.equals(type)) {
            LOGGER.info("Confusing data types for column \""
                    + colSpec.getName() + "\": " + oldType.toString()
                    + " vs. " + colSpec.getType().toString() + "\n"
                    + "Using common base type " + type.toString());
        }
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
}
