/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 * ---------------------------------------------------------------------
 *
 * Created on 20.03.2014 by hofer
 */
package org.knime.core.data;

import java.util.Comparator;
import java.util.LinkedHashSet;

import org.knime.core.data.container.BlobWrapperDataCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;

/**
 * Create or recreate domain of a data table.
 *
 * @author Heiko Hofer
 * @since 2.10
 */
public class DataTableDomainCreator {
    /** Defines columns to recreate or drop domain values. */
    private DomainCreatorColumnSelection m_domainValuesColumnSelection;

    /** Defines columns to recreate or drop min, max values of the domain. */
    private DomainCreatorColumnSelection m_domainMinMaxColumnSelection;

    /** Maximal domain values. */
    private int m_maxValues;

    /**
     * A new instance that recreates the domain of all columns. Domain values are recreated for NominalValue columns
     * whereas min/max values are recreated for BoundValue columns. This behavior can be changed by attaching different
     * DomainCreateColumnSelection objects.
     */
    public DataTableDomainCreator() {
        m_domainValuesColumnSelection = new DomainCreatorColumnSelection() {
            @Override
            public boolean createDomain(final DataColumnSpec colSpec) {
                return colSpec.getType().isCompatible(NominalValue.class);
            }

            @Override
            public boolean dropDomain(final DataColumnSpec colSpec) {
                return true;
            }
        };
        m_domainMinMaxColumnSelection = new DomainCreatorColumnSelection() {
            @Override
            public boolean createDomain(final DataColumnSpec colSpec) {
                return colSpec.getType().isCompatible(BoundedValue.class);
            }

            @Override
            public boolean dropDomain(final DataColumnSpec colSpec) {
                return true;
            }
        };
        m_maxValues = 60;
    }

    /**
     * Set the maximal number of values in the domain of a nominal value columns.
     *
     * @param maxValues the maximal number of values
     */
    public void setMaxValues(final int maxValues) {
        m_maxValues = maxValues;
    }

    /**
     * Change selection of columns processed on configure and execute.
     *
     * @param domainValuesColumnSelection defines columns to recreate or drop domain values
     * @param domainMinMaxColumnSelection defines columns to recreate or drop min, max values of the domain
     */
    public void attachColumnSelection(final DomainCreatorColumnSelection domainValuesColumnSelection,
        final DomainCreatorColumnSelection domainMinMaxColumnSelection) {
        m_domainValuesColumnSelection = domainValuesColumnSelection;
        m_domainMinMaxColumnSelection = domainMinMaxColumnSelection;
    }

