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
import org.knime.core.data.NominalValue;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * <code>NodeDialog</code> for the "one-way ANOVA" Node.
 *
 *
 * @author Heiko Hofer
 */
public class OneWayANOVANodeDialog extends NodeDialogPane {
	private final OneWayANOVANodeSettings m_settings;

	private DataColumnSpecFilterPanel m_testColumns;
	private ColumnSelectionPanel m_groupingColumn;
	private JTextField m_confidenceIntervalProb;

    /**
     * New pane for configuring the FoodProcess node.
     */
    protected OneWayANOVANodeDialog() {
    	this.m_settings = new OneWayANOVANodeSettings();
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
        p.add(new JLabel("Grouping column:"), c);
        c.gridx ++;
        c.insets = rightCategoryInsets;
        m_groupingColumn = new ColumnSelectionPanel(NominalValue.class,
                DoubleValue.class);
        m_groupingColumn.setBorder(null);
        p.add(m_groupingColumn, c);

        c.gridx = 0;
        c.gridy++;
        c.insets = leftInsets;
        c.weightx = 0;
        p.add(new JLabel("Confidence Interval (in %):"), c);
        c.gridx ++;
        c.insets = rightInsets;
        m_confidenceIntervalProb = new JTextField("95");
        p.add(m_confidenceIntervalProb, c);

        c.gridx = 0;
        c.gridy++;
        c.insets = leftInsets;
        c.gridwidth = 3;
        c.weighty = 1;
        c.weightx = 1;
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
	    m_settings.setGroupingColumn(m_groupingColumn.getSelectedColumn());

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
        m_groupingColumn.update(spec, m_settings.getGroupingColumn());
        m_confidenceIntervalProb.setText(Double.toString(
                        m_settings.getConfidenceIntervalProb() * 100.0));
	}


}

