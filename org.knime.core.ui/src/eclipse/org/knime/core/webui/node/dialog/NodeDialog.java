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
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataServiceProvider;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.text.TextApplyDataService;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.node.view.NodeViewManager;
import org.knime.core.webui.page.Page;

/**
 * Represents a dialog of a node.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.5
 */
public abstract class NodeDialog implements DataServiceProvider {

    private final NativeNodeContainer m_nnc;

    private final Set<SettingsType> m_settingsTypes;

    /**
     * Creates a new node dialog instance.
     *
     * NOTE: when called a {@link NodeContext} needs to be available
     *
     * @param settingsTypes the list of {@link SettingsType}s the {@link TextSettingsDataService} is able to deal with; must not be
     *            empty
     */
    protected NodeDialog(final SettingsType... settingsTypes) {
        CheckUtils.checkState(settingsTypes.length > 0, "At least one settings type must be provided");
        m_settingsTypes = Set.of(settingsTypes);
        m_nnc = (NativeNodeContainer)NodeContext.getContext().getNodeContainer();
    }

    /**
     * Returns the (html) page which represents the view UI.
     *
     * @return the page
     */
    public abstract Page getPage();

    @Override
    public final Optional<InitialDataService> createInitialDataService() {
        return Optional.of(new TextInitialDataServiceImpl());
    }

    @Override
    public final Optional<ApplyDataService> createApplyDataService() {
        return Optional.of(new TextApplyDataServiceImpl());
    }

