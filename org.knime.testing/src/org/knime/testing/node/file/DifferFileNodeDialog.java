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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   29.04.2011 (hofer): created
 */
package org.knime.testing.node.file;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;


/**
 * This is the dialog for the Differ File Node.
 *
 * @author Heiko Hofer
 */
public class DifferFileNodeDialog extends NodeDialogPane {
    private JComboBox m_urlFlowVar;
    private JComboBox m_referenceUrlFlowVar;

    /**
     * Creates a new dialog.
     */
    public DifferFileNodeDialog() {
        super();
        addTab("Settings", createFilePanel());
    }

    private JPanel createFilePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;

        m_urlFlowVar = new JComboBox();
        m_urlFlowVar.setEditable(false);
        m_urlFlowVar.setRenderer(new FlowVariableListCellRenderer());
        m_urlFlowVar.setBorder(BorderFactory.createTitledBorder("Test File"));
        p.add(m_urlFlowVar, c);

        c.gridy++;
        m_referenceUrlFlowVar = new JComboBox();
        m_referenceUrlFlowVar.setEditable(false);
        m_referenceUrlFlowVar.setRenderer(new FlowVariableListCellRenderer());
        m_referenceUrlFlowVar.setBorder(BorderFactory.createTitledBorder(
        		"Reference File"));
        p.add(m_referenceUrlFlowVar, c);

        c.gridy++;
        c.weighty = 1;
        p.add(new JPanel(), c);

        return p;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
    		throws InvalidSettingsException {
        DifferFileNodeSettings s = new DifferFileNodeSettings();

        FlowVariable urlFlow = (FlowVariable)m_urlFlowVar.getSelectedItem();
        String url = urlFlow != null ? urlFlow.getName() : null;
        s.setTestFileFlowVar(url);
        FlowVariable referenceUrlFlow =
        	(FlowVariable)m_referenceUrlFlowVar.getSelectedItem();
        String referenceUrl = referenceUrlFlow != null ?
        		referenceUrlFlow.getName() : null;
        s.setReferenceFileFlowVar(referenceUrl);

        s.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
    	DifferFileNodeSettings s = new DifferFileNodeSettings();
        s.loadSettingsDialog(settings);

        m_urlFlowVar.removeAllItems();
        Map<String, FlowVariable> scopeVars = getAvailableFlowVariables();
        for (FlowVariable v : scopeVars.values()) {
        	if (v.getType().equals(FlowVariable.Type.STRING)) {
	        	m_urlFlowVar.addItem(v);
	        	if (v.getName().equals(s.getTestFileFlowVar())) {
	        		m_urlFlowVar.setSelectedItem(v);
	        	}
        	}
        }

        m_referenceUrlFlowVar.removeAllItems();
        for (FlowVariable v : scopeVars.values()) {
        	if (v.getType().equals(FlowVariable.Type.STRING)) {
	        	m_referenceUrlFlowVar.addItem(v);
	        	if (v.getName().equals(s.getReferenceFileFlowVar())) {
	        		m_referenceUrlFlowVar.setSelectedItem(v);
	        	}
        	}
        }
    }

}
