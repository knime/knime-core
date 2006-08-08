/* Created on Aug 7, 2006 8:14:40 AM by thor
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
package org.knime.base.node.meta.looper;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * This class is the dialog for the looper node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class LooperDialog extends NodeDialogPane {
    private final JSpinner m_loops = new JSpinner(new SpinnerNumberModel(10, 1,
            Integer.MAX_VALUE, 1));

    private final LooperSettings m_settings = new LooperSettings();

    /**
     * Creates a new dialog for the looper node.
     */
    public LooperDialog() {
        JPanel p = new JPanel(new GridLayout(3, 2));
        p.add(new JLabel("Number of loops"));
        p.add(m_loops);

        p.setSize(400, 60);
        addTab("Standard settings", p);
    }

    /**
     * @see org.knime.core.node.NodeDialogPane
     *      #loadSettingsFrom(org.knime.core.node.NodeSettingsRO,
     *      org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsFrom(settings);
        m_loops.setValue(m_settings.loops());
    }

    /**
     * @see org.knime.core.node.NodeDialogPane
     *      #saveSettingsTo(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.loops((Integer)m_loops.getValue());
        m_settings.saveSettingsTo(settings);
    }
}
