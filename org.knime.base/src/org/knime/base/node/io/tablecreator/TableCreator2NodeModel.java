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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.08.2010 (hofer): created
 */
package org.knime.base.node.io.tablecreator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.node.io.filereader.ColProperty;
import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;




/**
 * The node model of the table creator node that allows the creation of a
 * data table.
 *
 * @author Heiko Hofer
 */
public class TableCreator2NodeModel extends NodeModel {
    private TableCreator2NodeSettings m_settings;


    /** Creates a new instance. */
    protected TableCreator2NodeModel() {
        super(new PortType[0], new PortType[]{BufferedDataTable.TYPE});
        m_settings = new TableCreator2NodeSettings();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        DataTableSpec spec = createSpec();
        return new PortObjectSpec[] {spec};
    }

    private DataTableSpec createSpec() throws InvalidSettingsException {
        int numColumns = max(m_settings.getColumnIndices()) + 1;
        if (!m_settings.getColumnProperties().isEmpty()) {
            numColumns = Math.max(numColumns,
                    m_settings.getColumnProperties().lastKey() + 1);
        }
        for (int i = 0; i < numColumns; i++) {
            if (!m_settings.getColumnProperties().containsKey(i)) {
                throw new InvalidSettingsException("Name and type is not "
                        + "specified for every column.");
            }
        }


        List<DataColumnSpec> colList = new ArrayList<DataColumnSpec>();
        for (int i = 0; i < numColumns; i++) {
            if (!m_settings.getColumnProperties().get(i).getSkipThisColumn()) {
                colList.add(
                       m_settings.getColumnProperties().get(i).getColumnSpec());
            }
        }
        DataColumnSpec[] cols =
            colList.toArray(new DataColumnSpec[colList.size()]);
        return new DataTableSpec(cols);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec)
            throws Exception {
        DataTableSpec outSpec = createSpec();

        BufferedDataContainer cont = exec.createDataContainer(
                outSpec, true);

        int numColProps = m_settings.getColumnProperties().size();
        ColProperty[] colProps = new ColProperty[numColProps];
        for (int i = 0; i < numColProps; i++) {
            colProps[i] = m_settings.getColumnProperties().get(i);
        }
        int cc = 0;
        int[] notSkippedMap = new int[numColProps];
        for (int i = 0; i < numColProps; i++) {
            notSkippedMap[i] = cc;
            if (!colProps[i].getSkipThisColumn()) {
                cc++;
            }
        }
        int numRows = max(m_settings.getRowIndices()) + 1;
        String rowIdPrefix = m_settings.getRowIdPrefix();
        String rowIdSuffix = m_settings.getRowIdSuffix();
        int rowIdStartWidth = m_settings.getRowIdStartValue();
        int c = 0;
        // fix for bug #2969
        Set<Integer> toRemove = new HashSet<Integer>();
        DataCellFactory cellFactory = new DataCellFactory();
        for (int i = 0; i < numRows; i++) {
            DataCell[] cells = new DataCell[outSpec.getNumColumns()];
            for (int k = 0; k < numColProps; k++) {
                // fix for bug #2969
                while (c < m_settings.getRowIndices().length
                        && (m_settings.getRowIndices()[c] < 0
                        || m_settings.getColumnIndices()[c] < 0)) {
                    toRemove.add(c);
                    c++;
                }
                String value = "";
                if (c < m_settings.getRowIndices().length
                        && m_settings.getRowIndices()[c] == i
                        && m_settings.getColumnIndices()[c] == k) {
                    value = m_settings.getValues()[c];
                    c++;
                }
                if (colProps[k].getSkipThisColumn()) {
                    continue;
                }
                String missValPattern = colProps[k].getMissingValuePattern();
                cellFactory.setMissingValuePattern(missValPattern);
                DataCell result = cellFactory.createDataCellOfType(
                            colProps[k].getColumnSpec().getType(), value);
                if (null != result) {
                    cells[notSkippedMap[k]] = result;
                } else {
                    throw new InvalidSettingsException(
                            cellFactory.getErrorMessage());
                }
            }
            StringBuilder rowId = new StringBuilder();
            rowId.append(rowIdPrefix);
            rowId.append(Integer.toString(i + rowIdStartWidth));
            rowId.append(rowIdSuffix);

            DataRow row = new DefaultRow(rowId.toString(), cells);
            cont.addRowToTable(row);
        }
        cont.close();

        BufferedDataTable out = cont.getTable();

        // fix for bug #2969
        if (!toRemove.isEmpty()) {
            int oldLength = m_settings.getRowIndices().length;
            int newLength = oldLength - toRemove.size();
            int[] rowIndices = new int[newLength];
            int[] colIndices = new int[newLength];
            String[] values = new String[newLength];
            c = 0;
            for (int i = 0; i < newLength; i++) {
                if (!toRemove.contains(i)) {
                    rowIndices[c] = m_settings.getRowIndices()[i];
                    colIndices[c] = m_settings.getColumnIndices()[i];
                    values[c] = m_settings.getValues()[i];
                    c++;
                }
            }
            m_settings.setRowIndices(rowIndices);
            m_settings.setColumnIndices(colIndices);
            m_settings.setValues(values);
        }
        return new BufferedDataTable[]{out};
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        TableCreator2NodeSettings s = new TableCreator2NodeSettings();
        s.loadSettings(settings);
    }

    private int max(final int[] values) {
        int max = -1;
        for (int i = 0; i < values.length; i++) {
            max = Math.max(max, values[i]);
        }
        return max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException {
        // Node has no internal data.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException {
        // Node has no internal data.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Node has no internal data.
    }
}
