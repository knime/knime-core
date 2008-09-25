/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.io.database;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
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
final class DBQueryNodeModel extends DBNodeModel {
    
    /** Place holder for the database input view. */
    static final String TABLE_PLACE_HOLDER = "#table#";
    
   private final SettingsModelString m_query = 
        DBQueryNodeDialogPane.createQueryModel();
    
    /**
     * Creates a new database reader.
     */
    DBQueryNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE}, 
                new PortType[]{DatabasePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_query.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        SettingsModelString query = 
            m_query.createCloneWithValidatedValue(settings);
        String queryString = query.getStringValue();
        if (queryString != null && !queryString.contains(TABLE_PLACE_HOLDER)) {
            throw new InvalidSettingsException(
                    "Database view place holder (" + TABLE_PLACE_HOLDER 
                    + ") must not be replaced.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_query.loadSettingsFrom(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DatabasePortObjectSpec spec = (DatabasePortObjectSpec) inSpecs[0];
        DatabaseQueryConnectionSettings conn = 
            new DatabaseQueryConnectionSettings(
                spec.getConnectionModel());
        String newQuery = createQuery(conn.getQuery(), getTableID());
        conn = new DatabaseQueryConnectionSettings(conn, newQuery);
        try {
            DatabaseReaderConnection reader = 
                new DatabaseReaderConnection(conn);
            DataTableSpec outSpec = reader.getDataTableSpec();
            DatabasePortObjectSpec dbSpec = new DatabasePortObjectSpec(
                    outSpec, conn.createConnectionModel());
            return new PortObjectSpec[]{dbSpec};
        } catch (InvalidSettingsException ise) {
            throw ise;
        } catch (Throwable t) {
            throw new InvalidSettingsException(t);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) 
            throws CanceledExecutionException, Exception {
        DatabasePortObject dbObj = (DatabasePortObject) inData[0];
        DatabaseQueryConnectionSettings conn = 
                new DatabaseQueryConnectionSettings(
                dbObj.getSpec().getConnectionModel());
        String newQuery = createQuery(conn.getQuery(), getTableID());
        conn = new DatabaseQueryConnectionSettings(conn, newQuery);
        DatabaseReaderConnection load = new DatabaseReaderConnection(conn);
        DataTableSpec outSpec = load.getDataTableSpec();
        DatabasePortObjectSpec dbSpec = new DatabasePortObjectSpec(
                outSpec, conn.createConnectionModel());
        DatabasePortObject outObj = new DatabasePortObject(dbSpec);
        return new PortObject[]{outObj};
    }
    
    private String createQuery(final String query, final String tableID) {
        return m_query.getStringValue().replaceAll(
                TABLE_PLACE_HOLDER, "(" + query + ") " + tableID);
    }
        
}
