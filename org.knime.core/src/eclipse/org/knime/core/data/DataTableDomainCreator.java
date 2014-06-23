/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * Created on 20.03.2014 by hofer
 */
package org.knime.core.data;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.container.BlobWrapperDataCell;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

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

    private final DataCell[] m_maxs;

    private final LinkedHashSet<DataCell>[] m_possVals;

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
        m_maxs = new DataCell[inputSpec.getNumColumns()];
        m_possVals = new LinkedHashSet[inputSpec.getNumColumns()];
        m_comparators = new DataValueComparator[inputSpec.getNumColumns()];
        m_domainValuesColumnSelection = domainValuesColumnSelection;
        m_domainMinMaxColumnSelection = domainMinMaxColumnSelection;
        m_maxPossibleValues = DataContainer.MAX_POSSIBLE_VALUES;

        int i = 0;
        for (DataColumnSpec colSpec : inputSpec) {
            if (m_domainValuesColumnSelection.createDomain(colSpec)) {
                Set<DataCell> values = colSpec.getDomain().getValues();

                if (!m_domainValuesColumnSelection.dropDomain(colSpec) && (values != null)) {
                    m_possVals[i] = new LinkedHashSet<DataCell>(values);
                } else {
                    m_possVals[i] = new LinkedHashSet<DataCell>();
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
                    }
                    m_maxs[i] = colSpec.getDomain().getUpperBound();
                    if (m_maxs[i] == null) { // input spec may not have bounds, but we are asked to compute them
                        m_maxs[i] = DataType.getMissingCell();
                    }
                }
                m_comparators[i] = colSpec.getType().getComparator();
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

    /**
     * Set the maximum number of possible values in the domain of a nominal value columns.
     *
     * @param maxValues the maximal number of values, must be >= 0
     */
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
        if (mins[col] == null || cell.isMissing()) {
            return;
        }
        DataCell value = handleNaN(cell instanceof BlobWrapperDataCell ? ((BlobWrapperDataCell)cell).getCell() : cell);
        if (value.isMissing()) {
            return;
        }

        Comparator<DataCell> comparator = comparators[col];
        if (mins[col].isMissing() || (comparator.compare(value, mins[col]) < 0)) {
            mins[col] = value;
        }
        if (maxs[col].isMissing() || (comparator.compare(value, maxs[col]) > 0)) {
            maxs[col] = value;
        }
    }

    /*
     * Returns
     * - the cell if it is not a DoubleValue
     * - the cell if it is not NaN
     * - a missing cell if it is NaN
     */
    private DataCell handleNaN(final DataCell cell) {
        if (cell.getType().isCompatible(DoubleValue.class)) {
            if (Double.isNaN(((DoubleValue)cell).getDoubleValue())) {
                return DataType.getMissingCell();
            } else {
                return cell;
            }
        } else {
            return cell;
        }
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

    /**
     * Updates the domain values with a single row. Note that the row structure must match the table spec that has been
     * provided to the constructor.
     *
     * @param row a data row
     */
    public void updateDomain(final DataRow row) {
        assert row.getNumCells() == m_inputSpec.getNumColumns() : "Unequal number of columns in spec and row: "
            + m_inputSpec.getNumColumns() + " vs. " + row.getNumCells();

        int i = 0;
        for (DataCell c : row) {
            if (!c.isMissing() && m_possVals[i] != null) {
                if (m_possVals[i].add(c) && (m_possVals[i].size() > m_maxPossibleValues)) {
                    m_possVals[i] = null;
                }
            }
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
     */
    public void updateDomain(final DataTable table, final ExecutionMonitor exec, final int rowCount)
        throws CanceledExecutionException {
        if (!m_inputSpec.equalStructure(table.getDataTableSpec())) {
            throw new IllegalArgumentException("Spec of table to scan does not match spec given in constructor");
        }

        int row = 0;
        for (RowIterator it = table.iterator(); it.hasNext(); row++) {
            if (exec != null) {
                exec.checkCanceled();
                final String message = "Row " + row + "/" + rowCount;
                if (rowCount > 0) {
                    double progress = row / (double)rowCount;
                    exec.setProgress(Math.min(progress, 1.0), message);
                } else {
                    exec.setMessage(message);
                }
            }

            DataRow r = it.next();
            updateDomain(r);
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
        updateDomain(table, exec, table.getRowCount());
    }
}
