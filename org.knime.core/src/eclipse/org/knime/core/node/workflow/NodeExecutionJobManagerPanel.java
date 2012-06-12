/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
     */
    public abstract void loadSettings(final NodeSettingsRO settings);

    /**
     * Notifies the panel of the (or a new) port object spec at the input ports.
     *
     * @param inSpecs the specs of the input port objects.
     */
    public abstract void updateInputSpecs(final PortObjectSpec[] inSpecs);
}
