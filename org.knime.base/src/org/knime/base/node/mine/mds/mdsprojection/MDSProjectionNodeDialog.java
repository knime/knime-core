/*
 * ------------------------------------------------------------------ *
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
