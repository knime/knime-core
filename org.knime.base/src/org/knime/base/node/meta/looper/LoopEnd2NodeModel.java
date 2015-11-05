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
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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

    // array with most common super types throughout all tables
    private DataType[] m_commonDataTypes1;

    // array with most common super types throughout all tables
    private DataType[] m_commonDataTypes2;

    /** Creates a new model. */
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
        if (m_settings.ignoreEmptyTables1() || m_settings.tolerateColumnTypes1()) {
            spec0 = null;
        } else {
            spec0 = createSpec(inSpecs[0], false);
        }
        final DataTableSpec spec1;
        if (m_settings.ignoreEmptyTables2() || m_settings.tolerateColumnTypes2()) {
            spec1 = null;
        } else {
            spec1 = createSpec(inSpecs[1], false);
        }
        return new DataTableSpec[]{spec0, spec1};
    }

    private DataTableSpec createSpec(final DataTableSpec inSpec, final boolean tolerate) {
        final DataTableSpec outSpec;
        if (tolerate) {
            DataColumnSpec[] commonSpecs = new DataColumnSpec[inSpec.getNumColumns()];
            for (int i = 0; i < commonSpecs.length; i++) {
                DataColumnSpecCreator cr = new DataColumnSpecCreator(inSpec.getColumnSpec(i));
                // init with most common types
                cr.setType(DataType.getType(DataCell.class));
                commonSpecs[i] = cr.createSpec();
            }
            outSpec = new DataTableSpec(commonSpecs);
        } else {
            outSpec = inSpec;
        }
        if (m_settings.addIterationColumn()) {
            DataColumnSpecCreator crea = new DataColumnSpecCreator(
                            DataTableSpec.getUniqueColumnName(outSpec, "Iteration"), IntCell.TYPE);
            DataTableSpec newSpec = new DataTableSpec(crea.createSpec());
            return new DataTableSpec(outSpec, newSpec);
        } else {
            return outSpec;
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

        boolean tolerate1 = m_settings.tolerateColumnTypes1();
        boolean tolerate2 = m_settings.tolerateColumnTypes2();

        final DataTableSpec inSpec1 = inData[0].getDataTableSpec();
        final DataTableSpec inSpec2 = inData[1].getDataTableSpec();

        if (m_settings.ignoreEmptyTables1() && inData[0].getRowCount() < 1) {
            if (m_emptyTable[0] == null) {
                BufferedDataContainer cont = exec.createDataContainer(createSpec(inSpec1, tolerate1));
                cont.close();
                m_emptyTable[0] = cont.getTable();
            }
        } else if (m_resultContainer[0] == null) {
            m_resultContainer[0] = exec.createDataContainer(createSpec(inSpec1, tolerate1));
        }
        if (m_settings.ignoreEmptyTables2() && inData[1].getRowCount() < 1) {
            if (m_emptyTable[1] == null) {
                BufferedDataContainer cont = exec.createDataContainer(createSpec(inSpec2, tolerate2));
                cont.close();
                m_emptyTable[1] = cont.getTable();
            }
        } else if (m_resultContainer[1] == null) {
            m_resultContainer[1] = exec.createDataContainer(createSpec(inSpec2, tolerate2));
        }

        final IntCell currIterCell = new IntCell(m_count);
        if (!m_settings.ignoreEmptyTables1() || inData[0].getRowCount() > 0) {
            if (m_commonDataTypes1 == null) {
                m_commonDataTypes1 = new DataType[inSpec1.getNumColumns()];
            }
            for (int i = 0; i < inSpec1.getNumColumns(); i++) {
                final DataType type = inSpec1.getColumnSpec(i).getType();
                if (m_commonDataTypes1[i] == null) {
                    m_commonDataTypes1[i] = type;
                } else {
                    m_commonDataTypes1[i] = DataType.getCommonSuperType(m_commonDataTypes1[i], type);
                }
            }
            checkSpec(createSpec(inSpec1, tolerate1), m_resultContainer[0].getTableSpec());
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
            if (m_commonDataTypes2 == null) {
                m_commonDataTypes2 = new DataType[inSpec2.getNumColumns()];
            }
            for (int i = 0; i < inSpec2.getNumColumns(); i++) {
                final DataType type = inSpec2.getColumnSpec(i).getType();
                if (m_commonDataTypes2[i] == null) {
                    m_commonDataTypes2[i] = type;
                } else {
                    m_commonDataTypes2[i] = DataType.getCommonSuperType(m_commonDataTypes2[i], type);
                }
            }
            checkSpec(createSpec(inSpec2, tolerate2), m_resultContainer[1].getTableSpec());
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

            //bugfix 6482: if the table is empty and are to be ignored, no common data type for a column has to be set
            if (tolerate1 && (inData[0].getRowCount() > 0 || !m_settings.ignoreEmptyTables1())) {
                DataTableSpec outSpec = outTables[0].getSpec();
                DataColumnSpec[] cspecs = new DataColumnSpec[outSpec.getNumColumns()];
                for (int i = 0; i < m_commonDataTypes1.length; i++) {
                    DataColumnSpecCreator cr = new DataColumnSpecCreator(outSpec.getColumnSpec(i));
                    cr.setType(m_commonDataTypes1[i]);
                    cspecs[i] = cr.createSpec();
                }
                // add iteration column spec as last column
                if (m_settings.addIterationColumn()) {
                    cspecs[cspecs.length - 1] = outSpec.getColumnSpec(cspecs.length - 1);
                }
                outTables[0] = exec.createSpecReplacerTable(outTables[0], new DataTableSpec(cspecs));
            }

            //bugfix 6482: if the table is empty and are to be ignored, no common data type for a column has to be set
            if (tolerate2 && (inData[1].getRowCount() > 0 || !m_settings.ignoreEmptyTables2())) {
                DataTableSpec outSpec = outTables[1].getSpec();
                DataColumnSpec[] cspecs = new DataColumnSpec[outSpec.getNumColumns()];
                for (int i = 0; i < m_commonDataTypes2.length; i++) {
                    DataColumnSpecCreator cr = new DataColumnSpecCreator(outSpec.getColumnSpec(i));
                    cr.setType(m_commonDataTypes2[i]);
                    cspecs[i] = cr.createSpec();
                }
                // add iteration column spec as last column
                if (m_settings.addIterationColumn()) {
                    cspecs[cspecs.length - 1] = outSpec.getColumnSpec(cspecs.length - 1);
                }
                outTables[1] = exec.createSpecReplacerTable(outTables[1], new DataTableSpec(cspecs));
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
        m_commonDataTypes1 = null;
        m_commonDataTypes2 = null;
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
