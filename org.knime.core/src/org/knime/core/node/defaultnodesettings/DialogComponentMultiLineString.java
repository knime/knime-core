/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   12.01.2007 (thiel): created
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class DialogComponentMultiLineString extends DialogComponent {

    // the min/max and default width of the editfield, if not set explicitly
    private static final int FIELD_DEFCOLS = 50;

    private static final int FIELD_DEFROWS = 3;

    private final JTextArea m_valueField;

    private final JLabel m_label;

    private final boolean m_disallowEmtpy;

    /**
     * Constructor put label and JTextArea into panel. It will accept empty
     * strings as legal input.
     *
     * @param stringModel the model that stores the value for this component.
     * @param label label for dialog in front of JTextArea
     */
    public DialogComponentMultiLineString(
            final SettingsModelString stringModel, final String label) {
        this(stringModel, label, false, FIELD_DEFCOLS, FIELD_DEFROWS);
    }

    /**
     * Constructor put label and JTextArea into panel.
     *
     * @param label label for dialog in front of JTextArea
     * @param stringModel the model that stores the value for this component.
     * @param disallowEmptyString if set true, the component request a non-empty
     *            string from the user.
     * @param cols the number of columns.
     * @param rows the number of rows.
     */
    public DialogComponentMultiLineString(
            final SettingsModelString stringModel, final String label,
            final boolean disallowEmptyString, final int cols, final int rows) {
        super(stringModel);

        m_disallowEmtpy = disallowEmptyString;

        getComponentPanel().setLayout(new BorderLayout());

        m_label = new JLabel(label);
        getComponentPanel().add(m_label, BorderLayout.NORTH);

        m_valueField = new JTextArea();
        m_valueField.setColumns(cols);
        m_valueField.setRows(rows);

        final JScrollPane jsp = new JScrollPane(m_valueField);
        getComponentPanel().add(jsp, BorderLayout.CENTER);

        m_valueField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
                    // Ignore it here.
                }
            }

            public void insertUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
                    // Ignore it here.
                }
            }

            public void changedUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
                    // Ignore it here.
                }
            }
        });

        // update the text field, whenever the model changes
        getModel().prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

        //call this method to be in sync with the settings model
        updateComponent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {

        clearError(m_valueField);

        // update component only if values are out of sync
        final String str = ((SettingsModelString)getModel()).getStringValue();
        if (!m_valueField.getText().equals(str)) {
            m_valueField.setText(str);
        }

        setEnabledComponents(getModel().isEnabled());
    }

    /**
     * Transfers the current value from the component into the model.
     *
     * @throws InvalidSettingsException if the string was not accepted.
     */
    private void updateModel() throws InvalidSettingsException {
        if (m_disallowEmtpy
                && ((m_valueField.getText() == null) || (m_valueField.getText()
                        .length() == 0))) {
            showError(m_valueField);
            throw new InvalidSettingsException("Please enter a string value.");
        }

        // we transfer the value from the field into the model
        ((SettingsModelString)getModel())
                .setStringValue(m_valueField.getText());
    }

    private void showError(final JTextArea field) {

        if (!getModel().isEnabled()) {
            // don't flag an error in disabled components.
            return;
        }

        if (field.getText().length() == 0) {
            field.setBackground(Color.RED);
        } else {
            field.setForeground(Color.RED);
        }
        field.requestFocusInWindow();

        // change the color back as soon as he changes something
        field.getDocument().addDocumentListener(new DocumentListener() {

            public void removeUpdate(final DocumentEvent e) {
                field.setForeground(DEFAULT_FG);
                field.setBackground(DEFAULT_BG);
                field.getDocument().removeDocumentListener(this);
            }

            public void insertUpdate(final DocumentEvent e) {
                field.setForeground(DEFAULT_FG);
                field.setBackground(DEFAULT_BG);
                field.getDocument().removeDocumentListener(this);
            }

            public void changedUpdate(final DocumentEvent e) {
                field.setForeground(DEFAULT_FG);
                field.setBackground(DEFAULT_BG);
                field.getDocument().removeDocumentListener(this);
            }

        });
    }

    /**
     * Clears the error status of the specified component by reseting its color
     * to the normal default colors.
     *
     * @param field the component to set the colors back to normal for.
     */
    private void clearError(final JTextArea field) {
        field.setForeground(DEFAULT_FG);
        field.setBackground(DEFAULT_BG);
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
        // we are always good.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_valueField.setEnabled(enabled);
    }

    /**
     * Sets the preferred size of the internal component.
     *
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_valueField.setPreferredSize(new Dimension(width, height));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_label.setToolTipText(text);
        m_valueField.setToolTipText(text);
    }

}
