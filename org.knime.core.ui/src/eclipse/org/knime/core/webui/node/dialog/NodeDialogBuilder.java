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
 *  NodeDialog, and NodeDialog) and that only interoperate with KNIME through
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
 *   Oct 15, 2021 (hornm): created
 */
package org.knime.core.webui.node.dialog;

import java.io.IOException;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.text.TextApplyDataService;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.node.dialog.settings.NodeSettingsService;
import org.knime.core.webui.node.dialog.settings.TextNodeSettingsService;
import org.knime.core.webui.page.Page;

/**
 * Builder to create {@link NodeDialog}-instances.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.5
 */
public class NodeDialogBuilder {

    private final Page m_page;

    private DataService m_dataService;

    private NodeSettingsService m_nodeSettingsService;

    NodeDialogBuilder(final Page p) {
        m_page = p;
    }

    /**
     * See {@link NodeDialog#getDataService()}.
     *
     * @param dataService
     * @return this builder instance
     */
    public NodeDialogBuilder dataService(final DataService dataService) {
        m_dataService = dataService;
        return this;
    }

    /**
     * Sets a {@link NodeSettingsService}.
     *
     * @param nodeSettingsService
     * @return this builder instance
     */
    public NodeDialogBuilder nodeSettingsService(final NodeSettingsService nodeSettingsService) {
        m_nodeSettingsService = nodeSettingsService;
        return this;
    }

    /**
     * Creates a new node view from the builder. Expects a {@link NodeContext} to be available.
     *
     * @return a new node view instance
     */
    public NodeDialog build() {
        if (m_nodeSettingsService instanceof TextNodeSettingsService) {
            return createNodeDialogWithSettings((TextNodeSettingsService)m_nodeSettingsService);
        }
        return new NodeDialog(m_page, null, m_dataService, null);
    }

    private NodeDialog createNodeDialogWithSettings(final TextNodeSettingsService textSettingsService) {
        var nc = NodeContext.getContext().getNodeContainer();
        var wfm = nc.getParent();

        var nodeID = nc.getID();
        var initialDataService = createTextInitialDataService(textSettingsService, (NativeNodeContainer)nc);
        var applyDataService = createTextApplyDataService(textSettingsService, nodeID, wfm);
        return new NodeDialog(m_page, initialDataService, m_dataService, applyDataService);
    }

    private static TextInitialDataService createTextInitialDataService(
        final TextNodeSettingsService textSettingsService, final NativeNodeContainer nnc) {
        return new TextInitialDataService() {

            @Override
            public String getInitialData() {
                var settings = new NodeSettings("node_settings");
                nnc.getNode().saveModelSettingsTo(settings);
                return textSettingsService.readSettings(settings);
            }
        };
    }

    private static TextApplyDataService createTextApplyDataService(final TextNodeSettingsService textSettingsService,
        final NodeID nodeID, final WorkflowManager wfm) {
        return new TextApplyDataService() {

            @Override
            public Optional<String> validateData(final String data) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void applyData(final String data) throws IOException {
                var settings = new NodeSettings("node_settings");
                try {
                    wfm.saveNodeSettings(nodeID, settings);
                    NodeSettingsWO modelSettings;
                    if (settings.containsKey("model")) {
                        modelSettings = settings.getNodeSettings("model");
                    } else {
                        modelSettings = settings.addNodeSettings("model");
                    }
                    textSettingsService.writeSettings(data, modelSettings);
                    wfm.loadNodeSettings(nodeID, settings);
                } catch (InvalidSettingsException ex) {
                    throw new IOException("Invalid node settings", ex);
                }
            }
        };
    }

}
