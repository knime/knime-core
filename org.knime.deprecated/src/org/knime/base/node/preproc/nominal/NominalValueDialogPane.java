/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   12.01.2006 (gabriel): created
 */
package org.knime.base.node.preproc.nominal;

import static org.knime.core.node.util.ColumnFilterPanel.INCLUDED_COLUMNS;

import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * A dialog to selct columns for which nominal values are collected.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class NominalValueDialogPane extends NodeDialogPane {
    /** Column selection panel. */
    private final ColumnFilterPanel m_panel;

    /**
     * Creates a new dialog pane.
     */
    NominalValueDialogPane() {
        super();
        m_panel = new ColumnFilterPanel();
        addTab("Columns", m_panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        if ((specs[0] == null) || (specs[0].getNumColumns() < 1)) {
            throw new NotConfigurableException("Need a DataTableSpec at the"
                    + " input port. Connect node and/or execute predecessor");
        }
        String[] cells = settings.getStringArray(INCLUDED_COLUMNS,
                new String[0]);
        m_panel.update(specs[0], false, cells);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        Set<String> set = m_panel.getIncludedColumnSet();
        settings.addStringArray(INCLUDED_COLUMNS, set.toArray(new String[0]));
    }
}
