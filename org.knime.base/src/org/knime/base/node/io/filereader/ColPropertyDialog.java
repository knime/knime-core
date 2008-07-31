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
 * -------------------------------------------------------------------
 * 
 * History
 *   01.06.2005 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;


/**
 * 
 * @author Peter Ohl, University of Konstanz
 */
public final class ColPropertyDialog extends JDialog {

    // constants for the combobox
    private static final int TYPE_DOUBLE = 0;

    private static final int TYPE_INT = 1;

    private static final int TYPE_STRING = 2;
    
    private static final int TYPE_SMILES = 3;

    private static final String[] TYPES;
    
    static {
        if (SmilesTypeHelper.INSTANCE.isSmilesAvailable()) {
            TYPES = new String[] {"Double", "Integer", "String", "Smiles"};
        } else {
            TYPES = new String[] {"Double", "Integer", "String"};
        }
    }
    

    // the index of the column to change settings for
    private int m_colIdx;

    // current settings of the table. Read them only!
    private Vector<ColProperty> m_allColProps;

    // the components to read new user settings from
    private JCheckBox m_skipColumn;
    
    private JTextField m_colNameField;

    private JComboBox m_typeChooser;

    private JTextField m_missValueField;

    // the index in the type combobox of the old type.
    private int m_oldType;

    // the properties object we store (only) domain settings in. If null user
    // did not open the domain dialog and we are supposed to use default values.
    private ColProperty m_userDomainSettings;

    /* the vector filled with the new settings will be returned */
    private Vector<ColProperty> m_result;

    private JLabel m_warnLabel;

    private JButton m_domainButton;

    private ColPropertyDialog() {
        // don't call me.
        assert false;
    }

