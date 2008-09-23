/*
 * ------------------------------------------------------------------ *
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

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

    private final JLabel m_label;

    private final JLabel m_labelMin;

    private final JLabel m_labelMax;

    private final JSpinner m_spinnerMin;

    private final JSpinner m_spinnerMax;

    /**
     * Constructor assumes the range between 0 and 10.
     * 
     * @param model stores the double numbers entered.
     * @param label the text showing next to the components.
     * @deprecated use {@link #DialogComponentDoubleRange(
     * SettingsModelDoubleRange, double, double, double, String)} or this
     * {@link #DialogComponentDoubleRange(SettingsModelDoubleRange, 
     * double, double, double, double, double, double, String)} constructor 
     * instead.
     */
    @Deprecated
    public DialogComponentDoubleRange(final SettingsModelDoubleRange model,
            final String label) {
        // old behavior
        this(model, 0.0, 10.0, 0.01, 0.0, 10.0, 0.1, label);
    }
    
    /**
     * Creates two spinner to enter the lower and upper value of the range.
     * @param model stores the double numbers entered
     * @param lowerMin minimum value to be entered
     * @param upperMax maximum value to be entered
     * @param stepSize step size for the spinners
     * @param label label for this component
     */
    public DialogComponentDoubleRange(final SettingsModelDoubleRange model,
            final double lowerMin, final double upperMax, 
            final double stepSize, final String label) {
        this(model, lowerMin, upperMax, stepSize, 
                lowerMin, upperMax, stepSize, label);
    }
    
    /**
     * Finegrain constructor to specify minimum and maximum values for the 
     * lower and upper bound and different step sizes for each spinner.
     * 
     * @param model stores the double numbers entered
     * @param lowerMin minimum value for the lower bound spinner
     * @param lowerMax maximum value for the lower bound spinner
     * @param lowerStepSize step size for the lower bound spinner
     * @param upperMin minimum value for the upper bound spinner
     * @param upperMax maximum value for the upper bound spinner
     * @param upperStepSize step size for the upper bound spinner
     * @param label label for this component
     */
    public DialogComponentDoubleRange(final SettingsModelDoubleRange model,
            final double lowerMin, final double lowerMax, 
            final double lowerStepSize,
            final double upperMin, final double upperMax, 
            final double upperStepSize, final String label) {
        super(model);
        
        model.prependChangeListener(new ChangeListener() {
           public void stateChanged(final ChangeEvent e) {
               updateComponent();
           } 
        });
        
        JPanel myPanel = getComponentPanel();
        m_label = new JLabel(label);
        m_labelMin = new JLabel("min=");
        m_labelMax = new JLabel("max=");
        m_spinnerMin =
                new JSpinner(new SpinnerNumberModel(model.getMinRange(), 
                        lowerMin, lowerMax, lowerStepSize));
        m_spinnerMin.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent arg0) {
                    updateMinModel();
            }
        });
        JSpinner.DefaultEditor editor =
                (JSpinner.DefaultEditor)m_spinnerMin.getEditor();
        editor.getTextField().setColumns(new String("" + lowerMax).length());
        editor.getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT);
        m_spinnerMax =
                new JSpinner(new SpinnerNumberModel(model.getMaxRange(), 
                        upperMin, upperMax, upperStepSize));
        m_spinnerMax.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent arg0) {
                    updateMaxModel();
            }
        });
        editor = (JSpinner.DefaultEditor)m_spinnerMax.getEditor();
        editor.getTextField().setColumns(new String("" + upperMax).length());
        editor.getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT);
        myPanel.add(m_label);
        myPanel.add(m_labelMin);
        myPanel.add(m_spinnerMin);
        myPanel.add(m_labelMax);
        myPanel.add(m_spinnerMax);        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // Nothing to check, we don't care about the specs.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_spinnerMin.setEnabled(enabled);
        m_spinnerMax.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_spinnerMin.setToolTipText(text);
        m_spinnerMax.setToolTipText(text);
    }
    
    
    /**
     * Transfers the value from the spinner into the model. Colors the spinner
     * red, if the number is not accepted by the settings model. And throws an
     * exception then.
     * 
     */
    private void updateMinModel() {
        try {
            m_spinnerMin.commitEdit();
            if (getModel() instanceof SettingsModelDoubleRange) {
                SettingsModelDoubleRange model 
                    = (SettingsModelDoubleRange)getModel();
                model.setMinRange(((Double)m_spinnerMin.getValue())
                        .doubleValue());
            } 
        } catch (ParseException e) {
            JComponent editorMin = m_spinnerMin.getEditor();
            if (editorMin instanceof DefaultEditor) {
                showError(((DefaultEditor)editorMin).getTextField());
            }
        }
    }
    
    private void updateMaxModel() {
        try {
            m_spinnerMax.commitEdit();
            if (getModel() instanceof SettingsModelDoubleRange) {
                SettingsModelDoubleRange model 
                    = (SettingsModelDoubleRange)getModel();
                model.setMaxRange(((Double)m_spinnerMax.getValue())
                        .doubleValue());
            } 
        } catch (ParseException e) {
            JComponent editorMax = m_spinnerMax.getEditor();
            if (editorMax instanceof DefaultEditor) {
                showError(((DefaultEditor)editorMax).getTextField());
            }
        }        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        
        // clear any possible error indication
        JComponent editor = m_spinnerMin.getEditor();
        if (editor instanceof DefaultEditor) {
            clearError(((DefaultEditor)editor).getTextField());
        }
        editor = m_spinnerMax.getEditor();
        if (editor instanceof DefaultEditor) {
            clearError(((DefaultEditor)editor).getTextField());
        }
        
        // update the spinners
        SettingsModelDoubleRange model = (SettingsModelDoubleRange)getModel();
        double valMin = ((Double)m_spinnerMin.getValue()).doubleValue();
        if (valMin != model.getMinRange()) {
            m_spinnerMin.setValue(new Double(model.getMinRange()));
        }
        double valMax = ((Double)m_spinnerMax.getValue()).doubleValue();
        if (valMax != model.getMaxRange()) {
            m_spinnerMax.setValue(new Double(model.getMaxRange()));
        }

        // update enable status
        setEnabledComponents(model.isEnabled());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
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

        try {
            new SettingsModelDoubleRange(model.getConfigName(), newMin, newMax);
        } catch (IllegalArgumentException iae) {
            JComponent editor = m_spinnerMax.getEditor();
            if (editor instanceof DefaultEditor) {
                showError(((DefaultEditor)editor).getTextField());
            }
            editor = m_spinnerMin.getEditor();
            if (editor instanceof DefaultEditor) {
                showError(((DefaultEditor)editor).getTextField());
            }
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

}
