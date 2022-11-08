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
package org.knime.core.webui.node.dialog.impl;

import java.util.Map;
import java.util.Optional;

import org.knime.core.webui.data.DataService;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.TextNodeSettingsService;
import org.knime.core.webui.page.Page;

/**
 * Default node dialog implementation where all the dialog widgets are defined through a
 * {@link DefaultNodeSettings}-implementation.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class DefaultNodeDialog extends NodeDialog {

    /**
     * @return the {@link Page} used by this dialog
     */
    public static Page buildPage() {
        return Page.builder(DefaultNodeDialog.class, "js-src/dist", "NodeDialog.umd.min.js").build();
    }

    private final DefaultNodeSettingsService m_settingsDataService;

    /**
     * Creates a new instance.
     *
     * @param settingsType the type of settings this dialog provides
     * @param settingsClass the class which defining the dialog
     */
    public DefaultNodeDialog(final SettingsType settingsType,
        final Class<? extends DefaultNodeSettings> settingsClass) {
        super(settingsType);
        m_settingsDataService = new DefaultNodeSettingsService(Map.of(settingsType, settingsClass));
    }

    /**
     * Creates a new instance.
     *
     * @param settingsType1 a settings type this dialog is able to provide
     * @param settingsClass1 dialog definition for the first settings type
     * @param settingsType2 another settings type this dialog is able to provide
     * @param settingsClass2 dialog definition for the second settings type
     *
     */
    public DefaultNodeDialog(final SettingsType settingsType1,
        final Class<? extends DefaultNodeSettings> settingsClass1, final SettingsType settingsType2,
        final Class<? extends DefaultNodeSettings> settingsClass2) {
        super(settingsType1, settingsType2);
        m_settingsDataService =
            new DefaultNodeSettingsService(Map.of(settingsType1, settingsClass1, settingsType2, settingsClass2));
    }

    @Override
    public Page getPage() {
        return buildPage();
    }

    @Override
    public Optional<DataService> createDataService() {
        return Optional.empty();
    }

    @Override
    protected TextNodeSettingsService getNodeSettingsService() {
        return m_settingsDataService;
    }

}
