/*
 * ------------------------------------------------------------------------
 *
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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.testing.node.filestore.fsloopend;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.testing.data.filestore.LargeFileStorePortObject;
import org.knime.testing.data.filestore.LargeFileStorePortObjectSpec;
import org.knime.testing.data.filestore.LargeFileStoreValue;

/** Loop end that creates a {@link LargeFileStorePortObject}.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class LoopEndFileStorePortObjectTestNodeModel extends NodeModel implements LoopEndNode, BufferedDataTableHolder {

    private final SettingsModelString m_fsColNameModel = createFSColNameModel();

    private BufferedDataTable[] m_internalsTable;

    private LargeFileStorePortObject.PortObjectCreator m_objectCreator;

    static final SettingsModelString createFSColNameModel() {
        return new SettingsModelString("fsColName", null);
    }

    LoopEndFileStorePortObjectTestNodeModel() {
        super(new PortType[] {BufferedDataTable.TYPE}, new PortType[] {LargeFileStorePortObject.TYPE});
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec inSpec = (DataTableSpec)inSpecs[0];
        String colName = m_fsColNameModel.getStringValue();
        // TODO check
        DataColumnSpec colSpec = inSpec.getColumnSpec(colName);
        if (colSpec == null || !colSpec.getType().isCompatible(LargeFileStoreValue.class)) {
            throw new InvalidSettingsException("Column " + colName + " doesn't exist or is not a file store column");
        }
        return new PortObjectSpec[] {LargeFileStorePortObjectSpec.INSTANCE};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        LoopStartNode loopStartNode = getLoopStartNode();
        if (!(loopStartNode instanceof LoopStartNodeTerminator)) {
            throw new IllegalStateException("No matching start node");
        }
        boolean isToTerminate = ((LoopStartNodeTerminator)loopStartNode).terminateLoop();
        BufferedDataTable inTable = (BufferedDataTable)inObjects[0];
        int colIndex = inTable.getDataTableSpec().findColumnIndex(m_fsColNameModel.getStringValue());
        int index = 0;
        if (m_objectCreator == null) {
            m_objectCreator = new LargeFileStorePortObject.PortObjectCreator();
        }
        BufferedDataContainer internalAppendContainer =
                exec.createDataContainer(new DataTableSpec(inTable.getDataTableSpec().getColumnSpec(colIndex)));
        for (DataRow r : inTable) {
            exec.setProgress(index / (double)inTable.getRowCount(), "Row " + (index++));
            DataCell c = r.getCell(colIndex);
            if (c.isMissing()) {
                throw new IllegalStateException(
                    String.format("Missing values not allowed (column %d, row ID %s)", colIndex, r.getKey()));
            } else {
                LargeFileStoreValue value = (LargeFileStoreValue) c;
                final long expected = value.getSeed();
                internalAppendContainer.addRowToTable(new DefaultRow(r.getKey(), c));
                m_objectCreator.add(r.getKey().getString(), expected, value.getLargeFile());
                final long actual = value.getLargeFile().read();
                if (actual != expected) {
                    throw new IllegalStateException(String.format(
                        "Data inconsistency in row %s, column %d (%d vs. %d)", r.getKey(), colIndex, expected, actual));
                }
            }
        }
        internalAppendContainer.close();
        m_internalsTable = ArrayUtils.add(m_internalsTable, internalAppendContainer.getTable());
        if (isToTerminate) {
            final LargeFileStorePortObject object = m_objectCreator.create();
            m_objectCreator = null;
            return new PortObject[] {object};
        } else {
            super.continueLoop();
            return new PortObject[] {null};
        }
    }

    @Override
    public BufferedDataTable[] getInternalTables() {
        return m_internalsTable;
    }

    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        // ignored, not needed after restore
    }

    @Override
    protected void reset() {
        m_internalsTable = null;
        m_objectCreator = null;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_fsColNameModel.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fsColNameModel.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fsColNameModel.loadSettingsFrom(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

}
