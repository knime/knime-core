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
 *   18.04.2006 (mb): created
 */
package org.knime.core.node.defaultnodesettings;

import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;

/**
 * Abstract implementation of an encapsulating class holding a (usually rather
 * basic) model of NodeModel Settings. The main motivation for this class is the
 * need to access (read/write) the settings of model at various places
 * (NodeModel, NodeDialog) and the need to unify and simplify this. It also
 * enables the user to register to change-events so that other models/components
 * can be updated accordingly (enable/disable...).
 * 
 * @author M. Berthold, University of Konstanz
 */
public abstract class SettingsModel {

    /**
     * Models write some internal settings into the settings object.
     */
    private static final String CFGKEY_INTERNAL = "_Internals";

    /**
     * for example to ensure that the model reading the value from the object is
     * the same than the one that wrote it. Compatible models should use the
     * same id. I.e. for example integer and bounded integer models should share
     * the same id. Also the enable status is preserved.
     */
    private static final String CFGKEY_MODELID = "SettingsModelID";

    /**
     * The enable status of the settings model is stored in the nodesettings.
     */
    private static final String CFGKEY_ENABLESTAT = "EnabledStatus";

    private final CopyOnWriteArrayList<ChangeListener> m_listeners;

    private boolean m_enabled;

    /**
     * Default constructor.
     */
    public SettingsModel() {
        m_listeners = new CopyOnWriteArrayList<ChangeListener>();
        m_enabled = true;
    }

    /**
     * Creates a new settings model with identical values for everything except
     * the stored value (also except the list of listeners). The value stored in
     * the model will be retrieved from the specified settings object. If the
     * settings object doesn't contain a (valid) value it will throw an
     * InvalidSettingsException.
     * 
     * @param <T> the actual type returned is determined by the implementation
     *            of the {@link #createClone()} method.
     * @param settings the object to read the new model's value(s) from
     * @return a new settings model with the same constraints and configName but
     *         a value read from the specified settings object.
     * @throws InvalidSettingsException if the settings object passed doesn't
     *             contain a valid value for the newly created settings model.
     */
    public final <T extends SettingsModel> T createCloneWithValidatedValue(
            final NodeSettingsRO settings) throws InvalidSettingsException {

        T result = createClone();
        result.m_enabled = m_enabled;

        result.readEnableStatusAndCheckModelID(settings);

        // call the clone to actually read the values
        result.loadSettingsForModel(settings);

        return result;

    }

    /**
     * @param <T> determined by the implementation class. Must be the same than
     *            the class implementing this method.
     * @return a new instance of the same object with identical state and
     *         value(s).
     */
    protected abstract <T extends SettingsModel> T createClone();

    /**
     * Each settings model provides an ID which will be stored with its values.
     * This is to ensure that the same type of model is used to read the values
     * back. Otherwise an assertion will go off. Make sure to provide a unique
     * ID - and to re-use that ID in all compatible models. IntegerModels for
     * example should use the same ID as BoundedInteger models.
     * 
     * @return a string that identifies all models that are able (and empowered)
     *         to read the values stored by this model.
     */
    protected abstract String getModelTypeID();

    /**
     * @return the name provided at settings model construction time. The id
     *         associated with the value.
     */
    protected abstract String getConfigName();

    /**
     * Read the value(s) of this settings model from configuration object. If
     * the value is not stored in the config, the objects value must remain
     * unchanged. Called only from within the components using this model. If
     * the model is disabled it should not throw the exception.<br>
     * This method must always notify change listeners!!<br>
     * NOTE: Do not call this method directly, rather call dlgLoadSettingsFrom
     * 
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     * @throws NotConfigurableException if the specs are not good enough to
     * 
     */
    protected abstract void loadSettingsForDialog(
            final NodeSettingsRO settings, final DataTableSpec[] specs)
            throws NotConfigurableException;

