/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBRowFilterNodeModel extends DBNodeModel {
    
    private final SettingsModelString m_column =
            DBRowFilterNodeDialogPane.createColumnModel();

    private final SettingsModelString m_operator =
            DBRowFilterNodeDialogPane.createOperatorModel();

    private final SettingsModelString m_value =
            DBRowFilterNodeDialogPane.createValueModel();
    
    /**
     * Creates a new database reader.
     */
    DBRowFilterNodeModel() {
        super(new PortType[]{DatabasePortObject.TYPE}, 
                new PortType[]{DatabasePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_column.saveSettingsTo(settings);
        m_operator.saveSettingsTo(settings);
        m_value.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        m_column.validateSettings(settings);
        m_operator.validateSettings(settings);
        m_value.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_column.loadSettingsFrom(settings);
        m_operator.loadSettingsFrom(settings);
        m_value.loadSettingsFrom(settings);
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
                dbObj.getConnectionModel());
        String newQuery = createQuery(conn.getQuery(), getTableID());
        conn = createDBQueryConnection(dbObj.getSpec(), newQuery);
        DatabasePortObject outObj = new DatabasePortObject(
                new DatabasePortObjectSpec(dbObj.getSpec().getDataTableSpec(),
                        conn.createConnectionModel()));
        return new PortObject[]{outObj};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DatabasePortObjectSpec spec = (DatabasePortObjectSpec) inSpecs[0];
        if (!spec.getDataTableSpec().containsName(m_column.getStringValue())) {
            throw new InvalidSettingsException("Can't filter according to "
                    + "selected column \"" + m_column.getStringValue() + "\".");
        }
        DatabaseQueryConnectionSettings conn = 
            new DatabaseQueryConnectionSettings(
                spec.getConnectionModel());
        String newQuery = createQuery(conn.getQuery(), getTableID());
        conn = createDBQueryConnection(spec, newQuery);
        return new PortObjectSpec[]{new DatabasePortObjectSpec(
                spec.getDataTableSpec(), conn.createConnectionModel())};
    }
    
    private String createQuery(final String query, final String tableID) {
        String buf = m_column.getStringValue()
            + " " + m_operator.getStringValue()
            + " " + m_value.getStringValue();
        return "SELECT * FROM (" + query + ") " + tableID + " WHERE " 
                + buf.toString(); 
    }
        
}
