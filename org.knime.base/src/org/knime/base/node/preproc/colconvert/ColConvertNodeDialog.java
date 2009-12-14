/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
