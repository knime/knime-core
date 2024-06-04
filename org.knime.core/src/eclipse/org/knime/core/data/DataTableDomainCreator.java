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
 * ---------------------------------------------------------------------
 *
 * Created on 20.03.2014 by hofer
 */
package org.knime.core.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnMetaDataCalculators.MetaDataCalculator;
import org.knime.core.data.container.BlobWrapperDataCell;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.CheckUtils;

/**
 * Create or recreate domain of a data table. The original spec has to be given in the constructor. The possible values
 * and minimum and maximum values are subsequently updated by calling {@link #updateDomain(DataRow)} or
 * {@link #updateDomain(DataTable, ExecutionMonitor, int)}. Finally the resulting spec including updated domains is
 * created by calling {@link #createSpec()}.
 *
 * @author Heiko Hofer
 * @since 2.10
 */
public class DataTableDomainCreator {
    /** Defines columns to recreate or drop domain values. */
    private final DomainCreatorColumnSelection m_domainValuesColumnSelection;

    /** Defines columns to recreate or drop min, max values of the domain. */
    private final DomainCreatorColumnSelection m_domainMinMaxColumnSelection;

    /** Maximal domain values. */
    private int m_maxPossibleValues;

    private final DataTableSpec m_inputSpec;

    private final DataCell[] m_mins;

    private final boolean[] m_minsMissing;

    private final DataCell[] m_maxs;

    private final boolean[] m_maxsMissing;

    private final Map<DataCell, Long>[] m_possVals;

    private final DataValueComparator[] m_comparators;

    private final MetaDataCalculator[] m_metaDataCalculators;

    /** A shortcut to avoid cell iteration in case no updates are needed. Long term we should change it to some
     * index-based data structure so that only cells/columns that are of interest (as per constructor args). */
    private final boolean m_isInEffect;

    /** The batch id. */
    private long m_batchId;

    /**
     * A new instance that recreates the domain of certains columns. Which columns are processed and if the domains
     * should be initialized with the domain from the incoming table can be controlled by the two
     * {@link DomainCreatorColumnSelection} arguments.
     *
     * @param inputSpec the spec of the input/original table
     * @param domainValuesColumnSelection defines columns to recreate or drop domain values
     * @param domainMinMaxColumnSelection defines columns to recreate or drop min, max values of the domain
     */
    public DataTableDomainCreator(final DataTableSpec inputSpec,
        final DomainCreatorColumnSelection domainValuesColumnSelection,
        final DomainCreatorColumnSelection domainMinMaxColumnSelection) {
        this(inputSpec, domainValuesColumnSelection, domainMinMaxColumnSelection,
            DomainCreatorColumnSelection.create(c -> true, c -> true));
    }

