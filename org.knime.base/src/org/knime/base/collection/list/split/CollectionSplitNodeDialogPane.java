/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   Aug 11, 2008 (wiswedel): created
 */
package org.knime.base.collection.list.split;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.knime.base.collection.list.split.CollectionSplitSettings.CountElementsPolicy;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
final class CollectionSplitNodeDialogPane extends NodeDialogPane {
    
    private final JCheckBox m_replaceChecker;
    private final JCheckBox m_useMostSpecificChecker;
    private final JRadioButton m_bestEffortPolicyButton;
    private final JRadioButton m_countElementsPolicyButton;
    private final JRadioButton m_useElementNamesPolicyButton;
    private final ColumnSelectionComboxBox m_columnSelectionBox;
    
    /**
     * 
     */
    @SuppressWarnings("unchecked")
    public CollectionSplitNodeDialogPane() {
        m_replaceChecker = new JCheckBox("Replace input column");
        m_useMostSpecificChecker = 
            new JCheckBox("Determine most specific type");
        ButtonGroup group = new ButtonGroup();
        m_bestEffortPolicyButton = new JRadioButton("Best effort");
        m_countElementsPolicyButton = new JRadioButton("Count in advance");
        m_useElementNamesPolicyButton = 
            new JRadioButton("Use input table information");
        group.add(m_bestEffortPolicyButton);
        group.add(m_countElementsPolicyButton);
        group.add(m_useElementNamesPolicyButton);
        m_bestEffortPolicyButton.doClick();
        m_columnSelectionBox = new ColumnSelectionComboxBox(
                (Border)null, CollectionDataValue.class);
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel panel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(m_columnSelectionBox, gbc);
        
        gbc.gridy += 1;
        panel.add(m_replaceChecker, gbc);
        
        gbc.gridy += 1;
        panel.add(m_useMostSpecificChecker, gbc);
        
        JPanel p = new JPanel(new GridLayout(0, 1));
        p.setBorder(BorderFactory.createTitledBorder("Element Count Policy"));
        p.add(m_bestEffortPolicyButton);
        p.add(m_useElementNamesPolicyButton);
        p.add(m_countElementsPolicyButton);
        
        gbc.gridy += 1;
        panel.add(p, gbc);
        
        addTab("Settings", panel);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        CollectionSplitSettings set = new CollectionSplitSettings();
        set.loadSettingsInDialog(settings, specs[0]);
        m_replaceChecker.setSelected(set.isReplaceInputColumn());
        m_useMostSpecificChecker.setSelected(
                set.isDetermineMostSpecificDataType());
        m_columnSelectionBox.update(specs[0], set.getCollectionColName());
        switch (set.getCountElementsPolicy()) {
        case BestEffort:
            m_bestEffortPolicyButton.doClick();
            break;
        case Count:
            m_countElementsPolicyButton.doClick();
            break;
        case UseElementNamesOrFail:
            m_useElementNamesPolicyButton.doClick();
            break;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        CollectionSplitSettings set = new CollectionSplitSettings();
        set.setReplaceInputColumn(m_replaceChecker.isSelected());
        set.setDetermineMostSpecificDataType(
                m_useMostSpecificChecker.isSelected());
        set.setCollectionColName(m_columnSelectionBox.getSelectedColumn());
        CountElementsPolicy pol;
        if (m_bestEffortPolicyButton.isSelected()) {
            pol = CountElementsPolicy.BestEffort;
        } else if (m_useElementNamesPolicyButton.isSelected()) {
            pol = CountElementsPolicy.UseElementNamesOrFail;
        } else {
            assert m_countElementsPolicyButton.isSelected();
            pol = CountElementsPolicy.Count;
        }
        set.setCountElementsPolicy(pol);
        set.saveSettingsTo(settings);
    }

}
