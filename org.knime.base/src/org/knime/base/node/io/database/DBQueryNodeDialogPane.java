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
 */
package org.knime.base.node.io.database;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabasePortObjectSpec;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBQueryNodeDialogPane extends DefaultNodeSettingsPane {
    
    /**
     * Create query dialog with text box to enter table name.
     */
    DBQueryNodeDialogPane() {
        super.addDialogComponent(new DialogComponentMultiLineString(
                createQueryModel(), "SQL query"));
    }
    
    /**
     * Create model to enter SQL statement on input database view.
     * @return a new model to enter SQL statement
     */
    static final SettingsModelString createQueryModel() {
        return new SettingsModelString("SQL_query", "SELECT * FROM "
                + DBQueryNodeModel.TABLE_PLACE_HOLDER);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec) specs[0];
        final DataTableSpec[] dataSpecs; 
        if (dbSpec == null) {
            dataSpecs = new DataTableSpec[]{null};
        } else {
            dataSpecs = new DataTableSpec[]{dbSpec.getDataTableSpec()};
        }
        super.loadAdditionalSettingsFrom(settings, dataSpecs);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);
    }
}
