/* 
 * --------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------
 * 
 * History
 *   03.07.2007 (cebron): created
 */
package org.knime.base.node.preproc.colconvert.stringtonumber;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * Dialog for the String to Number Node. Lets the user choose the columns to
 * use.
 * 
 * @author cebron, University of Konstanz
 */
public class StringToNumberNodeDialog extends NodeDialogPane {
    @SuppressWarnings("unchecked")
    private DialogComponentColumnFilter m_filtercomp =
            new DialogComponentColumnFilter(new SettingsModelFilterString(
                    StringToNumberNodeModel.CFG_INCLUDED_COLUMNS), 0,
                    new Class[]{StringValue.class});

    private JTextField m_decimalSeparator = new JTextField(".", 1);

    private JTextField m_thousandsSeparator = new JTextField(",", 1);

    /**
     * Constructor.
     * 
     */
    public StringToNumberNodeDialog() {
        JPanel contentpanel = new JPanel();
        contentpanel.setLayout(new BoxLayout(contentpanel, BoxLayout.Y_AXIS));
        JPanel separatorPanel = new JPanel();
        Border border = BorderFactory.createTitledBorder("Separator settings");
        separatorPanel.setBorder(border);
        separatorPanel
                .setLayout(new BoxLayout(separatorPanel, BoxLayout.X_AXIS));
        separatorPanel.add(new JLabel("Decimal separator: "));
        m_decimalSeparator.setMaximumSize(new Dimension(30, 30));
        separatorPanel.add(m_decimalSeparator);
        separatorPanel.add(Box.createHorizontalStrut(50));
        separatorPanel.add(new JLabel("Thousands separator: "));
        m_thousandsSeparator.setMaximumSize(new Dimension(30, 30));
        separatorPanel.add(m_thousandsSeparator);
        contentpanel.add(separatorPanel);
        contentpanel.add(m_filtercomp.getComponentPanel());
        super.addTab("Settings", contentpanel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_filtercomp.loadSettingsFrom(settings, specs);
        String decimalsep =
                settings.getString(StringToNumberNodeModel.CFG_DECIMALSEP,
                        StringToNumberNodeModel.DEFAULT_DECIMAL_SEPARATOR);
        m_decimalSeparator.setText(decimalsep);
        String thousandssep =
                settings.getString(StringToNumberNodeModel.CFG_THOUSANDSSEP,
                        StringToNumberNodeModel.DEFAULT_THOUSANDS_SEPARATOR);
        m_thousandsSeparator.setText(thousandssep);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_filtercomp.saveSettingsTo(settings);
        settings.addString(StringToNumberNodeModel.CFG_DECIMALSEP,
                m_decimalSeparator.getText());
        settings.addString(StringToNumberNodeModel.CFG_THOUSANDSSEP,
                m_thousandsSeparator.getText());
    }
}
