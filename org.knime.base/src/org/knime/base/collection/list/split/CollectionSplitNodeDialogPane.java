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
