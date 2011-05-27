/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 */
package org.knime.core.node;

import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.quickform.AbstractQuickFormConfiguration;
import org.knime.core.quickform.QuickFormConfigurationPanel;
import org.knime.core.quickform.in.QuickFormInputNode;

/**
 * An empty dialog, which is used to create dialogs with only miscellaneous tabs
 * (such as memory policy and job selector panel).
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class MetaNodeDialogPane extends NodeDialogPane {

    private final Map<QuickFormInputNode, QuickFormConfigurationPanel
            <AbstractQuickFormConfiguration>> m_nodes;

    /** Constructor. */
    public MetaNodeDialogPane() {
        m_nodes = null;
    }

    /** Constructor.
     * @param nodes map of <code>NodeID</code> to {@link QuickFormInputNode}
     */
    public MetaNodeDialogPane(final Map<NodeID, QuickFormInputNode> nodes) {
        m_nodes = new LinkedHashMap<QuickFormInputNode,
            QuickFormConfigurationPanel<AbstractQuickFormConfiguration>>();

        JPanel panel = new JPanel();
        final BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(boxLayout);

//        Collections.sort(m_nodes, new FormComparator<QuickFormInputNode>());

        // a list of quick form elements that will sorted below according to
        // the weight values
        for (Map.Entry<NodeID, QuickFormInputNode> e : nodes.entrySet()) {
//            m_forms.add(new Pair<NodeID, AbstractQuickFormInElement>(
//                    e.getKey(), e.getValue().getQuickFormElement()));
            AbstractQuickFormConfiguration config =
                e.getValue().getConfiguration();
            QuickFormConfigurationPanel<AbstractQuickFormConfiguration>
                quickform = (QuickFormConfigurationPanel
                    <AbstractQuickFormConfiguration>)
                            config.createController();

            JPanel qpanel = new JPanel();
            final BoxLayout boxLayout2 = new BoxLayout(qpanel,
                    BoxLayout.Y_AXIS);
            qpanel.setLayout(boxLayout2);
            qpanel.setBorder(BorderFactory.createTitledBorder(
                    config.getLabel()));

            for (JComponent comp : quickform.getComponent()) {
                JPanel p = new JPanel(new FlowLayout());
                p.add(new JLabel(comp.getName()));
                p.add(comp);
                qpanel.add(p);
            }
            panel.add(qpanel);
            m_nodes.put(e.getValue(), quickform);
        }
        addTab("Quickforms", panel);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        // TODO
        if (m_nodes != null) {
            for (Map.Entry<QuickFormInputNode, QuickFormConfigurationPanel
                    <AbstractQuickFormConfiguration>> e : m_nodes.entrySet()) {
                AbstractQuickFormConfiguration config =
                    e.getKey().getConfiguration();
                e.getValue().saveSettings(config);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        // TODO
        if (m_nodes != null) {
            for (Map.Entry<QuickFormInputNode, QuickFormConfigurationPanel
                    <AbstractQuickFormConfiguration>> e : m_nodes.entrySet()) {
                AbstractQuickFormConfiguration config =
                    e.getKey().getConfiguration();
                e.getValue().loadSettings(config);
            }
        }
    }



}
