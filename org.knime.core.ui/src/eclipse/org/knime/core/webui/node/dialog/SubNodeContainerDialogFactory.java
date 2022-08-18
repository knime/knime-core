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
 *   28 Jul 2022 (Carsten Haubold): created
 */
package org.knime.core.webui.node.dialog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.DialogNodeRepresentation;
import org.knime.core.node.dialog.DialogNodeValue;
import org.knime.core.node.dialog.util.ConfigurationLayoutUtil;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager.NodeModelFilter;
import org.knime.core.util.ui.converter.JsonFormsDialogBuilder;
import org.knime.core.util.ui.converter.UiComponentConverterRegistry;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.page.Page;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The SubNodeContainerDialogFactory creates a {@link NodeDialog} for all the configuration nodes inside
 * a {@link SubNodeContainer} by parsing the {@link DialogNodeRepresentation}s of those nodes and converting those
 * to jsonforms which is parsed by the NodeDialog page.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
final class SubNodeContainerDialogFactory implements NodeDialogFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SubNodeContainerDialogFactory.class);

    private static final String SUB_NODE_CONTAINER_UI_MODE_PROPERTY = "org.knime.component.ui.mode";

    private static final String SUB_NODE_CONTAINER_UI_MODE_SWING = "swing";

    private static final String SUB_NODE_CONTAINER_UI_MODE_JS = "js";

    private static final String SUB_NODE_CONTAINER_UI_MODE_DEFAULT = SUB_NODE_CONTAINER_UI_MODE_SWING;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SubNodeContainer m_snc;

    private static String getSubNodeContainerUiMode() {
        var mode = System.getProperty(SUB_NODE_CONTAINER_UI_MODE_PROPERTY);
        if (mode == null
            || (!mode.equals(SUB_NODE_CONTAINER_UI_MODE_SWING) && !mode.equals(SUB_NODE_CONTAINER_UI_MODE_JS))) {
            return SUB_NODE_CONTAINER_UI_MODE_DEFAULT;
        }
        return mode;
    }

    static boolean isSubNodeContainerNodeDialogEnabled() {
        return SUB_NODE_CONTAINER_UI_MODE_JS.equals(getSubNodeContainerUiMode());
    }

    /**
     * Initialize a SubNodeContainerDialogFactory with the {@link SubNodeContainer} for which the dialog should
     * be constructed.
     *
     * @param snc The SubNodeContainer for which the dialog will be built
     */
    public SubNodeContainerDialogFactory(final SubNodeContainer snc) {
        m_snc = snc;
    }

    /**
     * @return Create the dialog containing all the dialog elements that were found in the {@link SubNodeContainer}
     */
    @Override
    @SuppressWarnings("rawtypes")
    public NodeDialog createNodeDialog() {
        var wfm = m_snc.getWorkflowManager();
        Map<NodeID, DialogNode> nodes = wfm.findNodes(DialogNode.class, new NodeModelFilter<DialogNode>() { // NOSONAR
            @Override
            public boolean include(final DialogNode nodeModel) {
                return !nodeModel.isHideInDialog();
            }
        }, false);

        return new SubNodeContainerNodeDialog(nodes);
    }

    @SuppressWarnings("rawtypes")
    private static String getWorkflowRepresentationJson(final DialogNode node) throws IOException {
        var dialogRepr = node.getDialogRepresentation();
        if (!(dialogRepr instanceof WebViewContent)) {
            throw new IOException("Configuration Dialogs only work with elements that extend JSONViewContent");
        }
        var jsonDialogRepr = (WebViewContent)dialogRepr;
        try (var stream = jsonDialogRepr.saveToStream()) {
            if (!(stream instanceof ByteArrayOutputStream)) {
                throw new IllegalStateException(
                    "Cannot read json dialog representation from stream other than ByteArrayOutputStream");
            }
            return ((ByteArrayOutputStream)stream).toString(StandardCharsets.UTF_8);
        }
    }

    private class SubNodeContainerNodeDialog extends NodeDialog {
        private final TextNodeSettingsService m_settingsService;

        @SuppressWarnings("rawtypes")
        public SubNodeContainerNodeDialog(final Map<NodeID, DialogNode> dialogNodes) {
            super(SettingsType.MODEL);
            m_settingsService = new SubNodeContainerJsonSettingsService(dialogNodes);
        }

        @Override
        public Optional<DataService> createDataService() {
            return Optional.empty();
        }

        @Override
        public Page getPage() {
            // TODO: use the same files as referenced by the DefaultNodeDialog after this ticket has been closed:
            //       https://knime-com.atlassian.net/browse/UIEXT-437
            return Page.builder(NodeDialogManager.class, "js-src/vue/dist", "NodeDialog.umd.min.js").build();
        }

        @Override
        protected TextNodeSettingsService getNodeSettingsService() {
            return m_settingsService;
        }
    }

    private class SubNodeContainerJsonSettingsService implements JsonNodeSettingsService<String> {
        @SuppressWarnings("rawtypes")
        private final Map<NodeID, DialogNode> m_dialogNodes;

        @SuppressWarnings("rawtypes")
        public SubNodeContainerJsonSettingsService(final Map<NodeID, DialogNode> dialogNodes) {
            m_dialogNodes = dialogNodes;
        }

        @Override
        public void getDefaultNodeSettings(final Map<SettingsType, NodeSettingsWO> settings,
            final PortObjectSpec[] specs) {
            // Nothing to do, because we initialize the dialog from the workflow representation,
            // not the settings directly.
        }

        @SuppressWarnings("unchecked")
        @Override
        public void toNodeSettingsFromObject(final String jsonSettings,
            final Map<SettingsType, NodeSettingsWO> settings) {

            JsonNode newSettingsJson;
            try {
                newSettingsJson = OBJECT_MAPPER.readTree(jsonSettings);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Error occurred when parsing the settings provided by the dialog", ex);
            }

            var modelSettings = settings.get(SettingsType.MODEL);
            List<NodeID> orderedNodeIDs = getNodeOrder(m_dialogNodes);
            for (var dialogNodeId : orderedNodeIDs) {
                var dialogNode = m_dialogNodes.get(dialogNodeId);

                try {
                    DialogNodeValue value = dialogNode.getDialogValue();
                    var jsonStr = getWorkflowRepresentationJson(dialogNode);
                    String parameterName = "param_" + dialogNodeId.getIndex();
                    value.loadFromJson(UiComponentConverterRegistry.getConverter(jsonStr, parameterName)
                        .getDialogNodeValueJsonFromJsonFormsModel(newSettingsJson.get("model")));

                    dialogNode.validateDialogValue(value);
                    dialogNode.setDialogValue(value);
                    String settingsParameterName = dialogNode.getParameterName();
                    if (settingsParameterName == null || settingsParameterName.strip().isEmpty()) {
                        throw new IllegalStateException("Dialog Node has no valid parameter name, can't save settings");
                    }
                    settingsParameterName += "-" + dialogNodeId.getIndex();
                    value.saveToNodeSettings(modelSettings.addNodeSettings(settingsParameterName));
                } catch (Exception e) { // We want to catch everything here, or settings won't be saved!
                    LOGGER.error("Could not read dialog node " + dialogNode.toString(), e);
                }
            }
        }

        @Override
        public String fromJson(final String json) {
            return json;
        }

        @Override
        public String fromNodeSettingsToObject(final Map<SettingsType, NodeSettingsRO> settings,
            final PortObjectSpec[] specs) {
            JsonFormsDialogBuilder dialogBuilder = new JsonFormsDialogBuilder();

            List<NodeID> orderedNodeIDs = getNodeOrder(m_dialogNodes);
            for (var dialogNodeId : orderedNodeIDs) {
                var dialogNode = m_dialogNodes.get(dialogNodeId);
                try {
                    var jsonStr = getWorkflowRepresentationJson(dialogNode);
                    dialogBuilder.addUiComponent(jsonStr, "param_" + dialogNodeId.getIndex());
                } catch (IOException | IllegalStateException e) {
                    LOGGER.error("Could not read dialog node " + dialogNode.toString(), e);
                }
            }

            return dialogBuilder.build();
        }

        @Override
        public String toJson(final String obj) {
            return obj;
        }

        /**
         * Sort the dialog node IDs according to the user provided preference.
         *
         * Note: The ordering is requested each time the dialog is opened. Otherwise, the ordering would stay as it was when
         * the dialog was first created, because they are cached.
         */
        @SuppressWarnings("rawtypes")
        private List<NodeID> getNodeOrder(final Map<NodeID, DialogNode> nodes) {
            List<Integer> order = ConfigurationLayoutUtil.getConfigurationOrder(
                m_snc.getSubnodeConfigurationLayoutStringProvider(), nodes, m_snc.getWorkflowManager());

            // Will contain the nodes in the ordering given by `order`.
            // Nodes not mentioned in `order` will be placed at the end in arbitrary order.
            TreeMap<Integer, NodeID> orderedNodeIDs = new TreeMap<>();
            List<NodeID> unorderedNodeIDs = new ArrayList<>(nodes.size());
            nodes.forEach((nodeId, node) -> {
                int targetIndex = order.indexOf(nodeId.getIndex());
                if (targetIndex == -1) {
                    unorderedNodeIDs.add(nodeId);
                } else {
                    orderedNodeIDs.put(targetIndex, nodeId);
                }
            });
            List<NodeID> res = new ArrayList<>();
            res.addAll(orderedNodeIDs.values()); // `values` is ordered
            res.addAll(unorderedNodeIDs);
            return res;
        }
    }
}
