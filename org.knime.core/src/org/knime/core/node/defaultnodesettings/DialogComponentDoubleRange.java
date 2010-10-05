/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

        final JPanel myPanel = getComponentPanel();
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
        editor.getTextField().setColumns(10);
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
        editor.getTextField().setColumns(10);
        editor.getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT);
        myPanel.add(m_label);
        myPanel.add(m_labelMin);
        myPanel.add(m_spinnerMin);

        myPanel.add(m_labelMax);
        myPanel.add(m_spinnerMax);
        //call this method to be in sync with the settings model
        updateComponent();
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
                final SettingsModelDoubleRange model
                    = (SettingsModelDoubleRange)getModel();
                model.setMinRange(((Double)m_spinnerMin.getValue())
                        .doubleValue());
            }
        } catch (final ParseException e) {
            final JComponent editorMin = m_spinnerMin.getEditor();
            if (editorMin instanceof DefaultEditor) {
                showError(((DefaultEditor)editorMin).getTextField());
            }
        }
    }

    private void updateMaxModel() {
        try {
            m_spinnerMax.commitEdit();
            if (getModel() instanceof SettingsModelDoubleRange) {
                final SettingsModelDoubleRange model
                    = (SettingsModelDoubleRange)getModel();
                model.setMaxRange(((Double)m_spinnerMax.getValue())
                        .doubleValue());
            }
        } catch (final ParseException e) {
            final JComponent editorMax = m_spinnerMax.getEditor();
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
        final SettingsModelDoubleRange model =
            (SettingsModelDoubleRange)getModel();
        final double valMin = ((Double)m_spinnerMin.getValue()).doubleValue();
        if (valMin != model.getMinRange()) {
            m_spinnerMin.setValue(new Double(model.getMinRange()));
        }
        final double valMax = ((Double)m_spinnerMax.getValue()).doubleValue();
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
        final SettingsModelDoubleRange model =
            (SettingsModelDoubleRange)getModel();
        double newMin;
        double newMax;
        // try to commit Minimum
        try {
            m_spinnerMin.commitEdit();
            newMin = ((Double)m_spinnerMin.getValue()).doubleValue();
        } catch (final ParseException e) {
            final JComponent editor = m_spinnerMin.getEditor();
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
        } catch (final ParseException e) {
            final JComponent editor = m_spinnerMax.getEditor();
            if (editor instanceof DefaultEditor) {
                showError(((DefaultEditor)editor).getTextField());
            }
            String errMsg = "Invalid number format. ";
            errMsg += "Please enter a valid maximum.";
            throw new InvalidSettingsException(errMsg);
        }

        try {
            new SettingsModelDoubleRange(model.getConfigName(), newMin, newMax);
        } catch (final IllegalArgumentException iae) {
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
