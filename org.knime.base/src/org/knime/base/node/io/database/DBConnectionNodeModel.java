/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.DatabaseReaderConnection;

/**
 *
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBConnectionNodeModel extends NodeModel {

    private DatabaseReaderConnection m_load = null;

    private DataTableSpec m_lastSpec = null;

    /** Config key to write last processed spec. */
    static final String CFG_SPEC_XML = "spec.xml";

    /**
     * Creates a new database connection reader.
     */
    DBConnectionNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec)
            throws CanceledExecutionException, Exception {
        if (m_load == null) {
            exec.setProgress("Opening database connection...");
            DatabasePortObject dbObj = (DatabasePortObject) inData[0];
            DatabaseQueryConnectionSettings conn =
                new DatabaseQueryConnectionSettings(
                    dbObj.getConnectionModel(), getCredentialsProvider());
            m_load = new DatabaseReaderConnection(conn);
        }
        m_lastSpec = m_load.getDataTableSpec();
        exec.setProgress("Reading data from database...");
        return new BufferedDataTable[]{m_load.createTable(exec)};
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
        File specFile = new File(nodeInternDir, CFG_SPEC_XML);
        if (!specFile.exists()) {
            throw new IOException("Spec file (\""
                    + specFile.getAbsolutePath() + "\") does not exist "
                    + "(node may have been saved by an older version!)");
        }
        NodeSettingsRO specSett =
            NodeSettings.loadFromXML(new FileInputStream(specFile));
        try {
            m_lastSpec = DataTableSpec.load(specSett);
        } catch (InvalidSettingsException ise) {
            m_lastSpec = null;
            throw new IOException("Could not read last spec.", ise);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        NodeSettings specSett = new NodeSettings(CFG_SPEC_XML);
        if (m_lastSpec != null) {
            m_lastSpec.save(specSett);
        }
        File specFile = new File(nodeInternDir, CFG_SPEC_XML);
        specSett.saveToXML(new FileOutputStream(specFile));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DatabasePortObjectSpec dbSpec =
            (DatabasePortObjectSpec) inSpecs[0];
        ModelContentRO newConn = dbSpec.getConnectionModel();
        if (m_load != null) {
            ModelContent oldConn = new ModelContent(
                    "database_query_connection_model");
            m_load.getQueryConnection().saveConnection(oldConn);
            if (oldConn.equals(newConn)) {
                return new DataTableSpec[]{m_lastSpec};
            }
        }
        try {
            DatabaseQueryConnectionSettings conn =
                new DatabaseQueryConnectionSettings(newConn,
                    getCredentialsProvider());
            m_load = new DatabaseReaderConnection(conn);
            m_lastSpec = m_load.getDataTableSpec();
            return new DataTableSpec[]{m_lastSpec};
        } catch (InvalidSettingsException ise) {
            m_lastSpec = null;
            throw ise;
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
        // empty

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // empty
    }

}
