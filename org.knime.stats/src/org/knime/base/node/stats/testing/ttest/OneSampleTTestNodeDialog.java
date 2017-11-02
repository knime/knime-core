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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.stats.testing.ttest;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * <code>NodeDialog</code> for the "One-Sample T-Test" Node.
 *
 *
 * @author Heiko Hofer
 */
public class OneSampleTTestNodeDialog extends NodeDialogPane {
	private final OneSampleTTestNodeSettings m_settings;

	private DataColumnSpecFilterPanel m_testColumns;
	private JTextField m_testValue;
	private JTextField m_confidenceIntervalProb;

    /**
     * New pane for configuring the FoodProcess node.
     */
    protected OneSampleTTestNodeDialog() {
    	this.m_settings = new OneSampleTTestNodeSettings();
		addTab("Settings", createPanel());

    }

    /** Create the configuration panel. */
    @SuppressWarnings("unchecked")
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
        Insets rightCategoryInsets = new Insets(11, 0, 3, 8);

        c.gridx = 0;
        c.insets = leftCategoryInsets;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        p.add(new JLabel("Test value:"), c);
        c.gridx ++;
        c.weightx = 0.5;
        c.insets = rightCategoryInsets;
        m_testValue = new JTextField();
        p.add(m_testValue, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.insets = leftInsets;
        p.add(new JLabel("Confidence Interval (in %):"), c);
        c.gridx ++;
        c.insets = rightInsets;
        m_confidenceIntervalProb = new JTextField("95");
        p.add(m_confidenceIntervalProb, c);

        c.gridx = 0;
        c.gridy++;
        c.insets = leftInsets;
        c.gridwidth = 3;
        c.weightx = 1;
        c.weighty = 1;
        m_testColumns = new DataColumnSpecFilterPanel(DoubleValue.class);
        m_testColumns.setBorder(
                BorderFactory.createTitledBorder("Test columns"));
        p.add(m_testColumns, c);

        // dummy panel to make the controls smaller above the last component
        // (the DataColumnSpecFilterPanel).
        c.gridheight = c.gridy;
        c.gridx = 2;
        c.gridy = 0;
        c.weighty = 1;
        c.weightx = 1;
        c.gridwidth = 1;
        p.add(new JPanel(), c);

		return p;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void saveSettingsTo( final NodeSettingsWO s ) {
	    m_testColumns.saveConfiguration(m_settings.getTestColumns());
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
        try {
            double testValue = Double.valueOf(m_testValue.getText());
            m_settings.setTestValue(testValue);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Cannot parse the value of"
                    + " \"Test value\"."
                    + " Please enter a number.");
        }

    	m_settings.saveSettings(s);
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
	        final DataTableSpec[] specs) throws NotConfigurableException {
	    DataTableSpec spec = specs[0];
	    m_settings.loadSettingsForDialog(settings, spec);

	    m_testColumns.loadConfiguration(m_settings.getTestColumns(), spec);
        m_testValue.setText(Double.toString(m_settings.getTestValue()));
        m_confidenceIntervalProb.setText(Double.toString(
                        m_settings.getConfidenceIntervalProb() * 100.0));
	}


}

