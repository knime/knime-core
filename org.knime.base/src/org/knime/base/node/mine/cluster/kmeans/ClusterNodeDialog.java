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
