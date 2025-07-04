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
 *   Jun 28, 2025 (Paul Bärnreuther): created
 */
package org.knime.node;

import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectHolder;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.page.Page;
import org.knime.node.DefaultView.ViewInput;

/**
 * Implementation of the {@link DefaultView}.
 *
 * @author Paul Bärnreuther
 */
final class DefaultViewToNodeViewAdapter implements NodeView {

    private DefaultView m_view;

    private Supplier<PortObjectHolder> m_getPortObjectHolder;

    DefaultViewToNodeViewAdapter(final DefaultView view, final Supplier<PortObjectHolder> getPortObjectHolder) {
        this.m_view = view;
        this.m_getPortObjectHolder = getPortObjectHolder;
    }

    private ViewInput m_input = new ViewInput() {

        @Override
        public <S extends DefaultNodeSettings> S getSettings() {
            return (S)m_viewSettings;
        }

        @Override
        public PortObject[] getInternalPortObjects() {
            return m_getPortObjectHolder.get().getInternalPortObjects();
        }

    };

    private DefaultNodeSettings m_viewSettings;

    @Override
    public Page getPage() {
        return m_view.m_page;
    }

    @Override
    public <D> Optional<InitialDataService<D>> createInitialDataService() {
        if (m_view.m_initialDataServiceFct == null) {
            return Optional.empty();
        } else {
            return Optional.of((InitialDataService<D>)m_view.m_initialDataServiceFct.apply(m_input));
        }
    }

    @Override
    public Optional<RpcDataService> createRpcDataService() {
        if (m_view.m_rpcDataServiceFct == null) {
            return Optional.empty();
        } else {
            return Optional.of(m_view.m_rpcDataServiceFct.apply(m_input));
        }
    }

    @Override
    public <D> Optional<ApplyDataService<D>> createApplyDataService() {
        return Optional.empty();
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (m_view.getSettingsClass().isEmpty()) {
            return;
        }
        final var viewSettingsClass = m_view.getSettingsClass().get();
        // This can already throw if loading from node settings throws
        final var loadedSettings = DefaultNodeSettings.loadSettings(settings, viewSettingsClass);
        // Additional custom validation of the settings
        loadedSettings.validate();
    }

    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
        m_viewSettings = loadValidatedViewSettingsFrom(settings);
    }

    private DefaultNodeSettings loadValidatedViewSettingsFrom(final NodeSettingsRO settings) {
        if (m_view.getSettingsClass().isEmpty()) {
            return null;
        }
        final var viewSettingsClass = m_view.getSettingsClass().get();
        try {
            return DefaultNodeSettings.loadSettings(settings, viewSettingsClass);
        } catch (InvalidSettingsException ex) {
            throw new IllegalStateException("Settings should be valid.", ex);
        }
    }

};
