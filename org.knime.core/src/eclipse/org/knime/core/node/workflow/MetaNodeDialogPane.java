/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.DialogNodePanel;
import org.knime.core.node.dialog.DialogNodeRepresentation;
import org.knime.core.node.dialog.DialogNodeValue;
import org.knime.core.node.dialog.MetaNodeDialogNode;
import org.knime.core.node.port.PortObjectSpec;
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
 */
@SuppressWarnings({"unchecked", "rawtypes" })
public final class MetaNodeDialogPane extends NodeDialogPane {

    private final Map<NodeID, MetaNodeDialogNode> m_nodes;

    private final Map<NodeID, QuickFormConfigurationPanel> m_quickFormInputNodePanels;

    private final Map<NodeID, DialogNodePanel> m_dialogNodePanels;

    private final JPanel m_panel;

    private final boolean m_usedInSubnode;

    /** Constructor. */
    public MetaNodeDialogPane() {
        this(false);
    }

    /** Constructor.
     * @param usedInSubnode false for meta nodes, true for sub node. */
    MetaNodeDialogPane(final boolean usedInSubnode) {
        m_nodes = new LinkedHashMap<NodeID, MetaNodeDialogNode>();
        m_quickFormInputNodePanels = new LinkedHashMap<NodeID, QuickFormConfigurationPanel>();
        m_dialogNodePanels = new LinkedHashMap<NodeID, DialogNodePanel>();

        m_panel = new JPanel();
        final BoxLayout boxLayout = new BoxLayout(m_panel, BoxLayout.Y_AXIS);
        m_panel.setLayout(boxLayout);
        addTab("QuickForms", new JScrollPane(m_panel));
        m_usedInSubnode = usedInSubnode;
    }

    /** {@inheritDoc} */
    @Override
    protected void addFlowVariablesTab() {
        if (m_usedInSubnode) {
            super.addFlowVariablesTab();
        } else {
            // no op: disables flow variables tab
        }
    }

    /**
     * Set quickform nodes into this dialog; called just before
     * {@link #loadSettingsFrom(NodeSettingsRO,
     * org.knime.core.data.DataTableSpec[])} is called.
     * @param nodes the quickform nodes to show settings for
     */
    final void setQuickformNodes(final Map<NodeID, MetaNodeDialogNode> nodes) {
        m_nodes.clear();
        m_quickFormInputNodePanels.clear();
        m_dialogNodePanels.clear();
        // If meta node contains new quickform nodes, ignore old ones
        boolean containsNewNodes = m_usedInSubnode;
        for (Map.Entry<NodeID, MetaNodeDialogNode> e : nodes.entrySet()) {
            containsNewNodes |= e.getValue() instanceof DialogNode;
        }
        // remove all quickform components from current panel
        m_panel.removeAll();
        List<Pair<Integer, Pair<NodeID, MetaNodeDialogNode>>> sortedNodeList =
                new ArrayList<Pair<Integer, Pair<NodeID, MetaNodeDialogNode>>>();
        for (Map.Entry<NodeID, MetaNodeDialogNode> e : nodes.entrySet()) {
            if (!containsNewNodes && e.getValue() instanceof QuickFormInputNode) {
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
                Pair<Integer, Pair<NodeID, MetaNodeDialogNode>> weightNodePair =
                    new Pair<Integer, Pair<NodeID, MetaNodeDialogNode>>(
                            config.getWeight(), new Pair<NodeID, MetaNodeDialogNode>(e.getKey(), e.getValue()));
                sortedNodeList.add(weightNodePair);
            } else if (e.getValue() instanceof DialogNode) {
                DialogNodeRepresentation<? extends DialogNodeValue> representation
                    = ((DialogNode)e.getValue()).getDialogRepresentation();
                if (((DialogNode)e.getValue()).isHideInDialog() || representation == null) {
                    // no valid representation
                    continue;
                }
                DialogNodePanel dialogPanel = representation.createDialogPanel();
//                dialogPanel.loadNodeValue(((DialogNode)e.getValue()).getDialogValue());
                m_nodes.put(e.getKey(), e.getValue());
                m_dialogNodePanels.put(e.getKey(), dialogPanel);
                Pair<Integer, Pair<NodeID, MetaNodeDialogNode>> weightNodePair =
                        new Pair<Integer, Pair<NodeID, MetaNodeDialogNode>>(
                                Integer.MAX_VALUE, new Pair<NodeID, MetaNodeDialogNode>(e.getKey(), e.getValue()));
                    sortedNodeList.add(weightNodePair);
            }
        }

        Collections.sort(sortedNodeList, new Comparator<Pair<Integer, Pair<NodeID, MetaNodeDialogNode>>>() {
            /** {@inheritDoc} */
            @Override
            public int compare(final Pair<Integer, Pair<NodeID, MetaNodeDialogNode>> o1,
                final Pair<Integer, Pair<NodeID, MetaNodeDialogNode>> o2) {
                return o1.getFirst() - o2.getFirst();
            }
        });

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

        if (m_nodes.isEmpty()) {
            m_panel.add(new JLabel("No valid Quickform configurations."));
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
                DialogNodeValue nodeValue = ((DialogNode)e.getValue()).createEmptyDialogValue();
                DialogNodePanel nodePanel = m_dialogNodePanels.get(key);
                nodePanel.saveNodeValue(nodeValue);
                NodeSettingsWO subSettings = settings.addNodeSettings((Integer.toString(key.getIndex())));
                nodeValue.saveToNodeSettings(subSettings);
            }
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
                    QuickFormConfigurationPanel value = (QuickFormConfigurationPanel)e.getValue();
                    value.loadSettings(valueConfig);
                } catch (InvalidSettingsException ise) {
                    // no op
                }
            } else if (e.getValue() instanceof DialogNode) {
                DialogNodeValue nodeValue = ((DialogNode)e.getValue()).createEmptyDialogValue();
                try {
                    NodeSettingsRO subSettings = settings.getNodeSettings(Integer.toString(e.getKey().getIndex()));
                    nodeValue.loadFromNodeSettingsInDialog(subSettings);
                    final DialogNodePanel dialogNodePanel = m_dialogNodePanels.get(e.getKey());
                    dialogNodePanel.loadNodeValue(nodeValue);
                } catch (InvalidSettingsException ex) {
                    // no op
                }
            }
        }
    }

}
