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
 * load/store into config object. The type of the number entered is determined
 * by the {@link SettingsModel} passed to the constructor (currently supported
 * are double and int).
 * 
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentNumber extends DialogComponent {

    // the default width of the spinner if no width and no default value is set
    private static final int FIELD_DEFWIDTH = 10;
    
    private static final int FIELD_MINWIDTH = 2;

    private JSpinner m_spinner;

    /**
     * Constructor puts a label and spinner (10 characters wide) into a panel.
     * 
     * @param numberModel the SettingsModel determining the number type (double
     *            or int)
     * @param label label for dialog in front of the spinner
     * @param stepSize step size for the spinner
     */
    public DialogComponentNumber(final SettingsModelNumber numberModel,
            final String label, final Number stepSize) {
        this(numberModel, label, stepSize, 
                calcDefaultWidth(numberModel.getNumberValueStr()));
    }

    /**
     * Constructor put label and spinner into panel.
     * 
     * @param numberModel the SettingsModel determining the number type (double
     *            or int)
     * @param label label for dialog in front of the spinner
     * @param stepSize step size for the spinner
     * @param compWidth the width (number of columns/characters) of the spinner
     */
    public DialogComponentNumber(final SettingsModelNumber numberModel,
            final String label, final Number stepSize, final int compWidth) {
        super(numberModel);

        if (compWidth < 1) {
            throw new IllegalArgumentException("Width of component can't be "
                    + "smaller than 1");
        }
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
        editor.getTextField().setColumns(compWidth);
        editor.getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT);

        m_spinner.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                try {
                    updateModel();
                } catch (InvalidSettingsException ise) {
                    // ignore it here.
                }
            }
        });

        // We are not updating the model immediately when the user changes
        // the value. We update the model right before save.

        // update the spinner, whenever the model changed
        getModel().prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

        this.add(m_spinner);
    }

    /**
     * @param defaultValue the default value in the component
     * @return the width of the spinner, derived from the defaultValue.
     */
    private static int calcDefaultWidth(final String defaultValue) {
        if ((defaultValue == null) || (defaultValue.length() == 0)) {
            // no default value, return the default width of 10
            return FIELD_DEFWIDTH;
        }
        if (defaultValue.length() < FIELD_MINWIDTH) {
            // spinner should be at least 2 columns wide.
            return FIELD_MINWIDTH;
        }
        return defaultValue.length();
        
    }
    
    /**
     * @see org.knime.core.node.defaultnodesettings.DialogComponent
     *      #updateComponent()
     */
    @Override
    void updateComponent() {
        // update the component only if it contains a different value than the
        // model
        try {
            m_spinner.commitEdit();
            if (getModel() instanceof SettingsModelDouble) {
                SettingsModelDouble model = (SettingsModelDouble)getModel();
                double val = ((Double)m_spinner.getValue()).doubleValue();
                if (val != model.getDoubleValue()) {
                    m_spinner.setValue(new Double(model.getDoubleValue()));
                }
            } else {
                SettingsModelInteger model = (SettingsModelInteger)getModel();
                int val = ((Integer)m_spinner.getValue()).intValue();
                if (val != model.getIntValue()) {
                    m_spinner.setValue(new Integer(model.getIntValue()));
                }
            }
        } catch (ParseException e) {
            // spinner contains invalid value - update component!
            if (getModel() instanceof SettingsModelDouble) {
                SettingsModelDouble model = (SettingsModelDouble)getModel();
                m_spinner.setValue(new Double(model.getDoubleValue()));
            } else {
                SettingsModelInteger model = (SettingsModelInteger)getModel();
                m_spinner.setValue(new Integer(model.getIntValue()));
            }
        }
    }

    /**
     * Transfers the value from the spinner into the model. Colors the spinner
     * red, if the number is not accepted by the settingsmodel. And throws an
     * exception then.
     * 
     * @throws InvalidSettingsException if the number was not accepted by the
     *             model (reason could be an out of range, or just an invalid
     *             input).
     * 
     */
    private void updateModel() throws InvalidSettingsException {
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
     * @see DialogComponent#validateStettingsBeforeSave()
     */
    @Override
    void validateStettingsBeforeSave() throws InvalidSettingsException {
        updateModel();
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
