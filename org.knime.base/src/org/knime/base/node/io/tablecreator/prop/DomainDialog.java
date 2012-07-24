/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
 *
 * History
 *   06.06.2005 (ohl): created
 */
package org.knime.base.node.io.tablecreator.prop;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import org.knime.base.node.io.filereader.ColProperty;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;


/**
 *
 * @author Peter Ohl, University of Konstanz
 */
public class DomainDialog extends JDialog {

    // the user edit panel for poss. values inside the section
    // checkbox for nominal int columns
    private JCheckBox m_containsVals;

    // components needed to handle int possible values adding/removal
    private JTextField m_editField;

    private JList m_valueList;

    private JLabel m_errorLabel;

    private JButton m_addButton;

    private JButton m_remButton;

    // the default values
    private ColProperty m_colProp;

    // and the new user settings - if valid and okay is pressed.
    private ColProperty m_result;

    /**
     * Creates a new dialog for user domain settings of one column. Provide
     * current column name and type in the colProp object, and call the
     * {@link #showDialog} method to get user input. After {@link #showDialog}
     * returns the method getDomainSettings will return the new settings.
     *
     * @param parent the owner of this dialog
     * @param colProp current column settings. The column type will be used to
     *            determine required settings, values in the domain will be used
     *            as default settings.
     */
    DomainDialog(final JDialog parent, final ColProperty colProp) {
        super(parent);

        assert colProp != null;

        m_colProp = colProp;
        m_result = null;

        JPanel domainPanel = createDomainPanel();

        // the OK and Cancel button
        JPanel control = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        // add action listener
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onOk();
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

        // add dialog and control panel to the content pane
        Container cont = getContentPane();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));
        cont.add(domainPanel);
        cont.add(control);

        setModal(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private JPanel createDomainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "domain values for nominal data"));

        if (m_colProp.getColumnSpec().getType().isCompatible(IntValue.class)) {

            // Integer column domain panel

            Box nomBox = Box.createHorizontalBox();
            m_containsVals = new JCheckBox("Integer column contains "
                    + "nominal values");
            m_containsVals.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    containsValsChanged();
                }
            });
            nomBox.add(m_containsVals);
            nomBox.add(Box.createHorizontalGlue());

            // part for the nominal values
            Box valueBox = Box.createHorizontalBox();
            valueBox.add(createIntValuesPanel());
            valueBox.add(Box.createHorizontalGlue());
            panel.add(valueBox);

        } else if (m_colProp.getColumnSpec().getType().isCompatible(
                StringValue.class)) {

            // String column domain panel

            Box valueBox = Box.createHorizontalBox();
            valueBox.add(createStringValuesPanel());
            valueBox.add(Box.createHorizontalGlue());
            panel.add(valueBox);

        } else {
            assert false : "unsupported type";
        }

        panel.add(Box.createVerticalStrut(5));
        panel.add(new JLabel("Values found in the table will be added "
                + "automatically."));
        panel.add(new JLabel("Enter only additional values here that you want "
                + "to be in the domain"));

        return panel;
    }

    /*
     * returns the bordered possible values box. It contains a box with the edit
     * fields, which can be dis/enabled seperately (by the read from file
     * checkbox). This method will set the global member m_intValsEditBox to
     * point to this edit fields.
     */
    private JPanel createIntValuesPanel() {
        return createValuesPanel(false);
    }

    /*
     * returns the bordered possible values box. It contains a box with the edit
     * fields, which can be dis/enabled seperately (by the read from file
     * checkbox). This method will set the global member m_stringValsEditBox to
     * point to this edit fields.
     */
    private JPanel createStringValuesPanel() {
        return createValuesPanel(true);
    }

    /*
     * create a panel to enter possible values. Depending on the stringValues
     * parameter it will create one to enter integers or strings. The integer
     * panel will also have an additional checkbox to decide whether the integer
     * column has nominal values at all.
     *
     * @param stringValues the flag indicating that the panel should be for
     * entering possible values of type string. Otherwise (if set false) it will
     * allow only integer values to be entered.
     */
    private JPanel createValuesPanel(final boolean stringValues) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "poss. Values"));

        if (!stringValues) {
            // the checkbox to tell if this columns contains nominal values
            m_containsVals = new JCheckBox(
                    "this integer column contains nominal values");
            m_containsVals.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    containsValsChanged();
                }
            });
            Box checkBox = Box.createHorizontalBox();
            checkBox.add(m_containsVals);
            checkBox.add(Box.createHorizontalGlue());
            panel.add(checkBox);
            panel.add(Box.createVerticalStrut(3));
        }

        // the box to manually add/remove possible values
        m_editField = new JTextField(10);
        m_editField.setMaximumSize(new Dimension(100, 25));
        m_editField.setMinimumSize(new Dimension(100, 25));
        m_editField.setPreferredSize(new Dimension(100, 25));
        m_valueList = new JList();
        m_valueList.setMinimumSize(new Dimension(100, 150));
        m_valueList.setMaximumSize(new Dimension(100, 150));
        m_valueList.setPreferredSize(new Dimension(100, 150));
        m_errorLabel = new JLabel("");

        m_addButton = new JButton("Add");
        if (stringValues) {
            m_addButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    addStringPosValue();
                }
            });
        } else {
            m_addButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    addIntPosValue();
                }
            });
        }

        m_remButton = new JButton("Remove");
        m_remButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                remSelPosValues();
            }
        });

        Box buttonBox = Box.createVerticalBox();
        buttonBox.add(m_addButton);
        buttonBox.add(Box.createVerticalStrut(3));
        buttonBox.add(m_remButton);
        buttonBox.add(Box.createVerticalGlue());

        Box fieldBox = Box.createVerticalBox();
        fieldBox.add(m_editField);
        fieldBox.add(Box.createVerticalGlue());

        Box editBox = Box.createHorizontalBox();
        editBox.add(fieldBox);
        editBox.add(Box.createHorizontalStrut(3));
        editBox.add(buttonBox);
        // now this contains the textfield and the buttons arranged nicely

        // a box for the error label to ensure a certain height - even if empty
        Box errBox = Box.createHorizontalBox();
        errBox.add(Box.createVerticalStrut(25));
        errBox.add(m_errorLabel);
        errBox.add(Box.createHorizontalGlue());

        Box leftBox = Box.createVerticalBox();
        leftBox.add(editBox);
        leftBox.add(Box.createVerticalStrut(6));
        leftBox.add(errBox);
        leftBox.add(Box.createVerticalGlue());

        Box rightBox = Box.createHorizontalBox();
        rightBox.add(new JScrollPane(m_valueList));
        rightBox.add(Box.createVerticalStrut(150));

        // the over all nominal values boss box
        Box valsEditBox = Box.createHorizontalBox();
        valsEditBox.add(leftBox);
        valsEditBox.add(Box.createHorizontalStrut(3));
        valsEditBox.add(rightBox);
        valsEditBox.add(Box.createHorizontalGlue());

        panel.add(valsEditBox);

        return panel;

    }

    /**
     * Called when the state of the "contains nominal values" box changes.
     */
    void containsValsChanged() {
        setEnableStatus();
    }

    /**
     * Called when user pressed "Remove" to remove selected item from the list
     * of possible values. The Range settings are not changed.
     */
    protected void remSelPosValues() {
        // clear the error.
        m_errorLabel.setText("");

        int[] sel = m_valueList.getSelectedIndices(); // they are ordered.

        if (sel.length == 0) {
            return;
        }

        // what an odd way to delete elements
        Vector<Object> v = new Vector<Object>();
        int s = 0;

        for (int i = 0; i < m_valueList.getModel().getSize(); i++) {
            // take over all elements except for those selected.
            if (i == sel[s]) {
                s++;
                if (s >= sel.length) {
                    s--;
                }
                continue; // skip this element
            }
            v.add(m_valueList.getModel().getElementAt(i));
        }

        m_valueList.setListData(v);

    }

    /**
     * Called when the user pressed the "Add" button to add an integer value to
     * the list of possible values. Will add the number entered, or set the
     * error text, if user input is invalid. It will adjust the range settings
     * (if any) to include the new value.
     */
    protected void addIntPosValue() {
        // clear the error.
        m_errorLabel.setText("");

        String newVal = m_editField.getText();
        if (newVal.length() < 1) {
            return;
        }

        int newInt;
        try {
            newInt = Integer.parseInt(newVal);
        } catch (NumberFormatException nfe) {
            m_errorLabel.setText("Invalid integer! Not added!");
            return;
        }

        IntCell newIntCell = new IntCell(newInt);

        addDataCellPossValue(newIntCell);

        m_editField.setText("");

    }

    /**
     * Called when the user pressed the "Add" button to add a string value to
     * the list of possible values.
     */
    protected void addStringPosValue() {
        if (m_editField.getText().length() > 0) {

            addDataCellPossValue(new StringCell(m_editField.getText()));

            m_editField.setText("");
        }
    }

    /*
     * adds the passed object to the list of possible values. Returns true if
     * the values was added, false if it already existed in the list.
     */
    private boolean addDataCellPossValue(final DataCell newVal) {
        assert newVal != null;
        if (newVal == null) {
            return false;
        }

        int doubleIdx = -1; // avoid double entries

        // what an odd way to add an element: we read all items from the
        // list in a vector and set a new data model with this vector then.
        Vector<Object> v = new Vector<Object>();
        for (int i = 0; i < m_valueList.getModel().getSize(); i++) {
            Object iVal = m_valueList.getModel().getElementAt(i);
            // the good thing is: this way we can avoid double entries
            if (newVal.equals(iVal)) {
                doubleIdx = i;
            }
            v.add(iVal);
        }
        if (doubleIdx == -1) {
            // new value wasn't in the list - add it.
            v.add(newVal);
            m_valueList.setListData(v);
            // select the new value - which is at the last index
            m_valueList.setSelectedIndex(m_valueList.getModel().getSize() - 1);
            return true;
        } else {
            // new value is already in the list - select it.
            m_valueList.setSelectedIndex(doubleIdx);
            return false;
        }
    }

    /**
     * Shows the dialog with the passed default settings (passed to the
     * constructor). It will not return until the user closes the dialog. If the
     * dialog was canceled, <code>null</code> will be returned as result,
     * otherwise the column property passed to the constructor with a modified
     * domain and nominal value flag will be returned.
     *
     * @return a modified col property object, or <code>null</code> if user
     *         canceled
     */
    public ColProperty showDialog() {

        // fill in the values from the passed col property object

        if (m_colProp.getColumnSpec().getType().isCompatible(IntValue.class)) {
            m_containsVals.setSelected(m_colProp
                    .getReadPossibleValuesFromFile());
        }

        // and the possible values - if set
        DataColumnDomain domain = m_colProp.getColumnSpec().getDomain();
        if ((domain != null) && (domain.getValues() != null)) {
            Set<DataCell> valList = domain.getValues();
            if (m_valueList != null) {
                m_valueList.setListData(valList.toArray());
            }
        }

        // now show the dialog, show it and wait until it comes back.

        setTitle("New domain settings for column '"
                + m_colProp.getColumnSpec().getName().toString() + "'");

        pack();
        centerDialog();
        setVisible(true);
        /* --- won't return before dialog is disposed ------------- */
        /* --- m_result is set, if user pressed okay -------------- */

        return m_result;

    }

    /**
     * Called when user presses okay.
     */
    void onOk() {
        m_result = takeOverSettings();
        if (m_result != null) {
            shutDown();
        }
    }

    /**
     * called when user presses cancel.
     */
    void onCancel() {
        m_result = null;
        shutDown();
    }

    /**
     * blows away the dialog.
     */
    private void shutDown() {
        setVisible(false);
        dispose();
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

    /**
     * @return an object with domain values set by the user. Or
     *         <code>null</code> if settings are invalid. Then, a error
     *         message box is displayed.
     */
    private ColProperty takeOverSettings() {

        ColProperty result = new ColProperty();
        DataColumnSpecCreator dcsc = new DataColumnSpecCreator(
                m_colProp.getColumnSpec().getName(),
                m_colProp.getColumnSpec().getType());

        if (m_containsVals != null) {
            result.setReadPossibleValuesFromFile(m_containsVals.isSelected());
        }

        if ((m_containsVals == null) || m_containsVals.isSelected()) {
            // if it's null we have a string column

            Set<DataCell> pVals = null;
            // tranfser possible values
            int valCount = m_valueList.getModel().getSize();
            pVals = new LinkedHashSet<DataCell>();
            for (int i = 0; i < valCount; i++) {
                DataCell val = (DataCell)m_valueList.getModel().getElementAt(i);
                pVals.add(val);
            }

            if (pVals.size() > 0) {
                DataColumnDomainCreator domainCreator =
                    new DataColumnDomainCreator(pVals);
                dcsc.setDomain(domainCreator.createDomain());
            }
        }

        result.setColumnSpec(dcsc.createSpec());

        return result;

    }

    /*
     * sets the enable status of all components depending on the different
     * checkboxes: "no domain" disables all. "read from file" disables most edit
     * fields", and "contains values" disables the possible values fields. But
     * they all depend on each other.
     */
    private void setEnableStatus() {

        if (m_containsVals == null) {
            // we have a string column, poss values can always be edited
            enableAllValueFields(true);
        } else {
            enableAllValueFields(m_containsVals.isSelected());
        }
    }

    private void enableAllValueFields(final boolean enable) {
        // the poss values stuff:
        if (m_editField != null) {
            m_editField.setEnabled(enable);
        }
        if (m_addButton != null) {
            m_addButton.setEnabled(enable);
        }
        if (m_remButton != null) {
            m_remButton.setEnabled(enable);
        }
        if (m_valueList != null) {
            m_valueList.setEnabled(enable);
        }
        if (m_errorLabel != null) {
            m_errorLabel.setEnabled(enable);
        }
    }
}
