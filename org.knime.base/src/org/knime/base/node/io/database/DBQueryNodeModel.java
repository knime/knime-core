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
        String newQuery = createQuery(conn.getQuery());
        conn = createDBQueryConnection(spec, newQuery);
        try {
            DatabaseReaderConnection reader = 
                new DatabaseReaderConnection(conn);
            DataTableSpec outSpec = reader.getDataTableSpec();
            DatabasePortObjectSpec dbSpec = new DatabasePortObjectSpec(
                    outSpec, conn.createConnectionModel());
            return new PortObjectSpec[]{dbSpec};
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
        String newQuery = createQuery(conn.getQuery());
        conn = createDBQueryConnection(dbObj.getSpec(),	newQuery);
        DatabaseReaderConnection load = new DatabaseReaderConnection(conn);
        DataTableSpec outSpec = load.getDataTableSpec();
        DatabasePortObjectSpec dbSpec = new DatabasePortObjectSpec(
                outSpec, conn.createConnectionModel());
        DatabasePortObject outObj = new DatabasePortObject(dbSpec);
        return new PortObject[]{outObj};
    }
    
    private String createQuery(final String query) {
        return m_query.getStringValue().replaceAll(
                TABLE_PLACE_HOLDER, "(" + query + ")");
    }
        
}
