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

    private final CopyOnWriteArrayList<ChangeListener> m_listeners;

    /**
     * Default constructor.
     */
    public SettingsModel() {
        m_listeners = new CopyOnWriteArrayList<ChangeListener>();
    }

    /**
     * Read value(s) of this component model from configuration object. If the
     * value is not stored in the config, the objects value must remain
     * unchanged. Called only from the components using this model. This method
     * must notify change listeners about a model change!
     * 
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     * @throws NotConfigurableException if the specs are not good enough to
     * 
     */
    abstract void dlgLoadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException;

    /**
     * Write value(s) of this component model to configuration object. Called
     * only from the components using this model.
     * 
     * @param settings The {@link NodeSettings} to read from.
     * @throws InvalidSettingsException if the user has entered wrong values.
     */
    abstract void dlgSaveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException;

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
     * Read value(s) of this component model from configuration object. If the
     * value is not stored in the config, an exception will be thrown.
     * 
     * @param settings The {@link NodeSettings} to read from.
     * @throws InvalidSettingsException if load fails.
     */
    public abstract void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException;

    /**
     * Write value(s) of this component model to configuration object.
     * 
     * @param settings The {@link NodeSettings} to write into.
     */
    public abstract void saveSettingsTo(final NodeSettingsWO settings);

}
