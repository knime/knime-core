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
 *   26.09.2014 (koetter): created
 */
package org.knime.base.node.io.database.drop;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabaseConnectionPortObject;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseUtility;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * Drops the given table in the given db.
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 */
public class DBDropTableNodeModel extends NodeModel {

    /**Table name config key. */
    static final String CFG_TABLE_NAME = "tableName";
    private final SettingsModelString m_tableName = createTableNameModel();
    private final SettingsModelBoolean m_cascade = createCascadeModel();
    private final SettingsModelBoolean m_failIfNotExists = createFailIfNotExistsModel();

    /**
     * Constructor.
     */
    public DBDropTableNodeModel() {
        super(new PortType[] {DatabaseConnectionPortObject.TYPE}, new PortType[]{DatabaseConnectionPortObject.TYPE});
    }

    /**
     * @return the table name settings model
     */
    static SettingsModelString createTableNameModel() {
        return new SettingsModelString(CFG_TABLE_NAME, null);
    }

    /**
     * @return the drop cascade model
     */
    static SettingsModelBoolean createCascadeModel() {
        return new SettingsModelBoolean("cascade", false);
    }

    /**
     * @return the if exists model
     */
    static SettingsModelBoolean createFailIfNotExistsModel() {
        return new SettingsModelBoolean("failIfNotExists", false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        exec.setMessage("Droping table");
        final DatabaseConnectionPortObject incomingConnection = (DatabaseConnectionPortObject)inObjects[0];
        final CredentialsProvider cp = getCredentialsProvider();
        final DatabaseConnectionSettings connSettings = incomingConnection.getConnectionSettings(cp);
        final DatabaseUtility dbUtility = connSettings.getUtility();
        final StatementManipulator manipulator = dbUtility.getStatementManipulator();
        final String table2Drop = m_tableName.getStringValue();
        try {
            if (m_failIfNotExists.getBooleanValue()
                    || dbUtility.tableExists(connSettings.createConnection(cp), table2Drop)) {
                connSettings.execute(manipulator.dropTable(table2Drop, m_cascade.getBooleanValue()), cp);
                exec.setMessage("Table " + table2Drop + " sucessful droped");
            } else {
                exec.setMessage("Table " + table2Drop + " does not exist in db");
            }
        } catch (SQLException ex) {
            Throwable cause = ExceptionUtils.getRootCause(ex);
            if (cause == null) {
                cause = ex;
            }
            throw new InvalidSettingsException("Error while validating drop statement: " + cause.getMessage(), ex);
        }
        return inObjects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_tableName.saveSettingsTo(settings);
        m_cascade.saveSettingsTo(settings);
        m_failIfNotExists.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final String tableName =
                ((SettingsModelString)m_tableName.createCloneWithValidatedValue(settings)).getStringValue();
        if (tableName == null || tableName.isEmpty()) {
            throw new InvalidSettingsException("Please provide the table name to drop");
        }
        m_cascade.validateSettings(settings);
        m_failIfNotExists.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_tableName.loadSettingsFrom(settings);
        m_cascade.loadSettingsFrom(settings);
        m_failIfNotExists.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // nothing to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // nothing to save
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset
    }
}
