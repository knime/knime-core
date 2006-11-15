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
 * Abstract implementation of an ecapsulating class holding a (usually rather
 * basic) model of NodeModel Settings. The main motivation for this class is the
 * need to access (read/write) the settings of model at various places
 * (NodeModel, NodeDialog) and the need to unify and simplify this. It also
 * enables the user to register to change-events so that it other
 * models/components can be updated accordingly (enable/disable...).
 * 
 * @author M. Berthold, University of Konstanz
 */
public abstract class SettingsModel {

    /**
     * Models write an ID into the settings object to ensure that the model
     * reading the value from the object is the same than the one that wrote it.
     * Compatible models should use the same id. I.e. for example integer and
     * bounded integer models should share the same id.
     */
    static final String CFGKEY_MODELID = "SettingsModel_ID";

    private final CopyOnWriteArrayList<ChangeListener> m_listeners;

    /**
     * Default constructor.
     */
    public SettingsModel() {
        m_listeners = new CopyOnWriteArrayList<ChangeListener>();
    }

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
    abstract String getModelTypeID();

    /**
     * @return the name provided at settings model construction time. The id
     *         associated with the value.
     */
    abstract String getConfigName();

    /**
     * Read the value(s) of this component model from configuration object. If
     * the value is not stored in the config, the objects value must remain
     * unchanged. Called only from within the components using this model. This
     * method must always notify change listeners!<br>
     * NOTE: Do not call this method directly, rather call dlgLoadSettingsFrom
     * 
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     * @throws NotConfigurableException if the specs are not good enough to
     * 
     */
    abstract void loadSettingsForDialog(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException;

    /**
     * This is the method called from the default dialog to load the model
     * specific settings from the settings object. It calls the model specific
     * implementations. (This method doesn't really do anything, we just keep it
     * to have equal names for the load and save methods.)
     * 
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     * @throws NotConfigurableException if the specs are not good enough to
     */
    final void dlgLoadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // the load method for the dialog is not checking the id - we must
        // be able to load anything.

        // just call the implementation of the derivative.
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
    abstract void saveSettingsForDialog(final NodeSettingsWO settings)
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
        // save the id of the writing settings model
        Config idCfg = settings.addConfig(getConfigName() + CFGKEY_MODELID);
        idCfg.addString(CFGKEY_MODELID, getModelTypeID());
        // now add the settings from the derived implementation
        saveSettingsForDialog(settings);
    }

    /**
     * Add a listener which is notified, when ever the dialog loads new values
     * into the model.
     * 
     * @param l ChangeListener to add.
     */
    void addChangeListener(final ChangeListener l) {
        if (!m_listeners.contains(l)) {
            m_listeners.add(l);
        }
    }

    /**
     * Remove a specific listener.
     * 
     * @param l ChangeListener
     */
    void removeChangeListener(final ChangeListener l) {
        m_listeners.remove(l);
    }

    /**
     * Notifies all registered listeners about a new model content. Call this,
     * when the model is used by a component and the model content changed.
     */
    void notifyChangeListeners() {
        for (ChangeListener l : m_listeners) {
            l.stateChanged(new ChangeEvent(this));
        }
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
        if (settings.keySet().size() > 0) {
            // chekc only if this is not an empty config. Otherwise the loading
            // will fail anyway.
            try {
                Config idCfg =
                        settings.getConfig(getConfigName() + CFGKEY_MODELID);
                String settingsID = idCfg.getString(CFGKEY_MODELID);
                assert getModelTypeID().equals(settingsID) : "Implementation"
                        + "Error: The SettingsModel used to write the values is"
                        + " different from the one that reads them. (WriteID = "
                        + settingsID + ", ReadID = " + getModelTypeID()
                        + ", Reading settings model: " + this.toString() + ")";
            } catch (InvalidSettingsException ise) {
                assert false : "Implementation Error: You are trying to read a "
                        + "value with a SettingsModel but didn't use a "
                        + "SettingsModel to write it. (WriteID = <missing>, "
                        + "ReadID = "
                        + getModelTypeID()
                        + ", Reading settings model: " + this.toString() + ")";

            }
        }
        validateSettingsForModel(settings);
    }

    /**
     * Read the expected values from the settings object, without assigning them
     * to the internal variables!
     * 
     * @param settings the object to read the value(s) from
     * @throws InvalidSettingsException if the value(s) in the settings object
     *             are invalid.
     */
    abstract void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException;

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
        if (settings.keySet().size() > 0) {
            // chekc only if this is not an empty config. Otherwise the loading
            // will fail anyway.
            try {
                Config idCfg =
                        settings.getConfig(getConfigName() + CFGKEY_MODELID);
                String settingsID = idCfg.getString(CFGKEY_MODELID);
                assert getModelTypeID().equals(settingsID) : "Implementation"
                        + "Error: The SettingsModel used to write the values is"
                        + " different from the one that reads them. (WriteID = "
                        + settingsID + ", ReadID = " + getModelTypeID()
                        + ", Reading settings model: " + this.toString() + ")";
            } catch (InvalidSettingsException ise) {
                assert false : "Implementation Error: You are trying to read a "
                        + "value with a SettingsModel but didn't use a "
                        + "SettingsModel to write it. (WriteID = <missing>, "
                        + "ReadID = "
                        + getModelTypeID()
                        + ", Reading settings model: " + this.toString() + ")";

            }
        }
        // call the derived implementation to actually read the values
        loadSettingsForModel(settings);
    }

    /**
     * Read value(s) of this settings model from the configuration object. If
     * the value is not stored in the config, an exception will be thrown. <br>
     * NOTE: Don't call this method directly, rather call loadSettingsFrom.
     * 
     * @param settings The {@link org.knime.core.node.NodeSettings} to read
     *            from.
     * @throws InvalidSettingsException if load fails.
     */
    abstract void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException;

    /**
     * Write value(s) of this setttings model to configuration object.
     * 
     * @param settings The {@link org.knime.core.node.NodeSettings} to write
     *            into.
     */
    public final void saveSettingsTo(final NodeSettingsWO settings) {
        // save the id of the writing settings model
        Config idCfg = settings.addConfig(getConfigName() + CFGKEY_MODELID);
        idCfg.addString(CFGKEY_MODELID, getModelTypeID());
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
    abstract void saveSettingsForModel(final NodeSettingsWO settings);

    /**
     * Derived classes should print their class name plus the config name for
     * nice and useful error messages. Like that,<br>
     * return getClass().getSimpleName() + " ('" + m_configName + "')";
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public abstract String toString();

}
