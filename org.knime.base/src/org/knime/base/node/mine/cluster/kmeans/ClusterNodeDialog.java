/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.cluster.kmeans;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;


/**
 * Dialog for
 * {@link ClusterNodeModel} - allows
 * to adjust number of clusters and other properties.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class ClusterNodeDialog extends DefaultNodeSettingsPane {
    // private members holding new values
//    private final JTextField m_nrClustersTextField;
//
//    private final JTextField m_maxNrIterationsTextField;
//    
//    private final ColumnFilterPanel m_columnFilter;
    
    private DialogComponentNumber m_nrOfClusters;
    
    private DialogComponentNumber m_maxNrOfIterations;
    
    private DialogComponentColumnFilter m_columnFilter;

    /**
     * Constructor - set name of k-means cluster node. Also initialize special
     * property panel holding the variables that can be adjusted by the user.
     */
    @SuppressWarnings("unchecked")
    ClusterNodeDialog() {
        super();
        SettingsModelIntegerBounded smib = new SettingsModelIntegerBounded(
                ClusterNodeModel.CFG_NR_OF_CLUSTERS, 
                ClusterNodeModel.INITIAL_NR_CLUSTERS, 
                1, Integer.MAX_VALUE);
        m_nrOfClusters = new DialogComponentNumber(smib,
                "number of clusters: ", 1,
                createFlowVariableModel(smib));
        m_maxNrOfIterations = new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                        ClusterNodeModel.CFG_MAX_ITERATIONS,
                        ClusterNodeModel.INITIAL_MAX_ITERATIONS,
                        1, Integer.MAX_VALUE),
                        "max. number of iterations: ", 10);
        m_columnFilter = new DialogComponentColumnFilter(
                new SettingsModelFilterString(ClusterNodeModel.CFG_COLUMNS), 
                0, DoubleValue.class);
        addDialogComponent(m_nrOfClusters);
        addDialogComponent(m_maxNrOfIterations);
        addDialogComponent(m_columnFilter);
        setDefaultTabTitle("K-Means Properties");
    }

    
    
    /*
     * Update content of dialog fields according to new settings provided.
     * 
     * @param config the config to write into the current settings
     * @param specs the spec for each input
     * @throws NotConfigurableException never
     *
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO config,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // set content of fields in special property-tab
        int nrClusters = config.getInt("nrClusters", 0);
        m_nrClustersTextField.setText("" + nrClusters);
        int maxIterations = config.getInt("maxNrIterations", 0);
        m_maxNrIterationsTextField.setText("   " + maxIterations);
    }

    /**
     * Apply copies #clusters and max #iterations into settings object. Note
     * that no sanity checks are performed - the model needs to check these
     * itself and throw appropriate exceptions.
     * 
     * @param settings the object to write into the current settings
     * 
     * @throws InvalidSettingsException if settings don't make sense
     * 
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO,DataTableSpec[])
     *
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        // number of clusters:
        try {
            // parse text field and copy to settings object
            int newNrClusters = Integer.parseInt(m_nrClustersTextField
                    .getText().trim());
            settings.addInt("nrClusters", newNrClusters);
        } catch (NumberFormatException nfe) {
            throw new InvalidSettingsException("number of Clusters: not"
                    + " a number");
        }
        // maximum number of iterations:
        try {
            // parse text field and copy to settings object
            int newMaxNrIterations = Integer
                    .parseInt(m_maxNrIterationsTextField.getText().trim());
            settings.addInt("maxNrIterations", newMaxNrIterations);
        } catch (NumberFormatException nfe) {
            throw new InvalidSettingsException("max Number of Iterations:"
                    + " not a number");
        }
    }
    */
}
