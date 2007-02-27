/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   17.01.2007 (mb): created
 */
package org.knime.core.node.defaultnodesettings;

import java.text.ParseException;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner.DefaultEditor;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;

/**
 * Allows the user to enter a floating point number range. It shows two spinners
 * labeled "min=" and "max=" which expect each a floating point number. The
 * component requires a SettingsModelDoubleRange with its constructor, that
 * holds the two values entered.
 * 
 * @see SettingsModelDoubleRange
 * 
 * @author berthold, University of Konstanz
 */
public class DialogComponentDoubleRange extends DialogComponent {

    private JLabel m_label;

    private JLabel m_labelMin;

    private JLabel m_labelMax;

    private JSpinner m_spinnerMin;

    private JSpinner m_spinnerMax;

    /**
     * Constructor.
     * 
     * @param model stores the double numbers entered.
     * @param label the text showing next to the components.
     */
    public DialogComponentDoubleRange(final SettingsModelDoubleRange model,
            final String label) {
        super(model);
        JPanel myPanel = getComponentPanel();
        m_label = new JLabel(label);
        m_labelMin = new JLabel("min=");
        m_labelMax = new JLabel("max=");
        m_spinnerMin =
                new JSpinner(new SpinnerNumberModel(0.0, 0.0, 10.0, 0.01));
        JSpinner.DefaultEditor editor =
                (JSpinner.DefaultEditor)m_spinnerMin.getEditor();
        editor.getTextField().setColumns(10);
        editor.getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT);
        m_spinnerMax =
                new JSpinner(new SpinnerNumberModel(0.0, 0.0, 10.0, 0.01));
        editor = (JSpinner.DefaultEditor)m_spinnerMax.getEditor();
        editor.getTextField().setColumns(10);
        editor.getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT);
        myPanel.add(m_label);
        myPanel.add(m_labelMin);
        myPanel.add(m_spinnerMin);
        myPanel.add(m_labelMax);
        myPanel.add(m_spinnerMax);
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.DialogComponent
     *      #checkConfigurabilityBeforeLoad(org.knime.core.data.DataTableSpec[])
     */
    @Override
    void checkConfigurabilityBeforeLoad(final DataTableSpec[] specs)
            throws NotConfigurableException {
        // Nothing to check, we don't care about the specs.
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.DialogComponent
     *      #setEnabledComponents(boolean)
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_spinnerMin.setEnabled(enabled);
        m_spinnerMax.setEnabled(enabled);
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.DialogComponent
     *      #setToolTipText(java.lang.String)
     */
    @Override
    public void setToolTipText(final String text) {
        m_spinnerMin.setToolTipText(text);
        m_spinnerMax.setToolTipText(text);
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.DialogComponent
     *      #updateComponent()
     */
    @Override
    void updateComponent() {
        SettingsModelDoubleRange model = (SettingsModelDoubleRange)getModel();
        double valMin = ((Double)m_spinnerMin.getValue()).doubleValue();
        if (valMin != model.getMinRange()) {
            m_spinnerMin.setValue(new Double(model.getMinRange()));
        }
        double valMax = ((Double)m_spinnerMax.getValue()).doubleValue();
        if (valMax != model.getMaxRange()) {
            m_spinnerMax.setValue(new Double(model.getMaxRange()));
        }
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.DialogComponent
     *      #validateStettingsBeforeSave()
     */
    @Override
    void validateStettingsBeforeSave() throws InvalidSettingsException {
        SettingsModelDoubleRange model = (SettingsModelDoubleRange)getModel();
        double newMin;
        double newMax;
        // try to commit Minimum
        try {
            m_spinnerMin.commitEdit();
            newMin = ((Double)m_spinnerMin.getValue()).doubleValue();
        } catch (ParseException e) {
            JComponent editor = m_spinnerMin.getEditor();
            if (editor instanceof DefaultEditor) {
                showError(((DefaultEditor)editor).getTextField());
            }
            String errMsg = "Invalid number format. ";
            errMsg += "Please enter a valid minimum.";
            throw new InvalidSettingsException(errMsg);
        }
        // try to commit Maximum
        try {
            m_spinnerMax.commitEdit();
            newMax = ((Double)m_spinnerMax.getValue()).doubleValue();
        } catch (ParseException e) {
            JComponent editor = m_spinnerMax.getEditor();
            if (editor instanceof DefaultEditor) {
                showError(((DefaultEditor)editor).getTextField());
            }
            String errMsg = "Invalid number format. ";
            errMsg += "Please enter a valid maximum.";
            throw new InvalidSettingsException(errMsg);
        }
        
        model.setRange(newMin, newMax);
    }

}
