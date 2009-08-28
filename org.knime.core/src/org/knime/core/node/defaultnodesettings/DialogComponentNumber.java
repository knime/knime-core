/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.ScopeVariableModel;
import org.knime.core.node.ScopeVariableModelButton;
import org.knime.core.node.port.PortObjectSpec;

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

    private final JSpinner m_spinner;
    private final ScopeVariableModelButton m_svmButton;

    private final JLabel m_label;

    /**
     * Constructor puts a label and spinner (with default width) into a panel.
     *
     * @param numberModel the SettingsModel determining the number type (double
     *            or int)
     * @param label label for dialog in front of the spinner
     * @param stepSize step size for the spinner
     */
    public DialogComponentNumber(final SettingsModelNumber numberModel,
            final String label, final Number stepSize) {
        this(numberModel, label, stepSize, calcDefaultWidth(numberModel),
                null);
    }

    /** Puts a label and spinner with default width into panel, offers also
     * the registration of a {@link ScopeVariableModel}.
     * @param numberModel the SettingsModel determining the number type (double
     *            or int)
     * @param label label for dialog in front of the spinner
     * @param stepSize step size for the spinner
     * @param svm The variable model (for displaying a little icon next to the 
     * component to overwrite the settings with variables). Can be null.
     */
    public DialogComponentNumber(final SettingsModelNumber numberModel,
            final String label, final Number stepSize,
            final ScopeVariableModel svm) {
        this(numberModel, label, stepSize, calcDefaultWidth(numberModel),
                svm);
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
        this (numberModel, label, stepSize, compWidth, null);
    }

    /**
     * Constructor put label and spinner into panel.
     * @param numberModel the SettingsModel determining the number type (double
     *            or int)
     * @param label label for dialog in front of the spinner
     * @param stepSize step size for the spinner
     * @param compWidth the width (number of columns/characters) of the spinner
     * @param svm The variable model (for displaying a little icon next to the 
     * component to overwrite the settings with variables). Can be null.
     */
    public DialogComponentNumber(final SettingsModelNumber numberModel,
            final String label, final Number stepSize, final int compWidth,
            final ScopeVariableModel svm) {
        super(numberModel);

        if (compWidth < 1) {
            throw new IllegalArgumentException("Width of component can't be "
                    + "smaller than 1");
        }
        m_label = new JLabel(label);
        getComponentPanel().add(m_label);

        SpinnerNumberModel spinnerModel;
        if (numberModel instanceof SettingsModelDouble) {
            final SettingsModelDouble dblModel =
                (SettingsModelDouble)numberModel;
            Double min = null;
            Double max = null;
            if (dblModel instanceof SettingsModelDoubleBounded) {
                min = ((SettingsModelDoubleBounded)dblModel).getLowerBound();
                max = ((SettingsModelDoubleBounded)dblModel).getUpperBound();
            }
            spinnerModel =
                    new SpinnerNumberModel(dblModel.getDoubleValue(), min, max,
                            stepSize);
        } else if (numberModel instanceof SettingsModelInteger) {
            final SettingsModelInteger intModel =
                (SettingsModelInteger)numberModel;
            Integer min = null;
            Integer max = null;
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
        if (numberModel instanceof SettingsModelDouble) {
            m_spinner.setEditor(new JSpinner.NumberEditor(m_spinner,
                       "0.0##############"));
        }
        final JSpinner.DefaultEditor editor =
                (JSpinner.DefaultEditor)m_spinner.getEditor();
        editor.getTextField().setColumns(compWidth);
        editor.getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT);

        m_spinner.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
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

        getComponentPanel().add(m_spinner);
        
        // add variable editor button if so desired
        if (svm != null) {
            svm.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent evt) {
                    m_spinner.setEnabled(!svm.isVariableReplacementEnabled());
                }
            });
            m_svmButton = new ScopeVariableModelButton(svm);
            getComponentPanel().add(m_svmButton);
        } else {
            m_svmButton = null;
        }

        //call this method to be in sync with the settings model
        updateComponent();
    }

    /**
     * Tries to calculate a field width from the model specified. If the model
     * is a bounded int/double model, is uses the max value to determine the
     * width, if its not bounded, it uses the actual value.
     *
     * @param model number model to derive field width from
     * @return the width of the spinner, derived from the values in the model.
     */
    private static int calcDefaultWidth(final SettingsModelNumber model) {

        if (model == null) {
            // no model, return the default width of 10
            return FIELD_DEFWIDTH;
        }

        String value = "12";
        if (model instanceof SettingsModelIntegerBounded) {
            value =
                    Integer.toString(((SettingsModelIntegerBounded)model)
                            .getUpperBound());
        } else if (model instanceof SettingsModelDoubleBounded) {
            value =
                    Double.toString(((SettingsModelDoubleBounded)model)
                            .getUpperBound());
        } else {
            value = model.getNumberValueStr();
        }

        if (value.length() < FIELD_MINWIDTH) {
            // spinner should be at least 2 columns wide.
            return FIELD_MINWIDTH;
        }
        return value.length();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {

        final JComponent editor = m_spinner.getEditor();
        if (editor instanceof DefaultEditor) {
            clearError(((DefaultEditor)editor).getTextField());
        }

        // update the component only if it contains a different value than the
        // model
        try {
            m_spinner.commitEdit();
            if (getModel() instanceof SettingsModelDouble) {
                final SettingsModelDouble model =
                    (SettingsModelDouble)getModel();
                final double val = ((Double)m_spinner.getValue()).doubleValue();
                if (val != model.getDoubleValue()) {
                    m_spinner.setValue(new Double(model.getDoubleValue()));
                }
            } else {
                final SettingsModelInteger model =
                    (SettingsModelInteger)getModel();
                final int val = ((Integer)m_spinner.getValue()).intValue();
                if (val != model.getIntValue()) {
                    m_spinner.setValue(new Integer(model.getIntValue()));
                }
            }
        } catch (final ParseException e) {
            // spinner contains invalid value - update component!
            if (getModel() instanceof SettingsModelDouble) {
                final SettingsModelDouble model =
                    (SettingsModelDouble)getModel();
                m_spinner.setValue(new Double(model.getDoubleValue()));
            } else {
                final SettingsModelInteger model =
                    (SettingsModelInteger)getModel();
                m_spinner.setValue(new Integer(model.getIntValue()));
            }
        }

        // also update the enable status of all components...
        setEnabledComponents(getModel().isEnabled());
    }

    /**
     * Transfers the value from the spinner into the model. Colors the spinner
     * red, if the number is not accepted by the settings model. And throws an
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
                final SettingsModelDouble model =
                    (SettingsModelDouble)getModel();
                model.setDoubleValue(((Double)m_spinner.getValue())
                        .doubleValue());
            } else {
                final SettingsModelInteger model =
                    (SettingsModelInteger)getModel();
                model.setIntValue(((Integer)m_spinner.getValue()).intValue());
            }
        } catch (final ParseException e) {
            final JComponent editor = m_spinner.getEditor();
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
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // we're always good - independent of the incoming spec
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        boolean spinnerEnabled = enabled;
        // enable the spinner according to the variable model
        if (m_svmButton != null) {
            ScopeVariableModel svmModel = m_svmButton.getScopeVariableModel();
            if (svmModel.isVariableReplacementEnabled()) {
                spinnerEnabled = false;
            }
            m_svmButton.setEnabled(enabled);
        }
        m_spinner.setEnabled(spinnerEnabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_spinner.setToolTipText(text);
        m_label.setToolTipText(text);
    }

}
