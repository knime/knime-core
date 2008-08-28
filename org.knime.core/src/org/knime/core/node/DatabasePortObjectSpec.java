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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Class used as database port object holding a {@link DataTableSpec}
 * and a <code>ModelContentRO</code> to create a database connection.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabasePortObjectSpec implements PortObjectSpec {

    private final DataTableSpec m_spec;
    
    private final ModelContentRO m_conn;
    
    /**
     * Creates a new database port object spec.
     * @param spec underlying table spec
     * @param conn connection model
     */
    public DatabasePortObjectSpec(final DataTableSpec spec, 
            final ModelContentRO conn) {
        m_spec = spec;
        m_conn = conn;
    }
    
    /**
     * @return underlying table spec
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }
    
    /**
     * @return connection model
     */
    public ModelContentRO getConnectionModel() {
        return m_conn;
    }
    
    /**
     * Serializer used to save <code>DatabasePortObjectSpec</code>.
     * @return a new database spec serializer
     */
    public static PortObjectSpecSerializer<DatabasePortObjectSpec> 
            getPortObjectSpecSerializer() {
        return new PortObjectSpecSerializer<DatabasePortObjectSpec>() {

            @Override
            protected DatabasePortObjectSpec loadPortObjectSpec(
                    final File directory) throws IOException {
                return load(directory);
            }

            @Override
            protected void savePortObjectSpec(
                    final DatabasePortObjectSpec portObjectSpec, 
                    final File directory) throws IOException {
                save(directory, portObjectSpec);
            }
        };
    }
    
    private static DatabasePortObjectSpec load(final File dir)
            throws IOException {
        File connFile = new File(dir, "db_connection.xml");
        ModelContentRO conn = ModelContent.loadFromXML(
                new FileInputStream(connFile));
        File specFile = new File(dir, "spec.xml");
        ModelContentRO specModel = ModelContent.loadFromXML(
                new FileInputStream(specFile));
        DataTableSpec spec = null;
        try {
            spec = DataTableSpec.load(specModel);
        } catch (InvalidSettingsException ise) {
            throw new IOException(ise);
        }
        return new DatabasePortObjectSpec(spec, conn);
    }
    
    private static void save(final File dir, 
            final DatabasePortObjectSpec portObjectSpec) throws IOException {
        File connFile = new File(dir, "db_connection.xml");
        portObjectSpec.m_conn.saveToXML(new FileOutputStream(connFile));
        ModelContent specModel = new ModelContent("spec.xml");
        portObjectSpec.m_spec.save(specModel);
        File specFile = new File(dir, "spec.xml");
        specModel.saveToXML(new FileOutputStream(specFile));
    }
    
}
