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
package org.knime.core.node.port.database;

import java.io.IOException;
import java.util.zip.ZipEntry;

import org.knime.core.data.DataTable;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;

/**
 * Class used as database port object holding a {@link BufferedDataTable}
 * and a <code>ModelContentRO</code> to create a database connection.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabasePortObject implements PortObject {
    
    private final DataTable m_data;
    
    private final ModelContentRO m_conn;

    /**
     * Database port type formed <code>PortObjectSpec.class</code> and 
     * <code>PortObject.class</code> from this class.
     */
    public static final PortType TYPE = new PortType(DatabasePortObject.class);
    
    /** {@inheritDoc} */
    @Override
    public DatabasePortObjectSpec getSpec() {
        return new DatabasePortObjectSpec(
                    m_data.getDataTableSpec(), m_conn);
    }
    
    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return "No. of columns: " + m_data.getDataTableSpec().getNumColumns();
    }
    
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
                    "Database connection must not be null!");
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
            public void savePortObject(final DatabasePortObject portObject,
                    final PortObjectZipOutputStream out, 
                    final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                save(out, exec, portObject);

            }
            
            /** {@inheritDoc} */
            @Override
            public DatabasePortObject loadPortObject(
                    final PortObjectZipInputStream in, 
                    final PortObjectSpec spec, 
                    final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                return load(in, (DatabasePortObjectSpec) spec);
            }
        };
    }
    
    private static final String KEY_PREVIEW_TABLE = "preview_table.zip";
    
    private static DatabasePortObject load(
            final PortObjectZipInputStream in, 
            final DatabasePortObjectSpec spec) throws IOException {
        ZipEntry ze = in.getNextEntry();
        if (!ze.getName().equals(KEY_PREVIEW_TABLE)) {
            throw new IOException("Key \"" + ze.getName() + "\" does not "
                    + " match expected zip entry name \"" 
                    + KEY_PREVIEW_TABLE + "\".");
        }
        ContainerTable data = BufferedDataContainer.readFromStream(
                new NonClosableInputStream.Zip(in));
        return new DatabasePortObject(data, spec.getConnectionModel());
    }
    
    private static void save(final PortObjectZipOutputStream out, 
            final ExecutionMonitor em, final DatabasePortObject portObject) 
            throws IOException, CanceledExecutionException {
        out.putNextEntry(new ZipEntry(KEY_PREVIEW_TABLE));
        BufferedDataContainer.writeToStream(portObject.m_data, 
                new NonClosableOutputStream.Zip(out), em);
    }
    
    
}
