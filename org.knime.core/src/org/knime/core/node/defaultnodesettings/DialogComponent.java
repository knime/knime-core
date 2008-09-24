/* 
 * -------------------------------------------------------------------
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
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Abstract implementation of a component handling a standard type in a
 * NodeDialog. Actual implementations will make sure the label and editable
 * components are placed nicely in the underlying JPanel and their models will
 * handle save/load to and from config objects. Using the
 * {@link DefaultNodeSettingsPane} it is easy to add such Component to quickly
 * assemble a dialog dealing with typical parameters. <br>
 * Each component has a {@link SettingsModel} associated with it, which stores
 * the current value of the component and handles all value related operations,
 * like loading, saving, etc.
 * 
 * @see DefaultNodeSettingsPane
 * @see SettingsModel
 * 
 * @author M. Berthold, University of Konstanz
 */
public abstract class DialogComponent {

    /** default foreground color for editable components. */
    protected static final Color DEFAULT_FG = new JTextField().getForeground();

    /** default background color for editable components. */
    protected static final Color DEFAULT_BG = new JTextField().getBackground();

    private final SettingsModel m_model;

    private final JPanel m_panel;

    /**
     * the specs that came with the last loadSettings. Could be null.
     */
    private PortObjectSpec[] m_lastSpecs;

    /**
     * Abstract constructor expecting the model for this component.
     * 
     * @param model the value model for this component
     */
    public DialogComponent(final SettingsModel model) {
        if (model == null) {
            throw new NullPointerException("SettingsModel can't be null.");
        }
        m_panel = new JPanel();
        m_model = model;
        m_lastSpecs = null;
    }

    /**
     * @return the panel in which all sub-components of this component are
     *         arranged. This panel can be added to the dialog pane.
     */
    public JPanel getComponentPanel() {
        return m_panel;
    }

    /**
     * @return the Settings model associated with this component.
     */
    public final SettingsModel getModel() {
        return m_model;
    }

    /**
     * @param portID the id of the port the spec should be returned for
     * @return the spec for the specified port that came in through the last
     *         call to loadSettings. Could be null!
     * @see #loadSettingsFrom(NodeSettingsRO, PortObjectSpec[])
     */
    protected final PortObjectSpec getLastTableSpec(final int portID) {
        if (m_lastSpecs == null) {
            return null;
        }
        return m_lastSpecs[portID];
    }

    /**
     * @return the specs that came in through the last call to loadSettings.
     *         Could be null!
     * @see #loadSettingsFrom(NodeSettingsRO, PortObjectSpec[])
     */

    protected final PortObjectSpec[] getLastTableSpecs() {
        return m_lastSpecs;
    }

    /**
     * Read value(s) of this dialog component from the configuration object.
     * This method will be called by the dialog pane only.
     * 
     * @param settings the <code>NodeSettings</code> to read from
     * @param specs the input specs
     * @throws NotConfigurableException If there is no chance for the dialog
     *             component to be valid (i.e. the settings are valid), e.g. if
     *             the given specs lack some important columns or column types.
     */
    public final void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {

        m_lastSpecs = specs;

        checkConfigurabilityBeforeLoad(specs);

        if (!(m_model instanceof EmptySettingsModel)) {
            // our special empty settings model will not load anything
            m_model.dlgLoadSettingsFrom(settings, specs);
        }

        // make sure the component displays the new value (listeners are not
        // notified if the model's value didn't change (is not different)).
        updateComponent();
    }

    /**
     * Read the value from the {@link SettingsModel} and set/display it in the
     * component. (Called after loading new values in the model to ensure they
     * are transfered into the component.) Implementations should set the new
     * value(s) in the components, should clear any possible error indications,
     * and should also take over the enable state.
     */
    protected abstract void updateComponent();

    /**
     * Write value(s) of this dialog component to the configuration object. This
     * method will be called by the dialog pane only. (Is not called if the
     * component is disabled.)
     * 
     * @param settings the <code>NodeSettings</code> to read from
     * @throws InvalidSettingsException if the user has entered wrong values.
     */
    public final void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {

        try {
            validateSettingsBeforeSave();
        } catch (InvalidSettingsException ise) {
            if (m_model.isEnabled()) {
                // forward the exception only if the component is enabled.
                // it's okay for disabled components to hold invalid values.
                throw ise;
            }
        }

        // if the model is not enabled - save its settings anyway.
        if (!(m_model instanceof EmptySettingsModel)) {
            // the special model EmptySettings model will not save anything
            m_model.dlgSaveSettingsTo(settings);
        }
    }