    /**
     * This is the method called from the default dialog component to load the
     * model specific settings from the settings object. It calls the model
     * specific implementations.
     * 
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     * @throws NotConfigurableException if the specs are not good enough to load
     *             settings for this model
     */
    final void dlgLoadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {

        try {

            readEnableStatusAndCheckModelID(settings);

        } catch (InvalidSettingsException ise) {
            // we need to be able to load anything (especially empty settings)
            // Also, don't change the enable state, in case it got intentionally
            // disabled before.
        }

        // call the implementation of the derivative - even if it's disabled
        loadSettingsForDialog(settings, specs);
    }

    /**
     * Write value(s) of this component model to configuration object. Called
     * only from within the components using this model.<br>
     * NOTE: Don't call this method directly, rather use dlgSaveSettingsTo.
     * 
     * @param settings The {@link org.knime.core.node.NodeSettings} to read
     *            from.
     * @throws InvalidSettingsException if the user has entered wrong values.
     */
    protected abstract void saveSettingsForDialog(final NodeSettingsWO settings)
            throws InvalidSettingsException;

    /**
     * This method is called by the default dialog to save the model specific
     * settings into the settings object. It saves the model's ID before it
     * delegates to the derived implementation.
     * 
     * @param settings The {@link org.knime.core.node.NodeSettings} to read
     *            from.
     * @throws InvalidSettingsException if the user has entered wrong values.
     */
    final void dlgSaveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {

        saveEnableStatusAndModelID(settings);

        // now add the settings from the derived implementation
        try {
            saveSettingsForDialog(settings);
        } catch (InvalidSettingsException ise) {
            if (m_enabled) {
                // only forward the exception if the component is enabled.
                // invalid settings in disabled components will not be saved
                // then - which is okay.
                throw ise;
            }

        }
    }

    /**
     * Adds a listener (to the end of the listener list) which is notified,
     * whenever a new values is set in the model or the enable status changes.
     * Does nothing if the listener is already registered.
     * 
     * @param l listener to add.
     */
    public void addChangeListener(final ChangeListener l) {
        if (!m_listeners.contains(l)) {
            m_listeners.add(l);
        }
    }

    /**
     * Adds a listener (to the beginning of the listener list) which is
     * notified, whenever a new values is set in the model or the enable status
     * changes. Does nothing if the listener is already registered.
     * 
     * @param l listener to add.
     */
    protected void prependChangeListener(final ChangeListener l) {
        if (!m_listeners.contains(l)) {
            m_listeners.add(0, l);
        }
    }

    /**
     * Remove a specific listener.
     * 
     * @param l listener to remove.
     */
    public void removeChangeListener(final ChangeListener l) {
        m_listeners.remove(l);
    }

    /**
     * Notifies all registered listeners about a new model content. Call this,
     * whenever the value in the model changes!
     */
    protected void notifyChangeListeners() {
        for (ChangeListener l : m_listeners) {
            l.stateChanged(new ChangeEvent(this));
        }
    }

    /**
     * Sets the enabled status of the model. If a model is disabled it doesn't
     * validate new values or save it's current value into a settings object.
     * Also loading will be skipped. (The model does store its enable status in
     * the settings object though.)
     * 
     * @param enabled the new enable status. If true the model
     *            saves/validates/loads its value, if false, all these
     *            operations are skipped.
     */
    public void setEnabled(final boolean enabled) {
        boolean notify = m_enabled != enabled;
        m_enabled = enabled;
        if (notify) {
            notifyChangeListeners();
        }
    }

    /**
     * @return the current enable status of the model.
     * @see #setEnabled(boolean)
     */
    public boolean isEnabled() {
        return m_enabled;
    }

    /**
     * Read the expected values from the settings object, without assigning them
     * to the internal variables!
     * 
     * @param settings the object to read the value(s) from
     * @throws InvalidSettingsException if the value(s) in the settings object
     *             are invalid.
     */
    public final void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        boolean oldEnableStatus = m_enabled;

        // it actually changes the enabled field
        readEnableStatusAndCheckModelID(settings);
        boolean settingsEnableStatus = m_enabled;

        m_enabled = oldEnableStatus;

