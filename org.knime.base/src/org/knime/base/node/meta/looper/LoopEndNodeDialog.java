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
 *   15.05.2009 (meinl): created
 */
package org.knime.base.node.meta.looper;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * This is the dialog for the loop end node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopEndNodeDialog extends NodeDialogPane {
    private final JCheckBox m_addIterationColumn = new JCheckBox("Add iteration column");

    private final LoopEndNodeSettings m_settings = new LoopEndNodeSettings();

    /**
     * Creates a new dialog.
     */
    public LoopEndNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        p.add(m_addIterationColumn, c);
        addTab("Standard settings", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.addIterationColumn(m_addIterationColumn.isSelected());
        m_settings.saveSettings(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettings(settings);
        m_addIterationColumn.setSelected(m_settings.addIterationColumn());
    }
}
