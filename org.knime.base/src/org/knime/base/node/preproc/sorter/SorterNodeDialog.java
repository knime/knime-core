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
 * -------------------------------------------------------------------
 *
 * History
 *   02.02.2005 (cebron): created
 */
package org.knime.base.node.preproc.sorter;

import java.util.Arrays;
import java.util.List;

import javax.swing.JScrollPane;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * Dialog for choosing the columns that will be sorted. It is also possible to
 * set the order of columns
 *
 * @author Nicolas Cebron, University of Konstanz
 */
public class SorterNodeDialog extends NodeDialogPane {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(SorterNodeDialog.class);

    /**
     * The tab's name.
     */
    private static final String TAB = "Sorting Filter";

    /*
     * Hold the Panel
     */
    private final SorterNodeDialogPanel2 m_panel;

    /*
     * The initial number of SortItems that the SorterNodeDialogPanel should
     * show.
     */
    private static final int NRSORTITEMS = 3;

    /**
     * Creates a new {@link NodeDialogPane} for the Sorter Node in order to
     * choose the desired columns and the sorting order (ascending/ descending).
     */
    SorterNodeDialog() {
        super();
        m_panel = new SorterNodeDialogPanel2();
        super.addTab(TAB, new JScrollPane(m_panel));
    }

    /**
     * Calls the update method of the underlying update method of the
     * {@link SorterNodeDialogPanel} using the input data table spec from this
     * {@link SorterNodeModel}.
     *
     * @param settings the node settings to read from
     * @param specs the input specs
     *
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     * @throws NotConfigurableException if the dialog can not be opened.
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        if (specs.length == 0 || specs[0] == null
                || specs[0].getNumColumns() == 0) {
            throw new NotConfigurableException("No columns to sort.");
        }
        List<String> list = null;
        boolean[] sortOrder = null;
        boolean sortinMemory = false;

        if (settings.containsKey(SorterNodeModel.INCLUDELIST_KEY)) {
            try {
                String[] alist =
                    settings.getStringArray(SorterNodeModel.INCLUDELIST_KEY);
                if (alist != null) {
                    list = Arrays.asList(alist);
                }
            } catch (InvalidSettingsException ise) {
                LOGGER.error(ise.getMessage());
            }
        }

        if (settings.containsKey(SorterNodeModel.SORTORDER_KEY)) {
            try {
                sortOrder = settings
                        .getBooleanArray(SorterNodeModel.SORTORDER_KEY);
            } catch (InvalidSettingsException ise) {
                LOGGER.error(ise.getMessage());
            }
        }
        if (list != null) {
            if (list.size() == 0 || list.size() != sortOrder.length) {
                list = null;
                sortOrder = null;
            }
        }

        if (settings.containsKey(SorterNodeModel.SORTINMEMORY_KEY)) {
            try {
                sortinMemory = settings
                        .getBoolean(SorterNodeModel.SORTINMEMORY_KEY);
            } catch (InvalidSettingsException ise) {
                LOGGER.error(ise.getMessage());
            }
        }
        boolean sortMissingToEnd = settings.getBoolean(
                SorterNodeModel.MISSING_TO_END_KEY, false);
        // set the values on the panel
        m_panel.update(specs[SorterNodeModel.INPORT], list, sortOrder,
                NRSORTITEMS, sortinMemory, sortMissingToEnd);
    }

    /**
     * Sets the list of columns to include and the sorting order list inside the
     * underlying {@link SorterNodeModel} retrieving them from the
     * {@link SorterNodeDialogPanel}.
     *
     * @param settings the node settings to write into
     * @throws InvalidSettingsException if settings are not valid
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        m_panel.checkValid();
        List<String> inclList = m_panel.getIncludedColumnList();
        settings.addStringArray(SorterNodeModel.INCLUDELIST_KEY, inclList
                .toArray(new String[inclList.size()]));
        settings.addBooleanArray(SorterNodeModel.SORTORDER_KEY, m_panel
                .getSortOrder());
        settings.addBoolean(SorterNodeModel.SORTINMEMORY_KEY, m_panel
                .sortInMemory());
        settings.addBoolean(SorterNodeModel.MISSING_TO_END_KEY,
                m_panel.isSortMissingToEnd());
    }
}