        if (settingsEnableStatus) {
            // if the model is disabled we don't care if the settings are valid
            validateSettingsForModel(settings);
        }

    }

    /**
     * Read the expected values from the settings object, without assigning them
     * to the internal variables! (Is not called when the model was disabled at
     * the time the settings were saved.)
     * 
     * @param settings the object to read the value(s) from
     * @throws InvalidSettingsException if the value(s) in the settings object
     *             are invalid.
     */
    protected abstract void validateSettingsForModel(
            final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Read value(s) of this component model from configuration object. If the
     * value is not stored in the config, an exception will be thrown.
     * 
     * @param settings The {@link org.knime.core.node.NodeSettings} to read
     *            from.
     * @throws InvalidSettingsException if load fails.
     */
    public final void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // make sure, the settings model that saved the values is the same
        // model than this (or compatible to us).
        readEnableStatusAndCheckModelID(settings);

        // call the derived implementation to actually read the values, only if
        // the model enable status is true now
        try {
            loadSettingsForModel(settings);
        } catch (InvalidSettingsException ise) {
            // forward the exception only if the model is enabled
            if (m_enabled) {
                throw ise;
            }
        }
    }

    /**
     * Read value(s) of this settings model from the configuration object. If
     * the value is not stored in the config, an exception will be thrown. <br>
     * NOTE: Don't call this method directly, rather call loadSettingsFrom.<br>
     * 
     * @param settings The {@link org.knime.core.node.NodeSettings} to read
     *            from.
     * @throws InvalidSettingsException if load fails.
     */
    protected abstract void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException;

    /**
     * Write value(s) of this setttings model to configuration object.
     * 
     * @param settings The {@link org.knime.core.node.NodeSettings} to write
     *            into.
     */
    public final void saveSettingsTo(final NodeSettingsWO settings) {

        saveEnableStatusAndModelID(settings);

        // now add the settings from the derived implementation
        saveSettingsForModel(settings);
    }

    /**
     * Write value(s) of this settings model to configuration object.<br>
     * NOTE: Don't call this method directly, rather call saveSettingsTo.
     * 
     * @param settings The {@link org.knime.core.node.NodeSettings} to write
     *            into.
     */
    protected abstract void saveSettingsForModel(final NodeSettingsWO settings);

    /**
     * Checks the modelID stored in the settings object and throws an assertion
     * if it doesn't match the ID of this model. It also reads the enabled
     * status back in - and throws an exeption if any of these settings is
     * missing (the enabled status will remain unchanged then).
     * 
     * @param settings the config object to read the enable state and model ID
     *            from
     */
    private void readEnableStatusAndCheckModelID(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        Config idCfg = null;
        try {
            idCfg = settings.getConfig(getConfigName() + CFGKEY_INTERNAL);
        } catch (InvalidSettingsException ise) {
            // for backward compatibility we just ignore if the internal config
            // doesn't exist.
            return;
        }
        String settingsID = idCfg.getString(CFGKEY_MODELID);

        m_enabled = idCfg.getBoolean(CFGKEY_ENABLESTAT);

        assert getModelTypeID().equals(settingsID) : "Incorrect Implementation:"
                + "The SettingsModel used to write the values is"
                + " different from the one that reads them. (WriteID = "
                + settingsID
                + ", ReadID = "
                + getModelTypeID()
                + ", Reading settings model: " + this.toString() + ")";

    }

    /**
     * Saves this' model id and the current enable status into the specified
     * settings object. It creates a new sub config for that.
     * 
     * @param settings the settings object to add the settings to.
     */
    private void saveEnableStatusAndModelID(final NodeSettingsWO settings) {

        Config intCfg = settings.addConfig(getConfigName() + CFGKEY_INTERNAL);
        intCfg.addString(CFGKEY_MODELID, getModelTypeID());
        intCfg.addBoolean(CFGKEY_ENABLESTAT, m_enabled);

    }

    /**
     * Derived classes should print their class name plus the config name for
     * nice and useful error messages. Like that,<br>
     * return getClass().getSimpleName() + " ('" + m_configName + "')";
     * 
     * {@inheritDoc}
     */
    @Override
    public abstract String toString();

}
