/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   18.09.2005 (mb): created
 *   25.09.2006 (ohl): using SettingModel
 */
package org.knime.core.node.defaultnodesettings;

import java.text.ParseException;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;

/**
 * Provide a standard component for a dialog that allows to edit number value.
 * Provides label and spinner that checks ranges as well as functionality to
 * load/store into config object. The type of the number entered is detemined by
 * the {@link SettingsModel} passed to the contructor (currenty supported are
 * double and int).
 * 
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentNumber extends DialogComponent {

    private JSpinner m_spinner;

    /**
     * Constructor put label and spinner into panel.
     * 
     * @param numberModel the SettingsModel determining the number type (double
     *            or int)
     * @param label label for dialog in front of the spinner
     * @param stepSize step size for the spinner
     */
    public DialogComponentNumber(final SettingsModelNumber numberModel,
            final String label, final Number stepSize) {
        super(numberModel);

        this.add(new JLabel(label));

        SpinnerNumberModel spinnerModel;
        if (numberModel instanceof SettingsModelDouble) {
            SettingsModelDouble dblModel = (SettingsModelDouble)numberModel;
            Comparable min = null;
            Comparable max = null;
            if (dblModel instanceof SettingsModelDoubleBounded) {
                min = ((SettingsModelDoubleBounded)dblModel).getLowerBound();
                max = ((SettingsModelDoubleBounded)dblModel).getUpperBound();
            }
            spinnerModel =
                    new SpinnerNumberModel(dblModel.getDoubleValue(), min, max,
                            stepSize);
        } else if (numberModel instanceof SettingsModelInteger) {
            SettingsModelInteger intModel = (SettingsModelInteger)numberModel;
            Comparable min = null;
            Comparable max = null;
            if (intModel instanceof SettingsModelIntegerBounded) {
                min = ((SettingsModelIntegerBounded)intModel).getLowerBound();
                max = ((SettingsModelIntegerBounded)intModel).getUpperBound();
            }
            spinnerModel =
                    new SpinnerNumberModel(intModel.getIntValue(), min, max,
                            stepSize);
        } else {
            throw new IllegalArgumentException("Only Double and Integer are "
                    + "currently supported by the NumberComponent");
        }
        m_spinner = new JSpinner(spinnerModel);

        JSpinner.DefaultEditor editor =
                (JSpinner.DefaultEditor)m_spinner.getEditor();
        editor.getTextField().setColumns(6);
        editor.getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT);

        // We are not updating the model immediately when the user changes
        // the value. We update the model right before save.

        // update the spinner, whenever the model changed
        getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                SettingsModelNumber model = (SettingsModelNumber)getModel();
                if (model instanceof SettingsModelDouble) {
                    m_spinner.setValue(new Double(((SettingsModelDouble)model)
                            .getDoubleValue()));
                } else {
                    m_spinner.setValue(new Integer(
                            ((SettingsModelInteger)model).getIntValue()));
                }
                // show the new value in the component
                JComponent ed = m_spinner.getEditor();
                if (ed instanceof DefaultEditor) {
                    ((DefaultEditor)ed).getTextField().setValue(
                            m_spinner.getValue());
                }

            }
        });

        this.add(m_spinner);
    }

    /**
     * @see DialogComponent#validateStettingsBeforeSave()
     */
    @Override
    void validateStettingsBeforeSave() throws InvalidSettingsException {
        try {
            m_spinner.commitEdit();
            if (getModel() instanceof SettingsModelDouble) {
                SettingsModelDouble model = (SettingsModelDouble)getModel();
                model.setDoubleValue(((Double)m_spinner.getValue())
                        .doubleValue());
            } else {
                SettingsModelInteger model = (SettingsModelInteger)getModel();
                model.setIntValue(((Integer)m_spinner.getValue()).intValue());
            }
        } catch (ParseException e) {
            JComponent editor = m_spinner.getEditor();
            if (editor instanceof DefaultEditor) {
                showError(((DefaultEditor)editor).getTextField());
            }
            String errMsg = "Invalid number format. ";
            if (getModel() instanceof SettingsModelDouble) {
                errMsg += "Please enter a valid floating point number.";
            }
            if (getModel() instanceof SettingsModelInteger) {
                errMsg += "Please enter a valid integer number.";
            }
            throw new InvalidSettingsException(errMsg);
        }
    }

    /**
     * @see DialogComponent
     *      #checkConfigurabilityBeforeLoad(org.knime.core.data.DataTableSpec[])
     */
    @Override
    void checkConfigurabilityBeforeLoad(final DataTableSpec[] specs)
            throws NotConfigurableException {
        // we're always good - independent of the incoming spec
    }

    /**
     * @see DialogComponent#setEnabledComponents(boolean)
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_spinner.setEnabled(enabled);
    }
}