    /**
     * Call this in the configure method of a {@link NodeModel} when this class is used in the execute of the
     * {@link NodeModel} to create the {@link DataTableSpec} of the output table.
     *
     * @param inSpec the {@link DataTableSpec} of the intput table
     * @return the {@link DataTableSpec} of the output table
     */
    public DataTableSpec configure(final DataTableSpec inSpec) {
        int colCount = inSpec.getNumColumns();
        DataColumnSpec[] outColSpecs = new DataColumnSpec[colCount];
        for (int i = 0; i < outColSpecs.length; i++) {
            DataColumnSpec colSpec = inSpec.getColumnSpec(i);
            DataColumnSpecCreator specCreator = new DataColumnSpecCreator(colSpec);
            DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(colSpec.getDomain());
            if (m_domainValuesColumnSelection.createDomain(colSpec)
                || m_domainValuesColumnSelection.dropDomain(colSpec)) {
                domainCreator.setValues(null);
            }
            if (m_domainMinMaxColumnSelection.createDomain(colSpec)
                || m_domainMinMaxColumnSelection.dropDomain(colSpec)) {
                domainCreator.setLowerBound(null);
                domainCreator.setUpperBound(null);
            }

            specCreator.setDomain(domainCreator.createDomain());
            outColSpecs[i] = specCreator.createSpec();
        }

        return new DataTableSpec(inSpec.getName(), outColSpecs);
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
     * Recreate spec of the given table. Affected columns can be controlled by calling
     * {@link #attachColumnSelection(DomainCreatorColumnSelection, DomainCreatorColumnSelection)}.
     *
     * @param table the table to be processed
     * @param exec an execution monitor to check for cancellation and report progress. Might be <code>null</code> if not
     *            needed.
     * @param rowCount used for reporting progress. If value is -1 progress is not reported.
     * @return the data driven table spec
     * @throws CanceledExecutionException when execution is cancelled
     */
    public DataTableSpec createSpec(final DataTable table, final ExecutionMonitor exec, final int rowCount)
        throws CanceledExecutionException {
        final DataTableSpec oldSpec = table.getDataTableSpec();
        final int colCount = oldSpec.getNumColumns();

        @SuppressWarnings("unchecked")
        LinkedHashSet<DataCell>[] possVals = new LinkedHashSet[colCount];
        DataCell[] mins = new DataCell[colCount];
        DataCell[] maxs = new DataCell[colCount];
        DataValueComparator[] comparators = new DataValueComparator[colCount];

        for (int i = 0; i < colCount; i++) {
            if (exec != null) {
                exec.checkCanceled();
            }
            DataColumnSpec col = oldSpec.getColumnSpec(i);
            if (m_domainValuesColumnSelection.createDomain(col)) {
                possVals[i] = new LinkedHashSet<DataCell>();
            }
            if (m_domainMinMaxColumnSelection.createDomain(col)) {
                mins[i] = DataType.getMissingCell();
                maxs[i] = DataType.getMissingCell();
                comparators[i] = col.getType().getComparator();
            }
        }

        int row = 0;
        for (RowIterator it = table.iterator(); it.hasNext(); row++) {
            if (exec != null) {
                exec.checkCanceled();
                if (rowCount > -1) {
                    double progress = row / (double)rowCount;
                    exec.setProgress(Math.max(progress, 1.0));
                }
            }
            DataRow r = it.next();
            for (int i = 0; i < colCount; i++) {
                DataCell c = r.getCell(i);
                if (!c.isMissing() && possVals[i] != null) {
                    possVals[i].add(c);
                    if (m_maxValues >= 0 && possVals[i].size() > m_maxValues) {
                        possVals[i] = null;
                    }
                }
                updateMinMax(i, c, mins, maxs, comparators);
            }
        }

        DataColumnSpec[] outColSpecs = new DataColumnSpec[colCount];
        for (int i = 0; i < outColSpecs.length; i++) {
            if (exec != null) {
                exec.checkCanceled();
            }
            DataColumnSpec original = oldSpec.getColumnSpec(i);
            DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(original.getDomain());

            if (m_domainValuesColumnSelection.dropDomain(original)) {
                domainCreator.setValues(null);
            }
            if (m_domainMinMaxColumnSelection.dropDomain(original)) {
                domainCreator.setLowerBound(null);
                domainCreator.setUpperBound(null);
            }

            if (m_domainValuesColumnSelection.createDomain(original)) {
                domainCreator.setValues(possVals[i]);
            }
            if (m_domainMinMaxColumnSelection.createDomain(original)) {
                DataCell min = mins[i] != null && !mins[i].isMissing() ? mins[i] : null;
                DataCell max = mins[i] != null && !maxs[i].isMissing() ? maxs[i] : null;
                domainCreator.setLowerBound(min);
                domainCreator.setUpperBound(max);
            }

            DataColumnSpecCreator specCreator = new DataColumnSpecCreator(original);
            specCreator.setDomain(domainCreator.createDomain());
            outColSpecs[i] = specCreator.createSpec();
        }

        if (exec != null) {
            exec.checkCanceled();
            if (rowCount > -1) {
                exec.setProgress(1.0);
            }
        }
        return new DataTableSpec(oldSpec.getName(), outColSpecs);
    }

    /**
     * Convenient method typically called in the execute method of a {@link NodeModel}. Affected columns can be
     * controlled by calling {@link #attachColumnSelection(DomainCreatorColumnSelection, DomainCreatorColumnSelection)}.
     *
     * @param table the input table
     * @param exec the execution context of the node
     * @return the input table with updated {@link DataTableSpec}
     * @throws CanceledExecutionException when execution is cancelled
     */
    public BufferedDataTable execute(final BufferedDataTable table, final ExecutionContext exec)
        throws CanceledExecutionException {
        DataTableSpec spec = createSpec(table, exec, table.getRowCount());
        return exec.createSpecReplacerTable(table, spec);
    }

}
