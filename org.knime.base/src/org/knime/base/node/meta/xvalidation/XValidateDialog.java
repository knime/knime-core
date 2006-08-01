/* Created on Jun 12, 2006 11:03:30 AM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.meta.xvalidation;

import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;


/**
 * This is the simple dialog for the cross validation node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class XValidateDialog extends NodeDialogPane {
    private final XValidateSettings m_settings = new XValidateSettings();
    private final JSpinner m_validations = new JSpinner(
            new SpinnerNumberModel(10, 2, 100, 1));
    private final JCheckBox m_randomSampling = new JCheckBox();
    private final ColumnSelectionComboxBox m_classColumn = 
        new ColumnSelectionComboxBox((Border) null, StringValue.class);
    
    
    /**
     * Creates a new dialog for the cross validation settings.
     */
    public XValidateDialog() {
        super();
        
        JPanel p = new JPanel(new GridLayout(3, 2));
        p.add(new JLabel("Number of validations"));
        p.add(m_validations);
        
        p.add(new JLabel("Random sampling"));
        p.add(m_randomSampling);
        
        p.add(new JLabel("Column with class labels"));
        p.add(m_classColumn);
        p.setSize(400, 90);
        addTab("Standard settings", p);        
    }

    /**
     * @see org.knime.core.node.NodeDialogPane
     *  #loadSettingsFrom(NodeSettingsRO,
     *  org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsFrom(settings);

        m_validations.setValue(m_settings.validations());
        m_randomSampling.setSelected(m_settings.randomSampling());
        if (specs[0] != null) {
            m_classColumn.update(specs[0], m_settings.classColumnName());
        }
    }

    /**
     * @see org.knime.core.node.NodeDialogPane
     *  #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.validations((byte) Math.min(100,
                ((Number) m_validations.getValue()).intValue()));
        m_settings.randomSampling(m_randomSampling.isSelected());
        m_settings.classColumnName(m_classColumn.getSelectedColumn());
        m_settings.saveSettingsTo(settings);
    }
}
