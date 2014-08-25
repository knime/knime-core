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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.meta.looper;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopEnd2NodeModel extends NodeModel implements LoopEndNode {

    private BufferedDataContainer[] m_resultContainer = new BufferedDataContainer[2];

    private int m_count = 0;

    private final LoopEnd2NodeSettings m_settings = new LoopEnd2NodeSettings();

    private BufferedDataTable[] m_emptyTable = new BufferedDataTable[2];

    /**
     * Creates a new model.
     */
    public LoopEnd2NodeModel() {
        super(2, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        final DataTableSpec spec0;
        if (m_settings.ignoreEmptyTables1()) {
            spec0 = null;
        } else {
            spec0 = createSpec(inSpecs[0]);
        }
        final DataTableSpec spec1;
        if (m_settings.ignoreEmptyTables2()) {
            spec1 = null;
        } else {
            spec1 = createSpec(inSpecs[1]);
        }
        return new DataTableSpec[]{spec0, spec1};
    }

    private DataTableSpec createSpec(final DataTableSpec inSpec) {
        if (m_settings.addIterationColumn()) {
            DataColumnSpecCreator crea =
                    new DataColumnSpecCreator(
                            DataTableSpec.getUniqueColumnName(inSpec,
                                    "Iteration"), IntCell.TYPE);
            DataTableSpec newSpec = new DataTableSpec(crea.createSpec());

            return new DataTableSpec(inSpec, newSpec);
        } else {
            return inSpec;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        if (!(this.getLoopStartNode() instanceof LoopStartNodeTerminator)) {
            throw new IllegalStateException("Loop end is not connected"
                   + " to matching/corresponding loop start node. You"
                   + "are trying to create an infinite loop!");
        }

        if (m_settings.ignoreEmptyTables1() && inData[0].getRowCount() < 1) {
            if (m_emptyTable[0] == null) {
                BufferedDataContainer cont = exec.createDataContainer(inData[0].getDataTableSpec());
                cont.close();
                m_emptyTable[0] = cont.getTable();
            }
        } else if (m_resultContainer[0] == null) {
            m_resultContainer[0] = exec.createDataContainer(createSpec(inData[0]
                    .getDataTableSpec()));
        }
        if (m_settings.ignoreEmptyTables2() && inData[1].getRowCount() < 1) {
            if (m_emptyTable[1] == null) {
                BufferedDataContainer cont = exec.createDataContainer(inData[1].getDataTableSpec());
                cont.close();
                m_emptyTable[1] = cont.getTable();
            }
        } else if (m_resultContainer[1] == null) {
            m_resultContainer[1] = exec.createDataContainer(createSpec(inData[1]
                    .getDataTableSpec()));
        }

        final IntCell currIterCell = new IntCell(m_count);
        if (!m_settings.ignoreEmptyTables1() || inData[0].getRowCount() > 0) {
            checkSpec(createSpec(inData[0].getDataTableSpec()), m_resultContainer[0].getTableSpec());
            if (m_settings.addIterationColumn()) {
                for (DataRow row : inData[0]) {
                    AppendedColumnRow newRow = new AppendedColumnRow(
                           createNewRow(row), currIterCell);
                    m_resultContainer[0].addRowToTable(newRow);
                }
            } else {
                for (DataRow row : inData[0]) {
                    m_resultContainer[0].addRowToTable(createNewRow(row));
                }
            }
        }
        if (!m_settings.ignoreEmptyTables2() || inData[1].getRowCount() > 0) {
            checkSpec(createSpec(inData[1].getDataTableSpec()), m_resultContainer[1].getTableSpec());
            if (m_settings.addIterationColumn()) {
                for (DataRow row : inData[1]) {
                    AppendedColumnRow newRow = new AppendedColumnRow(
                           createNewRow(row), currIterCell);
                    m_resultContainer[1].addRowToTable(newRow);
                }
            } else {
                for (DataRow row : inData[1]) {
                    m_resultContainer[1].addRowToTable(createNewRow(row));
                }
            }
        }

        final boolean terminateLoop =
            ((LoopStartNodeTerminator)this.getLoopStartNode()).terminateLoop();
        if (terminateLoop) {
            // this was the last iteration - close container and continue
            final BufferedDataTable[] outTables = new BufferedDataTable[2];
            if (m_settings.ignoreEmptyTables1() && m_resultContainer[0] == null) {
                outTables[0] = m_emptyTable[0];
            } else {
                m_resultContainer[0].close();
                outTables[0] = m_resultContainer[0].getTable();
            }
            if (m_settings.ignoreEmptyTables2() && m_resultContainer[1] == null) {
                outTables[1] = m_emptyTable[1];
            } else {
                m_resultContainer[1].close();
                outTables[1] = m_resultContainer[1].getTable();
            }
            m_resultContainer = new BufferedDataContainer[2];
            m_count = 0;

            return outTables;
        } else {
            continueLoop();
            m_count++;
            return new BufferedDataTable[2];
        }
    }

    /**
     * Checks if the two given specs are equal in structure and throws an exception if this is not the case.
     *
     * @param spec1 The spec to check
     * @param spec2 The spec to check against
     * @throws IllegalArgumentException If the two specs are not equal in structure
     */
    private void checkSpec(final DataTableSpec spec1, final DataTableSpec spec2) throws IllegalArgumentException {
        if (!spec1
                .equalStructure(spec2)) {
            StringBuilder error =
                    new StringBuilder(
                            "Input table's structure differs from reference "
                                    + "(first iteration) table: ");
            if (spec1.getNumColumns() != spec2.getNumColumns()) {
                error.append("different column counts ");
                error.append(spec1.getNumColumns());
                error.append(" vs. ").append(spec2.getNumColumns());
            } else {
                for (int i = 0; i < spec1.getNumColumns(); i++) {
                    DataColumnSpec inCol = spec1.getColumnSpec(i);
                    DataColumnSpec predCol = spec2.getColumnSpec(i);
                    if (!inCol.equalStructure(predCol)) {
                        error.append("Column ").append(i).append(" [");
                        error.append(inCol).append("] vs. [");
                        error.append(predCol).append("]");
                    }
                }
            }
            throw new IllegalArgumentException(error.toString());
        }
    }

    private DataRow createNewRow(final DataRow row) {
        RowKey newKey;
        if (m_settings.uniqueRowIDs()) {
            newKey = new RowKey(row.getKey() + "#" + m_count);
        } else {
            newKey = row.getKey();
        }
        return new DefaultRow(newKey, row);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty
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
    protected void reset() {
        m_resultContainer = new BufferedDataContainer[2];
        m_emptyTable = new BufferedDataTable[2];
        m_count = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty
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
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        AbstractLoopEndNodeSettings s = new LoopEndNodeSettings();
        s.loadSettings(settings);
    }
}
