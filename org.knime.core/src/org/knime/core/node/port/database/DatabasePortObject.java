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

import javax.swing.JComponent;

import org.knime.core.data.DataTable;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.BufferedDataTableView;

/**
 * Class used as database port object holding a {@link BufferedDataTable}
 * and a <code>ModelContentRO</code> to create a database connection.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabasePortObject implements PortObject {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            DatabasePortObject.class);
    
    private DataTable m_data;
    
    private final DatabasePortObjectSpec m_spec;

    /**
     * Database port type formed <code>PortObjectSpec.class</code> and 
     * <code>PortObject.class</code> from this class.
     */
    public static final PortType TYPE = new PortType(DatabasePortObject.class);
    
    /** {@inheritDoc} */
    @Override
    public DatabasePortObjectSpec getSpec() {
        return m_spec;
    }
    
    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return "No. of columns: " + m_spec.getDataTableSpec().getNumColumns();
    }
    
    /**
     * Creates a new database port object.
     * @param spec database port object spec
     * @throws NullPointerException if one of the arguments is null
     */
    public DatabasePortObject(final DatabasePortObjectSpec spec) {
        this(spec, null);
    }
    
    /**
     *
     */
    private DatabasePortObject(final DatabasePortObjectSpec spec,
            final DataTable data) {
        if (spec == null) {
            throw new NullPointerException(
                    "DatabasePortObjectSpec must not be null!");
        }
        m_spec = spec;
        m_data = data;
    }
    
    /**
     * @return underlying data
     */
    private DataTable getDataTable() {
        if (m_data == null) {
            try {
                DatabaseReaderConnection load = new DatabaseReaderConnection(
                    new DatabaseQueryConnectionSettings(
                           m_spec.getConnectionModel()));
                m_data = load.createTable();
            } catch (Throwable t) {
                LOGGER.error("Could not fetch data from database, reason: "
                        + t.getMessage(), t);
                m_data = null;
            }
        }
        return m_data;
    }
    
    /**
     * @return connection model
     */
    public ModelContentRO getConnectionModel() {
        return m_spec.getConnectionModel();
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
                return load(in, (DatabasePortObjectSpec) spec, exec);
            }
        };
    }
    
    private static final String KEY_PREVIEW_TABLE = "preview_table.zip";
    
    private static DatabasePortObject load(
            final PortObjectZipInputStream in, 
            final DatabasePortObjectSpec spec,
            final ExecutionMonitor exec) throws IOException {
        assert exec == exec;
        ZipEntry ze = in.getNextEntry();
        if (ze != null && ze.getName().equals(KEY_PREVIEW_TABLE)) {
            ContainerTable data = DataContainer.readFromStream(
                    new NonClosableInputStream.Zip(in));
            return new DatabasePortObject(spec, data);
        }
        return new DatabasePortObject(spec, null);
    }
    
    private static void save(final PortObjectZipOutputStream out, 
            final ExecutionMonitor em, final DatabasePortObject portObject) 
            throws IOException, CanceledExecutionException {
        if (portObject.m_data != null) {
            out.putNextEntry(new ZipEntry(KEY_PREVIEW_TABLE));
            BufferedDataContainer.writeToStream(portObject.m_data, 
                    new NonClosableOutputStream.Zip(out), em);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        JComponent[] specPanels = m_spec.getViews();
        JComponent[] panels = new JComponent[specPanels.length + 1];
        panels[0] = new BufferedDataTableView(getDataTable());
        for (int i = 1; i < panels.length; i++) {
            panels[i] = specPanels[i - 1];
        }
        return panels;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        m_data = null;
    }
    
}
