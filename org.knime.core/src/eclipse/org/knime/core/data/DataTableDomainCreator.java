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

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

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
public class DataTableDomainCreator implements IDataTableDomainCreator {
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

    private final Set<DataCell>[] m_possVals;

    private final DataValueComparator[] m_comparators;

    /**
     * A new instance that recreates the domain of certains columns. Which columns are processed and if the domains
     * should be initialized with the domain from the incoming table can be controlled by the two
     * {@link DomainCreatorColumnSelection} arguments.
     *
     * @param inputSpec the spec of the input/original table
     * @param domainValuesColumnSelection defines columns to recreate or drop domain values
     * @param domainMinMaxColumnSelection defines columns to recreate or drop min, max values of the domain
     */
    @SuppressWarnings("unchecked")
    public DataTableDomainCreator(final DataTableSpec inputSpec,
        final DomainCreatorColumnSelection domainValuesColumnSelection,
        final DomainCreatorColumnSelection domainMinMaxColumnSelection) {
        m_inputSpec = inputSpec;
        m_mins = new DataCell[inputSpec.getNumColumns()];
        m_minsMissing = new boolean[inputSpec.getNumColumns()];
        m_maxs = new DataCell[inputSpec.getNumColumns()];
        m_maxsMissing = new boolean[inputSpec.getNumColumns()];
        m_possVals = new LinkedHashSet[inputSpec.getNumColumns()];
        m_comparators = new DataValueComparator[inputSpec.getNumColumns()];
        m_domainValuesColumnSelection = domainValuesColumnSelection;
        m_domainMinMaxColumnSelection = domainMinMaxColumnSelection;
        m_maxPossibleValues = DataContainerSettings.getDefault().getMaxDomainValues();

        int i = 0;
        for (DataColumnSpec colSpec : inputSpec) {
            if (m_domainValuesColumnSelection.createDomain(colSpec)) {
                Set<DataCell> values = colSpec.getDomain().getValues();

                if (!m_domainValuesColumnSelection.dropDomain(colSpec) && (values != null)) {
                    m_possVals[i] = new LinkedHashSet<>(values);
                } else {
                    // since we're doing a lot of checks for whether a DataCell is contained in the set of possible
                    // values, we should reduce the amount of expected hash collisions by creating a sufficiently
                    // large hash set with a low load factor.
                    m_possVals[i] = new LinkedHashSet<>(2 * m_maxPossibleValues, 1 / 3f);
                }
            }

            if (m_domainMinMaxColumnSelection.createDomain(colSpec)) {
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
            i++;
        }
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
        this(inputSpec, new DomainCreatorColumnSelection() {
            @Override
            public boolean createDomain(final DataColumnSpec colSpec) {
                return colSpec.getType().isCompatible(NominalValue.class);
            }

            @Override
            public boolean dropDomain(final DataColumnSpec colSpec) {
                return !initDomain;
            }
        }, new DomainCreatorColumnSelection() {
            @Override
            public boolean createDomain(final DataColumnSpec colSpec) {
                return colSpec.getType().isCompatible(BoundedValue.class);
            }

            @Override
            public boolean dropDomain(final DataColumnSpec colSpec) {
                return !initDomain;
            }
        });
    }

    @Override
    public void setMaxPossibleValues(final int maxValues) {
        if (maxValues < 0) {
            throw new IllegalArgumentException("Maximum possible values must be >= 0 but is " + maxValues);
        }
        m_maxPossibleValues = maxValues;
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
            if (m_possVals[col].add(cell) && (m_possVals[col].size() > m_maxPossibleValues)) {
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

    private boolean isNaN(final DataCell cell) {
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
    @Override
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
                domainCreator.setValues(m_possVals[i]);
            }
            if (m_domainMinMaxColumnSelection.createDomain(original)) {
                DataCell min = m_mins[i] != null && !m_mins[i].isMissing() ? m_mins[i] : null;
                DataCell max = m_mins[i] != null && !m_maxs[i].isMissing() ? m_maxs[i] : null;
                domainCreator.setLowerBound(min);
                domainCreator.setUpperBound(max);
            }

            DataColumnSpecCreator specCreator = new DataColumnSpecCreator(original);
            specCreator.setDomain(domainCreator.createDomain());
            outColSpecs[i] = specCreator.createSpec();
        }

        return new DataTableSpec(m_inputSpec.getName(), outColSpecs);
    }

    @Override
    public DataTableSpec getInputSpec() {
        return m_inputSpec;
    }

    @Override
    public void updateDomain(final DataRow row) {
        assert row.getNumCells() == m_inputSpec.getNumColumns() : "Unequal number of columns in spec and row: "
            + m_inputSpec.getNumColumns() + " vs. " + row.getNumCells();

        int i = 0;
        for (DataCell c : row) {
            updateMinMax(i, c, m_mins, m_maxs, m_comparators);
            i++;
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

    @Override
    public int getMaxPossibleVals() {
        return m_maxPossibleValues;
    }

    @Override
    public void merge(final IDataTableDomainCreator dataTableDomainCreator) {
        CheckUtils.checkArgument(getInputSpec().equals(dataTableDomainCreator.getInputSpec()),
            "Cannot merge data table domain creators based on different table specs");
        CheckUtils.checkArgument(getMaxPossibleVals() == dataTableDomainCreator.getMaxPossibleVals(),
            "Cannot merge data table domain creators using a different number of unique values");
        final DataColumnSpec[] columnSpecs = dataTableDomainCreator.createSpec().getColumnSpecs();
        for (int i = 0; i < m_inputSpec.getNumColumns(); i++) {
            final DataColumnDomain domain = columnSpecs[i].getDomain();
            if (m_possVals[i] != null && domain.getValues() != null) {
                for (final DataCell c : domain.getValues()) {
                    if (m_possVals[i].add(c) && (m_possVals[i].size() > m_maxPossibleValues)) {
                        m_possVals[i] = null;
                        break;
                    }
                }
            } else {
                m_possVals[i] = null;
            }
            final Comparator<DataCell> comparator = m_comparators[i];
            final DataCell otherMin = domain.getLowerBound();
            if (otherMin != null) {
                updateMin(i, m_mins, otherMin, comparator);
            }
            final DataCell otherMax = domain.getUpperBound();
            if (otherMax != null) {
                updateMax(i, m_maxs, otherMax, comparator);
            }
        }
    }
}