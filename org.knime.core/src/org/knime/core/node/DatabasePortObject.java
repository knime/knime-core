/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   13.02.2008 (gabriel): created
 */
package org.knime.core.node;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTable;
import org.knime.core.data.container.ContainerTable;

/**
 * Class used as database port object holding a {@link BufferedDataTable}
 * and a <code>ModelContentRO</code> to create a database connection.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabasePortObject implements PortObject {
    

    /**
     * Database port type formed <code>PortObjectSpec.class</code> and 
     * <code>PortObject.class</code> from this class.
     */
    public static final PortType TYPE = new PortType(DatabasePortObject.class);
    
    /**
     * {@inheritDoc}
     */
    public DatabasePortObjectSpec getSpec() {
        return new DatabasePortObjectSpec(
                    m_data.getDataTableSpec(), m_conn);
    }

    private final DataTable m_data;
    
    private final ModelContentRO m_conn;
    
    /**
     * Creates a new database port object.
     * @param data underlying data
     * @param conn connection model
     * @throws NullPointerException if one of the arguments is null
     */
    public DatabasePortObject(final DataTable data, 
            final ModelContentRO conn) {
        if (data == null) {
            throw new NullPointerException("DataTable must not be null!");
        }
        if (conn == null) {
            throw new NullPointerException(
                    "Datbase connection must not be null!");
        }
        m_data = data;
        m_conn = conn;
    }
    
    /**
     * @return underlying data
     */
    public DataTable getDataTable() {
        return m_data;
    }
    
    /**
     * @return connection model
     */
    public ModelContentRO getConnectionModel() {
        return m_conn;
    }
    
    /**
     * Serializer used to save <code>DatabasePortObject</code>.
     * @return a new database port object serializer
     */
    public static PortObjectSerializer<DatabasePortObject> 
            getPortObjectSerializer() {
        return new PortObjectSerializer<DatabasePortObject>() {
            /** {@inheritDoc} */
            @Override
            protected void savePortObject(final DatabasePortObject portObject,
                    final File directory, final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                save(directory, exec, portObject);

            }
            
            /** {@inheritDoc} */
            @Override
            protected DatabasePortObject loadPortObject(final File directory,
                    final PortObjectSpec spec, final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                return load(directory, (DatabasePortObjectSpec) spec);
            }

        };
    }
    
    private static DatabasePortObject load(
            final File dir, final DatabasePortObjectSpec spec) 
                throws IOException {
        File dataFile = new File(dir, "data.zip");
        ContainerTable data = BufferedDataContainer.readFromZip(dataFile);
        return new DatabasePortObject(data, spec.getConnectionModel());
    }
    
    private static void save(final File dir, final ExecutionMonitor em,
            final DatabasePortObject portObject) 
            throws IOException, CanceledExecutionException {
        File dataFile = new File(dir, "data.zip");
        BufferedDataContainer.writeToZip(portObject.m_data, dataFile, em);
    }
    
    
}
