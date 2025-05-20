/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   Oct 14, 2015 (hornm): created
 */
package org.knime.testing.streaming.testexecutor;

import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.streamable.RowOutput;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
class BufferedDataContainerRowOutput extends RowOutput {

    private BufferedDataContainer m_dataContainer;

    private BufferedDataTable m_setFullyTable;

    private boolean m_closeCalled;

    /**
     * Constructor.
     *
     * @param dataContainer the data container to be filled
     */
    public BufferedDataContainerRowOutput(final BufferedDataContainer dataContainer) {
        m_dataContainer = dataContainer;
    }

    /**
     * Constructor. In this case the data table can only be set by calling
     * {@link #setFully(org.knime.core.node.BufferedDataTable)}. If {@link #push(DataRow)} is used an
     * {@link IllegalStateException} will be thrown.
     */
    public BufferedDataContainerRowOutput() {
        m_dataContainer = null;
    }

    @Override
    public void push(final DataRow row) throws InterruptedException {
        if (m_dataContainer == null) {
            throw new IllegalStateException(
                "Table can only be set by the 'setFully'-method. "
                + "Rows can not be added individually. "
                + "Possible reason: DataTableSpec==null at configure-time (must be non-null for streamable ports).");
        }
        m_dataContainer.addRowToTable(row);
    }

    @Override
    public void close() {
        m_closeCalled = true;
    }

    /**
     * @return whether {@link #close()} has been called at least once
     */
    public boolean closeCalled() {
        return m_closeCalled;
    }

    BufferedDataTable getDataTable() {
        if (m_dataContainer != null) {
            m_dataContainer.close();
            return m_dataContainer.getTable();
        } else if (m_setFullyTable != null) {
            return m_setFullyTable;
        } else {
            throw new IllegalStateException("No table set. Use 'setFully'-method to set table first.");
        }
    }

    @Override
    public void setInactive() {
        super.setInactive();
        this.close();
    }

    @Override
    public void setFully(final BufferedDataTable table) throws InterruptedException {
        m_dataContainer = null;
        m_setFullyTable = table;
        m_closeCalled = true;
    }

}
