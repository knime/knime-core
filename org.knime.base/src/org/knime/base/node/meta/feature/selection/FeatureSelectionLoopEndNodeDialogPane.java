/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   15.03.2016 (adrian): created
 */
package org.knime.base.node.meta.feature.selection;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Collection;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class FeatureSelectionLoopEndNodeDialogPane extends NodeDialogPane {

    private JComboBox<FlowVariable> m_scoreVariableComboBox;

    private JCheckBox m_isMinimizeCheckBox;

    /**
     * The constructor
     */
    public FeatureSelectionLoopEndNodeDialogPane() {
        m_scoreVariableComboBox = new JComboBox<FlowVariable>();
        m_scoreVariableComboBox.setRenderer(new FlowVariableListCellRenderer());
        m_isMinimizeCheckBox = new JCheckBox("Minimize score");
        init();
    }

    private void init() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        panel.add(new JLabel("Score"), gbc);
        gbc.gridy += 1;
        panel.add(m_scoreVariableComboBox, gbc);
        gbc.gridy += 1;
        panel.add(m_isMinimizeCheckBox, gbc);
        addTab("Options", new JScrollPane(panel));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final FeatureSelectionLoopEndSettings cfg = new FeatureSelectionLoopEndSettings();
        cfg.setScoreVariableName(((FlowVariable)m_scoreVariableComboBox.getSelectedItem()).getName());
        cfg.setIsMinimize(m_isMinimizeCheckBox.isSelected());
        cfg.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        final FeatureSelectionLoopEndSettings cfg = new FeatureSelectionLoopEndSettings();
        cfg.loadInDialog(settings);
        final Collection<FlowVariable> flowVars = getAvailableFlowVariables().values();
        m_scoreVariableComboBox.removeAllItems();
        FlowVariable selected = null;
        final String scoreVariableName = cfg.getScoreVariableName();
        boolean compatibleFVexists = false;
        for (FlowVariable flowVar : flowVars) {
            FlowVariable.Type flowvarType = flowVar.getType();
            if (flowvarType == FlowVariable.Type.DOUBLE) {
                if (flowVar.getName().equals(scoreVariableName)) {
                    selected = flowVar;
                }
                m_scoreVariableComboBox.addItem(flowVar);
                compatibleFVexists = true;
            }
        }
        if (!compatibleFVexists) {
            throw new NotConfigurableException("There is no compatible Flow Variable (Double) at the inport.");
        }
        m_scoreVariableComboBox.setSelectedItem(selected);
    }

}