    /**
     * Will be called before the value of the component is saved into the
     * NodeSettings object. Can be used to commit values, to update the model
     * and must be used to validate the entered value. NOTE: it will be called
     * even if the model is disabled.
     * 
     * @throws InvalidSettingsException if the entered values are invalid
     */
    protected abstract void validateSettingsBeforeSave()
            throws InvalidSettingsException;

    /**
     * Will be called before the new values are loaded from the NodeSettings
     * object. Can be used to avoid loading due to missing, invalid, or
     * inappropriate incoming table specs. <br>
     * Note: This is called even if the component is disabled. Don't reject
     * specs that might be handled by other components
     * 
     * @param specs the specs from the input ports.
     * @throws NotConfigurableException if the component can't be used due to
     *             inappropriate table specs. (Prevents the dialog from being
     *             opened.)
     */
    protected abstract void checkConfigurabilityBeforeLoad(
            final PortObjectSpec[] specs) throws NotConfigurableException;

    /**
     * Sets the enabled status of the component. Disabled components don't take
     * user input. Retrieving the value from a disabled model (SettingsModel)
     * could lead to unexpected results.
     * 
     * @param enabled if <code>true</code> the contained components will be
     *            enabled
     * @see #setEnabledComponents(boolean)
     * @see java.awt.Component#setEnabled(boolean)
     * @deprecated rather use the component's {@link SettingsModel} to
     *             enable/disable the component.
     */
    @Deprecated
    public final void setEnabled(final boolean enabled) {
        m_model.setEnabled(enabled);
        setEnabledComponents(enabled);
        getComponentPanel().setEnabled(enabled);
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
    protected void showError(final JTextField field) {

        if (!getModel().isEnabled()) {
            // don't show no error, if the model is not enabled.
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
     * Sets the foreground and background colors of the specified component back
     * to the normal default colors.
     * 
     * @param field the textfield to clear the error for
     */
    protected void clearError(final JTextField field) {
        field.setForeground(DEFAULT_FG);
        field.setBackground(DEFAULT_BG);
    }

    /**
     * Implement this so it sets the tooltip on your component(s).
     * 
     * @param text the tool tip text to set.
     * @see javax.swing.JComponent#setToolTipText(java.lang.String)
     */
    public abstract void setToolTipText(final String text);

    /**
     * -------------------------------------------------------------------------
     * Components deriving from {@link DialogComponent} can use this model if
     * they don't need or want to store any value (but are only displaying
     * stuff). Do not call any of the methods of this model. No value will be
     * stored in this model, no value will be saved or loaded. You cannot change
     * the value of the component through this model.
     * 
     * @author ohl, University of Konstanz
     */
    protected static final class EmptySettingsModel extends SettingsModel {

        /**
         * Creates an empty settings model, that will not hold any value.
         */
        public EmptySettingsModel() {

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return getClass().getSimpleName() + " ('EMPTYMODELID')";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addChangeListener(final ChangeListener l) {
            // don't listen to me.
            assert false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void prependChangeListener(final ChangeListener l) {
            // not listening
            assert false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void notifyChangeListeners() {
            // I have nothing to say.
            assert false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeChangeListener(final ChangeListener l) {
            // nobody is listening
            assert false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEnabled() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setEnabled(final boolean enabled) {
            // I don't care.
            assert false;
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        protected EmptySettingsModel createClone() {
            return new EmptySettingsModel();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String getConfigName() {
            // shouldn't be used anyway
            assert false;
            return Integer.toString(System.identityHashCode(this));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String getModelTypeID() {
            // shouldn't be used
            assert false;
            return "EMPTYMODELID";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void loadSettingsForDialog(final NodeSettingsRO settings,
                final PortObjectSpec[] specs) throws NotConfigurableException {
            // not loading nor saving any settings
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void loadSettingsForModel(final NodeSettingsRO settings)
                throws InvalidSettingsException {
            // not loading nor saving any settings
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void saveSettingsForDialog(final NodeSettingsWO settings)
                throws InvalidSettingsException {
            // not loading nor saving any settings
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void saveSettingsForModel(final NodeSettingsWO settings) {
            // not loading nor saving any settings
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void validateSettingsForModel(final NodeSettingsRO settings)
                throws InvalidSettingsException {
            // not loading nor saving any settings
        }
    }

}
