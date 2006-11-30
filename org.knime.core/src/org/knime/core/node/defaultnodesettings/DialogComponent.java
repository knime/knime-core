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
 *   21.09.2005 (mb): created
 *   2006-05-24 (tm): reviewed
 *   2006-09-22 (ohl): using SettingsModels
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Abstract implementation of a component handling a standard type in a
 * NodeDialog. Actual implementations will make sure the label and editable
 * components are placed nicely in the underlying JPanel and their models will
 * handle save/load to and from config objects. Using the
 * {@link org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane} it is
 * easy to add such Component to quickly assemble a dialog dealing with typical
 * parameters. <br>
 * Each component has a {@link SettingsModel} associated with it, which stores
 * the current value of the component and handles all value related operations,
 * like loading, saving, etc.
 * 
 * @see DefaultNodeSettingsPane
 * @see SettingsModel
 * 
 * @author M. Berthold, University of Konstanz
 */
public abstract class DialogComponent extends JPanel {

    /** default foreground color for editable components. */
    protected static final Color DEFAULT_FG = new JTextField().getForeground();

    /** default background color for editable components. */
    protected static final Color DEFAULT_BG = new JTextField().getBackground();

    private final SettingsModel m_model;

    /**
     * the specs that came with the last loadSettings. Could be null.
     */
    private DataTableSpec[] m_lastSpecs;

    /**
     * Abstract constructor expecting the model for this component.
     * 
     * @param model the value model for this component
     */
    protected DialogComponent(final SettingsModel model) {
        if (model == null) {
            throw new NullPointerException("SettingsModel can't be null.");
        }
        m_model = model;
        m_lastSpecs = null;
    }

    /**
     * @return the Settings model associated with this component.
     */
    final SettingsModel getModel() {
        return m_model;
    }

    /**
     * @param portID the id of the port the spec should be returned for
     * @return the spec for the specified port that came in through the last
     *         call to loadSettings. Could be null!
     * @see #loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    final DataTableSpec getLastTableSpec(final int portID) {
        if (m_lastSpecs == null) {
            return null;
        }
        return m_lastSpecs[portID];
    }

    /**
     * @return the specs that came in through the last call to loadSettings.
     *         Could be null!
     * @see #loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */

    final DataTableSpec[] getLastTableSpecs() {
        return m_lastSpecs;
    }

    /**
     * Read value(s) of this dialog component from the configuration object.
     * This method will be called by the dialog pane only. (Is not called if the
     * component is disabled.)
     * 
     * @param settings the <code>NodeSettings</code> to read from
     * @param specs the input specs
     * @throws NotConfigurableException If there is no chance for the dialog
     *             component to be valid (i.e. the settings are valid), e.g. if
     *             the given specs lack some important columns or column types.
     */
    final void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {

        m_lastSpecs = specs;

        checkConfigurabilityBeforeLoad(specs);
        m_model.dlgLoadSettingsFrom(settings, specs);
        // make sure the component displays the new value (listeners are not
        // notified if the model's value didn't change (is not different)).
        updateComponent();
    }

    /**
     * Read the value from the {@link SettingsModel} and set/display it in the
     * component. (Called after loading new values in the model to ensure they
     * are transfered into the component.)
     */
    abstract void updateComponent();

    /**
     * Write value(s) of this dialog component to the configuration object. This
     * method will be called by the dialog pane only. (Is not called if the
     * component is disabled.)
     * 
     * @param settings the <code>NodeSettings</code> to read from
     * @throws InvalidSettingsException if the user has entered wrong values.
     */
    final void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        if (m_model.isEnabled()) {
            validateStettingsBeforeSave();
        }
        m_model.dlgSaveSettingsTo(settings);
    }

    /**
     * Will be called before the value of the component is saved into the
     * settings object. Can be used to commit values, to update the model and
     * must be used to validate the entered value. (Is not called if the
     * component is disabled.)
     * 
     * @throws InvalidSettingsException if the entered values are invalid
     */
    abstract void validateStettingsBeforeSave() throws InvalidSettingsException;

    /**
     * Will be called before the values are loaded from the settings object. Can
     * be used to avoid loading due to missing, invalid, or inappropriate
     * incoming table specs. <br>
     * Note: This is called even if the component is disabled. Don't reject
     * specs that might be handled by other components
     * 
     * @param specs the specs from the input ports.
     * @throws NotConfigurableException if the component can't be used due to
     *             inappropriate table specs. (Prevents the dialog from being
     *             opened.)
     */
    abstract void checkConfigurabilityBeforeLoad(final DataTableSpec[] specs)
            throws NotConfigurableException;

    /**
     * Sets the enabled status of the component. Disabled components don't take
     * user input and don't store any value in NodeSettings objects! Trying to
     * retrieve the value of a disabled component from the settings object will
     * fail.
     * 
     * @param enabled if <code>true</code> the contained components will be
     *            enabled
     * @see #setEnabledComponents(boolean)
     * @see java.awt.Component#setEnabled(boolean)
     */
    @Override
    public final void setEnabled(final boolean enabled) {
        m_model.setEnabled(enabled);
        setEnabledComponents(enabled);
        super.setEnabled(enabled);
    }

    /**
     * This method is called by the above (final) {@link #setEnabled(boolean)}
     * method. Derived classes should disable all the contained components in
     * here.
     * 
     * @param enabled the new status of the component
     * @see #setEnabled(boolean)
     */
    protected abstract void setEnabledComponents(final boolean enabled);

    /**
     * Colors the component red, and registers a listener to the edit field that
     * sets the colors back to the default as soon as the user edits something
     * in the field.
     * 
     * @param field the component to set the color in
     */
    void showError(final JTextField field) {
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
            }

            public void insertUpdate(final DocumentEvent e) {
                field.setForeground(DEFAULT_FG);
                field.setBackground(DEFAULT_BG);
            }

            public void changedUpdate(final DocumentEvent e) {
                field.setForeground(DEFAULT_FG);
                field.setBackground(DEFAULT_BG);
            }

        });

    }

}
