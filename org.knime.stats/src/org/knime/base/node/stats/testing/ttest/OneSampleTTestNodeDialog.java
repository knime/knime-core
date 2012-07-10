package org.knime.base.node.stats.testing.ttest;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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
        c.insets = rightCategoryInsets;
        m_testValue = new JTextField();
        p.add(m_testValue, c);

        c.gridx = 0;
        c.gridy++;
        c.insets = leftInsets;
        p.add(new JLabel("Confidence Interval (in %):"), c);
        c.gridx ++;
        c.insets = rightInsets;
        m_confidenceIntervalProb = new JTextField("75");
        p.add(m_confidenceIntervalProb, c);

        c.gridx = 0;
        c.gridy++;
        c.insets = leftInsets;
        c.gridwidth = 2;
        c.weighty = 1;
        m_testColumns = new DataColumnSpecFilterPanel(true, DoubleValue.class);
        p.add(m_testColumns, c);

//        c.gridx = 0;
//        c.insets = new Insets(0, 0, 0, 0);
//        c.gridwidth = 2;
//        c.weightx = 1;
//        c.weighty = 1;
//        p.add(new JPanel(), c);

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

