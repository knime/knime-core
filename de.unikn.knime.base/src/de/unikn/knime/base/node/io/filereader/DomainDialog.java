/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   06.06.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.filereader;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataCellComparator;
import de.unikn.knime.core.data.DataColumnDomain;
import de.unikn.knime.core.data.DataColumnDomainCreator;
import de.unikn.knime.core.data.DataColumnSpecCreator;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DoubleValue;
import de.unikn.knime.core.data.IntValue;
import de.unikn.knime.core.data.StringValue;
import de.unikn.knime.core.data.def.DoubleCell;
import de.unikn.knime.core.data.def.IntCell;
import de.unikn.knime.core.data.def.StringCell;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class DomainDialog extends JDialog {

    // the range min/max values. Only two of them will be not null.
    private JSpinner m_minDblValue;

    private JSpinner m_maxDblValue;

    private JSpinner m_minIntValue;

    private JSpinner m_maxIntValue;

    // the user edit panel for poss. values inside the section
    // checkbox for nominal int columns
    private JCheckBox m_containsVals;

    // components needed to handle int possible values adding/removal
    private JTextField m_editField;

    private JList m_valueList;

    private JLabel m_errorLabel;

    private JButton m_addButton;

    private JButton m_remButton;

    // checkbox to let filereader analyze domain
    private JCheckBox m_readFromFile;

    // checkbox for no domain settings at all
    private JCheckBox m_noDomain;

    // the default values
    private ColProperty m_colProp;

    // and the new user settings - if valid and okay is pressed.
    private ColProperty m_result;

    /**
     * creates a new dialog for user domain settings of one column. Provide
     * current column name and type in the colProp object, and call the
     * showDialog method to get user input. After showDialog returns the method
     * getDomainSettings will return the new settings.
     * 
     * @param colProp current column settings. The column type will be used to
     *            determine required settings, values in the domain will be used
     *            as default settings.
     */
    DomainDialog(final ColProperty colProp) {

        assert colProp != null;

        m_colProp = colProp;
        m_result = null;

        // Create the panels of the dialog
        JPanel noDomainPanel = 
            new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        m_noDomain = new JCheckBox("No domain settings.");
        m_noDomain.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                noDomainChanged();
            }
        });
        noDomainPanel.add(m_noDomain);

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
        cont.add(noDomainPanel);
        cont.add(Box.createVerticalStrut(3));
        cont.add(domainPanel);
        cont.add(control);

        setModal(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    }

    private JPanel createDomainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Domain Settings"));

        // checkbox to let the file reader analyze the domain.
        m_readFromFile = new JCheckBox("fill values during file "
                + "reader execute");
        m_readFromFile.setToolTipText("if checked, the entire file will be "
                + "examined. To avoid this, uncheck in each column.");
        m_readFromFile.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                readFromFileChanged();
            }
        });
        Box fileBox = Box.createHorizontalBox();
        fileBox.add(m_readFromFile);
        fileBox.add(Box.createHorizontalGlue());
        panel.add(fileBox);
        panel.add(Box.createVerticalStrut(10));

        if (m_colProp.getColumnSpec().getType().isCompatible(IntValue.class)) {

            // panel for min/max values
            JPanel intRangePanel = createIntRangePanel();
            Box rangeBox = Box.createHorizontalBox();
            rangeBox.add(intRangePanel);
            rangeBox.add(Box.createHorizontalGlue());
            rangeBox.add(Box.createHorizontalGlue());
            panel.add(rangeBox);
            panel.add(Box.createVerticalStrut(10));

            // part for the nominal values
            Box valueBox = Box.createHorizontalBox();
            valueBox.add(createIntValuesPanel());
            valueBox.add(Box.createHorizontalGlue());
            panel.add(valueBox);

        } else if (m_colProp.getColumnSpec().getType().isCompatible(
                DoubleValue.class)) {
            // panel for min/max values
            Box rangeBox = Box.createHorizontalBox();
            rangeBox.add(createDoubleRangePanel());
            rangeBox.add(Box.createHorizontalGlue());
            panel.add(rangeBox);

        } else if (m_colProp.getColumnSpec().getType().isCompatible(
                StringValue.class)) {
            Box valueBox = Box.createHorizontalBox();
            valueBox.add(createStringValuesPanel());
            valueBox.add(Box.createHorizontalGlue());
            panel.add(valueBox);

        } else {
            assert false : "unsupported type";
        }

        return panel;
    }

    private JPanel createDoubleRangePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Range"));

        // the min value of the range
        m_minDblValue = new JSpinner(new SpinnerNumberModel(new Double(0.0),
                null, null, new Double(0.1)));
        m_minDblValue.setMaximumSize(new Dimension(65, 25));
        m_minDblValue.setMinimumSize(new Dimension(65, 25));
        m_minDblValue.setPreferredSize(new Dimension(65, 25));
        m_minDblValue.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                setDblRangeColor();
            }
        });
        // max value of the range
        m_maxDblValue = new JSpinner(new SpinnerNumberModel(new Double(1.0),
                null, null, new Double(0.1)));
        m_maxDblValue.setMaximumSize(new Dimension(65, 25));
        m_maxDblValue.setMinimumSize(new Dimension(65, 25));
        m_maxDblValue.setPreferredSize(new Dimension(65, 25));
        m_maxDblValue.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                setDblRangeColor();
            }
        });

        Box minBox = Box.createHorizontalBox();
        minBox.add(Box.createHorizontalGlue());
        minBox.add(new JLabel("min. value:"));
        minBox.add(Box.createHorizontalStrut(3));
        minBox.add(m_minDblValue);
        panel.add(minBox);

        Box maxBox = Box.createHorizontalBox();
        maxBox.add(Box.createHorizontalGlue());
        maxBox.add(new JLabel("max. value:"));
        maxBox.add(Box.createHorizontalStrut(3));
        maxBox.add(m_maxDblValue);
        panel.add(maxBox);

        return panel;
    }

    private JPanel createIntRangePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Range"));

        // the min value of the range
        m_minIntValue = new JSpinner(new SpinnerNumberModel(new Integer(0),
                null, null, new Integer(1)));
        m_minIntValue.setMaximumSize(new Dimension(65, 25));
        m_minIntValue.setMinimumSize(new Dimension(65, 25));
        m_minIntValue.setPreferredSize(new Dimension(65, 25));
        m_minIntValue.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                setIntRangeColor();
            }
        });
        // max value of the range
        m_maxIntValue = new JSpinner(new SpinnerNumberModel(new Integer(1),
                null, null, new Integer(1)));
        m_maxIntValue.setMaximumSize(new Dimension(65, 25));
        m_maxIntValue.setMinimumSize(new Dimension(65, 25));
        m_maxIntValue.setPreferredSize(new Dimension(65, 25));
        m_maxIntValue.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                setIntRangeColor();
            }
        });

        Box minBox = Box.createHorizontalBox();
        minBox.add(Box.createHorizontalGlue());
        minBox.add(new JLabel("min. value:"));
        minBox.add(Box.createHorizontalStrut(3));
        minBox.add(m_minIntValue);
        panel.add(minBox);

        Box maxBox = Box.createHorizontalBox();
        maxBox.add(Box.createHorizontalGlue());
        maxBox.add(new JLabel("max. value:"));
        maxBox.add(Box.createHorizontalStrut(3));
        maxBox.add(m_maxIntValue);
        panel.add(maxBox);

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
     * sets the foreground color of the min/max spinners for the double range
     * values. Depending on their value to red (if max val is less than min val)
     * or black (if values are valid).
     */
    protected void setDblRangeColor() {
        JComponent editor;
        JFormattedTextField minField;
        JFormattedTextField maxField;

        editor = m_minDblValue.getEditor();
        assert editor instanceof JSpinner.DefaultEditor;
        minField = ((JSpinner.DefaultEditor)editor).getTextField();

        editor = m_maxDblValue.getEditor();
        assert editor instanceof JSpinner.DefaultEditor;
        maxField = ((JSpinner.DefaultEditor)editor).getTextField();

        try {
            double min = readDblSpinner(m_minDblValue);
            double max = readDblSpinner(m_maxDblValue);
            if (min <= max) {
                minField.setForeground(Color.BLACK);
                maxField.setForeground(Color.BLACK);
                return;
            }
        } catch (Exception e) {
            // if user entered invalid number format: fall through
        }

        minField.setForeground(Color.RED);
        maxField.setForeground(Color.RED);
    }

    /**
     * sets the foreground color of the min/max spinners for the integer range
     * values. Depending on their value to red (if max val is less than min val)
     * or black (if values are valid).
     */
    protected void setIntRangeColor() {
        JComponent editor;
        JFormattedTextField minField;
        JFormattedTextField maxField;

        editor = m_minIntValue.getEditor();
        assert editor instanceof JSpinner.DefaultEditor;
        minField = ((JSpinner.DefaultEditor)editor).getTextField();

        editor = m_maxIntValue.getEditor();
        assert editor instanceof JSpinner.DefaultEditor;
        maxField = ((JSpinner.DefaultEditor)editor).getTextField();

        try {
            int min = readIntSpinner(m_minIntValue);
            int max = readIntSpinner(m_maxIntValue);
            if (min <= max) {
                minField.setForeground(Color.BLACK);
                maxField.setForeground(Color.BLACK);
                return;
            }
        } catch (Exception e) {
            // if user entered invalid number format: fall through
        }
        minField.setForeground(Color.RED);
        maxField.setForeground(Color.RED);
    }

    /**
     * called when the 'read domain from file' checkbox changes. Will enable or
     * disable the corresponding edit fields, depending in the state of the
     * checkbox.
     */
    protected void readFromFileChanged() {
        setEnableStatus();
    }

    /**
     * called when the state of the "contains nominal values" box changes.
     */
    void containsValsChanged() {
        setEnableStatus();
    }

    /**
     * called when the state of the 'no domain settings' checkbox changes.
     * En/disables all other components in the dialog, depending on the new
     * state
     */
    void noDomainChanged() {
        setEnableStatus();
    }

    /**
     * called when user pressed "Remove" to remove selected item from the list
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
     * called when the user pressed the "Add" button to add an integer value to
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

        boolean added = addDataCellPossValue(newIntCell);

        m_editField.setText("");

        // check the range and adjust it to include the new value.
        if (added) {
            IntCell min = null; // current range
            IntCell max = null;
            // get the currently set min/max values
            try {
                min = new IntCell(readIntSpinner(m_minIntValue));
                max = new IntCell(readIntSpinner(m_maxIntValue));
                DataCellComparator intComp = IntCell.TYPE.getComparator();

                // adjust the range (if we have any) to include the new value
                if (intComp.compare(min, newIntCell) > 0) {
                    m_minIntValue.setValue(new Integer(newInt));
                    m_errorLabel.setText("Info: range adjusted.");
                }
                if (intComp.compare(max, newIntCell) < 0) {
                    m_maxIntValue.setValue(new Integer(newInt));
                    m_errorLabel.setText("Info: range adjusted.");
                }
            } catch (ParseException pe) {
                // silently ignore range settings with errors
            }
        }

    }

    /**
     * called when the user pressed the "Add" button to add a string value to
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
     * constructor). It will return true if user pressed "Ok", or false if the
     * dialog was canceled. After a "true" result the settings can be obtained
     * by calling the getDomainSettings method.
     * 
     * @return true if user pressed Ok and settings were stored, otherwise (if
     *         dialog was canceled) false.
     */
    public ColProperty showDialog() {

        DataColumnDomain domain = m_colProp.getColumnSpec().getDomain();

        // the no domain info is set, if no range, no possible values, and none
        // of the read from file flags are set.
        m_noDomain.setSelected(false);
        if (!m_colProp.getReadBoundsFromFile()
                && !m_colProp.getReadPossibleValuesFromFile()) {

            if (domain == null) {
                m_noDomain.setSelected(true);
            } else {
                if (((domain.getLowerBound() == null) || (domain
                        .getUpperBound() == null))
                        && (domain.getValues() == null)) {
                    m_noDomain.setSelected(true);
                }
            }
        }

        // read stuff from file??
        if (m_colProp.getReadBoundsFromFile()
                || m_colProp.getReadPossibleValuesFromFile()) {
            m_readFromFile.setSelected(true);
        } else {
            m_readFromFile.setSelected(false);
        }

        // set the range's values - if specified
        if ((domain != null) && (domain.getUpperBound() != null)
                && (domain.getLowerBound() != null)) {
            DataCell min = domain.getLowerBound();
            DataCell max = domain.getUpperBound();

            if ((m_minDblValue != null) && (min instanceof DoubleValue)) {
                DoubleValue dblMin = (DoubleValue)min;
                m_minDblValue.setValue(new Double(dblMin.getDoubleValue()));
            }
            if ((m_maxDblValue != null) && (max instanceof DoubleValue)) {
                DoubleValue dblMax = (DoubleValue)max;
                m_maxDblValue.setValue(new Double(dblMax.getDoubleValue()));
            }
            if ((m_minIntValue != null) && (min instanceof IntValue)) {
                IntValue intMin = (IntValue)min;
                m_minIntValue.setValue(new Integer(intMin.getIntValue()));
            }
            if ((m_maxIntValue != null) && (max instanceof IntValue)) {
                IntValue intMax = (IntValue)max;
                m_maxIntValue.setValue(new Integer(intMax.getIntValue()));
            }
        }

        // and the possible values - if set
        if ((domain != null) && (domain.getValues() != null)) {
            Set<DataCell> valList = domain.getValues();
            if (m_valueList != null) {
                m_valueList.setListData(valList.toArray());
            }
            if (m_containsVals != null) {
                m_containsVals.setSelected(true);
            }
        } else {
            if (m_containsVals != null) {
                m_containsVals.setSelected(false);
            }
        }

        // now show the dialog, show it and wait until it comes back.

        setTitle("New settings for column '"
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
     * @return an object with domain values set by the user. Or null if settings
     *         are invalid. Then, a error message box is displayed.
     */
    private ColProperty takeOverSettings() {

        ColProperty result = new ColProperty();
        DataColumnSpecCreator dcsc = 
            new DataColumnSpecCreator(
                m_colProp.getColumnSpec().getName(), 
                m_colProp.getColumnSpec().getType());
        
        if (m_noDomain.isSelected()) {
            // create a colProp without domain - and don't read from file
            result.setReadBoundsFromFile(false);
            result.setReadPossibleValuesFromFile(false);
            result.setColumnSpec(dcsc.createSpec());
        } else {
            result.setReadBoundsFromFile(false);
            result.setReadPossibleValuesFromFile(false);
            if (((m_minDblValue != null) && (m_maxDblValue != null))
                    || ((m_minIntValue != null) && (m_maxIntValue != null))) {
                // If we have a range edit field, check the read range from file
                result.setReadBoundsFromFile(m_readFromFile.isSelected());
            }
            if (m_valueList != null) {
                // if we have a list of poss vals, also check the from file
                result.setReadPossibleValuesFromFile(m_readFromFile
                        .isSelected());
            }
            if ((m_containsVals != null) && (!m_containsVals.isSelected())) {
                // set it back to false if user said no values!
                result.setReadPossibleValuesFromFile(false);
            }

            DataCell min = null;
            DataCell max = null;
            Set<DataCell> pVals = null;
            // transfer range settings - if not supposed to read from file
            if (!result.getReadBoundsFromFile()) {
                if ((m_minDblValue != null) && (m_maxDblValue != null)) {
                    try {
                        min = new DoubleCell(
                                readDblSpinner(m_minDblValue));
                        max = new DoubleCell(
                                readDblSpinner(m_maxDblValue));
                    } catch (ParseException pe) {
                        JOptionPane.showMessageDialog(
                                this,
                                "The specified range is invalid. "
                                + "Enter valid numbers or press cancel.",
                                "Invalid range numbers",
                                JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                }
                if ((m_minIntValue != null) && (m_maxIntValue != null)) {
                    try {
                        min = new IntCell(readIntSpinner(m_minIntValue));
                        max = new IntCell(readIntSpinner(m_maxIntValue));
                    } catch (ParseException pe) {
                        JOptionPane.showMessageDialog(
                                this,
                                "The specified range is invalid. "
                                + "Enter valid numbers or press cancel.",
                                "Invalid range numbers",
                                JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                }
                if ((min != null) && (max != null)) {
                    DataCellComparator comp = DataType.getCommonSuperType(
                            min.getType(), max.getType()).getComparator();
                    if (comp.compare(min, max) > 0) {
                        JOptionPane.showMessageDialog(
                                this,
                                "The maximum value is smaller than the mini"
                                + "mum. Enter a valid range or press cancel.",
                                "Invalid range",
                                JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                }
            }
            // tranfser possible values - if not supposed to read them from file
            if (!result.getReadPossibleValuesFromFile()) {
                if (m_valueList != null) {
                    if ((m_containsVals == null) 
                            || m_containsVals.isSelected()) {
                        int valCount = m_valueList.getModel().getSize();
                        pVals = new LinkedHashSet<DataCell>();
                        for (int i = 0; i < valCount; i++) {
                            DataCell val = (DataCell)m_valueList.getModel()
                                    .getElementAt(i);
                            pVals.add(val);
                            // if we also have a range it must be inside
                            if ((min != null) && (max != null)) {
                                DataCellComparator minComp = DataType
                                        .getCommonSuperType(min.getType(),
                                                val.getType()).getComparator();
                                DataCellComparator maxComp = DataType
                                        .getCommonSuperType(max.getType(),
                                                val.getType()).getComparator();
                                if ((minComp.compare(min, val) > 0)
                                        || (maxComp.compare(max, val) < 0)) {
                                    JOptionPane.showMessageDialog(
                                            this,
                                            "A possible value is outside the "
                                            + "specified range. Adjust range or"
                                            + " press cancel.",
                                            "Incompatible range and possible"
                                            + " value",
                                            JOptionPane.ERROR_MESSAGE);
                                    return null;

                                }
                            }
                        }
                    }
                }
            }

            // if we got range or values create domain now
            if (((min != null) && (max != null))
                    || ((pVals != null) && (pVals.size() > 0))) {

                // all or some of these values could be null, which is okay.
                DataColumnDomainCreator domainCreator = 
                    new DataColumnDomainCreator(pVals, min, max);
                dcsc.setDomain(domainCreator.createDomain());
            }
            
            result.setColumnSpec(dcsc.createSpec());
            
        } // end of else of if (m_noDomain.isSelected())

        return result;

    }

    /*
     * sets the enable status of all components depending on the different
     * checkboxes: "no domain" disables all. "read from file" disables most edit
     * fields", and "contains values" disables the possible values fields. But
     * they all depend on each other.
     */
    private void setEnableStatus() {

        // if the "no domain settings" is checked _everything_ is disabled
        if (m_noDomain.isSelected()) {
            m_readFromFile.setEnabled(false);
            enableAllFields(false);
        } else {
            m_readFromFile.setEnabled(true);
            if (m_readFromFile.isSelected()) {
                // every thing is disabled - except the "contains nominal
                // values" box for integer values
                enableAllFields(false);
                if (m_containsVals != null) {
                    // enable it again
                    m_containsVals.setEnabled(true);
                }
            } else {
                // here we want domain settings and not reading them from file
                enableAllRangeFields(true);
                if (m_containsVals != null) {
                    m_containsVals.setEnabled(true);
                    enableAllValueFields(m_containsVals.isSelected());
                } else {
                    enableAllValueFields(true);
                }
            }
        }
    }

    /*
     * enables or disables all user editable fields related to domain values,
     * depending on the value of the passed parameter
     */
    private void enableAllFields(final boolean enable) {

        enableAllRangeFields(enable);

        if (m_containsVals != null) {
            m_containsVals.setEnabled(enable);
        }

        enableAllValueFields(enable);

    }

    private void enableAllRangeFields(final boolean enable) {
        // components for the range
        if (m_minDblValue != null) {
            m_minDblValue.setEnabled(enable);
        }
        if (m_maxDblValue != null) {
            m_maxDblValue.setEnabled(enable);
        }
        if (m_minIntValue != null) {
            m_minIntValue.setEnabled(enable);
        }
        if (m_maxIntValue != null) {
            m_maxIntValue.setEnabled(enable);
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

    /*
     * read the current value from the spinner assuming it contains Integers.
     * Commits user changes before reading it.
     */
    private int readIntSpinner(final JSpinner integerSpinner)
            throws ParseException {
        integerSpinner.commitEdit();
        // if the spinner has the focus, the currently edited value
        // might not be commited. Now it is!
        SpinnerNumberModel snm = (SpinnerNumberModel)integerSpinner.getModel();
        return snm.getNumber().intValue();
    }

    /*
     * read the current value from the spinner assuming it contains Doubles
     * Commits user changes before reading it.
     */
    private double readDblSpinner(final JSpinner dblSpinner)
            throws ParseException {
        dblSpinner.commitEdit();
        // if the spinner has the focus, the currently edited value
        // might not be commited. Now it is!
        SpinnerNumberModel snm = (SpinnerNumberModel)dblSpinner.getModel();
        return snm.getNumber().doubleValue();
    }

}
