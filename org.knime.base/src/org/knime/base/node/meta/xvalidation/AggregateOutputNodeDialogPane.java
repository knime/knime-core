/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   Nov 6, 2006 (wiswedel): created
 */
package org.knime.base.node.meta.xvalidation;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This dialog lets the user choose the column containing the predicted class
 * values and the column containing the real class values.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public class AggregateOutputNodeDialogPane extends NodeDialogPane {
    @SuppressWarnings("unchecked")
    private final ColumnSelectionComboxBox m_targetColumn =
            new ColumnSelectionComboxBox((Border)null, NominalValue.class,
                    DoubleValue.class);

    @SuppressWarnings("unchecked")
    private final ColumnSelectionComboxBox m_predictColumn =
            new ColumnSelectionComboxBox((Border)null, NominalValue.class,
                    DoubleValue.class);

    private final AggregateSettings m_settings = new AggregateSettings();

    /**
     * Creates a new dialog.
     */
    public AggregateOutputNodeDialogPane() {
        JPanel p = new JPanel(new GridLayout(0, 2));
        JLabel l1 = new JLabel("Target Column ");
        p.add(getInFlowLayout(l1));
        p.add(getInFlowLayout(m_targetColumn));
        JLabel l2 = new JLabel("Prediction Column ");
        p.add(getInFlowLayout(l2));
        p.add(getInFlowLayout(m_predictColumn));
        addTab("Column Selection", p);
    }

    private static JPanel getInFlowLayout(final JComponent c) {
        JPanel result = new JPanel(new FlowLayout());
        result.add(c);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_settings.targetColumn(m_settings.targetColumn());
        m_settings.predictionColumn(m_settings.predictionColumn());
        m_targetColumn.update(specs[0], m_settings.targetColumn());
        m_predictColumn.update(specs[0], m_settings.predictionColumn());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.targetColumn(m_targetColumn.getSelectedColumn());
        m_settings.predictionColumn(m_predictColumn.getSelectedColumn());
        m_settings.saveSettings(settings);
    }
}
