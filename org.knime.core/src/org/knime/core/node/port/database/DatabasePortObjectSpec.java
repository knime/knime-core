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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   13.02.2008 (gabriel): created
 */
package org.knime.core.node.port.database;

import java.io.IOException;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.workflow.DataTableSpecView;
import org.knime.core.node.workflow.ModelContentOutPortView;

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
        if (spec == null) {
            throw new NullPointerException("DataTableSpec must not be null.");
        }
        if (conn == null) {
            throw new NullPointerException(
                    "Database Connection must not be null.");
        }
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
        os.close();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        JComponent connPanel = new ModelContentOutPortView(m_conn);
        connPanel.setName("Connection");
        JComponent specPanel = new DataTableSpecView(m_spec);
        return new JComponent[]{specPanel, connPanel};
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DatabasePortObjectSpec)) {
            return false;
        }
        DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec) obj;
        return m_conn.equals(dbSpec.m_conn) && m_spec.equals(dbSpec.m_spec); 
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_conn.hashCode() ^ m_spec.hashCode();
    }
    
}
