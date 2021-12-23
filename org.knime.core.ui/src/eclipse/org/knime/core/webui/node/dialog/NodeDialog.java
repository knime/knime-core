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
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
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

    private static final String CFG_MODEL = "model";

    private static final String CFG_VIEW = "view";

    private final NativeNodeContainer m_nnc;

    private final TextSettingsMapper m_settingsMapper;

    private final Set<SettingsType> m_settingsTypes;

    /**
     * Creates a new node dialog instance.
     *
     * NOTE: when called a {@link NodeContext} needs to be available
     *
     * @param settingsMapper
     * @param settingsTypes the list of {@link SettingsType}s the settings mapper is able to deal with; must not be
     *            empty
     */
    protected NodeDialog(final TextSettingsMapper settingsMapper, final SettingsType... settingsTypes) {
        CheckUtils.checkState(settingsTypes.length > 0, "At least one settings type must be provided");
        m_settingsMapper = settingsMapper;
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
    public final Optional<InitialDataService> getInitialDataService() {
        return Optional.of(new TextInitialDataServiceImpl());
    }

    @Override
    public final Optional<ApplyDataService> getApplyDataService() {
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
                    // TODO
                    // var viewSettings = m_nnc.getSingleNodeContainerSettings().getViewSettings();
                    // settings.put(SettingsType.VIEW, viewSettings);
                }
                return m_settingsMapper.fromSettings(settings, specs);
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
            var settings = new NodeSettings("node_settings");
            var wfm = m_nnc.getParent();
            var nodeID = m_nnc.getID();
            try {
                wfm.saveNodeSettings(nodeID, settings);
                Map<SettingsType, NodeSettingsWO> settingsMap = new EnumMap<>(SettingsType.class);
                NodeSettings viewSettings = null;
                if (hasModelSettings()) {
                    NodeSettings modelSettings;
                    modelSettings = getOrCreateSubSettings(settings, CFG_MODEL);
                    settingsMap.put(SettingsType.MODEL, modelSettings);
                }
                if (hasViewSettings()) {
                    viewSettings = getOrCreateSubSettings(settings, CFG_VIEW);
                    settingsMap.put(SettingsType.VIEW, viewSettings);
                }

                m_settingsMapper.toSettings(data, settingsMap);
                wfm.loadNodeSettings(nodeID, settings);

                if (viewSettings != null) {
                    var nodeView = NodeViewManager.getInstance().getNodeView(m_nnc);
                    nodeView.validateSettings(viewSettings);
                    nodeView.loadValidatedSettingsFrom(viewSettings);
                }
            } catch (InvalidSettingsException ex) {
                throw new IOException("Invalid node settings", ex);
            }
        }

        private NodeSettings getOrCreateSubSettings(final NodeSettings settings, final String key)
            throws InvalidSettingsException {
            NodeSettings modelSettings;
            if (settings.containsKey(key)) {
                modelSettings = settings.getNodeSettings(key);
            } else {
                modelSettings = new NodeSettings(key);
                settings.addNodeSettings(modelSettings);
            }
            return modelSettings;
        }

        private boolean hasModelSettings() {
            return m_settingsTypes.contains(SettingsType.MODEL);
        }

        private boolean hasViewSettings() {
            return m_settingsTypes.contains(SettingsType.VIEW);
        }
    }

}
