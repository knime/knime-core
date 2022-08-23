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
 *   Aug 18, 2022 (benjamin): created
 */
package org.knime.core.webui.node.dialog;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;

/**
 * Write-only settings for using and exposing other settings via flow variables.
 * <P>
 * EXAMPLE: Take the following settings tree:
 *
 * <pre>
 * - first_setting: "my_value"
 * - group_setting (added via NodeSettings#addNodeSettings)
 *   - first_child_setting: "one_value"
 *   - second_child_setting: "another_value"
 * </pre>
 *
 * To set the setting "first_setting" to use the value of the variable named "my_variable" use
 *
 * <pre>
 * rootVariableSettingsObj.addUsedVariable("first_setting", "my_variable");
 * </pre>
 *
 * To set the variable "my_other_variable" to take the value of the setting "first_child_setting" use
 *
 * <pre>
 * rootVariableSettingsObj.getChild("group_setting").addExposedVariable("first_child_setting", "my_other_variable");
 * </pre>
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public interface VariableSettingsWO {

    /**
     * Access the variable settings of a settings group that was added by {@link NodeSettings#addNodeSettings(String)}.
     * See the class javadoc for an example.
     *
     * @param key the key of the child settings
     * @return the child settings
     * @throws InvalidSettingsException if there are no child settings with the given key
     */
    VariableSettingsWO getChild(String key) throws InvalidSettingsException;

    /**
     * Set that the setting with the given key uses the value of the variable.
     *
     * @param settingsKey
     * @param usedVariable the name of the variable which should be used
     * @throws InvalidSettingsException if there is no setting with the given key
     */
    void addUsedVariable(String settingsKey, String usedVariable) throws InvalidSettingsException;

    /**
     * Set that the setting with the given key is exposed as a variable.
     *
     * @param settingsKey
     * @param exposedVariable the name of the variable which should be exposed
     * @throws InvalidSettingsException if there is no setting with the given key
     */
    void addExposedVariable(String settingsKey, String exposedVariable) throws InvalidSettingsException;
}
