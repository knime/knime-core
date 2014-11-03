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
 * ---------------------------------------------------------------------
 *
 * History
 *   15.10.2014 (Tim-Oliver Buchholz): created
 */
package org.knime.base.node.io.fixedwidthfr;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Tim-Oliver Buchholz
 */
public class FixedWidthFRNodeModel extends NodeModel {

    private FixedWidthFRSettings m_nodeSettings = new FixedWidthFRSettings();
    private FixedWidthFRSettings m_workSettings = new FixedWidthFRSettings();

    /**
     * No input port but one output port. it is a file reader
     */
    FixedWidthFRNodeModel() {
        super(0, 1);
    }

    /**
     * @param context the creation context
     */
    public FixedWidthFRNodeModel(final NodeCreationContext context) {
        this();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data, final ExecutionContext exec)
        throws CanceledExecutionException, InvalidSettingsException {

        getLogger().info("Preparing to read from '" + m_workSettings.getFileLocation().toString() + "'.");

        DataTableSpec tSpec = m_workSettings.createDataTableSpec();

        try (FixedWidthFRTable fTable = new FixedWidthFRTable(tSpec, m_workSettings, exec)) {

            // create a DataContainer and fill it with the rows read. It is faster
            // then reading the file every time (for each row iterator), and it
            // collects the domain for each column for us. Also, if things fail,
            // the error message is printed during file reader execution (were it
            // belongs to) and not some time later when a node uses the row
            // iterator from the file table.

            BufferedDataContainer c = exec.createDataContainer(fTable.getDataTableSpec(), false);
            int row = 0;
            for (DataRow next : fTable) {
                row++;
                String message = "Caching row #" + row + " (\"" + next.getKey() + "\")";
                exec.setMessage(message);
                exec.checkCanceled();
                c.addRowToTable(next);
            }

            c.close();

            BufferedDataTable out = c.getTable();

            return new BufferedDataTable[]{out};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        assert inSpecs.length == 0;

        // create workSettings, because we only want to remove the last column for execution
        m_workSettings = new FixedWidthFRSettings(m_nodeSettings);
        // remove 'remaining characters' column
        m_workSettings.removeColAt(m_nodeSettings.getNumberOfColumns() - 1);

        m_workSettings.checkSettings();

        return new DataTableSpec[]{m_workSettings.createDataTableSpec()};

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_nodeSettings.saveToConfiguration(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // check all settings
        new FixedWidthFRSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_nodeSettings = new FixedWidthFRSettings(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }
}
