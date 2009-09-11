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

package org.knime.base.node.mine.pca;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * Port model object transporting the pca transformation.
 * 
 * @author uwe, University of Konstanz
 */
public class PCAModelPortObject extends AbstractSimplePortObject {

    private static final String EIGENVECTOR_ROW_KEYPREFIX = "eigenvectorRow";

    private static final String EIGENVALUES_KEY = "eigenvalues";

    private static final String COLUMN_NAMES_KEY = "columnNames";

    private static final String CENTER_KEY = "center";

    /**
     * Define port type of objects of this class when used as PortObjects.
     */
    public static final PortType TYPE = new PortType(PCAModelPortObject.class);

    private String[] m_inputColumnNames;

    private double[] m_center;

    private double[][] m_eigenVectors;

    private double[] m_eigenvalues;

    /**
     * empty constructor.
     */
    public PCAModelPortObject() {
        //
    }

    /**
     * construct port model object with values.
     * 
     * @param eigenVectors eigenvectors of pca matrix
     * @param eigenvalues eigenvalues of pca matrix
     * @param inputColumnNames names of input columns
     * @param center center of original data (data must be centered)
     */
    public PCAModelPortObject(final double[][] eigenVectors,
            final double[] eigenvalues, final String[] inputColumnNames,
            final double[] center) {

        m_eigenVectors = eigenVectors;
        m_eigenvalues = eigenvalues;
        m_inputColumnNames = inputColumnNames;
        m_center = center;

    }

    /**
     * get center of input data (for centering test data).
     * 
     * @return center
     */
    public double[] getCenter() {
        return m_center;
    }

    /**
     * get names of input columns.
     * 
     * @return names of input columns
     */
    public String[] getInputColumnNames() {
        return m_inputColumnNames;
    }

    /**
     * @return eigenvalues of pca matrix
     */
    public double[] getEigenvalues() {
        return m_eigenvalues;
    }

    /**
     * @return eigenvectors of pca matrix
     */
    public double[][] getEigenVectors() {
        return m_eigenVectors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getSpec() {

        final PCAModelPortObjectSpec spec =
                new PCAModelPortObjectSpec(m_inputColumnNames);
        if (m_eigenvalues != null) {
            spec.setEigenValues(m_eigenvalues);
        }
        return spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {

        return m_eigenvalues.length + " principal components";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        final String description = "<html>" + getSummary() + "</html>";

        final JLabel label = new JLabel(description);
        label.setName("PCA port");
        return new JComponent[]{label};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws InvalidSettingsException,
            CanceledExecutionException {
        m_center = model.getDoubleArray(CENTER_KEY);
        m_inputColumnNames = model.getStringArray(COLUMN_NAMES_KEY);
        m_eigenvalues = model.getDoubleArray(EIGENVALUES_KEY);
        m_eigenVectors = new double[m_eigenvalues.length][];
        for (int i = 0; i < m_eigenVectors.length; i++) {
            m_eigenVectors[i] =
                    model.getDoubleArray(EIGENVECTOR_ROW_KEYPREFIX + i);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        model.addDoubleArray(CENTER_KEY, m_center);
        model.addStringArray(COLUMN_NAMES_KEY, m_inputColumnNames);
        model.addDoubleArray(EIGENVALUES_KEY, m_eigenvalues);
        for (int i = 0; i < m_eigenVectors.length; i++) {
            model.addDoubleArray(EIGENVECTOR_ROW_KEYPREFIX + i,
                    m_eigenVectors[i]);
        }
    }

}
