/*
 * ------------------------------------------------------------------ *
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
 *   26.02.2008 (thor): created
 */
package org.knime.base.node.meta.feature.backwardelim;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This class is the dialog for the elimination loop's tail node. The user
 * must select the prediction column.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BWElimLoopEndNodeDialog extends NodeDialogPane {
    @SuppressWarnings("unchecked")
    private final ColumnSelectionComboxBox m_targetColumn =
            new ColumnSelectionComboxBox((Border)null, DataValue.class);

    @SuppressWarnings("unchecked")
    private final ColumnSelectionComboxBox m_predictionColumn =
            new ColumnSelectionComboxBox((Border)null, DataValue.class);

    private final BWElimLoopEndSettings m_settings = new BWElimLoopEndSettings();

    /**
     * Creates a new dialog.
     */
    public BWElimLoopEndNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Target column   "), c);
        c.gridx = 1;
        p.add(m_targetColumn, c);

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Prediction column   "), c);
        c.gridx = 1;
        p.add(m_predictionColumn, c);

        addTab("Standard settings", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_targetColumn.update(specs[0], m_settings.targetColumn());
        m_predictionColumn.update(specs[0], m_settings.predictionColumn());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.targetColumn(m_targetColumn.getSelectedColumn());
        m_settings.predictionColumn(m_predictionColumn.getSelectedColumn());
        m_settings.saveSettings(settings);
    }
}
