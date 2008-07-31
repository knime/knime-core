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
 */
package org.knime.base.node.preproc.filter.column;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;


/**
 * This is the dialog for the column filter. The user can specify which columns
 * should be excluded in the output table.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Thomas Gabriel, University of Konstanz
 */
final class FilterColumnNodeDialog extends NodeDialogPane {

    /*
     * The tab's name.
     */
    private static final String TAB = "Column Filter";

    /**
     * Creates a new {@link NodeDialogPane} for the column filter in order to
     * set the desired columns.
     */
    FilterColumnNodeDialog() {
        super();
        super.addTab(TAB, new ColumnFilterPanel());
    }

    /**
     * Calls the update method of the underlying filter panel using the input
     * data table spec from this {@link FilterColumnNodeModel}.
     * 
     * @param settings the node settings to read from
     * @param specs the input specs
     * @throws NotConfigurableException if no columns are available for
     *             filtering
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        assert (settings != null && specs.length == 1);
        if (specs[FilterColumnNodeModel.INPORT] == null
                || specs[FilterColumnNodeModel.INPORT].getNumColumns() == 0) {
            throw new NotConfigurableException("No columns available for "
                    + "selection.");
        }
        String[] columns = settings.getStringArray(FilterColumnNodeModel.KEY,
                new String[0]);
        HashSet<String> list = new HashSet<String>();
        for (int i = 0; i < columns.length; i++) {
            if (specs[FilterColumnNodeModel.INPORT].containsName(columns[i])) {
                list.add(columns[i]);
            }
        }
        // set exclusion list on the panel
        ColumnFilterPanel p = (ColumnFilterPanel)getTab(TAB);
        p.update(specs[FilterColumnNodeModel.INPORT], true, list);
    }

    /**
     * Sets the list of columns to exclude inside the underlying
     * {@link FilterColumnNodeModel} retrieving them from the filter panel.
     * 
     * @param settings the node settings to write into
     * 
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        ColumnFilterPanel p = (ColumnFilterPanel)getTab(TAB);
        Set<String> list = p.getExcludedColumnSet();
        settings.addStringArray(FilterColumnNodeModel.KEY, list
                .toArray(new String[0]));
    }
}
