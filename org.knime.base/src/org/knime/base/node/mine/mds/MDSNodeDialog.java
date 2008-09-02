/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
package org.knime.base.node.mine.mds;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.mine.mds.distances.DistanceManagerFactory;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The dialog of the MDS node.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class MDSNodeDialog extends DefaultNodeSettingsPane {

    /**
     * @return Creates and returns a new instance of 
     * <code>SettingsModelIntegerBounded</code> containing the number of rows
     * to use.  
     */
    public static SettingsModelIntegerBounded getRowsModel() {
        return new SettingsModelIntegerBounded(MDSConfigKeys.CFGKEY_ROWS,
                MDSNodeModel.DEF_NO_ROWS, MDSNodeModel.MIN_NO_ROWS, 
                MDSNodeModel.MAX_NO_ROWS);
    }
 
    /**
     * @return Creates and returns a new instance of 
     * <code>SettingsModelBoolean</code> specifying if the number of max rows
     * have to be used or not.
     */
    public static SettingsModelBoolean getUseMaxRowsModel() {
        return new SettingsModelBoolean(MDSConfigKeys.CFGKEY_USE_ROWS,
                MDSNodeModel.DEF_USE_MAX_ROWS);
    }
    
    /**
     * @return Creates and returns a new instance of
     * <code>SettingsModelDoubleBounded</code> containing the learning rate 
     * of rows. 
     */
    public static SettingsModelDoubleBounded getLearnrateModel() {
        return new SettingsModelDoubleBounded(
                MDSConfigKeys.CFGKEY_LEARNINGRATE, 
                MDSNodeModel.DEF_LEARNINGRATE, MDSNodeModel.MIN_LEARNINGRATE,
                MDSNodeModel.MAX_LEARNINGRATE);
    }
    
    /**
     * @return Creates and returns a new instance of 
     * <code>SettingsModelIntegerBounded</code> containing the number of epochs
     * to train.
     */    
    public static SettingsModelIntegerBounded getEpochModel() {
        return new SettingsModelIntegerBounded(
                MDSConfigKeys.CFGKEY_EPOCHS, MDSNodeModel.DEF_EPOCHS, 
                MDSNodeModel.MIN_EPOCHS, MDSNodeModel.MAX_EPOCHS);
    }
    
    /**
     * @return Creates and returns a new instance of 
     * <code>SettingsModelIntegerBounded</code> containing the number of output
     * dimension to create.
     */
    public static SettingsModelIntegerBounded getOutputDimModel() {
        return new SettingsModelIntegerBounded(
                MDSConfigKeys.CFGKEY_OUTDIMS, MDSNodeModel.DEF_OUTPUTDIMS,
                MDSNodeModel.MIN_OUTPUTDIMS, MDSNodeModel.MAX_OUTPUTDIMS);
    }
    
    /**
     * @return Creates and returns a new instance of
     * <code>SettingsModelString</code> containing the distance metric to use.
     */
    public static SettingsModelString getDistanceModel() {
        return new SettingsModelString(MDSConfigKeys.CFGKEY_DISTANCE,
                MDSNodeModel.DEF_DISTANCE);
    }
    
    /**
     * @return Creates and returns a new instance of 
     * <code>SettingsModelFilterString</code> containing the names of the
     * filered columns to use.
     */
    public static SettingsModelFilterString getColumnModel() {
        return new SettingsModelFilterString(MDSConfigKeys.CFGKEY_COLS);
    }
    
    /**
     * @return Creates and returns a new instance of 
     * <code>SettingsModelDouble</code> containing the random seed to use.
     */
    public static SettingsModelIntegerBounded getSeedModel() {
        return new SettingsModelIntegerBounded(MDSConfigKeys.CFGKEY_SEED,
                MDSManager.DEFAULT_SEED, MDSManager.MIN_SEED, 
                MDSManager.MAX_SEED);
    }
    
    
    private SettingsModelIntegerBounded m_rowsModel;
    
    private SettingsModelBoolean m_useRowsModel;
    
    /**
     * Creates a new instance of <code>MDSNodeDialog</code>.
     */
    @SuppressWarnings("unchecked")
    public MDSNodeDialog() {
        
        createNewGroup("Number of rows");
        
        m_rowsModel = getRowsModel();
        addDialogComponent(new DialogComponentNumber(m_rowsModel,
                "Number of rows to use: " , 100));
        
        m_useRowsModel = getUseMaxRowsModel();
        addDialogComponent(new DialogComponentBoolean(m_useRowsModel,
                "Use all"));
       
        m_useRowsModel.addChangeListener(new CheckBoxChangeListener());
        
        closeCurrentGroup();
        
        
        createNewGroup("MDS settings");
        
        addDialogComponent(new DialogComponentNumber(getSeedModel(),
                "Random seed: ", 1000));
        
        addDialogComponent(new DialogComponentNumber(getEpochModel(),
                "Epochs: ", 10));
        
        addDialogComponent(new DialogComponentNumber(getOutputDimModel(),
                "Output dimensions: ", 1));
        
        addDialogComponent(new DialogComponentNumber(getLearnrateModel(),
                "Learningrate: ", 0.1));
        
        List<String> distanceMetric = new ArrayList<String>();
        distanceMetric.add(DistanceManagerFactory.EUCLIDEAN_DIST);
        distanceMetric.add(DistanceManagerFactory.MANHATTAN_DIST);
        //distanceMetric.add(DistanceManagerFactory.COS_DIST);
        
        addDialogComponent(new DialogComponentStringSelection(
                getDistanceModel(), "Distance metric: ", distanceMetric));
        
        closeCurrentGroup();

        createNewTab("Input data");
        
        createNewGroup("Data to project");
        
        addDialogComponent(new DialogComponentColumnFilter(getColumnModel(),
                0, FuzzyIntervalValue.class, DoubleValue.class));

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