    private final class TextInitialDataServiceImpl implements TextInitialDataService {
        @Override
        public String getInitialData() {
            final var specs = new PortObjectSpec[m_nnc.getNrInPorts()];
            final var wfm = m_nnc.getParent();
            for (var cc : wfm.getIncomingConnectionsFor(m_nnc.getID())) {
                specs[cc.getDestPort()] =
                    wfm.getNodeContainer(cc.getSource()).getOutPort(cc.getSourcePort()).getPortObjectSpec();
            }

            NodeContext.pushContext(m_nnc);
            try {
                Map<SettingsType, NodeSettingsRO> settings = new EnumMap<>(SettingsType.class);
                if (m_settingsTypes.contains(SettingsType.MODEL)) {
                    var modelSettings = new NodeSettings("node_settings");
                    m_nnc.getNode().saveModelSettingsTo(modelSettings);
                    settings.put(SettingsType.MODEL, modelSettings);
                }
                if (m_settingsTypes.contains(SettingsType.VIEW)) {
                    NodeSettings viewSettings;
                    try {
                        viewSettings = m_nnc.getNodeSettings().getNodeSettings(SettingsType.VIEW.getConfigKey());
                    } catch (InvalidSettingsException ex) { // NOSONAR
                        viewSettings = new NodeSettings(SettingsType.VIEW.getConfigKey());
                    }
                    settings.put(SettingsType.VIEW, viewSettings);
                }
                return getSettingsDataService().getInitialData(settings, specs);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private final class TextApplyDataServiceImpl implements TextApplyDataService {
        @Override
        public Optional<String> validateData(final String data) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void applyData(final String data) throws IOException {
            var nodeSettings = new NodeSettings("node_settings");
            // to keep another copy of the settings to be able to tell whether
            // settings have been changed
            var previousNodeSettings = new NodeSettings("previous_settings");
            var wfm = m_nnc.getParent();
            var nodeID = m_nnc.getID();
            try {
                wfm.saveNodeSettings(nodeID, nodeSettings);
                wfm.saveNodeSettings(nodeID, previousNodeSettings);

                Map<SettingsType, NodeSettingsWO> settingsMap = new EnumMap<>(SettingsType.class);
                NodeSettings modelSettings = getModelSettings(nodeSettings, settingsMap);
                NodeSettings viewSettings =  getViewSettings(nodeSettings, settingsMap);

                // transfer data into settings
                getSettingsDataService().applyData(data, settingsMap);

                var modelSettingsChanged =
                    settingsChanged(previousNodeSettings, modelSettings, SettingsType.MODEL.getConfigKey());
                var viewSettingsChanged =
                    settingsChanged(previousNodeSettings, viewSettings, SettingsType.VIEW.getConfigKey());

                if (viewSettingsChanged) {
                    // load settings into node view
                    var nodeView = NodeViewManager.getInstance().getNodeView(m_nnc);
                    nodeView.validateSettings(viewSettings);
                    nodeView.loadValidatedSettingsFrom(viewSettings);
                }

                if (modelSettingsChanged) {
                    // 'persist' settings and load model settings into the node model
                    wfm.loadNodeSettings(nodeID, nodeSettings);
                } else if (viewSettingsChanged) {
                    // 'persist' view settings only (without resetting the node)
                    wfm.loadNodeViewSettings(nodeID, nodeSettings);
                }

            } catch (InvalidSettingsException ex) {
                throw new IOException("Invalid node settings", ex);
            }
        }

        private boolean settingsChanged(final NodeSettings previousNodeSettings, final NodeSettings subSettings, final String subSettingsKey)
            throws InvalidSettingsException {
            if (subSettings != null) {
                var previousViewSettings = getOrCreateSubSettings(previousNodeSettings, subSettingsKey);
                return !previousViewSettings.equals(subSettings);
            }
            return false;
        }

        private NodeSettings getViewSettings(final NodeSettings settings,
            final Map<SettingsType, NodeSettingsWO> settingsMap) throws InvalidSettingsException {
            if (hasViewSettings()) {
                var viewSettings = getOrCreateSubSettings(settings, SettingsType.VIEW.getConfigKey());
                settingsMap.put(SettingsType.VIEW, viewSettings);
                return viewSettings;
            }
            return null;
        }

        private NodeSettings getModelSettings(final NodeSettings settings,
            final Map<SettingsType, NodeSettingsWO> settingsMap) throws InvalidSettingsException {
            if (hasModelSettings()) {
                var modelSettings = getOrCreateSubSettings(settings, SettingsType.MODEL.getConfigKey());
                settingsMap.put(SettingsType.MODEL, modelSettings);
                return modelSettings;
            } else {
                // even if the node has no model settings,
                // we still have to add empty model settings since the wfm expects node settings to be present
                settings.addNodeSettings(SettingsType.MODEL.getConfigKey());
                return null;
            }
        }

        private NodeSettings getOrCreateSubSettings(final NodeSettings settings, final String key)
            throws InvalidSettingsException {
            NodeSettings subSettings;
            if (settings.containsKey(key)) {
                subSettings = settings.getNodeSettings(key);
            } else {
                subSettings = new NodeSettings(key);
                settings.addNodeSettings(subSettings);
            }
            return subSettings;
        }

        private boolean hasModelSettings() {
            return m_settingsTypes.contains(SettingsType.MODEL);
        }

        private boolean hasViewSettings() {
            return m_settingsTypes.contains(SettingsType.VIEW);
        }
    }

    /**
     * @return a {@link TextSettingsDataService}-instance
     */
    protected abstract TextSettingsDataService getSettingsDataService();

    /**
     * @return a legacy flow variable node dialog
     */
    public final NodeDialogPane createLegacyFlowVariableNodeDialog() {
        return new LegacyFlowVariableNodeDialog();
    }

    final class LegacyFlowVariableNodeDialog extends NodeDialogPane {

        @Override
        public void onOpen() {
            setSelected("Flow Variables");
        }

        @Override
        protected boolean hasModelSettings() {
            return m_settingsTypes.contains(SettingsType.MODEL);
        }

        @Override
        protected boolean hasViewSettings() {
            return m_settingsTypes.contains(SettingsType.VIEW);
        }

        @Override
        protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
            throws NotConfigurableException {
            //
        }

        @Override
        protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
            //
        }

        @Override
        protected NodeSettingsRO getDefaultViewSettings(final PortObjectSpec[] specs) {
            if (hasViewSettings()) {
                var ns = new NodeSettings("default_view_settings");
                getSettingsDataService().saveDefaultSettings(Map.of(SettingsType.VIEW, ns), specs);
                return ns;
            } else {
                return super.getDefaultViewSettings(specs);
            }
        }

        /**
         * For testing purposes only!
         *
         * @throws NotConfigurableException
         */
        void initDialogForTesting(final NodeSettingsRO settings, final PortObjectSpec[] spec)
            throws NotConfigurableException {
            Node.invokeDialogInternalLoad(this, settings, spec, null,
                FlowObjectStack.createFromFlowVariableList(Collections.emptyList(), new NodeID(0)),
                CredentialsProvider.EMPTY_CREDENTIALS_PROVIDER, false);
            setSelected("Flow Variables");
        }

    }

}
