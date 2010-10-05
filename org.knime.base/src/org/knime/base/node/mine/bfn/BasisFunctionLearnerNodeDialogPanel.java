/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

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

    /** Holds all possible distance functions. */
    private final JComboBox m_distance;

    /** Shrink after commit. */
    private final JCheckBox m_shrinkAfterCommit;
    
    /** Cover only pattern with maximum degree. */
    private final JCheckBox m_coverMax;

    /** Missing replacement function. */
    private final JComboBox m_missings;

    /**
     * Creates a new panel used to select select a target column which the class
     * information and a column name for the basisfunction model column.
     */
    public BasisFunctionLearnerNodeDialogPanel() {
        super.setName("Options");
        super.setLayout(new GridLayout(0, 1));
        
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
        m_shrinkAfterCommit = new JCheckBox(" Shrink after commit ");
        m_shrinkAfterCommit.setPreferredSize(new Dimension(200, 25));
        
        // maximum coverage degree
        m_coverMax = new JCheckBox(" Use class with max coverage ");
        m_coverMax.setPreferredSize(new Dimension(200, 25));
        
        JPanel advancedPanel = new JPanel(new GridLayout(2, 1));
        advancedPanel.setBorder(BorderFactory.createTitledBorder(" Advanced "));
        advancedPanel.add(m_shrinkAfterCommit);
        advancedPanel.add(m_coverMax);
        super.add(advancedPanel);
        
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
                " Maximum no. Epochs "));
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
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) {
        assert specs == specs;
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
        // max class coverage
        boolean coverMax = settings.getBoolean(
                BasisFunctionLearnerNodeModel.MAX_CLASS_COVERAGE, true);
        setCoverMax(coverMax);
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

        // distance
        settings.addInt(BasisFunctionLearnerNodeModel.DISTANCE, distance);

        // missing
        settings.addInt(BasisFunctionLearnerTable.MISSING, getMissing());

        // shrink after commit
        settings.addBoolean(BasisFunctionLearnerNodeModel.SHRINK_AFTER_COMMIT,
                isShrinkAfterCommit());

        // max class coverage
        settings.addBoolean(BasisFunctionLearnerNodeModel.MAX_CLASS_COVERAGE,
                isCoverMax());
        
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
     * Sets the <i>cover_max</i> flag.
     * 
     * @param flag the flag
     */
    private void setShrinkAfterCommit(final boolean flag) {
        m_shrinkAfterCommit.setSelected(flag);
    }
    
    /**
     * @return <code>true</code> if the <i>cover_max</i> check box
     *         has been selected
     */
    private boolean isCoverMax() {
        return m_coverMax.isSelected();
    }

    /**
     * Sets the <i>shrink_after_commit</i> flag.
     * 
     * @param flag the flag
     */
    private void setCoverMax(final boolean flag) {
        m_coverMax.setSelected(flag);
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
