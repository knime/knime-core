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
 * --------------------------------------------------------------------- *
 * 
 */
package org.knime.base.node.io.database;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.PortObjectSpec;

/**
 * 
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBReaderConnectionDialogPane extends DBReaderDialogPane {
    
    private final DBConnectionDialogPanel m_panel = 
        new DBConnectionDialogPanel();

    /**
     * Creates a new reader dialog pane with table options used the create
     * SQL output connection.
     */
    DBReaderConnectionDialogPane() {
        super();
        super.addTab("Table Options", m_panel);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        List<DataTableSpec> specList = new ArrayList<DataTableSpec>();
        for (PortObjectSpec spec : specs) {
            if (spec instanceof DataTableSpec) {
                specList.add((DataTableSpec) spec);
            }
        }
        m_panel.loadSettingsFrom(settings, 
                specList.toArray(new DataTableSpec[specList.size()]));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        m_panel.saveSettingsTo(settings);
    }
}
