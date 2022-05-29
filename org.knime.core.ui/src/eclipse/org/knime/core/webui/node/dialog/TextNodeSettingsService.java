/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Jan 5, 2022 (hornm): created
 */
package org.knime.core.webui.node.dialog;

import java.util.Map;

import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Functionality around {@link NodeSettings} as required by the {@link NodeDialog}.
 *
 * Translates text-based settings (strings) used on the frontend-side from and to the backend-side settings
 * representation (i.e. {@link NodeSettings}).
 *
 * Also provides default node settings.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public interface TextNodeSettingsService {

    /**
     * Called when a dialog is initialized.
     *
     * Infers a single text-based settings representation from possibly multiple {@link NodeSettingsRO}-instances (one
     * per {@link SettingsType}).
     *
     * @param settings the settings to read from; if there are no settings with the node stored, yet, the default node
     *            settings will be supplied (see {@link #getDefaultNodeSettings(Map, PortObjectSpec[])})
     * @param specs the specs for configuring the settings (includes the flow variable port). NOTE: can contain
     *            {@code null}-values, e.g., in case an input port is not connected
     * @return a new text-based settings representation
     */
    String fromNodeSettings(Map<SettingsType, NodeSettingsRO> settings, PortObjectSpec[] specs);

    /**
     * Called when dialog settings are applied.
     *
     * Translates text-based settings to {@link NodeSettingsWO}-instances of certain {@link SettingsType}.
     *
     * @param textSettings the text-based settings object
     * @param settings the settings instances to write into
     */
    void toNodeSettings(final String textSettings, Map<SettingsType, NodeSettingsWO> settings);

    /**
     * Saves the default settings to the given settings objects. Will be called if there are no settings stored with the
     * node, yet, and is mainly used to be able to properly show the flow variable configuration (e.g., to overwrite
     * settings).
     *
     * @param settings the settings to write into
     * @param specs the node's input specs (includes the flow variable port). NOTE: can contain {@code null}-values,
     *            e.g., in case the input port is not connected
     */
    void getDefaultNodeSettings(Map<SettingsType, NodeSettingsWO> settings, PortObjectSpec[] specs);

}
