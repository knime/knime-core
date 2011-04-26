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
 *   Mar 31, 2011 (wiswedel): created
 */
package org.knime.core.node.workflow.virtual.parallelchunkstart;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;


/**
 * 
 * @author wiswedel, University of Konstanz
 */
final class ParallelChunkStartNodeDialogPane extends NodeDialogPane {

	private final JSpinner m_chunkCountSpinner;
	private final JRadioButton m_automaticChunkingButton;
	private final JRadioButton m_customChunkingButton;
	private final JCheckBox m_inactiveMainBranch;
	
	/**
	 * 
	 */
	ParallelChunkStartNodeDialogPane() {
		m_chunkCountSpinner = new JSpinner(
				new SpinnerNumberModel(
						ParallelChunkStartNodeConfiguration.DEFAULT_PROC_COUNT,
						1, Integer.MAX_VALUE, 1));
		
		m_automaticChunkingButton = new JRadioButton(
				"Use automatic chunk count");
		m_automaticChunkingButton.setToolTipText("Determine parallel chunk " 
				+ "count based on system's CPU count (" 
				+ ParallelChunkStartNodeConfiguration.DEFAULT_PROC_COUNT 
				+ " parallel threads)");
		
		m_customChunkingButton = new JRadioButton("Use custom chunk count");
		
		m_inactiveMainBranch = new JCheckBox("Disable Main Branch");
		
		ActionListener l = new ActionListener() {
			
			@Override
			public void actionPerformed(final ActionEvent e) {
				m_chunkCountSpinner.setEnabled(
						m_customChunkingButton.isSelected());
			}
		};
		m_automaticChunkingButton.addActionListener(l);
		m_customChunkingButton.addActionListener(l);
		
		ButtonGroup b = new ButtonGroup();
		b.add(m_automaticChunkingButton);
		b.add(m_customChunkingButton);
		m_automaticChunkingButton.doClick();
		
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		p.add(m_automaticChunkingButton, gbc);
		
		gbc.gridy += 1;
		gbc.gridwidth = 1;
		p.add(m_customChunkingButton, gbc);
		
		gbc.gridx += 1;
		p.add(m_chunkCountSpinner, gbc);

        gbc.gridy += 1;
        gbc.gridwidth = 1;
        p.add(m_inactiveMainBranch, gbc);

        addTab("Chunk Count Settings", p);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		ParallelChunkStartNodeConfiguration c = 
			new ParallelChunkStartNodeConfiguration();
		if (m_automaticChunkingButton.isSelected()) {
			c.setAutomaticChunking();
		} else {
			c.setChunkCount((Integer)m_chunkCountSpinner.getValue());
		}
		c.setInactiveMainBranch(m_inactiveMainBranch.isSelected());
		c.saveConfiguration(settings);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings,
			final PortObjectSpec[] specs) throws NotConfigurableException {
		ParallelChunkStartNodeConfiguration c =
			new ParallelChunkStartNodeConfiguration();
		c.loadConfigurationDialog(settings);
		if (c.isAutomaticChunking()) {
			m_automaticChunkingButton.doClick();
		} else {
			m_customChunkingButton.doClick();
			m_chunkCountSpinner.setValue(c.getChunkCount());
		}
		m_inactiveMainBranch.setSelected(c.hasInactiveMainBranch());
	}

}
