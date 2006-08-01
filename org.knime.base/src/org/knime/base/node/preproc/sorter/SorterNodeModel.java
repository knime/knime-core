/* 
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
 * History
 *   02.02.2005 (cebron): created
 */
package org.knime.base.node.preproc.sorter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataContainer;
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
 * Comparator compares the {@link org.knime.core.data.DataCell}s with
 * their <code>compareTo</code>-method on each position.
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

    /*
     * List contains the data cells to include.
     */
    private List<String> m_inclList = null;

    /*
     * Number of rows for each container
     */
    private static final int CONTAINERSIZE = 1000;

    /*
     * Number of Rows
     */
    private int m_nrRows = 0;

    /*
     * Array containing information about the sort order for each column. true:
     * ascending false: descending
     */
    private boolean[] m_sortOrder = null;

    /*
     * Array containing indices of the columns that will be sorted
     */
    private int[] m_indices;

    /*
     * The DataTableSpec
     */
    private DataTableSpec m_spec;

    /*
     * The RowComparator to compare two DataRows (inner class)
     */
    private final RowComparator m_rowComparator = new RowComparator();

    /**
     * Inits a new <code>SorterNodeModel</code> with one in- and one output.
     * 
     */
    SorterNodeModel() {
        super(1, 1);
    }

    /**
     * When the model gets executed, the {@link DataTable} is split in several
     * {@link DataContainer}s. Each one is first removed, then swapped back
     * into memory, gets sorted and is then removed again. At the end, all
     * containers are merged together in one Result-Container. The list of
     * columns that shall be sorted and their corresponding sort order in a
     * boolean array should be set, before executing the model.
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
        m_spec = inData[INPORT].getDataTableSpec();
        // get the column indices of the columns that will be sorted
        // also make sure that m_inclList and m_sortOrder both exist
        if (m_inclList == null) {
            throw new Exception("List of colums to include (incllist) is "
                    + "not set in the model");
        } else {
            m_indices = new int[m_inclList.size()];
        }
        if (m_sortOrder == null) {
            throw new Exception("Sortorder array is " + "not set in the model");
        }
        // If no columns are set, we do not start the sorting process
        if (m_inclList.size() == 0) {
            setWarningMessage("No columns were selected - returning "
                    + " original table");
            return new BufferedDataTable[]{inData[INPORT]};
        } else {
            Vector<DataContainer> containerVector = new Vector<DataContainer>();
            int pos = -1;
            for (int i = 0; i < m_inclList.size(); i++) {
                String dc = m_inclList.get(i);
                pos = m_spec.findColumnIndex(dc);
                if (pos == -1) {
                    throw new Exception("Could not find column name:"
                            + dc.toString());
                }
                m_indices[i] = pos;
            }
            // Initialize RowIterator
            RowIterator rowIt = inData[INPORT].iterator();
            int nrRows = inData[0].getRowCount();
            m_nrRows = 0;
            // wrap all DataRows in Containers of size containerSize
            // sort each container before it is'stored'.
            BufferedDataContainer newContainer = exec.createDataContainer(
                    m_spec, false);
            int nrRowsinContainer = 0;
            ArrayList<DataRow> containerrowlist = new ArrayList<DataRow>();
            ExecutionMonitor subexec = exec.createSubProgress(.5);
            while (rowIt.hasNext()) {
                subexec.setProgress((double)m_nrRows / (double)nrRows,
                        "Reading in data... ");
                exec.checkCanceled();
                if (newContainer.isClosed()) {
                    newContainer = exec.createDataContainer(m_spec, false);
                    nrRowsinContainer = 0;
                    containerrowlist = new ArrayList<DataRow>();
                }
                DataRow row = rowIt.next();
                m_nrRows++;
                nrRowsinContainer++;
                containerrowlist.add(row);
                if (nrRowsinContainer == CONTAINERSIZE) {
                    if (exec != null) {
                        exec.checkCanceled();
                    }
                    // sort list
                    DataRow[] temparray = new DataRow[containerrowlist.size()];
                    temparray = containerrowlist.toArray(temparray);
                    subexec.setMessage("Presorting Container");
                    Arrays
                            .sort(temparray, 0, temparray.length,
                                    m_rowComparator);
                    // write in container
                    for (int i = 0; i < temparray.length; i++) {
                        newContainer.addRowToTable(temparray[i]);
                    }
                    newContainer.close();
                    containerVector.add(newContainer);
                }
            }
            if (nrRowsinContainer % CONTAINERSIZE != 0) {
                if (exec != null) {
                    exec.checkCanceled();
                }
                // sort list
                DataRow[] temparray = new DataRow[containerrowlist.size()];
                temparray = containerrowlist.toArray(temparray);
                Arrays.sort(temparray, 0, temparray.length, m_rowComparator);
                // write in container
                for (int i = 0; i < temparray.length; i++) {
                    newContainer.addRowToTable(temparray[i]);
                }
                newContainer.close();
                containerVector.add(newContainer);
            }

            // merge all sorted containers together
            BufferedDataContainer mergeContainer = exec.createDataContainer(
                    m_spec, false);

            // an array of RowIterators gives access to all (sorted) containers
            RowIterator[] currentRowIterators = new RowIterator[containerVector
                    .size()];
            DataRow[] currentRowValues = new DataRow[containerVector.size()];

            // Initialize both arrays
            for (int c = 0; c < containerVector.size(); c++) {
                DataContainer tempContainer = containerVector.get(c);
                DataTable tempTable = tempContainer.getTable();
                currentRowIterators[c] = tempTable.iterator();
            }
            for (int c = 0; c < containerVector.size(); c++) {
                currentRowValues[c] = currentRowIterators[c].next();
            }
            int position = -1;

            // find the smallest/biggest element of all, put it in
            // mergeContainer
            ExecutionMonitor subexec2 = exec.createSubProgress(.5);
            for (int i = 0; i < m_nrRows; i++) {
                subexec2.setProgress((double)i / (double)m_nrRows, "Merging");
                exec.checkCanceled();
                position = findNext(currentRowValues);
                mergeContainer.addRowToTable(currentRowValues[position]);
                if (currentRowIterators[position].hasNext()) {
                    currentRowValues[position] = currentRowIterators[position]
                            .next();
                } else {
                    currentRowIterators[position] = null;
                    currentRowValues[position] = null;
                }
            }
            // Everything should be written out in the MergeContainer
            for (int i = 0; i < currentRowIterators.length; i++) {
                assert (currentRowValues[i] == null);
                assert (currentRowIterators[i] == null);
            }
            mergeContainer.close();
            BufferedDataTable dt = mergeContainer.getTable();
            assert (dt != null);
            return new BufferedDataTable[]{dt};
        }
    }

    /**
     * Resets all internal data.
     */
    @Override
    public void reset() {
        m_nrRows = 0;
    }

    /**
     * Check if the values of the include list also exist in the
     * {@link DataTableSpec} at the inport. If everything is ok, the v from the
     * inport is translated without modification to the outport.
     * 
     * @see NodeModel#configure(DataTableSpec[])
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
            if ((inSpecs[INPORT].findColumnIndex(ic) == -1)) {
                throw new InvalidSettingsException("Column " + ic
                        + " not in spec.");
            }
        }
        return new DataTableSpec[]{inSpecs[INPORT]};
    }

    /*
     * This method finds the next DataRow (position) that should be inserted in
     * the MergeContainer
     */
    private int findNext(final DataRow[] currentValues) {
        int min = 0;
        while (currentValues[min] == null) {
            min++;
        }

        for (int i = min + 1; i < currentValues.length; i++) {
            if (currentValues[i] != null) {
                if (m_rowComparator.compare(currentValues[i],
                        currentValues[min]) < 0) {
                    min = i;
                }
            }
        }
        return min;
    }

    /**
     * The list of included columns and their sort order are stored in the
     * settings.
     * 
     * @see org.knime.core.node.NodeModel#saveSettingsTo(NodeSettingsWO)
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
    }

    /**
     * Valid settings should contain the list of columns and a corresponding
     * sort order array of same size.
     * 
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        String[] inclList = settings.getStringArray(INCLUDELIST_KEY);
        if (inclList == null) {
            throw new InvalidSettingsException("StringArray " + INCLUDELIST_KEY
                    + " is null");
        }

        boolean[] sortorder = settings.getBooleanArray(SORTORDER_KEY);
        if (sortorder == null) {
            throw new InvalidSettingsException("Boolean array " + SORTORDER_KEY
                    + " is null");
        }
    }

    /**
     * Load the settings (includelist and sort order) in the SorterNodeModel.
     * 
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
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
    }

    /**
     * The private class RowComparator is used to compare two DataRows. It
     * implements the Comparator-interface, so we can use the Arrays.sort method
     * to sort an array of DataRows. If both DataRows are null they are
     * considered as equal. A null DataRow is considered as 'less than' an
     * initialized DataRow. On each position, the DataCells of the two DataRows
     * are compared with their compareTo-method.
     * 
     * @author Nicolas Cebron, University of Konstanz
     */
    private class RowComparator implements Comparator<DataRow> {

        /**
         * This method compares two DataRows based on a comparison for each
         * DataCell and the sorting order (m_sortOrder) for each column.
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(final DataRow dr1, final DataRow dr2) {

            if (dr1 == dr2) {
                return 0;
            }
            if (dr1 == null) {
                return 1;
            }
            if (dr2 == null) {
                return -1;
            }

            assert (dr1.getNumCells() == dr2.getNumCells());

            for (int i = 0; i < m_indices.length; i++) {
                // only if the cell is in the includeList
                // same column means that they have the same type
                DataValueComparator comp = m_spec.getColumnSpec(m_indices[i])
                        .getType().getComparator();
                int cellComparison = comp.compare(dr1.getCell(m_indices[i]),
                        dr2.getCell(m_indices[i]));

                if (cellComparison != 0) {
                    return (m_sortOrder[i] ? cellComparison : -cellComparison);
                }
            }
            return 0; // all cells in the DataRow have the same value
        }
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
}