    /**
     * A new instance that recreates the domain of certains columns. Which columns are processed and if the domains
     * should be initialized with the domain from the incoming table can be controlled by the two
     * {@link DomainCreatorColumnSelection} arguments.
     *
     * @param inputSpec the spec of the input/original table
     * @param domainValuesColumnSelection defines columns to recreate or drop domain values
     * @param domainMinMaxColumnSelection defines columns to recreate or drop min, max values of the domain
     * @param metaDataColumnSelection defines columns to calculate metaData for
     * @since 4.1
     */
    @SuppressWarnings("unchecked")
    public DataTableDomainCreator(final DataTableSpec inputSpec,
        final DomainCreatorColumnSelection domainValuesColumnSelection,
        final DomainCreatorColumnSelection domainMinMaxColumnSelection,
        final DomainCreatorColumnSelection metaDataColumnSelection) {
        m_inputSpec = inputSpec;
        m_mins = new DataCell[inputSpec.getNumColumns()];
        m_minsMissing = new boolean[inputSpec.getNumColumns()];
        m_maxs = new DataCell[inputSpec.getNumColumns()];
        m_maxsMissing = new boolean[inputSpec.getNumColumns()];
        m_possVals = new LinkedHashMap[inputSpec.getNumColumns()];
        m_comparators = new DataValueComparator[inputSpec.getNumColumns()];
        m_domainValuesColumnSelection = domainValuesColumnSelection;
        m_domainMinMaxColumnSelection = domainMinMaxColumnSelection;
        m_maxPossibleValues = DataContainerSettings.getDefault().getMaxDomainValues();
        m_metaDataCalculators = new MetaDataCalculator[inputSpec.getNumColumns()];

        boolean isInEffect = false;
        int i = 0;
        for (DataColumnSpec colSpec : inputSpec) {
            if (m_domainValuesColumnSelection.createDomain(colSpec)) {
                isInEffect = true;
                Set<DataCell> values = colSpec.getDomain().getValues();

                if (!m_domainValuesColumnSelection.dropDomain(colSpec) && (values != null)) {
                    m_possVals[i] = values.stream()//
                        .collect(Collectors.toMap(//
                            Function.identity(), //
                            v -> m_batchId, //
                            (v1, v2) -> {
                                throw new IllegalStateException();
                            }, //
                            LinkedHashMap::new));
                } else {
                    // since we're doing a lot of checks for whether a DataCell is contained in the set of possible
                    // values, we should reduce the amount of expected hash collisions by creating a sufficiently
                    // large hash set with a low load factor.
                    m_possVals[i] = new LinkedHashMap<>(2 * m_maxPossibleValues, 1 / 3f);
                }
            }

            if (m_domainMinMaxColumnSelection.createDomain(colSpec)) {
                isInEffect = true;
                if (m_domainMinMaxColumnSelection.dropDomain(colSpec)) {
                    m_mins[i] = DataType.getMissingCell();
                    m_maxs[i] = DataType.getMissingCell();
                } else {
                    m_mins[i] = colSpec.getDomain().getLowerBound();
                    if (m_mins[i] == null) { // input spec may not have bounds, but we are asked to compute them
                        m_mins[i] = DataType.getMissingCell();
                        m_minsMissing[i] = true;
                    }
                    m_maxs[i] = colSpec.getDomain().getUpperBound();
                    if (m_maxs[i] == null) { // input spec may not have bounds, but we are asked to compute them
                        m_maxs[i] = DataType.getMissingCell();
                    }
                }
                m_comparators[i] = colSpec.getType().getComparator();
            }
            if (m_mins[i] != null) {
                m_minsMissing[i] = m_mins[i].isMissing();
            }
            if (m_maxs[i] != null) {
                m_maxsMissing[i] = m_maxs[i].isMissing();
            }

            final boolean isCreateMetaData = metaDataColumnSelection.createDomain(colSpec);
            m_metaDataCalculators[i] = DataColumnMetaDataCalculators.createCalculator(colSpec,
                metaDataColumnSelection.dropDomain(colSpec), isCreateMetaData);
            isInEffect = isInEffect || isCreateMetaData;
            i++;
        }
        m_isInEffect = isInEffect;
    }

    /**
     * A new instance that recreates the domain of all columns. Domain values are recreated for all {@link NominalValue}
     * columns whereas min/max values are recreated for all {@link BoundedValue} columns.
     *
     * @param inputSpec the spec of the input/original table
     * @param initDomain <code>true</code> if the domain values should be initialized with the values from the input
     *            spec, <code>false</code> if the domain should be initially empty
     */
    public DataTableDomainCreator(final DataTableSpec inputSpec, final boolean initDomain) {
        this(inputSpec,
            DomainCreatorColumnSelection.create(c -> !initDomain, c -> c.getType().isCompatible(NominalValue.class)),
            DomainCreatorColumnSelection.create(c -> !initDomain, c -> c.getType().isCompatible(BoundedValue.class)),
            DomainCreatorColumnSelection.create(c -> !initDomain, c -> true));
    }

