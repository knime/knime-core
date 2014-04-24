/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
import java.util.Arrays;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.DataTableSpecView;

/**
 * Class used as database port object holding a {@link DataTableSpec}
 * and a <code>ModelContentRO</code> to create a database connection.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabasePortObjectSpec extends DatabaseConnectionPortObjectSpec {
    private static class Serializer extends ConnectionSpecSerializer {
        /**
         * {@inheritDoc}
         */
        @Override
        public DatabasePortObjectSpec loadPortObjectSpec(final PortObjectSpecZipInputStream in) throws IOException {
            ModelContentRO connSpecContent = super.loadModelContent(in);

            ZipEntry ze = in.getNextEntry();
            if (!ze.getName().equals(KEY_SPEC)) {
                throw new IOException("Key \"" + ze.getName() + "\" does not " + " match expected zip entry name \""
                    + KEY_SPEC + "\".");
            }
            ModelContentRO specModel = ModelContent.loadFromXML(new NonClosableInputStream.Zip(in));
            try {
                DataTableSpec spec = DataTableSpec.load(specModel);
                return new DatabasePortObjectSpec(spec, connSpecContent);
            } catch (InvalidSettingsException ise) {
                throw new IOException(ise);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void savePortObjectSpec(final DatabaseConnectionPortObjectSpec portObjectSpec,
            final PortObjectSpecZipOutputStream out) throws IOException {
            super.savePortObjectSpec(portObjectSpec, out);

            ModelContent specModel = new ModelContent(KEY_SPEC);
            ((DatabasePortObjectSpec) portObjectSpec).m_spec.save(specModel);
            out.putNextEntry(new ZipEntry(KEY_SPEC));
            specModel.saveToXML(new NonClosableOutputStream.Zip(out));
        }
    }

    private final DataTableSpec m_spec;

    /**
     * Creates a new database port object spec.
     * @param spec underlying table spec
     * @param conn connection model
     */
    public DatabasePortObjectSpec(final DataTableSpec spec,
            final ModelContentRO conn) {
        super(conn);
        if (spec == null) {
            throw new IllegalArgumentException("DataTableSpec must not be null.");
        }
        m_spec = spec;
    }

    /**
     * @return underlying table spec
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * @return connection model
     * @deprecated use {@link #getConnectionSettings(CredentialsProvider)} instead
     */
    @Override
    @Deprecated
    public ModelContentRO getConnectionModel() {
        return super.getConnectionModel();
    }

    /**
     * {@inheritDoc}
     * @since 2.10
     */
    @Override
    public DatabaseQueryConnectionSettings getConnectionSettings(final CredentialsProvider credProvider)
        throws InvalidSettingsException {
        return new DatabaseQueryConnectionSettings(super.getConnectionModel(), credProvider);
    }


    /**
     * Serializer used to save <code>DatabasePortObjectSpec</code>.
     * @return a new database spec serializer
     */
    public static PortObjectSpecSerializer<DatabaseConnectionPortObjectSpec>
            getPortObjectSpecSerializer() {
        return new Serializer();
    }

    private static final String KEY_SPEC = "spec_xml.zip";

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        JComponent[] views = super.getViews();
        views = Arrays.copyOf(views, views.length + 1);
        views[views.length - 1] = new DataTableSpecView(m_spec);
        return views;
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
        return super.equals(obj) && m_spec.equals(dbSpec.m_spec);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return super.hashCode() ^ m_spec.hashCode();
    }
}
