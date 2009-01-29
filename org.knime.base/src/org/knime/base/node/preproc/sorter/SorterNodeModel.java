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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.data.sort.SortedTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class implements the {@link NodeModel} for the sorter node. The input
 * table is segmented into containers that are sorted with guaranteed n*log(n)
 * performance, based on a selection of columns and a corresponding order
 * (ascending/descending). In the end, all sorted containers are merged together
 * and transformed in a output datatable. To compare two datarows, the
 * Comparator compares the {@link org.knime.core.data.DataCell}s with their
 * <code>compareTo</code>-method on each position.
 * 
 * @see org.knime.core.data.container.DataContainer
 * @see java.util.Arrays#sort(java.lang.Object[], int, int,
 *      java.util.Comparator)
 * @author Nicolas Cebron, University of Konstanz
 */
public class SorterNodeModel extends NodeModel {
    /**
     * The input port used here.
     */
    static final int INPORT = 0;

    /**
     * The output port used here.
     */
    static final int OUTPORT = 0;

    /**
     * The key for the IncludeList in the NodeSettings.
     */
    static final String INCLUDELIST_KEY = "incllist";

    /**
     * The key for the Sort Order Array in the NodeSettings.
     */
    static final String SORTORDER_KEY = "sortOrder";
    
    /**
     * The key for the memory-sort flag in the NodeSettings.
     */
    static final String SORTINMEMORY_KEY = "sortinmemory";

    /*
     * List contains the data cells to include.
     */
    private List<String> m_inclList = null;

    /*
     * Array containing information about the sort order for each column. true:
     * ascending false: descending
     */
    private boolean[] m_sortOrder = null;
    
    /*
     * Flag indicating whether to perform the sorting in memory or not.
     */
    private boolean m_sortInMemory = false;

    /**
     * Inits a new <code>SorterNodeModel</code> with one in- and one output.
     * 
     */
    SorterNodeModel() {
        super(1, 1);
    }

    /**
     * When the model gets executed, the {@link org.knime.core.data.DataTable}
     * is split in several {@link org.knime.core.data.container.DataContainer}s.
     * Each one is first removed, then swapped back into memory, gets sorted and
     * is then removed again. At the end, all containers are merged together in
     * one Result-Container. The list of columns that shall be sorted and their
     * corresponding sort order in a boolean array should be set, before
     * executing the model.
     * 
     * @param inData the data table at the input port
     * @param exec the execution monitor
     * @return the sorted data table
     * @throws Exception if the settings (includeList and sortOrder) are not set
     * 
     * @see java.util.Arrays sort(java.lang.Object[], int, int,
     *      java.util.Comparator)
     * @see org.knime.core.node.NodeModel#execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        // If no columns are set, we do not start the sorting process
        if (m_inclList.size() == 0) {
            setWarningMessage("No columns were selected - returning "
                    + "original table");
            return new BufferedDataTable[]{inData[INPORT]};
        }

        // create a sorted table
        SortedTable sortedTable =
                new SortedTable(inData[INPORT], m_inclList, m_sortOrder,
                        m_sortInMemory, exec);

        return new BufferedDataTable[]{sortedTable.getBufferedDataTable()};
    }

    /**
     * Resets all internal data.
     */
    @Override
    public void reset() {

        // do nothing yet
    }

    /**
     * Check if the values of the include list also exist in the
     * {@link DataTableSpec} at the inport. If everything is ok, the v from the
     * inport is translated without modification to the outport.
     * 
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_inclList == null) {
            throw new InvalidSettingsException("No selected columns to sort");
        }
        // check if the values of the include List
        // exist in the DataTableSpec
        for (String ic : m_inclList) {
            if (!ic.equals(SorterNodeDialogPanel2.NOSORT.getName())) {
                if ((inSpecs[INPORT].findColumnIndex(ic) == -1)) {
                    throw new InvalidSettingsException("Column " + ic
                            + " not in spec.");
                }
            }
        }
        return new DataTableSpec[]{inSpecs[INPORT]};
    }

    /**
     * The list of included columns and their sort order are stored in the
     * settings.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_inclList != null) {
            
            settings.addStringArray(INCLUDELIST_KEY, m_inclList
                    .toArray(new String[0]));
        }
        if (!(m_sortOrder == null)) {
            settings.addBooleanArray(SORTORDER_KEY, m_sortOrder);
        }
        settings.addBoolean(SORTINMEMORY_KEY, m_sortInMemory);
    }

    /**
     * Valid settings should contain the list of columns and a corresponding
     * sort order array of same size.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        String[] inclList = settings.getStringArray(INCLUDELIST_KEY);
        if (inclList == null) {
            throw new InvalidSettingsException("No column selected.");
        }
        
        // scan fur duplicate entries in include list
        for (int i = 0; i < inclList.length; i++) {
            String entry = inclList[i];
            for (int j = i + 1; j < inclList.length; j++) {
                if (entry.equals(inclList[j])) {
                    throw new InvalidSettingsException("Duplicate column '"
                            + entry + "' at positions " + i + " and " + j);
                }
            }
        }

        boolean[] sortorder = settings.getBooleanArray(SORTORDER_KEY);
        if (sortorder == null) {
            throw new InvalidSettingsException("No sort order specified.");
        }
    }

    /**
     * Load the settings (includelist and sort order) in the SorterNodeModel.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        m_inclList = new ArrayList<String>();
        String[] inclList = settings.getStringArray(INCLUDELIST_KEY);
        for (int i = 0; i < inclList.length; i++) {
            m_inclList.add(inclList[i]);
        }
        m_sortOrder = settings.getBooleanArray(SORTORDER_KEY);
        if (settings.containsKey(SORTINMEMORY_KEY)) {
            m_sortInMemory = settings.getBoolean(SORTINMEMORY_KEY);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
}