    /**
     * Creates a copy of the given {@link DataTableDomainCreator instance}. Note that this is not a deep-copy, i.e.,
     * none of the read-only members are copied.
     *
     * @param toCopy the instance to be copied
     * @since 4.0
     */
    @SuppressWarnings("unchecked")
    public DataTableDomainCreator(final DataTableDomainCreator toCopy) {
        m_domainValuesColumnSelection = toCopy.m_domainValuesColumnSelection;
        m_domainMinMaxColumnSelection = toCopy.m_domainMinMaxColumnSelection;
        m_maxPossibleValues = toCopy.m_maxPossibleValues;
        m_inputSpec = toCopy.m_inputSpec;
        m_mins = toCopy.m_mins.clone();
        m_minsMissing = toCopy.m_minsMissing.clone();
        m_maxs = toCopy.m_maxs.clone();
        m_maxsMissing = toCopy.m_maxsMissing.clone();
        m_possVals = new LinkedHashMap[toCopy.m_possVals.length];
        for (int i = 0; i < m_possVals.length; i++) {
            Map<DataCell, Long> copyMap = toCopy.m_possVals[i];
            m_possVals[i] = copyMap == null ? null : new LinkedHashMap<>(toCopy.m_possVals[i]);
        }
        m_comparators = toCopy.m_comparators.clone();
        m_batchId = toCopy.m_batchId;
        m_metaDataCalculators = Arrays.stream(toCopy.m_metaDataCalculators).map(DataColumnMetaDataCalculators::copy)
            .toArray(MetaDataCalculator[]::new);
        m_isInEffect = toCopy.m_isInEffect;
    }

    /**
     * Set the maximum number of possible values in the domain of a nominal value columns.
     *
     * @param maxValues the maximal number of values, must be &gt;= 0
     */
    public void setMaxPossibleValues(final int maxValues) {
        if (maxValues < 0) {
            throw new IllegalArgumentException("Maximum possible values must be >= 0 but is " + maxValues);
        }
        m_maxPossibleValues = maxValues;
    }

    /**
     * Returns the maximum number of possible unique values.
     *
     * @return the maximum number of possible unique values
     * @since 4.0
     */
    public int getMaxPossibleValues() {
        return m_maxPossibleValues;
    }

    /**
     * Sets the batch ID, ensuring that after merging various {@link DataTableDomainCreator} instances the ordering of
     * the domain values coincides with their occurrence in the input table.
     *
     * @param id the batch index
     * @since 4.0
     */
    public void setBatchId(final long id) {
        m_batchId = id;
    }

    /**
     * Updates the min and max value for an respective column. This method does nothing if the min and max values don't
     * need to be stored, e.g. the column at hand contains string values.
     *
     * @param col the column of interest
     * @param cell the new value to check
     */
    private void updateMinMax(final int col, final DataCell cell, final DataCell[] mins, final DataCell[] maxs,
        final DataValueComparator[] comparators) {
        final boolean isMissing = cell.isMissing();

        if (!isMissing && m_possVals[col] != null) {
            if (m_possVals[col].putIfAbsent(cell, m_batchId) == null
                && (m_possVals[col].size() > m_maxPossibleValues)) {
                m_possVals[col] = null;
            }
        }

        if (mins[col] == null || isMissing) {
            return;
        }

        final DataCell unwrapped = (cell instanceof BlobWrapperDataCell) ? ((BlobWrapperDataCell)cell).getCell() : cell;

        if (isNaN(unwrapped)) {
            return;
        }

        Comparator<DataCell> comparator = comparators[col];
        updateMin(col, mins, unwrapped, comparator);
        updateMax(col, maxs, unwrapped, comparator);
    }

    private void updateMin(final int col, final DataCell[] mins, final DataCell cell,
        final Comparator<DataCell> comparator) {
        if (m_minsMissing[col] || (comparator.compare(cell, mins[col]) < 0)) {
            mins[col] = cell;
            m_minsMissing[col] = false;
        }
    }

    private void updateMax(final int col, final DataCell[] maxs, final DataCell cell,
        final Comparator<DataCell> comparator) {
        if (maxs[col].isMissing() || (comparator.compare(cell, maxs[col]) > 0)) {
            maxs[col] = cell;
            m_maxsMissing[col] = false;
        }
    }

