/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * History
 *   04.10.2006 (uwe): created
 */

package org.knime.base.nodes.mining.pca;

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
