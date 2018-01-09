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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.database;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.reader.DBReader;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 *
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBConnectionNodeModel extends NodeModel {

    private final SettingsModelBoolean m_useDbRowId = createUseRowIdModel();

    /** Creates a new database connection reader. */
    DBConnectionNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * @return
     */
    static SettingsModelBoolean createUseRowIdModel() {
        return new SettingsModelBoolean("useDbRowId", true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
            throws CanceledExecutionException, Exception {
        exec.setProgress("Opening database connection...");
        DatabasePortObject dbObj = (DatabasePortObject) inData[0];
        DatabaseQueryConnectionSettings conn = dbObj.getConnectionSettings(getCredentialsProvider());

        final DBReader reader = conn.getUtility().getReader(conn);
//		final DatabaseReaderConnection load = new DatabaseReaderConnection(conn);
        exec.setProgress("Reading data from database...");
        CredentialsProvider cp = getCredentialsProvider();
        return new BufferedDataTable[]{reader.createTable(exec, cp, m_useDbRowId.getBooleanValue())};
    }

/*

    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    STREAMING IS DISABLED UNTIL WE HAVE A PROPPER CONNECTION HANDLING SINCE MYSQL FOR EXAMPLE DOES NOT ALLOW
    CONCURRENT READS WHICH HAPPEN IF WE USE THE DBRowIterator!!!
    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                exec.setProgress("Opening database connection...");
                DatabasePortObject dbObj = (DatabasePortObject)((PortObjectInput)inputs[0]).getPortObject();
                DatabaseQueryConnectionSettings conn = dbObj.getConnectionSettings(getCredentialsProvider());

                final DBReader load = conn.getUtility().getReader(conn);
                exec.setProgress("Reading data from database...");
                try (DBRowIterator rowItConn =
                        load.createRowIteratorConnection(exec, getCredentialsProvider(), true);){
                    RowOutput out = (RowOutput)outputs[0];
                    RowIterator it = rowItConn.iterator();
                    int index = 0;
                    while (it.hasNext()) {
                        out.push(it.next());
                        exec.setMessage(String.format("Row %d", ++index));
                    }
                }
            }
        };
    }
    */

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec) inSpecs[0];
        return new PortObjectSpec[] {dbSpec.getDataTableSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        //nothing to validate
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            m_useDbRowId.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            //this option was introduced in KNIME 2.12
            m_useDbRowId.setBooleanValue(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_useDbRowId.saveSettingsTo(settings);
    }

}