    private static boolean isNaN(final DataCell cell) {
        return cell instanceof DoubleValue && Double.isNaN(((DoubleValue)cell).getDoubleValue());
    }

    /**
     * Creates an updated version of the input spec. The domains of all configured columns are set according to the data
     * that has been processed by {@link #updateDomain(DataRow)} or
     * {@link #updateDomain(DataTable, ExecutionMonitor, int)} so far. You may call this method multiple times even
     * while you are still presenting more data.
     *
     * @return an updated table spec
     */
    public DataTableSpec createSpec() {
        DataColumnSpec[] outColSpecs = new DataColumnSpec[m_inputSpec.getNumColumns()];
        for (int i = 0; i < outColSpecs.length; i++) {
            DataColumnSpec original = m_inputSpec.getColumnSpec(i);
            DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(original.getDomain());

            if (m_domainValuesColumnSelection.dropDomain(original)) {
                domainCreator.setValues(null);
            }
            if (m_domainMinMaxColumnSelection.dropDomain(original)) {
                domainCreator.setLowerBound(null);
                domainCreator.setUpperBound(null);
            }

            if (m_domainValuesColumnSelection.createDomain(original)) {
                domainCreator.setValues(getSortedValues(i));
            }
            if (m_domainMinMaxColumnSelection.createDomain(original)) {
                DataCell min = m_mins[i] != null && !m_mins[i].isMissing() ? m_mins[i] : null;
                DataCell max = m_mins[i] != null && !m_maxs[i].isMissing() ? m_maxs[i] : null;
                domainCreator.setLowerBound(min);
                domainCreator.setUpperBound(max);
            }

            DataColumnSpecCreator specCreator = new DataColumnSpecCreator(original);
            specCreator.setDomain(domainCreator.createDomain());
            // existing meta data is overwritten because the respective creator was initialized
            // with the existing meta data if the provided configuration required it
            m_metaDataCalculators[i].createMetaData().forEach(m -> specCreator.addMetaData(m, true));
            outColSpecs[i] = specCreator.createSpec();
        }

        return new DataTableSpec(m_inputSpec.getName(), outColSpecs, Collections.emptyMap(),
            m_inputSpec.getColumnNamesColorHandler().orElse(null));
    }

