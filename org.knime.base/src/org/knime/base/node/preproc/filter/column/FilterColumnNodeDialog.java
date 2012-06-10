/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.preproc.filter.column;

import java.util.HashSet;
import java.util.Set;

import org.knime.base.node.preproc.filter.column.FilterColumnPanel.SelectionOption;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


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
        super.addTab(TAB, new FilterColumnPanel());
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
        String selOptionS = settings.getString(
                FilterColumnNodeModel.CFG_KEY_SELECTIONOPTION, 
                SelectionOption.EnforceExclusion.toString());
        SelectionOption selectionOption = SelectionOption.parse(
                selOptionS, SelectionOption.EnforceExclusion);
        
        // set exclusion list on the panel
        FilterColumnPanel p = (FilterColumnPanel)getTab(TAB);
        p.update(specs[FilterColumnNodeModel.INPORT], selectionOption, list);
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
    protected void saveSettingsTo(final NodeSettingsWO settings) 
        throws InvalidSettingsException {
        FilterColumnPanel p = (FilterColumnPanel)getTab(TAB);
        SelectionOption selOption = p.getSelectionOption();
        Set<String> list;
        switch (selOption) {
        case EnforceExclusion:
            list = p.getExcludedColumnSet();
            break;
        case EnforceInclusion:
            list = p.getIncludedColumnSet();
            break;
        default:
            throw new InvalidSettingsException("No selection option chosen.");
        }
        settings.addString(FilterColumnNodeModel.CFG_KEY_SELECTIONOPTION,
                selOption.toString());
        settings.addStringArray(FilterColumnNodeModel.KEY, 
                list.toArray(new String[0]));
    }
}