    private ColPropertyDialog(final Frame parent, final int colIdx,
            final Vector<ColProperty> allColProps) {

        super(parent, true);

        m_colIdx = colIdx;
        m_allColProps = allColProps;
        m_result = null;

        // instantiate the components of the dialog
        m_skipColumn = new JCheckBox("DON'T include column in output table");
        m_skipColumn.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                skipColumnHasChanged();
            }
        });
        Box skipPanel = Box.createHorizontalBox();
        skipPanel.add(Box.createHorizontalGlue());
        skipPanel.add(m_skipColumn);
        skipPanel.add(Box.createHorizontalGlue());
        
        // column name goes first
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 5));
        m_colNameField = new JTextField(8);
        namePanel.add(new JLabel("Name: "));
        namePanel.add(m_colNameField);

        // panel for the type is next
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 5));
        typePanel.add(new JLabel("Type: "));
        m_typeChooser = new JComboBox(TYPES);
        m_typeChooser.setPrototypeDisplayValue("0123456789");
        m_typeChooser.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                typeSelectionChanged();
            }
        });
        typePanel.add(m_typeChooser);

        // the missing value components
        JPanel missPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 2));
        missPanel.add(new JLabel("miss. value pattern:"));
        m_missValueField = new JTextField(8);
        missPanel.add(m_missValueField);

        // the warning message
        JPanel warnPanel = new JPanel();
        warnPanel.setLayout(new BoxLayout(warnPanel, BoxLayout.X_AXIS));
        m_warnLabel = new JLabel("");
        warnPanel.add(Box.createVerticalStrut(30));
        warnPanel.add(m_warnLabel);
        warnPanel.add(Box.createHorizontalGlue());

        // the domain button
        JPanel domainPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 2));
        m_domainButton = new JButton("Domain...");
        domainPanel.add(m_domainButton);
        m_domainButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                openDomainDialog();
                m_warnLabel.setText("");
            }
        });
        m_userDomainSettings = null;

        // the OK and Cancel button
        JPanel control = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        // add action listener
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onOK();
            }
        });
        JButton cancel = new JButton("Cancel");
        // add action listener
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                onCancel();
            }
        });
        control.add(ok);
        control.add(cancel);

        // group components nicely - without those buttons
        JPanel dlgPanel = new JPanel();
        dlgPanel.setLayout(new BoxLayout(dlgPanel, BoxLayout.Y_AXIS));
        dlgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Column Properties"));
        dlgPanel.add(skipPanel);
        dlgPanel.add(namePanel);
        dlgPanel.add(typePanel);
        dlgPanel.add(missPanel);
        dlgPanel.add(warnPanel);
        dlgPanel.add(domainPanel);

        // add dialog and control panel to the content pane
        Container cont = getContentPane();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));
        cont.add(dlgPanel);
        cont.add(Box.createVerticalStrut(3));
        cont.add(control);

        setModal(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    }
 
    private void skipColumnHasChanged() {
        boolean useIt = !m_skipColumn.isSelected();
        m_colNameField.setEnabled(useIt);
        m_missValueField.setEnabled(useIt);
        m_typeChooser.setEnabled(useIt);
        setEnableStatusOfDomainButton();
    }
    
    /**
     * Called when "domain..." button is pressed. Opens the dialog for domain
     * settings, with components depending on the currently selected type.
     */
    void openDomainDialog() {

        // prepare a colProp with the default settings for the dialog
        ColProperty domainProperty = (ColProperty)m_allColProps.get(m_colIdx)
                .clone();

        // set the new name and type - regardless of their correctness
        domainProperty.changeColumnName(m_colNameField.getText());
        domainProperty.changeColumnType(getTypeFromComboIndex(m_typeChooser
                .getSelectedIndex()));

        if (m_userDomainSettings != null) {
            // calling this dialog for the 2nd time. Use vals from first call.
            domainProperty.changeDomain(m_userDomainSettings.getColumnSpec()
                    .getDomain());
            domainProperty.setReadPossibleValuesFromFile(m_userDomainSettings
                    .getReadPossibleValuesFromFile());

        }
        DomainDialog domDlg = new DomainDialog(this, domainProperty);
        ColProperty newSettings = domDlg.showDialog();
        if (newSettings != null) {
            m_userDomainSettings = newSettings;
        }
    }

    /**
     * Called whenever the selected type of the column changes. Will reset
     * domain settings to default values for the new type.
     */
    protected void typeSelectionChanged() {

        if (m_userDomainSettings != null) {
            m_userDomainSettings = null;
            m_warnLabel.setText("Domain settings were reset!!");
        }
        
        setEnableStatusOfDomainButton();
        
    }
    
    private void setEnableStatusOfDomainButton() {
        
        if (m_skipColumn.isSelected()) {
            m_domainButton.setEnabled(false);
            return;
        }
        
        DataType selectedType =
                getTypeFromComboIndex(m_typeChooser.getSelectedIndex());
        
        if (selectedType == null) {
            m_domainButton.setEnabled(false);
            return;
        } 
            // if (selectedType.isCompatible(StringValue.class)
        // || selectedType.isCompatible(IntValue.class)) {
        /*
         * TODO: The DataContainer doesn't support nominal values for integer
         * columns yet. If it does, replace the if stmt below with the one above
         * (the domain dialog already supports int types). Also, somehow, tell
         * the data container about the user's decision.
         */
        if (selectedType.isCompatible(StringValue.class)
                && selectedType.isCompatible(NominalValue.class)) {
            m_domainButton.setEnabled(true);
        } else {
            m_domainButton.setEnabled(false);
        }
    }

    
    /**
     * Opens a Dialog to receive user settings for column name, type, missing
     * value pattern, and domain. If the user cancels the dialog no changes will
     * be made and <code>null</code> is returned. If okay is pressed, the
     * settings from the dialog will be stored in a new {@link ColProperty}
     * object. A new {@link Vector} will be returned, containing references to
     * the old unchanged objects and to one new colProperty object containing
     * the new settings (at index colIdx). <br>
     * If the column type has changed, domain values will be cleared. On success
     * the 'set by user' flag is set. If user's settings are incorrect an error
     * dialog pops up and the user values are discarded.
     * 
     * @param parent frame who owns this dialog
     * @param colIdx the index of the column user changes settings for. Must be
     *            an index of the vector of <code>allColProps</code>.
     * @param allColProps the <code>colProperty</code> objects of all columns.
     *            The one specified by the <code>colIdx</code> parameter will
     *            be changed!
     * @return a Vector of ColProperty objects with the new and changed
     *         properties. (Currently only the index colIdx will be changed). Or
     *         null if the user canceled, or entered invalid settings.
     */
    static Vector<ColProperty> openUserDialog(final Frame parent,
            final int colIdx, final Vector<ColProperty> allColProps) {

        assert colIdx < allColProps.size();
        assert colIdx >= 0;

        ColPropertyDialog colPropDlg = new ColPropertyDialog(parent, colIdx,
                allColProps);

        return colPropDlg.showDialog();

    }

    /*
     * sets the current values of the column to change into the dialog's
     * components, shows the dialog and waits for it to return. If the user
     * pressed Ok it returns true, otherwise false.
     */
    private Vector<ColProperty> showDialog() {

        ColProperty theColProp = m_allColProps.get(m_colIdx);
        DataColumnSpec theColSpec = theColProp.getColumnSpec();

        // set the values in the components:
        // the skip flag
        m_skipColumn.setSelected(theColProp.getSkipThisColumn());
        skipColumnHasChanged();
        // the column name
        m_colNameField.setText(theColSpec.getName().toString());
        // figure out the old type index (in the combo box) to pre-set it
        m_oldType = getComboIndexFromType(theColSpec.getType());
        m_typeChooser.setSelectedIndex(m_oldType);
        // the missing value
        m_missValueField.setText(theColProp.getMissingValuePattern());

        setTitle("New settings for column '" + theColSpec.getName().toString()
                + "'");

        pack();
        centerDialog();

        setVisible(true);
        /* ---- won't come back before dialog is disposed -------- */
        /* ---- on Ok we tranfer the settings into the m_result -- */
        return m_result;
    }

    /**
     * Called when user presses the ok button.
     */
    void onOK() {
        m_result = takeOverSettings();
        if (m_result != null) {
            shutDown();
        }
    }

    /**
     * Called when user presses the cancel button or closes the window.
     */
    void onCancel() {
        m_result = null;
        shutDown();
    }

    /* blows away the dialog */
    private void shutDown() {
        setVisible(false);
        // (tg) dispose(); causes the parent to move to back
    }

    /**
     * Sets this dialog in the center of the screen observing the current screen
     * size.
     */
    private void centerDialog() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = getSize();
        setBounds(Math.max(0, (screenSize.width - size.width) / 2), Math.max(0,
                (screenSize.height - size.height) / 2), Math.min(
                screenSize.width, size.width), Math.min(screenSize.height,
                size.height));
    }

    private Vector<ColProperty> takeOverSettings() {
        ColProperty theColProp = m_allColProps.get(m_colIdx);

        // get the new values
        int newType = m_typeChooser.getSelectedIndex();
        String newName = m_colNameField.getText();
        String newMissVal = m_missValueField.getText();

        // create the new ColProperty object to return (start with the old vals)
        ColProperty newColProp = (ColProperty)theColProp.clone();
        // if he says okay its always user settings (even if nothing changed)
        newColProp.setUserSettings(true);
        newColProp.setSkipThisColumn(m_skipColumn.isSelected());
        
        // check name for uniqueness 
            
        // only if we include the column in the table
        if (!newColProp.getSkipThisColumn()) {
            /* user changed column name. */
            /* Make sure its valid */
            if (newName.length() < 1) {
                JOptionPane.showMessageDialog(this,
                        "Column names cannot be empty. "
                                + "Enter valid name or press cancel.",
                        "Invalid column name", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            /* Make sure new name is unique */
            for (int c = 0; c < m_allColProps.size(); c++) {
                if (c == m_colIdx) {
                    // don't compare against our own old name
                    continue;
                }
                ColProperty colProp = m_allColProps.get(c);
                if (colProp.getSkipThisColumn()) {
                    // don't compare with names we don't use.
                    continue;
                }
                String otherName = colProp.getColumnSpec().getName().toString();
                if (newName.equals(otherName)) {
                    JOptionPane.showMessageDialog(this,
                            "Specified column name ('" + newName
                                    + "') is already in use for "
                                    + "another column."
                                    + " Enter unique name or press "
                                    + "cancel.", "Duplicate column names",
                            JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            }
        }

        // take over the new value
        newColProp.changeColumnName(newName);

        if (newType != m_oldType) {
            /* user changed column type. Take it over. */
            DataType type = getTypeFromComboIndex(newType);
            if (type == null) {
                // internal error - shouldn't happen
                JOptionPane
                        .showMessageDialog(
                                this,
                                "Unexpected and invalid type (looks like an "
                                        + "internal error!). "
                                        + "Try selecting another type "
                                        + "or press cancel.",
                                "Internal Error: Unexpected type",
                                JOptionPane.ERROR_MESSAGE);
                return null;

            }
            newColProp.changeColumnType(type);
            /*
             * and change domain/poss.value settings as they are of the old type
             */
            newColProp.changeDomain(null);
        }

        if (!newMissVal.equals(theColProp.getMissingValuePattern())) {
            if (newMissVal.equals("")) {
                newColProp.setMissingValuePattern(null);
            } else {
                newColProp.setMissingValuePattern(newMissVal);
            }
        }

        if (m_userDomainSettings != null) {
            // user changed domain. take it over
            newColProp.changeDomain(m_userDomainSettings.getColumnSpec()
                    .getDomain());
            newColProp.setReadPossibleValuesFromFile(m_userDomainSettings
                    .getReadPossibleValuesFromFile());
        }

        // construct the result vector - which is a copy of the colProperties
        // passed in, only the item with index colIdx is replaced by the new one
        Vector<ColProperty> result = new Vector<ColProperty>(m_allColProps);
        result.setElementAt(newColProp, m_colIdx);
        return result;

    }

    private DataType getTypeFromComboIndex(final int comboBoxIndex) {
        // extract new type
        switch (comboBoxIndex) {
        case TYPE_STRING:
            return StringCell.TYPE;
        case TYPE_INT:
            return IntCell.TYPE;
        case TYPE_DOUBLE:
            return DoubleCell.TYPE;
        case TYPE_SMILES:
            return SmilesTypeHelper.INSTANCE.getSmilesType();
        default:
            return null;
        }
    }

    private int getComboIndexFromType(final DataType type) {
        if (type.equals(IntCell.TYPE)) {
            return TYPE_INT;
        } else if (type.equals(DoubleCell.TYPE)) {
            return TYPE_DOUBLE;
        } else if (type.equals(SmilesTypeHelper.INSTANCE.getSmilesType())) {
            return TYPE_SMILES;
        } else {
            assert (type.equals(StringCell.TYPE));
            return TYPE_STRING;
        }
    }
    
}