    private Set<DataCell> getSortedValues(final int index) {
        Map<DataCell, Long> vals = m_possVals[index];
        if (vals == null) {
            return null;
        }
        return vals.keySet().stream()//
            .sorted((d1, d2) -> Long.compare(vals.get(d1), vals.get(d2)))//
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Updates the domain values with a single row. Note that the row structure must match the table spec that has been
     * provided to the constructor.
     *
     * @param row a data row
     */
    public void updateDomain(final DataRow row) {
        assert row.getNumCells() == m_inputSpec.getNumColumns() : "Unequal number of columns in spec and row: "
            + m_inputSpec.getNumColumns() + " vs. " + row.getNumCells();

        if (m_isInEffect) {
            for (int i = 0, length = row.getNumCells(); i < length; i++) {
                DataCell c = row.getCell(i);
                updateMinMax(i, c, m_mins, m_maxs, m_comparators);
                m_metaDataCalculators[i].update(c);
            }
        }
    }

    /**
     * Updates the domain values by scanning a whole table. Note that the table's structure must match the table spec
     * that has been provided to the constructor.
     *
     * @param table the table to be processed
     * @param exec an execution monitor to check for cancellation and report progress. Might be <code>null</code> if not
     *            needed.
     * @param rowCount the number of rows in the data table
     * @throws CanceledExecutionException when execution is cancelled
     * @deprecated use {@link #updateDomain(DataTable, ExecutionMonitor, long)} instead which supports more than
     *             {@link Integer#MAX_VALUE} rows
     */
    @Deprecated
    public void updateDomain(final DataTable table, final ExecutionMonitor exec, final int rowCount)
        throws CanceledExecutionException {
        updateDomain(table, exec, (long)rowCount);
    }

    /**
     * Updates the domain values by scanning a whole table. Note that the table's structure must match the table spec
     * that has been provided to the constructor.
     *
     * @param table the table to be processed
     * @param exec an execution monitor to check for cancellation and report progress. Might be <code>null</code> if not
     *            needed.
     * @param rowCount the number of rows in the data table
     * @throws CanceledExecutionException when execution is cancelled
     * @since 3.0
     */
    public void updateDomain(final DataTable table, final ExecutionMonitor exec, final long rowCount)
        throws CanceledExecutionException {
        if (!m_inputSpec.equalStructure(table.getDataTableSpec())) {
            throw new IllegalArgumentException("Spec of table to scan does not match spec given in constructor");
        }

        long row = 0;
        for (final DataRow r : table) {
            final long finalRow = row;
            if (exec != null) {
                exec.checkCanceled();
                if (rowCount > 0) {
                    final double progress = row / (double)rowCount;
                    exec.setProgress(Math.min(progress, 1.0), () -> String.format("Row %,d/%,d", finalRow, rowCount));
                } else {
                    exec.setMessage(() -> String.format("Row %,d (\"%s\")", finalRow, r.getKey()));
                }
            }

            updateDomain(r);
            row++;
        }

        if (exec != null) {
            exec.checkCanceled();
            exec.setProgress(1.0);
        }
    }

    /**
     * Updates the domain values by scanning a whole table. Note that the table's structure must match the table spec
     * that has been provided to the constructor.
     *
     * @param table the table to be processed
     * @param exec an execution monitor to check for cancellation and report progress. Might be <code>null</code> if not
     *            needed.
     * @throws CanceledExecutionException when execution is cancelled
     */
    public void updateDomain(final BufferedDataTable table, final ExecutionMonitor exec)
        throws CanceledExecutionException {
        updateDomain(table, exec, table.size());
    }

    /**
     * Merges two distinct {@link DataTableDomainCreator}.
     *
     * @param dataTableDomainCreator the {@code DataTableDomainCreator} to be merged
     * @since 4.0
     */
    public void merge(final DataTableDomainCreator dataTableDomainCreator) {
        CheckUtils.checkArgument(m_inputSpec.equals(dataTableDomainCreator.m_inputSpec),
            "Cannot merge data table domain creators based on different table specs");
        CheckUtils.checkArgument(m_maxPossibleValues == dataTableDomainCreator.m_maxPossibleValues,
            "Cannot merge data table domain creators using a different number of unique values");
        for (int i = 0; i < m_inputSpec.getNumColumns(); i++) {
            if (m_possVals[i] != null && dataTableDomainCreator.m_possVals[i] != null) {
                for (final Entry<DataCell, Long> entry : dataTableDomainCreator.m_possVals[i].entrySet()) {
                    Map<DataCell, Long> vals = m_possVals[i];
                    if (!vals.containsKey(entry.getKey()) || vals.get(entry.getKey()) > entry.getValue()) {
                        // removing the key fixes AP-12357, since re-inserting items does not change the
                        // running order. Note that we know that all entries with the same batch index are
                        // stored according to their running order
                        vals.remove(entry.getKey());
                        vals.put(entry.getKey(), entry.getValue());
                        if ((m_possVals[i].size() > m_maxPossibleValues)) {
                            m_possVals[i] = null;
                            break;
                        }
                    }
                }
            } else {
                m_possVals[i] = null;
            }
            final Comparator<DataCell> comparator = m_comparators[i];
            final DataCell otherMin = dataTableDomainCreator.m_mins[i];
            if (!dataTableDomainCreator.m_minsMissing[i] && otherMin != null) {
                updateMin(i, m_mins, otherMin, comparator);
            }
            final DataCell otherMax = dataTableDomainCreator.m_maxs[i];
            if (!dataTableDomainCreator.m_maxsMissing[i] && otherMax != null) {
                updateMax(i, m_maxs, otherMax, comparator);
            }
            DataColumnMetaDataCalculators.merge(m_metaDataCalculators[i],
                dataTableDomainCreator.m_metaDataCalculators[i]);
        }
    }

}