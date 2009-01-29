/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   10.11.2006 (berthold): created
 */
package org.knime.base.node.mine.knn;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This class represens the dialog for the kNN node.
 * 
 * @author Michael Berthold, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public class KnnNodeDialogPane extends NodeDialogPane {
    private final ColumnSelectionComboxBox m_classColumn =
            new ColumnSelectionComboxBox((Border)null, NominalValue.class);

    private final JSpinner m_k =
            new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));

    private final JCheckBox m_weightByDistance = new JCheckBox();
    
    private KnnSettings m_settings = new KnnSettings();

    /**
     * Creates a new dialog pane for the kNN node.
     */
    public KnnNodeDialogPane() {
        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);

        p.add(new JLabel("Column with class labels   "), c);
        c.gridx = 1;
        p.add(m_classColumn, c);

        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Number of neighbours to consider (k)   "), c);
        c.gridx = 1;
        p.add(m_k, c);
        
        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Weight neighbours by distance   "), c);
        c.gridx = 1;
        p.add(m_weightByDistance, c);

        addTab("Standard settings", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        try {
            m_settings.loadSettings(settings);
        } catch (InvalidSettingsException ex) {
            // ignore it an use the defaults
        }

        m_classColumn.update(specs[0], m_settings.classColumn());
        m_k.setValue(m_settings.k());
        m_weightByDistance.setSelected(m_settings.weightByDistance());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.classColumn(m_classColumn.getSelectedColumn());
        m_settings.k(((Number)m_k.getValue()).intValue());
        m_settings.weightByDistance(m_weightByDistance.isSelected());
        m_settings.saveSettings(settings);
    }
}
