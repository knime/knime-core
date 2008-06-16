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
 *   15.06.2007 (cebron): created
 */
package org.knime.base.node.preproc.colconvert;

import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * Dialog for the {@link ColConvertNodeModel}.
 * Lets the user choose the columns to use and the convert mode.
 * 
 * @author cebron, University of Konstanz
 */
public class ColConvertNodeDialog extends NodeDialogPane {
    
    
    private JRadioButton m_stringToNumber;

    private JRadioButton m_numberToString;

    private ColumnFilterPanel m_filterpanel;

    /**
     * Constructor, Dialog initialization.
     */
    @SuppressWarnings("unchecked")
    public ColConvertNodeDialog() {
        JPanel contentpanel = new JPanel();
        BoxLayout blayout = new BoxLayout(contentpanel, BoxLayout.Y_AXIS);
        contentpanel.setLayout(blayout);

        ButtonGroup buttongroup = new ButtonGroup();
        m_numberToString = new JRadioButton("Number -> String", true);
        m_numberToString
                .setToolTipText("Convert selected columns from " 
                       + "DoubleValue to StringValue");
        m_stringToNumber = new JRadioButton("String -> Number", false);
        m_stringToNumber
                .setToolTipText("Convert selected columns from" 
                       + " StringValue to DoubleValue");
        buttongroup.add(m_stringToNumber);
        buttongroup.add(m_numberToString);
        JPanel radiopanel = new JPanel();
        blayout = new BoxLayout(radiopanel, BoxLayout.Y_AXIS);
        Border border = BorderFactory.createTitledBorder("Select mode");
        radiopanel.setBorder(border);
        radiopanel.setLayout(blayout);
        radiopanel.add(m_stringToNumber);
        radiopanel.add(m_numberToString);
        
        JPanel radiocontent = new JPanel();
        blayout = new BoxLayout(radiocontent, BoxLayout.X_AXIS);
        radiocontent.add(radiopanel);
        radiocontent.add(Box.createHorizontalGlue());
        
        contentpanel.add(radiocontent);
        contentpanel.add(Box.createVerticalStrut(20));
        m_filterpanel =
            new ColumnFilterPanel(new Class[]{StringValue.class,
                    DoubleValue.class});
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
                        ColConvertNodeModel.CFG_INCLUDED_COLUMNS,
                        new String[]{});
        m_filterpanel.update(specs[0], false, include);
        boolean stringtodouble =
                settings.getBoolean(ColConvertNodeModel.CFG_STRINGTODOUBLE,
                        true);
        if (stringtodouble) {
            m_stringToNumber.setSelected(true);
        } else {
            m_numberToString.setSelected(true);
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
        settings.addStringArray(ColConvertNodeModel.CFG_INCLUDED_COLUMNS,
                inclarr);
        if (m_stringToNumber.isSelected()) {
            settings.addBoolean(ColConvertNodeModel.CFG_STRINGTODOUBLE, true);
        }
        if (m_numberToString.isSelected()) {
            settings.addBoolean(ColConvertNodeModel.CFG_STRINGTODOUBLE, false);
        }
    }
}
