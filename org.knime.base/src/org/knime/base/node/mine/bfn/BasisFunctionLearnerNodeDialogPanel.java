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
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;


/**
 * Panel is used inside the basisfunction dialogs for general settings, such as
 * distance function, shrink after commit, ...
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class BasisFunctionLearnerNodeDialogPanel extends JPanel {
    
    /*
     * TODO add maximum number of epochs
     */
    
    /** Select target column with class-label. */
    private final JComboBox m_targetColumn;

    /** Combobo holds all possible distance functions. */
    private final JComboBox m_distance;

    /** Check box for shrink after commit. */
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
        m_targetColumn.setPreferredSize(new Dimension(150, 25));
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
        m_missings.setPreferredSize(new Dimension(150, 25));
        JPanel missingPanel = new JPanel();
        missingPanel.setBorder(BorderFactory
                .createTitledBorder(" Missing Values "));
        missingPanel.add(m_missings);
        super.add(missingPanel);
        // distance function
        m_distance = new JComboBox(BasisFunctionLearnerNodeModel.DISTANCES);
        m_distance.setPreferredSize(new Dimension(150, 25));
        JPanel distancePanel = new JPanel();
        distancePanel.setBorder(BorderFactory
                .createTitledBorder(" Distance Function "));
        distancePanel.add(m_distance);
        // super.add(distancePanel);
        // shrink after commit
        m_shrinkAfterCommit = new JCheckBox(" Shrink After Commit ");
        m_shrinkAfterCommit.setPreferredSize(new Dimension(150, 25));
        JPanel shrinkPanel = new JPanel();
        shrinkPanel.setBorder(BorderFactory.createTitledBorder(" Properties "));
        shrinkPanel.add(m_shrinkAfterCommit);
        super.add(shrinkPanel);
    }

    /**
     * Returns the selected target column name.
     * 
     * @return the target column name
     */
    public DataColumnSpec getSelectedTargetColumn() {
        return (DataColumnSpec)m_targetColumn.getSelectedItem();
    }

    /**
     * Sets a new selected target column.
     * 
     * @param target the column to select
     */
    public void setSelectedTargetColumn(final DataColumnSpec target) {
        if (target != null) {
            m_targetColumn.setSelectedItem(target);
        }
    }

    /**
     * Sets a new list of target column name using the input spec.
     * 
     * @param spec the spec to retrieve column names from
     * @throws NotConfigurableException if the spec is <code>null</code> or
     *             contains no columns
     */
    public void setTargetColumns(final DataTableSpec spec)
            throws NotConfigurableException {
        this.setTargetColumns(null, spec);
    }

    /**
     * Sets a new list of target column name using the input spec.
     * 
     * @param target the target column to select
     * @param spec the spec to retrieve column names from
     * @throws NotConfigurableException if the spec is <code>null</code> or
     *             conatins no columns
     */
    public void setTargetColumns(final String target, final DataTableSpec spec)
            throws NotConfigurableException {
        m_targetColumn.removeAllItems();
        if (spec == null || spec.getNumColumns() == 0) {
            throw new NotConfigurableException("No column to select.");
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
    public int getDistance() {
        return m_distance.getSelectedIndex();
    }

    /**
     * Sets a new distance function.
     * 
     * @param index the index to select
     */
    public void setDistance(final int index) {
        if (index >= 0
                && index < BasisFunctionLearnerNodeModel.DISTANCES.length) {
            m_distance.setSelectedIndex(index);
        }
    }

    /**
     * @return <code>true</code> if the <i>shrink_after_commit</i> check box
     *         has been selected
     */
    public boolean isShrinkAfterCommit() {
        return m_shrinkAfterCommit.isSelected();
    }

    /**
     * Sets the <i>shrink_after_commit</i> flag.
     * 
     * @param flag the flag
     */
    public void setShrinkAfterCommit(final boolean flag) {
        m_shrinkAfterCommit.setSelected(flag);
    }

    /**
     * Returns the selected missing replacement.
     * 
     * @return the replacement as string
     */
    public int getMissing() {
        return m_missings.getSelectedIndex();
    }

    /**
     * Selects the given value in the combo box, if not available the user
     * defined value is set to this value.
     * 
     * @param value the item to set
     */
    public void setMissing(final int value) {
        if (value >= 0 && value < m_missings.getItemCount()) {
            m_missings.setSelectedIndex(value);
        }
    }
}
