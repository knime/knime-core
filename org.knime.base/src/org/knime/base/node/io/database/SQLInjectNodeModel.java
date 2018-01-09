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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.io.database;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.port.database.DatabaseConnectionPortObject;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.reader.DBReader;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;

/**
 * This is the model implementation of "SQL Inject" node.
 *
 *
 * @author Alexander Fillbrunn, University of Konstanz, Germany
 * @since 2.10
 */
public class SQLInjectNodeModel extends NodeModel {

    private static final String CFG_FLOW_VAR = "flowVarName";

    /**
     * Constructor for the node model.
     */
    protected SQLInjectNodeModel() {
        super(new PortType[]{DatabaseConnectionPortObject.TYPE, FlowVariablePortObject.TYPE},
              new PortType[]{DatabasePortObject.TYPE});
    }

    /**
     * Creates a settings model for the flow variable which contains the SQL.
     * @return the settings model
     */
    static SettingsModelString createFlowVariableNameSettingsModel() {
        return new SettingsModelString(CFG_FLOW_VAR, null);
    }

    private SettingsModelString m_flowVariableName = createFlowVariableNameSettingsModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        exec.setMessage("Retrieving metadata from database");


        final String varName = m_flowVariableName.getStringValue();
        if (varName == null) {
            throw new InvalidSettingsException("No flow variable for the SQL statement selected");
        }

        final String sql = peekFlowVariableString(varName);
        if (sql == null) {
            throw new InvalidSettingsException("The selected flow variable is not assigned");
        }

        DatabaseConnectionPortObject dbIn = (DatabaseConnectionPortObject)inData[0];
        DatabaseConnectionSettings inSettings = dbIn.getConnectionSettings(getCredentialsProvider());
        // Attach the SQL query
        DatabaseQueryConnectionSettings outSettings = new DatabaseQueryConnectionSettings(inSettings, sql);

        // Probe the database to see how the result table looks like
        DBReader load = outSettings.getUtility().getReader(outSettings);
        DataTableSpec tableSpec = load.getDataTableSpec(getCredentialsProvider());
        DatabasePortObjectSpec outSpec = new DatabasePortObjectSpec(tableSpec, outSettings.createConnectionModel());

        return new PortObject[]{new DatabasePortObject(outSpec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        // We do not know the SQL query yet, so we cannot retrieve the DataTableSpec and therefore cannot
        // produce a DatabasePortObjectSpec
        return new PortObjectSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_flowVariableName.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_flowVariableName.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_flowVariableName.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}

