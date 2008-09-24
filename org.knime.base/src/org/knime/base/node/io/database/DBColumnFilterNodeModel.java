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

import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
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
final class DBColumnFilterNodeModel extends DBNodeModel {
    
    private final SettingsModelFilterString m_filter
         = DBColumnFilterNodeDialogPane.createColumnFilterModel();
    
    /**
     * Creates a new database reader.
     */
    DBColumnFilterNodeModel() {
            super(new PortType[]{DatabasePortObject.TYPE}, 
                    new PortType[]{DatabasePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_filter.saveSettingsTo(settings);
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_filter.validateSettings(settings);
        super.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_filter.loadSettingsFrom(settings);
        super.loadValidatedSettingsFrom(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) 
            throws CanceledExecutionException, Exception {
        DatabasePortObjectSpec spec = ((DatabasePortObject)inData[0]).getSpec();
        DatabaseQueryConnectionSettings conn = 
            new DatabaseQueryConnectionSettings(spec.getConnectionModel(),
                    getNumCachedRows());
        String newQuery = createQuery(conn.getQuery(), getTableID());
        conn = createDBQueryConnection(spec, newQuery);
        ColumnRearranger colre = new ColumnRearranger(spec.getDataTableSpec());
        colre.keepOnly(m_filter.getIncludeList().toArray(new String[0]));
        DatabasePortObjectSpec outSpec = new DatabasePortObjectSpec(
                colre.createSpec(), conn.createConnectionModel());
        DatabasePortObject outObj = new DatabasePortObject(outSpec);
        return new PortObject[]{outObj};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DatabasePortObjectSpec spec = (DatabasePortObjectSpec) inSpecs[0];
        StringBuilder buf = new StringBuilder();
        for (String column : m_filter.getIncludeList()) {
            if (!spec.getDataTableSpec().containsName(column)) {
                buf.append("\"" + column + "\" ");
            }
        }
        if (buf.length() > 0) {
            throw new InvalidSettingsException("Not all columns available in "
                    + "input spec: " + buf.toString());
        }
        DatabaseQueryConnectionSettings conn = 
            new DatabaseQueryConnectionSettings(
                spec.getConnectionModel(), getNumCachedRows());
        String newQuery = createQuery(conn.getQuery(), getTableID());
        conn = createDBQueryConnection(spec, newQuery);
        ColumnRearranger colre = new ColumnRearranger(spec.getDataTableSpec());
        colre.keepOnly(m_filter.getIncludeList().toArray(new String[0]));
        return new PortObjectSpec[]{new DatabasePortObjectSpec(
                colre.createSpec(), conn.createConnectionModel())};
    }
    
    private String createQuery(final String query, final String tableID) {
        StringBuilder buf = new StringBuilder();
        for (String s : m_filter.getIncludeList()) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(s);
        }
        return "SELECT " + buf.toString() + " FROM (" + query + ") " + tableID;
    }
        
}
