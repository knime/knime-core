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
 * -------------------------------------------------------------------
 *
 */
package org.knime.workbench.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Class used to initialize default preference values.
 *
 * @author Florian Georg, University of Konstanz
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {
        // get the preference store for the UI plugin
        IPreferenceStore store = KNIMEUIPlugin.getDefault()
                .getPreferenceStore();

        store.setDefault(PreferenceConstants.P_HIDE_TIPS_AND_TRICKS, false);

        store.setDefault(PreferenceConstants.P_CONFIRM_RESET, true);

        store.setDefault(PreferenceConstants.P_CONFIRM_DELETE, true);

        store.setDefault(PreferenceConstants.P_CONFIRM_RECONNECT, true);

        store.setDefault(
                PreferenceConstants.P_CONFIRM_EXEC_NODES_NOT_SAVED, true);

        store.setDefault(PreferenceConstants.P_FAV_FREQUENCY_HISTORY_SIZE, 10);

        store.setDefault(PreferenceConstants.P_FAV_LAST_USED_SIZE, 10);

        store.setDefault(PreferenceConstants.P_DEFAULT_NODE_LABEL, "Node");

        int defaultFontHeight = 8;
        Display current = Display.getCurrent();
        if (current != null) {
            Font systemFont = current.getSystemFont();
            FontData[] systemFontData = systemFont.getFontData();
            if (systemFontData.length >= 1) {
                defaultFontHeight = systemFontData[0].getHeight();
            }
        }
        store.setDefault(
                PreferenceConstants.P_NODE_LABEL_FONT_SIZE, defaultFontHeight);

        store.setDefault(PreferenceConstants.P_META_NODE_LINK_UPDATE_ON_LOAD,
                MessageDialogWithToggle.PROMPT);

    }
}
