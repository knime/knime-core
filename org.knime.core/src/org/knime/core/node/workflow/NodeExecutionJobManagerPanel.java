/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
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
 *   13.10.2008 (ohl): created
 */
package org.knime.core.node.workflow;

import javax.swing.JComponent;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Settings panel for {@link NodeExecutionJobManager}s. Contains components
 * displaying the settings and allowing the user to enter new values. Implements
 * save and load methods for these settings.
 *
 * @author ohl, University of Konstanz
 */
public abstract class NodeExecutionJobManagerPanel extends JComponent {

    /**
     * Save settings into the provided parameter. Throw an exception with a user
     * message if settings are unacceptable.
     *
     * @param settings object to save new values into
     * @throws InvalidSettingsException if the current values in the components
     *             are not acceptable
     */
    public abstract void saveSettings(final NodeSettingsWO settings)
            throws InvalidSettingsException;

    /**
     * Load the values from the specified settings object into the components.
     * If settings are invalid, use default values.
     *
     * @param settings the object holding new settings (as written by
     *            {@link #saveSettings(NodeSettingsWO)}
     * @param inSpecs the specs of the input port objects.
     */
    public abstract void loadSettings(final NodeSettingsRO settings,
            final PortObjectSpec[] inSpecs);

}
