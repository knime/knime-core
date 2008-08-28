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

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;

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
            public DatabasePortObjectSpec loadPortObjectSpec(
                    final PortObjectSpecZipInputStream in) throws IOException {
                return load(in);
            }

            @Override
            public void savePortObjectSpec(
                    final DatabasePortObjectSpec portObjectSpec, 
                    final PortObjectSpecZipOutputStream out) 
                    throws IOException {
                save(out, portObjectSpec);
            }
        };
    }
    
    private static final String KEY_DATABASE_CONNECTION = 
        "database_connection.zip";
    
    private static final String KEY_SPEC = "spec_xml.zip";
    
    private static DatabasePortObjectSpec load(
            final PortObjectSpecZipInputStream is) throws IOException {
        ZipEntry ze = is.getNextEntry();
        if (!ze.getName().equals(KEY_DATABASE_CONNECTION)) {
            throw new IOException("Key \"" + ze.getName() + "\" does not "
                    + " match expected zip entry name \"" 
                    + KEY_DATABASE_CONNECTION + "\".");
        }
        ModelContentRO conn = ModelContent.loadFromXML(
                new NonClosableInputStream.Zip(is));
        is.getNextEntry();
        ze = is.getNextEntry();
        if (!ze.getName().equals(KEY_SPEC)) {
            throw new IOException("Key \"" + ze.getName() + "\" does not "
                    + " match expected zip entry name \"" 
                    + KEY_SPEC + "\".");
        }
        ModelContentRO specModel = ModelContent.loadFromXML(
                new NonClosableInputStream.Zip(is));
        DataTableSpec spec = null;
        try {
            spec = DataTableSpec.load(specModel);
        } catch (InvalidSettingsException ise) {
            throw new IOException(ise);
        }
        return new DatabasePortObjectSpec(spec, conn);
    }
    
    private static void save(final PortObjectSpecZipOutputStream os, 
            final DatabasePortObjectSpec portObjectSpec) throws IOException {
        ZipEntry ze = new ZipEntry(KEY_DATABASE_CONNECTION);
        os.putNextEntry(ze);
        portObjectSpec.m_conn.saveToXML(new NonClosableOutputStream.Zip(os));
        ze.clone();
        ze = new ZipEntry(KEY_SPEC);
        ModelContent specModel = new ModelContent(KEY_SPEC);
        portObjectSpec.m_spec.save(specModel);
        os.putNextEntry(new ZipEntry(KEY_SPEC));
        specModel.saveToXML(new NonClosableOutputStream.Zip(os));
        ze.clone();
        os.close();
    }
    
}
