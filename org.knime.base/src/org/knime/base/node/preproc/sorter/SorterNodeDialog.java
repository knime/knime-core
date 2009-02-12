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
 *   02.02.2005 (cebron): created
 */
package org.knime.base.node.preproc.sorter;

import java.util.ArrayList;
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
                    list = new ArrayList<String>();
                    for (int i = 0; i < alist.length; i++) {
                        if (specs[0].findColumnIndex(alist[i]) >= 0) {
                            list.add(alist[i]);
                        }
                    }
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
        // set the values on the panel
        m_panel.update(specs[SorterNodeModel.INPORT], list, sortOrder,
                NRSORTITEMS, sortinMemory);
    }

    /**
     * Sets the list of columns to include and the sorting order list inside the
     * underlying {@link SorterNodeModel} retrieving them from the
     * {@link SorterNodeDialogPanel}.
     *
     * @param settings the node settings to write into
     *
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        assert (settings != null);
        List<String> inclList = m_panel.getIncludedColumnList();
        settings.addStringArray(SorterNodeModel.INCLUDELIST_KEY, inclList
                .toArray(new String[inclList.size()]));
        settings.addBooleanArray(SorterNodeModel.SORTORDER_KEY, m_panel
                .getSortOrder());
        settings.addBoolean(SorterNodeModel.SORTINMEMORY_KEY, m_panel
                .sortInMemory());
    }
}
