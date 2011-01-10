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
 *
 */
package org.knime.base.node.io.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.DatabaseReaderConnection;

/**
 *
 * @author Thomas Gabriel, University of Konstanz
 */
class DBReaderNodeModel extends NodeModel
        implements DBVariableSupportNodeModel {

    private DataTableSpec m_lastSpec = null;

    private String m_query = null;

    private final DatabaseReaderConnection m_load =
        new DatabaseReaderConnection(null);

    /**
     * Creates a new database reader with one data out-port.
     * @param ins number data input ports
     * @param outs number data output ports
     */
    DBReaderNodeModel(final int ins, final int outs) {
        super(ins, outs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec)
            throws CanceledExecutionException, Exception {
        exec.setProgress("Opening database connection...");
        try {
            m_load.setDBQueryConnection(new DatabaseQueryConnectionSettings(
                    m_load.getQueryConnection(), parseQuery(m_query)));
            m_lastSpec = m_load.getDataTableSpec();
            exec.setProgress("Reading data from database...");
            return new BufferedDataTable[]{m_load.createTable(exec)};
        } catch (CanceledExecutionException cee) {
            throw cee;
        } catch (Exception e) {
            m_lastSpec = null;
            throw e;
        } catch (Throwable t) {
            m_lastSpec = null;
            throw new Exception(t);
        }
    }

    /**
     * Parses the given R command and replaces the variables.
     * @param rCommand the R command to parse
     * @param model delegator to to retrieve variables
     * @return the R script where the variables have been replace with
     *         their actual value
     */
    private String parseQuery(final String query) {
        String command = new String(query);
        return DBVariableSupportNodeModel.Resolver.parse(command, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delegatePeekFlowVariableInt(final String name) {
        return super.peekFlowVariableInt(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double delegatePeekFlowVariableDouble(final String name) {
        return peekFlowVariableDouble(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String delegatePeekFlowVariableString(final String name) {
        return peekFlowVariableString(name);
    }

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
        File specFile = null;
        specFile = new File(nodeInternDir, "spec.xml");
        if (!specFile.exists()) {
            IOException ioe = new IOException("Spec file (\""
                    + specFile.getAbsolutePath() + "\") does not exist "
                    + "(node may have been saved by an older version!)");
            throw ioe;
        }
        NodeSettingsRO specSett =
            NodeSettings.loadFromXML(new FileInputStream(specFile));
        try {
            m_lastSpec = DataTableSpec.load(specSett);
        } catch (InvalidSettingsException ise) {
            IOException ioe = new IOException("Could not read output spec.");
            ioe.initCause(ise);
            throw ioe;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        assert (m_lastSpec != null) : "Spec must not be null!";
        NodeSettings specSett = new NodeSettings("spec.xml");
        m_lastSpec.save(specSett);
        File specFile = new File(nodeInternDir, "spec.xml");
        specSett.saveToXML(new FileOutputStream(specFile));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_lastSpec != null) {
            return new DataTableSpec[]{m_lastSpec};
        }
        try {
            DatabaseQueryConnectionSettings conn = m_load.getQueryConnection();
            if (conn == null) {
                throw new InvalidSettingsException(
                        "No database connection available.");
            }
            m_load.setDBQueryConnection(new DatabaseQueryConnectionSettings(
                    conn, parseQuery(m_query)));
            m_lastSpec = m_load.getDataTableSpec();
            return new DataTableSpec[]{m_lastSpec};
        } catch (InvalidSettingsException e) {
            m_lastSpec = null;
            throw e;
        } catch (Throwable t) {
            m_lastSpec = null;
            throw new InvalidSettingsException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String query = settings.getString(
                DatabaseConnectionSettings.CFG_STATEMENT);
        if (query != null && query.contains(
                DatabaseQueryConnectionSettings.TABLE_PLACEHOLDER)) {
            throw new InvalidSettingsException(
                    "Database table place holder ("
                    + DatabaseQueryConnectionSettings.TABLE_PLACEHOLDER
                    + ") not replaced.");
        }
        // validates the current settings on a temp. connection
        new DatabaseQueryConnectionSettings(settings, getCredentialsProvider());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String query = settings.getString(
                DatabaseConnectionSettings.CFG_STATEMENT);
        DatabaseQueryConnectionSettings conn = m_load.getQueryConnection();
        if (conn == null || !conn.loadValidatedConnection(settings,
                    getCredentialsProvider())
                || query == null || m_query == null || !query.equals(m_query)) {
            m_lastSpec = null;
            try {
                m_load.setDBQueryConnection(
                        new DatabaseQueryConnectionSettings(settings,
                            getCredentialsProvider()));
            } catch (Throwable t) {
                throw new InvalidSettingsException(t);
            }
        }
        m_query = query;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        DatabaseQueryConnectionSettings conn = m_load.getQueryConnection();
        if (conn != null) {
            conn.saveConnection(settings);
        }
        settings.addString(DatabaseConnectionSettings.CFG_STATEMENT, m_query);
    }

    /**
     * @param newQuery the new query to set
     */
    final void setQuery(final String newQuery) {
        m_query = newQuery;
    }

    /**
     * @return current query
     */
    final String getQuery() {
        return m_query;
    }
}
