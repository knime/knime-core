/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * History
 *   04.10.2006 (uwe): created
 */

package org.knime.base.node.mine.pca;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;

/**
 * Spec for pca model port object.
 * 
 * @author uwe, University of Konstanz
 */
public class PCAModelPortObjectSpec implements PortObjectSpec {

    private final String[] m_columnNames;

    private double[] m_eigenvalues;

    /**
     * create object spec.
     * 
     * @param columnNames names of input columns
     */
    public PCAModelPortObjectSpec(final String[] columnNames) {
        m_columnNames = columnNames;

    }

    /**
     * 
     * @return names of input columns
     */
    public String[] getColumnNames() {
        return m_columnNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        // final String description =
        // "<html>contains "
        // + (m_eigenvalues != null ? m_eigenvalues.length + " "
        // : "") + "principal components" + "</html>";
        // ;
        // final JLabel label = new JLabel(description);
        // label.setName("PCA model port");
        // return new JComponent[]{label};
        return new JComponent[]{};
    }

    /**
     * Method required by the interface {@link PortObjectSpec}. Not meant for
     * public use.
     * 
     * @return A new serializer responsible for loading/saving.
     */
    public static PortObjectSpecSerializer<PCAModelPortObjectSpec> getPortObjectSpecSerializer() {

        return new PortObjectSpecSerializer<PCAModelPortObjectSpec>() {

            @Override
            public PCAModelPortObjectSpec loadPortObjectSpec(
                    final PortObjectSpecZipInputStream in) throws IOException {
                in.getNextEntry();
                final ObjectInputStream ois = new ObjectInputStream(in);
                try {
                    final String[] columnNames = (String[])ois.readObject();
                    return new PCAModelPortObjectSpec(columnNames);
                } catch (final ClassNotFoundException e) {
                    throw new IOException(e.getMessage(), e.getCause());
                }

            }

            @Override
            public void savePortObjectSpec(
                    final PCAModelPortObjectSpec portObjectSpec,
                    final PortObjectSpecZipOutputStream out) throws IOException {
                out.putNextEntry(new ZipEntry("content.dat"));
                new ObjectOutputStream(out).writeObject(portObjectSpec
                        .getColumnNames());
            }

        };
    }

    /**
     * set eigenvalues of existing pca.
     * 
     * @param eigenvalues
     */
    public void setEigenValues(final double[] eigenvalues) {
        m_eigenvalues = eigenvalues;

    }

    /**
     * @return the eigenvalues
     */
    public double[] getEigenValues() {
        return m_eigenvalues;
    }

}
