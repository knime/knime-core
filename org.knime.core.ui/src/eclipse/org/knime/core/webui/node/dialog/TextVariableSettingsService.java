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
 *   Aug 25, 2022 (benjamin): created
 */
package org.knime.core.webui.node.dialog;

import java.util.Map;

/**
 * Similar to the {@link TextNodeSettingsService} but for handling settings that are overwritten or exposed by flow
 * variables. Translates the same text-based settings (strings) as used in the {@link TextNodeSettingsService} to the
 * backend-side representation (i.e. {@link VariableSettingsWO}).
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public interface TextVariableSettingsService {

    /*
     * TODO UIEXT-479
     * It would make sense to also have a fromNodeSettings method that knows about the variables.
     * Right now, the values of settings that are overwritten by variables are replaced by the
     * current value of the variable. Therefore the TextNodeSettingsService#fromNodeSettings method
     * has no idea if a setting was saved like this or is overwritten by a variable.
     * If the dialog is supposed to display these cases separately it must use the DialogService which
     * gets the flowVariableSettings from the extension config. This is not intuitive.
     */

    /**
     * Called when dialog settings are applied.
     *
     * Translates text-based settings to {@link VariableSettingsWO}-instances of certain {@link SettingsType}.
     *
     * @param textSettings the text-based settings object
     * @param settings the settings to overwrite other settings with flow variables
     */
    void toVariableSettings(final String textSettings, Map<SettingsType, VariableSettingsWO> settings);
}
