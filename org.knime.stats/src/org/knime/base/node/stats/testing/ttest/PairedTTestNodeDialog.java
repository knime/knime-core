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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.stats.testing.ttest;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnPairsSelectionPanel;

/**
 * <code>NodeDialog</code> for the "Paired T-Test" Node.
 *
 *
 * @author Heiko Hofer
 */
public class PairedTTestNodeDialog extends NodeDialogPane {
	private final PairedTTestNodeSettings m_settings;
	private ColumnPairsSelectionPanel m_columnPairs;

	private JTextField m_confidenceIntervalProb;

    /**
     * New pane for configuring the FoodProcess node.
     */
    protected PairedTTestNodeDialog() {
    	this.m_settings = new PairedTTestNodeSettings();
		addTab("Settings", createPanel());

    }

    /** Create the configuration panel. */
    @SuppressWarnings({"unchecked", "serial"})
	private Component createPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        Insets leftInsets = new Insets(3, 8, 3, 8);
        Insets rightInsets = new Insets(3, 0, 3, 8);
        Insets leftCategoryInsets = new Insets(11, 8, 3, 8);

        c.gridx = 0;
        c.insets = leftCategoryInsets;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 1;
        m_columnPairs = new ColumnPairsSelectionPanel() {
            @Override
            protected void initComboBox(final DataTableSpec spec,
                    final JComboBox comboBox,
                    final String selected) {
                DefaultComboBoxModel comboBoxModel =
                    (DefaultComboBoxModel)comboBox.getModel();
                comboBoxModel.removeAllElements();
                for (DataColumnSpec colSpec : spec) {
                    if (colSpec.getType().isCompatible(DoubleValue.class)) {
                        comboBoxModel.addElement(colSpec);
                        if (null != selected
                                && colSpec.getName().equals(selected)) {
                            comboBoxModel.setSelectedItem(colSpec);
                        }
                    }
                }
            }
        };
        JScrollPane scrollPane = new JScrollPane(m_columnPairs);
        m_columnPairs.setBackground(Color.white);
        Component header = m_columnPairs.getHeaderView("Left Column",
                "Right Column");
        header.setPreferredSize(new Dimension(300, 20));
        scrollPane.setColumnHeaderView(header);

        scrollPane.setPreferredSize(new Dimension(300, 200));
        scrollPane.setMinimumSize(new Dimension(300, 100));

        p.add(scrollPane, c);
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        c.gridx = 0;
        c.gridy++;
        c.insets = leftInsets;
        p.add(new JLabel("Confidence Interval (in %):"), c);
        c.gridx ++;
        c.insets = rightInsets;
        m_confidenceIntervalProb = new JTextField("95");
        p.add(m_confidenceIntervalProb, c);


		return p;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void saveSettingsTo( final NodeSettingsWO s ) {

	    String confIntv = m_confidenceIntervalProb.getText();
	    confIntv = confIntv.replace('%', ' ').trim();
	    try {
    	    double confIntvProp = Double.valueOf(confIntv) / 100.0;
    	    m_settings.setConfidenceIntervalProb(confIntvProp);
	    } catch (NumberFormatException e) {
	        throw new NumberFormatException("Cannot parse the value of"
	                + " \"Confidence Interval (in %)\"."
	                + " Please enter a number.");
	    }
        Object[] lr = m_columnPairs.getLeftSelectedItems();
        String[] ls = new String[lr.length];
        for (int i = 0; i < lr.length; i++) {
            ls[i] = ((DataColumnSpec)lr[i]).getName();
        }
        m_settings.setLeftColumns(ls);

        Object[] rr = m_columnPairs.getRightSelectedItems();
        String[] rs = new String[rr.length];
        for (int i = 0; i < rr.length; i++) {
            rs[i] = ((DataColumnSpec)rr[i]).getName();
        }
        m_settings.setRightColumns(rs);

    	m_settings.saveSettings(s);
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
	        final DataTableSpec[] specs) throws NotConfigurableException {
	    DataTableSpec spec = specs[0];
	    m_settings.loadSettingsForDialog(settings);
	    m_columnPairs.updateData(new DataTableSpec[]{spec, spec},
	            m_settings.getLeftColumns(),
                m_settings.getRightColumns());
        m_confidenceIntervalProb.setText(Double.toString(
                        m_settings.getConfidenceIntervalProb() * 100.0));
	}


}

