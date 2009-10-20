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
 * ---------------------------------------------------------------------
 * 
 * History
 *   07.03.2008 (Kilian Thiel): created
 */
package org.knime.base.node.mine.mds.mdsprojection;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.mine.mds.MDSNodeDialog;
import org.knime.base.node.mine.mds.distances.DistanceManagerFactory;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * The dialog of the MDS projection node.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class MDSProjectionNodeDialog extends DefaultNodeSettingsPane {
    
    /**
     * @return Creates and returns a new instance of 
     * <code>SettingsModelFilterString</code> containing the names of the
     * fixed mds columns to use.
     */
    public static SettingsModelFilterString getFixedColumnModel() {
        return new SettingsModelFilterString(
                MDSProjectionConfigKeys.CFGKEY_FIXED_COLS);
    }

    /**
     * @return Creates and returns a new instance of 
     * <code>SettingsModelBoolean</code> containing the settings if only
     * a projection by the usage of the fixed data points have to be applied or
     * if a a complete mds have to be done as well. 
     */
    public static SettingsModelBoolean getProjectOnlyModel() {
        return new SettingsModelBoolean(
                MDSProjectionConfigKeys.CFGKEY_PROJECT_ONLY, 
                MDSProjectionNodeModel.DEF_PROJECT_ONLY);
    }
    
    private SettingsModelIntegerBounded m_rowsModel;
    
    private SettingsModelBoolean m_useRowsModel;
    
    /**
     * Creates a new instance of <code>MDSNodeDialog</code>.
     */
    @SuppressWarnings("unchecked")
    public MDSProjectionNodeDialog() {
        
        createNewGroup("Number of rows");
        
        m_rowsModel = MDSNodeDialog.getRowsModel();
        addDialogComponent(new DialogComponentNumber(m_rowsModel,
                "Number of rows to use: " , 100));
        
        m_useRowsModel = MDSNodeDialog.getUseMaxRowsModel();
        addDialogComponent(new DialogComponentBoolean(m_useRowsModel,
                "Use all"));
       
        m_useRowsModel.addChangeListener(new CheckBoxChangeListener());
        
        closeCurrentGroup();
        
        
        createNewGroup("MDS settings");
        
        addDialogComponent(new DialogComponentNumber(
                MDSNodeDialog.getSeedModel(), "Random seed: ", 1000));
        
        addDialogComponent(new DialogComponentNumber(
                MDSNodeDialog.getEpochModel(), "Epochs: ", 10));
        
        addDialogComponent(new DialogComponentNumber(
                MDSNodeDialog.getOutputDimModel(), "Output dimensions: ", 1));
        
        addDialogComponent(new DialogComponentNumber(
                MDSNodeDialog.getLearnrateModel(), "Learningrate: ", 0.1));
        
        List<String> distanceMetric = new ArrayList<String>();
        distanceMetric.add(DistanceManagerFactory.EUCLIDEAN_DIST);
        distanceMetric.add(DistanceManagerFactory.MANHATTAN_DIST);
        //distanceMetric.add(DistanceManagerFactory.COS_DIST);
        
        addDialogComponent(new DialogComponentStringSelection(
                MDSNodeDialog.getDistanceModel(), "Distance metric: ", 
                distanceMetric));
        
        addDialogComponent(new DialogComponentBoolean(
                MDSProjectionNodeDialog.getProjectOnlyModel(), 
                "Project only"));
        
        closeCurrentGroup();
        
        
        createNewTab("Input data");
        
        createNewGroup("Data to project");
        
        addDialogComponent(new DialogComponentColumnFilter(
                MDSNodeDialog.getColumnModel(), 1, FuzzyIntervalValue.class, 
                DoubleValue.class));
        
        closeCurrentGroup();
        
        
        createNewTab("Fixed data");
        
        createNewGroup("Fixed data");
        
        // add fixed column selection panel
        addDialogComponent(new DialogComponentColumnFilter(
                MDSProjectionNodeDialog.getFixedColumnModel(), 0, 
                DoubleValue.class));
        
        closeCurrentGroup();
        
        checkUncheck();
    }
    
    /**
     * 
     * @author Kilian Thiel, University of Konstanz
     */
    class CheckBoxChangeListener implements ChangeListener {

        /**
         * {@inheritDoc}
         */
        public void stateChanged(final ChangeEvent e) {
            checkUncheck();
        }
    }
    
    private void checkUncheck() {
        if (m_useRowsModel.getBooleanValue()) {
            m_rowsModel.setEnabled(false);
        } else {
            m_rowsModel.setEnabled(true);
        }
    }
}
