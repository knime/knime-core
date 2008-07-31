/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Feb 1, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.rename;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * Dialog for the renaming node.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class RenameNodeDialogPane extends NodeDialogPane {
    /**
     * Panel containing for each column in the input table a row. (with JLabel
     * "old name", checkbox to set a new name, text field for the new name, and
     * a combo box where the user can pick a compatible type)
     */
    private final JPanel m_panel;

    private RenameColumnSetting[] m_colSettings;

    /**
     * Constructs new dialog, inits members.
     */
    public RenameNodeDialogPane() {
        super();
        m_panel = new JPanel(new GridLayout(0, 1));
        JScrollPane pane = new JScrollPane(m_panel);
        addTab("Change columns", pane);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        DataTableSpec spec = specs[0];
        if (spec.getNumColumns() == 0) {
            throw new NotConfigurableException("No columns at input.");
        }
        // construct for each column the default settings, i.e. where nothing
        // will change, neither name nor type.
        m_colSettings = new RenameColumnSetting[spec.getNumColumns()];
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);
            RenameColumnSetting colSet = new RenameColumnSetting(colSpec);
            m_colSettings[i] = colSet;
        }
        NodeSettingsRO subSettings;
        try {
            // this node settings object must contain only entry of type
            // NodeSetting
            subSettings = settings
                    .getNodeSettings(RenameNodeModel.CFG_SUB_CONFIG);
        } catch (InvalidSettingsException ise) {
            subSettings = null;
        }
        if (subSettings != null) {
            // process settings for individual column
            for (String id : subSettings) {
                NodeSettingsRO idSettings;
                String nameForSettings;
                try {
                    // idSettigs address the settings for one particular column
                    idSettings = subSettings.getNodeSettings(id);
                    // the name of the column - must match
                    nameForSettings = idSettings
                            .getString(RenameColumnSetting.CFG_OLD_COLNAME);
                } catch (InvalidSettingsException is) {
                    continue;
                }
                // does the data table spec contain this name?
                for (RenameColumnSetting colSet : m_colSettings) {
                    // update the column settings
                    if (colSet.getName().equals(nameForSettings)) {
                        colSet.loadSettingsFrom(idSettings);
                        break; // go to next identifier
                    }
                }
            }
        }
        // layout the panel, add new components - one for each column
        m_panel.removeAll();
        // determine the longest column name, used to make the dialog look nice
        int max = 0;
        for (RenameColumnSetting colSet : m_colSettings) {
            JLabel label = new JLabel(colSet.getName().toString());
            max = Math.max(max, label.getPreferredSize().width);
        }
        // hm, don't let it look squeezed
        max += 10;
        for (RenameColumnSetting colSet : m_colSettings) {
            addPanelFor(colSet, max);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        NodeSettingsWO subSettings = settings
                .addNodeSettings(RenameNodeModel.CFG_SUB_CONFIG);
        HashMap<String, Integer> duplicateHash = new HashMap<String, Integer>();
        for (int i = 0; i < m_colSettings.length; i++) {
            RenameColumnSetting colSet = m_colSettings[i];
            String newName = colSet.getNewColumnName();
            if (newName == null) {
                newName = colSet.getName();
            }
            if (newName == null || newName.length() == 0) {
                String warnMessage = "Column name at index " + i + " is empty.";
                throw new InvalidSettingsException(warnMessage);
            }
            Integer duplIndex = duplicateHash.put(newName, i);
            if (duplIndex != null) {
                String warnMessage = "Duplicate column name \"" + newName 
                + "\" at index " + duplIndex + " and " + i;
                throw new InvalidSettingsException(warnMessage);
            }
            NodeSettingsWO subSub = subSettings.addNodeSettings(newName);
            colSet.saveSettingsTo(subSub);
        }
    }

    /**
     * Adds one row to m_panel.
     * 
     * @param colSet the settings for the column
     * @param labelWidth the width to be used for the column's name
     */
    private void addPanelFor(final RenameColumnSetting colSet,
            final int labelWidth) {
        final String oldColName = colSet.getName();
        final String newColName = colSet.getNewColumnName();
        boolean hasNewName = newColName != null;
        // ensured not to be null as we used the "right" constructor above.
        Class<? extends DataValue>[] possibleTypes = colSet
                .getPossibleValueClasses();
        int selectedType = colSet.getNewValueClassIndex();
        final JLabel nameLabel = new JLabel(oldColName);
        int labelHeight = nameLabel.getPreferredSize().height;
        nameLabel.setPreferredSize(new Dimension(labelWidth, labelHeight));
        nameLabel.setMinimumSize(new Dimension(labelWidth, labelHeight));
        final JTextField newNameField = new JTextField(oldColName, 8);
        if (hasNewName) {
            newNameField.setText(newColName);
        }
        // add listeners to all fields that can change: They will update their
        // RenameColumnSetting immediately
        newNameField.addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent e) {
            }

            public void focusLost(final FocusEvent e) {
                String newText = newNameField.getText();
                colSet.setNewColumnName(newText);
            }
        });
        final JCheckBox checker = new JCheckBox("Change: ");
        newNameField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (!newNameField.isEnabled()) {
                    assert (!checker.isSelected());
                    checker.doClick();
                }
            }
        });
        checker.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                if (checker.isSelected()) {
                    newNameField.setEnabled(true);
                    newNameField.requestFocus();
                } else {
                    newNameField.setText(oldColName);
                    colSet.setNewColumnName(null);
                    newNameField.setEnabled(false);
                }
            }
        });
        checker.setSelected(hasNewName);
        newNameField.setEnabled(hasNewName);
        final JComboBox typeChooser = new JComboBox(possibleTypes);
        // no need to make it enabled when there is only one choice.
        typeChooser.setEnabled(possibleTypes.length != 1);
        typeChooser.setRenderer(new DataTypeNameRenderer());
        typeChooser.setSelectedIndex(selectedType);
        typeChooser.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                colSet.setNewValueClassIndex(typeChooser.getSelectedIndex());
            }
        });
        JPanel oneRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        oneRow.add(nameLabel);
        oneRow.add(checker);
        oneRow.add(newNameField);
        oneRow.add(typeChooser);
        m_panel.add(oneRow);
    }
}
