/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.cluster.kmeans;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * Dialog for
 * {@link ClusterNodeModel} - allows
 * to adjust number of clusters and other properties.
 * 
 * @author Michael Berthold, Konstanz University
 */
public class ClusterNodeDialog extends NodeDialogPane {
    // private members holding new values
    private final JTextField m_nrClustersTextField;

    private final JTextField m_maxNrIterationsTextField;

    /**
     * Constructor - set name of k-means cluster node. Also initialize special
     * property panel holding the variables that can be adjusted by the user.
     */
    ClusterNodeDialog() {
        super();
        // create panel content for special property-tab
        Box clusterPropBox = Box.createVerticalBox();
        Box box1 = Box.createHorizontalBox();
        JLabel nrClustersLabel = new JLabel("number of clusters: ");
        nrClustersLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        box1.add(nrClustersLabel);
        m_nrClustersTextField = new JTextField("____");
        m_nrClustersTextField.setAlignmentX(Component.LEFT_ALIGNMENT);
        box1.add(m_nrClustersTextField);
        box1.add(Box.createHorizontalGlue());
        Box box2 = Box.createHorizontalBox();
        JLabel maxNrIterationsLabel = new JLabel("max. number of iterations: ");
        box2.add(maxNrIterationsLabel);
        m_maxNrIterationsTextField = new JTextField("____");
        box2.add(m_maxNrIterationsTextField);
        box2.add(Box.createHorizontalGlue());
        clusterPropBox.add(box1);
        clusterPropBox.add(box2);
        clusterPropBox.add(Box.createVerticalGlue());
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        clusterPropBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        clusterPropBox.setAlignmentY(Component.TOP_ALIGNMENT);
        panel.add(clusterPropBox);
        super.addTab("K-Means Properties", panel);
    }

    /**
     * Update content of dialog fields according to new settings provided.
     * 
     * @param config the config to write into the current settings
     * @param specs the spec for each input
     * @throws NotConfigurableException never
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO config,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // set content of fields in special property-tab
        int nrClusters = config.getInt("nrClusters", 0);
        m_nrClustersTextField.setText("" + nrClusters);
        int maxIterations = config.getInt("maxNrIterations", 0);
        m_maxNrIterationsTextField.setText("" + maxIterations);
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
     */
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
}
