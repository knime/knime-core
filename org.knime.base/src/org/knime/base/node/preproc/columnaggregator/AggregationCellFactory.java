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
 */

package org.knime.base.node.preproc.columnaggregator;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.NamedAggregationOperator;
import org.knime.base.data.aggregation.OperatorColumnSettings;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.node.ExecutionMonitor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link CellFactory} implementation that aggregates a number of
 * columns per row using the given {@link AggregationOperator}s.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class AggregationCellFactory implements CellFactory {

    private final AggregationOperator[] m_operators;
    private final String[] m_colNames;
    private final int[] m_colIdxs;
    private final DataTableSpec m_origSpec;
    private final DataType m_superType;
    private final DataColumnSpec m_dummyOrigSpec;

    /**Constructor for class AggregationCellFactory.
     * @param origSpec the original {@link DataTableSpec}
     * @param colNames the names of the columns to aggregate
     * @param globalSettings the {@link GlobalSettings}
     * @param methods the {@link AggregationMethod}s
     */
    public AggregationCellFactory(final DataTableSpec origSpec,
            final List<String> colNames,
            final GlobalSettings globalSettings,
            final List<NamedAggregationOperator> methods) {
        m_origSpec = origSpec;
        m_colIdxs = new int[colNames.size()];
        final Set<String> inclCols =
            new HashSet<String>(colNames);
        final Set<DataType> types =
            new HashSet<DataType>(colNames.size());
        int colIdx = 0;
        for (int i = 0; i < m_origSpec.getNumColumns(); i++) {
            final DataColumnSpec colSpec = origSpec.getColumnSpec(i);
            if (inclCols.contains(colSpec.getName())) {
                m_colIdxs[colIdx++] = i;
                types.add(colSpec.getType());
            }
        }
        m_superType = CollectionCellFactory.getElementType(
                types.toArray(new DataType[0]));
        final DataColumnSpecCreator creator =
            new DataColumnSpecCreator("Super DataType of selected columns",
                    m_superType);
        m_dummyOrigSpec = creator.createSpec();
        m_operators = new AggregationOperator[methods.size()];
        m_colNames = new String[methods.size()];
        int i = 0;
        for (final NamedAggregationOperator method : methods) {
            final OperatorColumnSettings operatorSettings =
                new OperatorColumnSettings(method.inclMissingCells(),
                        m_dummyOrigSpec);
            m_operators[i] = method.getMethodTemplate().createOperator(
                    globalSettings, operatorSettings);
            m_colNames[i] = method.getName();
            i++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        final DataCell[] cells = new DataCell[m_operators.length];
        for (int i = 0; i < m_operators.length; i++) {
            final AggregationOperator operator = m_operators[i];
            operator.compute(row, m_colIdxs);
            cells[i] = operator.getResult();
            operator.reset();
        }
        return cells;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        final DataColumnSpec[] specs =
            new DataColumnSpec[m_operators.length];
        for (int i = 0; i < m_operators.length; i++) {
            final AggregationOperator operator = m_operators[i];
            specs[i] =
                operator.createColumnSpec(m_colNames[i], m_dummyOrigSpec);
        }
        return specs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress(curRowNr / (double) rowCount,
                "Processing row " + curRowNr + " of " + rowCount);
    }
}