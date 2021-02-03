/*
 * ------------------------------------------------------------------------
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
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.core.data.TableBackend;
import org.knime.core.data.TableBackendRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.DialogNodePanel;
import org.knime.core.node.dialog.DialogNodeRepresentation;
import org.knime.core.node.dialog.DialogNodeValue;
import org.knime.core.node.dialog.MetaNodeDialogNode;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.quickform.AbstractQuickFormConfiguration;
import org.knime.core.quickform.AbstractQuickFormValueInConfiguration;
import org.knime.core.quickform.QuickFormConfigurationPanel;
import org.knime.core.quickform.in.QuickFormInputNode;
import org.knime.core.util.Pair;

/**
 * An empty dialog, which is used to create dialogs with only miscellaneous tabs
 * (such as memory policy and job selector panel).
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings({"unchecked", "rawtypes" })
public final class MetaNodeDialogPane extends NodeDialogPane {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MetaNodeDialogPane.class);
    private static final String OPTIONS_TAB_NAME = "Options";

    /** Type of dialog.
     * @noreference This enum is not intended to be referenced by clients.
     * @since 4.3
     */
    public enum MetaNodeDialogType {
        WORKFLOW,
        METANODE,
        SUBNODE
    }

    private final MetaNodeDialogType m_metaNodeDialogType;
    private final Map<NodeID, MetaNodeDialogNode> m_nodes;
    private final Map<NodeID, QuickFormConfigurationPanel> m_quickFormInputNodePanels;
    private final Map<NodeID, DialogNodePanel> m_dialogNodePanels;
    private final TableBackendSelectorPanel m_tableBackendSelectorPanel;
    private final JPanel m_panel;

    /**  @param metaNodeDialogType represents where this dialog is used.
     * @since 4.3 */
    public MetaNodeDialogPane(final MetaNodeDialogType metaNodeDialogType) {
        m_nodes = new LinkedHashMap<>();
        m_quickFormInputNodePanels = new LinkedHashMap<>();
        m_dialogNodePanels = new LinkedHashMap<>();

        m_panel = new JPanel();
        final BoxLayout boxLayout = new BoxLayout(m_panel, BoxLayout.Y_AXIS);
        m_panel.setLayout(boxLayout);
        addTab(OPTIONS_TAB_NAME, new JScrollPane(m_panel));
        m_metaNodeDialogType = metaNodeDialogType;
        if (metaNodeDialogType == MetaNodeDialogType.WORKFLOW) {
            m_tableBackendSelectorPanel = new TableBackendSelectorPanel();
            addTab("Table Backend", new JScrollPane(m_tableBackendSelectorPanel));
        } else {
            m_tableBackendSelectorPanel = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void addFlowVariablesTab() {
        if (m_metaNodeDialogType == MetaNodeDialogType.SUBNODE) {
            super.addFlowVariablesTab();
        } else {
            // no op: disables flow variables tab
        }
    }

    /** used in test framework to assert node count.
     * @return the nodes
     */
    Map<NodeID, MetaNodeDialogNode> getNodes() {
        return m_nodes;
    }

    /**
     * Set dialog nodes into this dialog; called just before
     * {@link #loadSettingsFrom(NodeSettingsRO,
     * org.knime.core.data.DataTableSpec[])} is called.
     * Only supports the new QuickForm and Configuration Nodes.
     * @param nodes the dialog nodes to show settings for
     * @param order the order, in which the dialog nodes should be shown, possibly null
     * @since 4.3
     */
    public final void setQuickformNodes(final Map<NodeID, ? extends MetaNodeDialogNode> nodes,
        final List<Integer> order) {
        m_nodes.clear();
        m_quickFormInputNodePanels.clear();
        m_dialogNodePanels.clear();
        // remove all dialog components from current panel
        m_panel.removeAll();

        List<Pair<Integer, Pair<NodeID, MetaNodeDialogNode>>> sortedNodeList = new ArrayList<>();
        for (Map.Entry<NodeID, ? extends MetaNodeDialogNode> e : nodes.entrySet()) {
            Integer orderIndex = Integer.MAX_VALUE;
            // only accept old qf nodes for metanodes
            if (m_metaNodeDialogType != MetaNodeDialogType.SUBNODE && e.getValue() instanceof QuickFormInputNode) {
                AbstractQuickFormConfiguration
                    <? extends AbstractQuickFormValueInConfiguration> config =
                        ((QuickFormInputNode)e.getValue()).getConfiguration();
                if (config == null) { // quickform nodes has no valid configuration
                    continue;
                }
                QuickFormConfigurationPanel
                    <? extends AbstractQuickFormValueInConfiguration> quickform =
                        config.createController();
                m_nodes.put(e.getKey(), e.getValue());
                m_quickFormInputNodePanels.put(e.getKey(), quickform);
                Pair<Integer, Pair<NodeID, MetaNodeDialogNode>> weightNodePair = Pair.create(
                            config.getWeight(), new Pair<NodeID, MetaNodeDialogNode>(e.getKey(), e.getValue()));
                sortedNodeList.add(weightNodePair);
            // only accept new qf nodes for subnodes
            } else if (m_metaNodeDialogType == MetaNodeDialogType.SUBNODE && e.getValue() instanceof DialogNode) {
                // Add Dialogs in the order received
                if (order != null && order.contains(e.getKey().getIndex())) {
                    orderIndex = order.indexOf(e.getKey().getIndex());
                }
                DialogNodeRepresentation<? extends DialogNodeValue> representation
                    = ((DialogNode)e.getValue()).getDialogRepresentation();
                if (((DialogNode)e.getValue()).isHideInDialog() || representation == null) {
                    // no valid representation
                    continue;
                }
                try {
                    DialogNodePanel dialogPanel = representation.createDialogPanel();
                    m_nodes.put(e.getKey(), e.getValue());
                    m_dialogNodePanels.put(e.getKey(), dialogPanel);
                    Pair<Integer, Pair<NodeID, MetaNodeDialogNode>> weightNodePair =
                        Pair.create(orderIndex, Pair.create(e.getKey(), e.getValue()));
                    sortedNodeList.add(weightNodePair);
                } catch (Exception ex) {
                    LOGGER.error("The dialog pane for node " + e.getKey() + " could not be created.", ex);
                }
            }
        }

        Collections.sort(sortedNodeList, (o1, o2) -> o1.getFirst() - o2.getFirst());

        for (Pair<Integer, Pair<NodeID, MetaNodeDialogNode>> weightNodePair : sortedNodeList) {
            NodeID id = weightNodePair.getSecond().getFirst();
            MetaNodeDialogNode node = weightNodePair.getSecond().getSecond();
            if (node instanceof QuickFormInputNode) {
                final QuickFormConfigurationPanel<?> qconfPanel = m_quickFormInputNodePanels.get(id);
                JPanel qpanel = new JPanel();
                final BoxLayout boxLayout2 = new BoxLayout(qpanel, BoxLayout.Y_AXIS);
                qpanel.setLayout(boxLayout2);
                qpanel.setBorder(BorderFactory.createTitledBorder((String) null));
                JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
                p.add(qconfPanel);
                qpanel.add(p);
                m_panel.add(qpanel);
            } else if (node instanceof DialogNode) {
                DialogNodePanel<? extends DialogNodeValue> nodePanel = m_dialogNodePanels.get(id);

                JPanel dpanel = new JPanel();
                final BoxLayout boxLayout2 = new BoxLayout(dpanel, BoxLayout.Y_AXIS);
                dpanel.setLayout(boxLayout2);
                dpanel.setBorder(BorderFactory.createTitledBorder((String) null));

                JPanel p = new JPanel(new BorderLayout());
                p.add(nodePanel, BorderLayout.CENTER);
                dpanel.add(p);
                m_panel.add(dpanel);
            }
        }

        // no configuration on workflows using "quickforms", functionality got deprecated/legacy'ed with 3.5 (AP-7774)
        // only old style QuickFormInputNode are supported -- hide the tab by default
        if (m_metaNodeDialogType == MetaNodeDialogType.WORKFLOW) {
            removeTab(OPTIONS_TAB_NAME); // now removed by default starting with 4.3
            if (!m_nodes.isEmpty()) {
                addTabAt(0, OPTIONS_TAB_NAME, m_panel);
            }
        }

        if (m_nodes.isEmpty()) {
            String msg;
            if (m_metaNodeDialogType == MetaNodeDialogType.SUBNODE) {
                msg = "Component is not configurable.<br>Please include Configuration nodes.";
            } else {
                msg = "Metanode is not configurable.";
            }
            m_panel.add(new JLabel("<html>" + msg + "</html>"));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        for (Map.Entry<NodeID, MetaNodeDialogNode> e : m_nodes.entrySet()) {
            final NodeID key = e.getKey();
            if (e.getValue() instanceof QuickFormInputNode) {
                AbstractQuickFormConfiguration config = ((QuickFormInputNode)e.getValue()).getConfiguration();
                AbstractQuickFormValueInConfiguration valueConfig = config.createValueConfiguration();
                QuickFormConfigurationPanel value = m_quickFormInputNodePanels.get(key);
                value.saveSettings(valueConfig);
                NodeSettingsWO subSettings = settings.addNodeSettings((Integer.toString(key.getIndex())));
                valueConfig.saveValue(subSettings);
            } else if (e.getValue() instanceof DialogNode) {
                DialogNode dialogNode = (DialogNode)e.getValue();
                DialogNodePanel nodePanel = m_dialogNodePanels.get(key);
                DialogNodeValue nodeValue = nodePanel.getNodeValue();
                final String parameterName = SubNodeContainer.getDialogNodeParameterName(dialogNode, key);
                if (nodeValue != null) {
                    NodeSettingsWO subSettings = settings.addNodeSettings((parameterName));
                    nodeValue.saveToNodeSettings(subSettings);
                }
            }
        }
        if (m_metaNodeDialogType == MetaNodeDialogType.WORKFLOW) {
            m_tableBackendSelectorPanel.toBackendSettings().saveSettingsTo(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
        final PortObjectSpec[] specs) throws NotConfigurableException {
        // This method ignored the input specs - make sure to review optional
        // inputs in case quickforms use the input data (e.g. at some point
        // we may have quickform nodes to allow a column selection?)
        for (Map.Entry<NodeID, MetaNodeDialogNode> e : m_nodes.entrySet()) {
            if (e.getValue() instanceof QuickFormInputNode) {
                AbstractQuickFormConfiguration config = ((QuickFormInputNode)e.getValue()).getConfiguration();
                AbstractQuickFormValueInConfiguration valueConfig = config.getValueConfiguration();
                try {
                    NodeSettingsRO subSettings = settings.getNodeSettings(Integer.toString(e.getKey().getIndex()));
                    valueConfig.loadValueInDialog(subSettings);
                    QuickFormConfigurationPanel panel = m_quickFormInputNodePanels.get(e.getKey());
                    assert panel != null : "No panel instance for node " + e.getKey();
                    panel.loadSettings(valueConfig);
                } catch (InvalidSettingsException ise) {
                    // no op
                }
            } else if (e.getValue() instanceof DialogNode) {
                final DialogNode dialogNode = (DialogNode)e.getValue();
                final DialogNodeValue nodeValue = dialogNode.createEmptyDialogValue();
                final String parameterName = SubNodeContainer.getDialogNodeParameterName(dialogNode, e.getKey());
                try {
                    NodeSettingsRO subSettings = settings.getNodeSettings(parameterName);
                    nodeValue.loadFromNodeSettingsInDialog(subSettings);
                    final DialogNodePanel dialogNodePanel = m_dialogNodePanels.get(e.getKey());
                    dialogNodePanel.loadNodeValue(nodeValue);
                } catch (InvalidSettingsException ex) {
                    // no op
                }
            }
        }
        if (m_metaNodeDialogType == MetaNodeDialogType.WORKFLOW) {
            m_tableBackendSelectorPanel.load(WorkflowTableBackendSettings.loadSettingsInDialog(settings));
        }
    }

    @SuppressWarnings("serial")
    private static final class TableBackendSelectorPanel extends JPanel {

        private final JComboBox<TableBackend> m_tableBackendCombo;
        private final JPanel m_descriptionPanel;

        /**
         *
         */
        TableBackendSelectorPanel() {
            super(new BorderLayout());
            List<TableBackend> availableBackends = TableBackendRegistry.getInstance().getTableBackends();
            m_tableBackendCombo = new JComboBox<>(availableBackends.toArray(new TableBackend[0]));
            m_tableBackendCombo.setRenderer(new DefaultListCellRenderer() { // NOSONAR
                @Override
                public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                    Object newValue = value instanceof TableBackend ? ((TableBackend)value).getShortName() : value;
                    return super.getListCellRendererComponent(list, newValue, index, isSelected, cellHasFocus);
                }
            });
            m_descriptionPanel = new JPanel(new BorderLayout());
            m_tableBackendCombo.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    onNewBackendSelected();
                }
            });
            onNewBackendSelected();
            JPanel northPanel = ViewUtils.getInFlowLayout(FlowLayout.CENTER, m_tableBackendCombo);
            northPanel.setBorder(BorderFactory.createTitledBorder(" Selected Table Backend "));
            add(northPanel, BorderLayout.NORTH);
            m_descriptionPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
            add(m_descriptionPanel, BorderLayout.CENTER);
        }

        private void onNewBackendSelected() {
            m_descriptionPanel.removeAll();
            TableBackend selectedItem = (TableBackend)m_tableBackendCombo.getSelectedItem();
            JLabel jLabel = new JLabel(selectedItem.getDescription());
            m_descriptionPanel.add(ViewUtils.getInFlowLayout(jLabel));
            m_descriptionPanel.invalidate();
            m_descriptionPanel.validate();
            m_descriptionPanel.repaint();
        }

        void load(final WorkflowTableBackendSettings settings) {
            m_tableBackendCombo.setSelectedItem(settings.getTableBackend());
        }

        WorkflowTableBackendSettings toBackendSettings() {
            return new WorkflowTableBackendSettings((TableBackend)m_tableBackendCombo.getSelectedItem());
        }
    }

}
