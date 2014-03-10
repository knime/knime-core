/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 * ---------------------------------------------------------------------
 *
 * Created on 14.10.2013 by Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.core.node.workflow;

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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.DialogNodePanel;
import org.knime.core.node.dialog.DialogNodeRepresentation;
import org.knime.core.node.dialog.DialogNodeValue;
import org.knime.core.util.Pair;

/**
 *
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 * @since 2.9
 */
public final class SubNodeDialogPane extends NodeDialogPane {

    private final Map<Pair<NodeID, DialogNode<? extends DialogNodeRepresentation<?>,
        ? extends DialogNodeValue>>, DialogNodePanel<? extends DialogNodeValue>> m_nodes;

    private final JPanel m_panel;

    /**
     *
     */
    public SubNodeDialogPane() {
        m_nodes = new LinkedHashMap<Pair<NodeID, DialogNode<? extends DialogNodeRepresentation<?>,
            ? extends DialogNodeValue>>, DialogNodePanel<? extends DialogNodeValue>>();
        m_panel = new JPanel();
        final BoxLayout boxLayout = new BoxLayout(m_panel, BoxLayout.Y_AXIS);
        m_panel.setLayout(boxLayout);
        addTab("SubNode Settings", new JScrollPane(m_panel));
    }

    /**
     * Set dialog nodes into this dialog; called just before
     * {@link #loadSettingsFrom(NodeSettingsRO,
     * org.knime.core.data.DataTableSpec[])} is called.
     * @param nodes the dialog nodes to show settings for
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    final void setDialogNodes(final Map<NodeID, DialogNode> nodes) {
        m_panel.removeAll();
        List<Pair<Integer, DialogNodePanel<? extends DialogNodeValue>>> sortedPanelList
                = new ArrayList<Pair<Integer, DialogNodePanel<? extends DialogNodeValue>>>();
        // collect panels and weights
        for (Map.Entry<NodeID, DialogNode> entry : nodes.entrySet()) {
            DialogNodeRepresentation<? extends DialogNodeValue> representation
                = entry.getValue().getDialogRepresentation();
            if (representation == null) {
                // no valid representation
                continue;
            }
            DialogNodePanel dialogPanel = representation.createDialogPanel();
            dialogPanel.loadNodeValue(entry.getValue().getDialogValue());
            m_nodes.put(new Pair<NodeID, DialogNode<? extends DialogNodeRepresentation<?>,
                ? extends DialogNodeValue>>(entry.getKey(), entry.getValue()), dialogPanel);
            Pair<Integer, DialogNodePanel<? extends DialogNodeValue>> weightedPanelPair = new Pair<Integer,
                    DialogNodePanel<? extends DialogNodeValue>>(Integer.MAX_VALUE, dialogPanel);
            sortedPanelList.add(weightedPanelPair);
        }

        // sort panels according to weight
        Collections.sort(sortedPanelList, new Comparator<Pair<Integer, DialogNodePanel<? extends DialogNodeValue>>>() {
            @Override
            public int compare(final Pair<Integer, DialogNodePanel<? extends DialogNodeValue>> o1,
                    final Pair<Integer, DialogNodePanel<? extends DialogNodeValue>> o2) {
                return o1.getFirst() - o2.getFirst();
            }
        });

        // add panels
        for (Pair<Integer, DialogNodePanel<? extends DialogNodeValue>> weightedPanelPair : sortedPanelList) {
            DialogNodePanel<? extends DialogNodeValue> nodePanel = weightedPanelPair.getSecond();

            JPanel dpanel = new JPanel();
            final BoxLayout boxLayout2 = new BoxLayout(dpanel, BoxLayout.Y_AXIS);
            dpanel.setLayout(boxLayout2);
            dpanel.setBorder(BorderFactory.createTitledBorder((String) null));

            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
            p.add(nodePanel);
            dpanel.add(p);
            m_panel.add(dpanel);
        }
        if (m_nodes.isEmpty()) {
            m_panel.add(new JLabel("No valid dialog configurations."));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        for (Map.Entry<Pair<NodeID, DialogNode<? extends DialogNodeRepresentation<?>,
                ? extends DialogNodeValue>>, DialogNodePanel<? extends DialogNodeValue>> e : m_nodes.entrySet()) {
            Pair<NodeID, DialogNode<? extends DialogNodeRepresentation<?>,
                ? extends DialogNodeValue>> pair = e.getKey();
            DialogNodeValue nodeValue = pair.getSecond().getDialogValue();
            DialogNodePanel nodePanel = e.getValue();
            nodePanel.saveNodeValue(nodeValue);
            NodeSettingsWO subSettings = settings.addNodeSettings(
                (Integer.toString(pair.getFirst().getIndex())));
            nodeValue.saveToNodeSettings(subSettings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
        final DataTableSpec[] specs) throws NotConfigurableException {
        for (Map.Entry<Pair<NodeID, DialogNode<? extends DialogNodeRepresentation<?>,
            ? extends DialogNodeValue>>, DialogNodePanel<? extends DialogNodeValue>> e : m_nodes.entrySet()) {
            Pair<NodeID, DialogNode<? extends DialogNodeRepresentation<?>,
                ? extends DialogNodeValue>> pair = e.getKey();
            DialogNodeValue nodeValue = pair.getSecond().getDialogValue();
            try {
                NodeSettingsRO subSettings = settings.getNodeSettings(
                    Integer.toString(pair.getFirst().getIndex()));
                nodeValue.loadFromNodeSettings(subSettings);
            } catch (InvalidSettingsException ex) {
                // no op
            }
        }
    }
}
