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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * Panel is used inside the basisfunction dialogs for general settings, such as
 * distance function, shrink after commit, distance measure, missing value
 * handling, and maximum number of epochs.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class BasisFunctionLearnerNodeDialogPanel extends JPanel {
    
    /** If maximum number of epochs set. */
    private final JCheckBox m_isMaxEpochs;
    
    /** Value of maximum number of epochs. */
    private final JSpinner m_maxEpochs;
    
    /** Select target column with class-label. */
    private final JComboBox m_targetColumn;

    /** Holds all possible distance functions. */
    private final JComboBox m_distance;

    /** Shrink after commit. */
    private final JCheckBox m_shrinkAfterCommit;

    /** Missing replacement function. */
    private final JComboBox m_missings;

    /**
     * Creates a new panel used to select select a target column which the class
     * information and a column name for the basisfunction model column.
     */
    public BasisFunctionLearnerNodeDialogPanel() {
        super.setName(" Basics ");
        super.setLayout(new GridLayout(0, 1));
        
        // target column
        m_targetColumn = new JComboBox();
        m_targetColumn.setRenderer(new DataColumnSpecListCellRenderer());
        m_targetColumn.setPreferredSize(new Dimension(200, 25));
        JPanel targetPanel = new JPanel();
        targetPanel.setBorder(BorderFactory
                .createTitledBorder(" Target Column "));
        targetPanel.add(m_targetColumn);
        super.add(targetPanel);
        
        // missing function
        m_missings = new JComboBox(BasisFunctionLearnerTable.MISSINGS);
        // m_missings.addActionListener(new ActionListener() {
        // public void actionPerformed(final ActionEvent e) {
        // int i = m_missings.getSelectedIndex();
        // // if last index selected
        // if (i == m_missings.getItemCount() - 1) {
        // m_missings.setEditable(true);
        // } else {
        // m_missings.setEditable(false);
        // }
        // }
        // });
        m_missings.setPreferredSize(new Dimension(200, 25));
        JPanel missingPanel = new JPanel();
        missingPanel.setBorder(BorderFactory
                .createTitledBorder(" Missing Values "));
        missingPanel.add(m_missings);
        super.add(missingPanel);
        
        // distance function
        m_distance = new JComboBox(BasisFunctionLearnerNodeModel.DISTANCES);
        m_distance.setPreferredSize(new Dimension(200, 25));
        JPanel distancePanel = new JPanel();
        distancePanel.setBorder(BorderFactory
                .createTitledBorder(" Distance Function "));
        distancePanel.add(m_distance);

        // shrink after commit
        m_shrinkAfterCommit = new JCheckBox(" Shrink After Commit ");
        m_shrinkAfterCommit.setPreferredSize(new Dimension(200, 25));
        JPanel shrinkPanel = new JPanel();
        shrinkPanel.setBorder(BorderFactory.createTitledBorder(" Properties "));
        shrinkPanel.add(m_shrinkAfterCommit);
        super.add(shrinkPanel);
        
        // maximum number of epochs
        m_isMaxEpochs = new JCheckBox(" Use ", false);
        m_isMaxEpochs.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_maxEpochs.setEnabled(m_isMaxEpochs.isSelected());
            } 
        });
        m_isMaxEpochs.setPreferredSize(new Dimension(75, 25));
        m_maxEpochs = new JSpinner(new SpinnerNumberModel(
                42, 1, Integer.MAX_VALUE, 1));
        m_maxEpochs.setPreferredSize(new Dimension(125, 25));
        m_maxEpochs.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                try {
                    m_maxEpochs.commitEdit();
                } catch (ParseException pe) {
                    // ignore
                }
            }
        });
        m_maxEpochs.setEnabled(false);
        JPanel epochPanel = new JPanel(new FlowLayout());
        epochPanel.setBorder(BorderFactory.createTitledBorder(
                " Maximum #Epochs "));
        epochPanel.add(m_isMaxEpochs);
        epochPanel.add(m_maxEpochs);
        JPanel epochPanel2 = new JPanel(new GridLayout(1, 2));
        epochPanel2.add(epochPanel);
        super.add(epochPanel2);
    }
    
    /**
     * Loads given settings.
     * @param settings read settings from
     * @param specs data table spec from the input
     * @throws NotConfigurableException if no column to select available
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // update target columns
        setTargetColumns(settings.getString(
                BasisFunctionLearnerNodeModel.TARGET_COLUMN, null), specs[0]);
        // update choice of distance function
        setDistance(settings.getInt(
                BasisFunctionLearnerNodeModel.DISTANCE, 0));
        // set missing replacement value
        int missing = settings.getInt(BasisFunctionLearnerTable.MISSING, 0);
        setMissing(missing);
        // shrink after commit
        boolean shrinkAfterCommit = settings.getBoolean(
                BasisFunctionLearnerNodeModel.SHRINK_AFTER_COMMIT, false);
        setShrinkAfterCommit(shrinkAfterCommit);
        // maximum number of epochs
        int maxEpochs = settings.getInt(
                BasisFunctionLearnerNodeModel.MAX_EPOCHS, -1);
        if (maxEpochs <= 0) {
            m_isMaxEpochs.setSelected(false);
            m_maxEpochs.setEnabled(false);
        } else {
            m_isMaxEpochs.setSelected(true);
            m_maxEpochs.setEnabled(true);
            m_maxEpochs.setValue(maxEpochs);
        }
    }

    /**
     * Saves the settings.
     * @param settings used to write this settings into
     * @throws InvalidSettingsException if settings could not be read
     */
    public void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        assert (settings != null);

        // contains the error message
        StringBuilder errMsg = new StringBuilder();

        // distance
        int distance = getDistance();
        if (distance < 0
                || distance > BasisFunctionLearnerNodeModel.DISTANCES.length) {
            errMsg.append("Select a distance measure: " + distance);
        }

        // if error message's length greater zero throw exception
        if (errMsg.length() > 0) {
            throw new InvalidSettingsException(errMsg.toString());
        }

        //
        // everything fine, set values in the model
        // 

        // set target column
        settings.addString(BasisFunctionLearnerNodeModel.TARGET_COLUMN,
                getSelectedTargetColumn().getName());

        // distance
        settings.addInt(BasisFunctionLearnerNodeModel.DISTANCE, distance);

        // missing
        settings.addInt(BasisFunctionLearnerTable.MISSING, getMissing());

        // shrink after commit
        settings.addBoolean(BasisFunctionLearnerNodeModel.SHRINK_AFTER_COMMIT,
                isShrinkAfterCommit());
        
        // maximum number of epochs
        if (m_isMaxEpochs.isSelected()) {
            int maxEpochs = (Integer) m_maxEpochs.getValue();
            settings.addInt(BasisFunctionLearnerNodeModel.MAX_EPOCHS, 
                    maxEpochs);
        } else {
            settings.addInt(BasisFunctionLearnerNodeModel.MAX_EPOCHS, -1);
        }
    }
  

    /**
     * Returns the selected target column name.
     * 
     * @return the target column name
     */
    private DataColumnSpec getSelectedTargetColumn() {
        return (DataColumnSpec)m_targetColumn.getSelectedItem();
    }

    /**
     * Sets a new selected target column.
     * 
     * @param target the column to select
     */
    private void setSelectedTargetColumn(final DataColumnSpec target) {
        if (target != null) {
            m_targetColumn.setSelectedItem(target);
        }
    }

    /**
     * Sets a new list of target column name using the input spec.
     * 
     * @param target the target column to select
     * @param spec the spec to retrieve column names from
     * @throws NotConfigurableException if the spec is <code>null</code> or
     *             contains no columns
     */
    private void setTargetColumns(final String target, final DataTableSpec spec)
            throws NotConfigurableException {
        m_targetColumn.removeAllItems();
        if (spec == null || spec.getNumColumns() == 0) {
            throw new NotConfigurableException("No data spec found");
        }
        for (int i = 0; i < spec.getNumColumns(); i++) {
            m_targetColumn.addItem(spec.getColumnSpec(i));
        }
        // always select the last column first
        int cnt = m_targetColumn.getItemCount();
        if (cnt > 0) {
            m_targetColumn.setSelectedIndex(cnt - 1);
        }
        // then try to select given target column
        if (target != null) {
            int idxTarget = spec.findColumnIndex(target);
            if (idxTarget >= 0) {
                // select the target column
                setSelectedTargetColumn(spec.getColumnSpec(idxTarget));
            }
        }
    }

    /**
     * @return the selected index for the distance function
     */
    private int getDistance() {
        return m_distance.getSelectedIndex();
    }

    /**
     * Sets a new distance function.
     * 
     * @param index the index to select
     */
    private void setDistance(final int index) {
        if (index >= 0
                && index < BasisFunctionLearnerNodeModel.DISTANCES.length) {
            m_distance.setSelectedIndex(index);
        }
    }

    /**
     * @return <code>true</code> if the <i>shrink_after_commit</i> check box
     *         has been selected
     */
    private boolean isShrinkAfterCommit() {
        return m_shrinkAfterCommit.isSelected();
    }

    /**
     * Sets the <i>shrink_after_commit</i> flag.
     * 
     * @param flag the flag
     */
    private void setShrinkAfterCommit(final boolean flag) {
        m_shrinkAfterCommit.setSelected(flag);
    }

    /**
     * Returns the selected missing replacement.
     * 
     * @return the replacement as string
     */
    private int getMissing() {
        return m_missings.getSelectedIndex();
    }

    /**
     * Selects the given value in the combo box, if not available the user
     * defined value is set to this value.
     * 
     * @param value the item to set
     */
    private void setMissing(final int value) {
        if (value >= 0 && value < m_missings.getItemCount()) {
            m_missings.setSelectedIndex(value);
        }
    }
}
