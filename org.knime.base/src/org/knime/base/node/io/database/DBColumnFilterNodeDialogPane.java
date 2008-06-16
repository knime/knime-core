/*
 * ------------------------------------------------------------------
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
 */
package org.knime.base.node.io.database;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.DatabasePortObjectSpec;
import org.knime.core.node.GenericNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 *
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBColumnFilterNodeDialogPane extends GenericNodeDialogPane {

    private final DialogComponentColumnFilter m_panel;

    private final DBConnectionDialogPanel m_tableOptions =
        new DBConnectionDialogPanel();

    /**
     * Create query dialog with text box to enter table name.
     */
    DBColumnFilterNodeDialogPane() {
        m_panel = new DialogComponentColumnFilter(createColumnFilterModel(), 0);
        super.addTab("Column Filter", m_panel.getComponentPanel());
        super.addTab("Table Options", m_tableOptions);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        DataTableSpec spec =
            ((DatabasePortObjectSpec) specs[0]).getDataTableSpec();
        m_panel.loadSettingsFrom(settings, new DataTableSpec[]{spec});
        m_tableOptions.loadSettingsFrom(settings, new DataTableSpec[]{spec});
    }

    /**
     * @return new settings model for column filter
     */
    static final SettingsModelFilterString createColumnFilterModel() {
        return new SettingsModelFilterString("column_filter");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_panel.saveSettingsTo(settings);
        m_tableOptions.saveSettingsTo(settings);
    }
}
