/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   18.04.2006 (mb): created
 */
package de.unikn.knime.core.node.defaultnodesettings.settingsmodels;

import javax.swing.event.ChangeListener;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;

/** Abstract implementation of an ecapsulating class holding a (usually
 * rather basic) model of NodeModel Settings. The main motivation for this
 * class is the need to access (read/write) the settings of model at various
 * places (NodeModel, NodeDialog) and the need to unify and simplify this.
 * It also enables the user to register to change-events so that it other
 * models/components can be updated accordingly (enable/disable...).
 * 
 * @author M. Berthold, University of Konstanz
 */
public abstract class SettingsModel {

    /**
     * Read value(s) of this component model from configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     * @throws InvalidSettingsException if load fails.
     */
    public abstract void loadSettingsFrom(final NodeSettings settings,
            final DataTableSpec[] specs) throws InvalidSettingsException;

    /**
     * Write value(s) of this component model to configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to read from.
     * @throws InvalidSettingsException if the user has entered wrong values.
     */
    public abstract void saveSettingsTo(final NodeSettings settings)
            throws InvalidSettingsException;

    /** Add a listener.
     *
     * @param l ChangeListener
     */
    public void addChangeListener(final ChangeListener l) {
    }
    
    /** Remove a specific listener.
    *
    * @param l ChangeListener
    */
    public void removeChangeListener(final ChangeListener l) {
    }
}
