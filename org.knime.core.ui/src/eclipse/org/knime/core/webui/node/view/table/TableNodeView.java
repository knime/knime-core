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
 *   3 Nov 2022 (marcbux): created
 */
package org.knime.core.webui.node.view.table;

import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.rpc.json.impl.JsonRpcDataServiceImpl;
import org.knime.core.webui.data.rpc.json.impl.JsonRpcSingleServer;
import org.knime.core.webui.node.dialog.impl.DefaultInitialDataServiceImpl;
import org.knime.core.webui.node.dialog.impl.DefaultNodeSettings;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.page.Page;

/**
 *
 * @author marcbux
 */
public class TableNodeView implements NodeView {
    //    .withDataService(t -> TableViewUtil.createDataService(() -> t.getTable(), m_nodeID))//
    //    .withInitialData((s, t) -> TableViewUtil.createInitialData(s, () -> t.getTable(), m_nodeID))//
    private final String m_tableId;

    private final Supplier<BufferedDataTable> m_table;

    private TableViewViewSettings m_settings;

    public TableNodeView(final String tableId, final Supplier<BufferedDataTable> table) {
        m_tableId = tableId;
        m_table = table;
        TableViewUtil.onCreateNodeView(tableId);
    }

    @Override
    public Page getPage() {
        return TableViewUtil.createPage();
    }

    @Override
    public Optional<InitialDataService> createInitialDataService() {
        if (m_settings == null) {
            m_settings = new TableViewViewSettings(m_table.get().getSpec());
        }
        return Optional.of(new DefaultInitialDataServiceImpl<>(
            () -> TableViewUtil.createInitialData(m_settings, m_table.get(), m_tableId)));
    }

    @Override
    public Optional<DataService> createDataService() {
        return Optional.of(new JsonRpcDataServiceImpl(
            new JsonRpcSingleServer<>(TableViewUtil.createDataService(m_table.get(), m_tableId))));
    }

    @Override
    public Optional<ApplyDataService> createApplyDataService() {
        return Optional.empty();
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
        m_settings = DefaultNodeSettings.loadSettings(settings, TableViewViewSettings.class);
    }

}
