/* 
 * ---------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   15.06.2007 (cebron): created
 */
package org.knime.base.node.preproc.caseconvert;

import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * Dialog for the {@link CaseConvertNodeModel}. Lets the user choose the
 * columns to use and the convert mode.
 * 
 * @author cebron, University of Konstanz
 */
public class CaseConvertNodeDialog extends NodeDialogPane {

    private JRadioButton m_lowercase;

    private JRadioButton m_uppercase;

    private ColumnFilterPanel m_filterpanel;

    /**
     * Constructor.
     */
    @SuppressWarnings("unchecked")
    public CaseConvertNodeDialog() {
        JPanel contentpanel = new JPanel();
        BoxLayout blayout = new BoxLayout(contentpanel, BoxLayout.Y_AXIS);
        contentpanel.setLayout(blayout);

        

        ButtonGroup buttongroup = new ButtonGroup();
        m_uppercase = new JRadioButton("Convert to UPPERCASE", true);
        m_uppercase.setToolTipText("Convert selected columns to uppercase");
        m_lowercase = new JRadioButton("Convert to lowercase", false);
        m_lowercase.setToolTipText("Convert selected columns to lowercase");
        buttongroup.add(m_uppercase);
        buttongroup.add(m_lowercase);
        JPanel radiopanel = new JPanel();
        blayout = new BoxLayout(radiopanel, BoxLayout.Y_AXIS);
        Border border = BorderFactory.createTitledBorder("Select mode");
        radiopanel.setBorder(border);
        radiopanel.setLayout(blayout);
        radiopanel.add(m_uppercase);
        radiopanel.add(m_lowercase);
        JPanel radiocontent = new JPanel();
        blayout = new BoxLayout(radiocontent, BoxLayout.X_AXIS);
        radiocontent.add(radiopanel);
        radiocontent.add(Box.createHorizontalGlue());
        
        contentpanel.add(radiocontent);        
        contentpanel.add(Box.createVerticalStrut(20));
        m_filterpanel = new ColumnFilterPanel(StringValue.class);
        contentpanel.add(m_filterpanel);
        super.addTab("Options", contentpanel);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        String[] include =
                settings.getStringArray(
                        CaseConvertNodeModel.CFG_INCLUDED_COLUMNS,
                        new String[]{});
        m_filterpanel.update(specs[0], false, include);
        boolean uppercase =
                settings.getBoolean(CaseConvertNodeModel.CFG_UPPERCASE, true);
        if (uppercase) {
            m_uppercase.setSelected(true);
        } else {
            m_lowercase.setSelected(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        Set<String> incl = m_filterpanel.getIncludedColumnSet();
        String[] inclarr = new String[incl.size()];
        inclarr = incl.toArray(inclarr);
        settings.addStringArray(CaseConvertNodeModel.CFG_INCLUDED_COLUMNS,
                inclarr);
        if (m_uppercase.isSelected()) {
            settings.addBoolean(CaseConvertNodeModel.CFG_UPPERCASE, true);
        }
        if (m_lowercase.isSelected()) {
            settings.addBoolean(CaseConvertNodeModel.CFG_UPPERCASE, false);
        }
    }

}
