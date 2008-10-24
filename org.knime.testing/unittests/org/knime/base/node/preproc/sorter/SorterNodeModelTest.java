/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   11.02.2005 (cebron): created
 */
package org.knime.base.node.preproc.sorter;

import java.util.HashMap;

import junit.framework.TestCase;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.SingleNodeContainer;

/**
 * Test class for the SorterNodeModel.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class SorterNodeModelTest extends TestCase {
    private static final ExecutionContext EXEC_CONTEXT = new ExecutionContext(
            new DefaultNodeProgressMonitor(), new Node(new SorterNodeFactory()),
                SingleNodeContainer.MemoryPolicy.CacheSmallInMemory,
                new HashMap<Integer, ContainerTable>());

    private SorterNodeModel m_snm;

    private NodeSettings m_settings;

    /**
     * Initialisation of the instance variables.
     * 
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() {
        m_snm = new SorterNodeModel();
        m_settings = new NodeSettings("Sorter");

    }

    /**
     * Clean up.
     * 
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() {
        m_snm = null;
        m_settings = null;
    }

    /**
     * test the SaveSettingsTo method of the <code>SorterNodeModel</code>.
     * 
     */
    public void testSaveSettingsTo() {

        // save to empty includelist and sortorder
        m_snm.saveSettingsTo(m_settings);
        try {
            assertNull(m_settings
                    .getStringArray(SorterNodeModel.INCLUDELIST_KEY));
        } catch (InvalidSettingsException e) {
            // e.printStackTrace();
        }
        try {
            assertNull(m_settings
                    .getBooleanArray(SorterNodeModel.SORTORDER_KEY));
        } catch (InvalidSettingsException e) {
            // e.printStackTrace();
        }

        // load testsettings in the model and save them
        boolean[] testboolarray = {true, false};
        m_settings
                .addBooleanArray(SorterNodeModel.SORTORDER_KEY, testboolarray);
        DataCell[] dcarray = {new DoubleCell(3), new StringCell("Test")};
        m_settings.addDataCellArray(SorterNodeModel.INCLUDELIST_KEY, dcarray);

        try {
            m_snm.validateSettings(m_settings);
        } catch (InvalidSettingsException e) {
            e.printStackTrace();
        }
        try {
            m_snm.loadValidatedSettingsFrom(m_settings);
        } catch (InvalidSettingsException e) {
            e.printStackTrace();
        }

        NodeSettings newsettings = new NodeSettings("Sorter");
        m_snm.saveSettingsTo(newsettings);

        try {
            boolean[] booltestarray = newsettings
                    .getBooleanArray(SorterNodeModel.SORTORDER_KEY);
            assertNotNull(booltestarray);
        } catch (InvalidSettingsException e) {
            e.printStackTrace();
        }
        try {
            String[] dctestarray = newsettings
                    .getStringArray(SorterNodeModel.INCLUDELIST_KEY);
            assertNotNull(dctestarray);
        } catch (InvalidSettingsException e) {
            e.printStackTrace();
        }
    }

    /**
     * test the ValidateSettings method of the <code>SorterNodeModel</code>.
     * 
     */
    public void testValidateSettings() {

        // try to validate an empty settings-object
        try {
            m_snm.validateSettings(m_settings);
            fail("Should have raised an InvalidSettingsException");
        } catch (InvalidSettingsException expected) {
            // nothing, we can continue
        }

        // add two null objects with incorrect keys
        m_settings.addDataCellArray("Incorrect Key 1", (DataCell[])null);
        m_settings.addBooleanArray("Incorrect Key 2", null);
        try {
            m_snm.validateSettings(m_settings);
            fail("Should have raised an InvalidSettingsException");
        } catch (InvalidSettingsException expected) {
            // nothing, we can continue
        }

        // add two null objects with the correct keys
        // to the settings object
        m_settings.addDataCellArray(SorterNodeModel.INCLUDELIST_KEY,
                (DataCell[])null);
        m_settings.addBooleanArray(SorterNodeModel.SORTORDER_KEY, null);
        try {
            m_snm.validateSettings(m_settings);
            fail("Should have raised an InvalidSettingsException");
        } catch (InvalidSettingsException expected) {
            // nothing, we can continue
        }

        // add a includelist-object but a null Sortorder-object
        DataCell[] dcarray = {new DoubleCell(3), new StringCell("Test")};
        m_settings.addDataCellArray(SorterNodeModel.INCLUDELIST_KEY, dcarray);
        try {
            m_snm.validateSettings(m_settings);
            fail("Should have raised an InvalidSettingsException");
        } catch (InvalidSettingsException expected) {
            // nothing, we can continue
        }

        // add a sortorder-object, now everything should be fine
        boolean[] testboolarray = {true, false};
        m_settings
                .addBooleanArray(SorterNodeModel.SORTORDER_KEY, testboolarray);
        try {
            m_snm.validateSettings(m_settings);
        } catch (InvalidSettingsException e) {
            e.printStackTrace();
        }

    }

    /**
     * test the LoadValidateSettings method of the <code>SorterNodeModel</code>.
     * 
     */
    public void testLoadValidatedSettingsFrom() {
        DataCell[] dcarray = {new DoubleCell(3), new StringCell("Test")};
        m_settings.addDataCellArray(SorterNodeModel.INCLUDELIST_KEY, dcarray);
        boolean[] testboolarray = {true, false};
        m_settings
                .addBooleanArray(SorterNodeModel.SORTORDER_KEY, testboolarray);

        try {
            m_snm.loadValidatedSettingsFrom(m_settings);
        } catch (InvalidSettingsException e) {
            e.printStackTrace();
        }

    }

    /**
     * test the execute method of the <code>SorterNodeModel</code>.
     * 
     */
    public void testExecute() {

        // try to sort a table with 1 entry
        String[] columnNames = {"col1", "col2", "col3", "col4"};
        DataType[] columnTypes = {DoubleCell.TYPE, StringCell.TYPE,
                IntCell.TYPE, DoubleCell.TYPE};
        DataRow[] rows = new DataRow[1];
        DataCell[] myRow = new DataCell[4];
        myRow[0] = new DoubleCell(2.4325);
        myRow[1] = new StringCell("Test");
        myRow[2] = new IntCell(7);
        myRow[3] = new DoubleCell(32432.324);
        rows[0] = new DefaultRow(Integer.toString(1), myRow);

        DataTable[] inputTable = {new DefaultTable(rows, columnNames,
                columnTypes)};
        DataTable[] resultTable = {new DefaultTable(rows, columnNames,
                columnTypes)};

        // set settings
        DataCell[] dcarray = {new StringCell("col1"), new StringCell("col2"),
                new StringCell("col3"), new StringCell("col4")};
        m_settings.addDataCellArray(SorterNodeModel.INCLUDELIST_KEY, dcarray);
        boolean[] sortorder = {true, true, true, true};
        m_settings.addBooleanArray(SorterNodeModel.SORTORDER_KEY, sortorder);
        try {
            m_snm.loadValidatedSettingsFrom(m_settings);
        } catch (InvalidSettingsException e) {
            e.printStackTrace();
        }
        try {
            resultTable = m_snm.execute(EXEC_CONTEXT.createBufferedDataTables(
                    inputTable, null), EXEC_CONTEXT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // test output

        RowIterator rowIt = resultTable[0].iterator();
        assertTrue(rowIt.hasNext());
        assertEquals(rows[0], rowIt.next());
        assertFalse(rowIt.hasNext());
        m_snm.reset();

        // *********************************************//
        // try to sort a large array of DataRows
        // In this case we generate a unit matrix
        // *********************************************//

        // start with a little one
        int dimension = 50;
        // *********************************************//
        // set settings
        dcarray = new DataCell[dimension];
        for (int i = 0; i < dimension; i++) {
            dcarray[i] = new StringCell("col" + i);
        }
        m_settings.addDataCellArray(SorterNodeModel.INCLUDELIST_KEY, dcarray);
        sortorder = new boolean[dimension];
        for (int i = 0; i < dimension; i++) {
            sortorder[i] = true;
        }
        m_settings.addBooleanArray(SorterNodeModel.SORTORDER_KEY, sortorder);

        DataTable[] inputTable2 = {generateUnitMatrixTable(dimension)};

        try {
            m_snm.loadValidatedSettingsFrom(m_settings);
        } catch (InvalidSettingsException e) {
            e.printStackTrace();
        }
        try {
            resultTable = m_snm.execute(EXEC_CONTEXT.createBufferedDataTables(
                    inputTable2, null), EXEC_CONTEXT);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // test output (should have sorted all rows in reverse order)
        rowIt = resultTable[0].iterator();
        assertTrue(rowIt.hasNext());
        int k = dimension - 1;
        // TODO while loop
        // while (!rowIt.atEnd()) {
        // IntCell ic = (IntCell) rowIt.next().getKey();
        // assertEquals(k, ic.getIntValue());
        k--;
        // }
        // assertTrue(rowIt.atEnd());
        m_snm.reset();

        // *********************************************//
        // try to sort a very large array of DataRows
        // In this case we generate a unit matrix
        // *********************************************//
        // dimension 300 => 15,8 secs.
        // dimension 500 => 49,7 secs.
        dimension = 100;
        // *********************************************//
        // set settings
        dcarray = new DataCell[dimension];
        for (int i = 0; i < dimension; i++) {
            dcarray[i] = new StringCell("col" + i);
        }
        m_settings.addDataCellArray(SorterNodeModel.INCLUDELIST_KEY, dcarray);
        sortorder = new boolean[dimension];
        for (int i = 0; i < dimension; i++) {
            sortorder[i] = true;
        }
        m_settings.addBooleanArray(SorterNodeModel.SORTORDER_KEY, sortorder);

        DataTable[] inputTable3 = {generateUnitMatrixTable(dimension)};

        try {
            m_snm.loadValidatedSettingsFrom(m_settings);
        } catch (InvalidSettingsException e) {
            e.printStackTrace();
        }
        try {
            resultTable = m_snm.execute(EXEC_CONTEXT.createBufferedDataTables(
                    inputTable3, null), EXEC_CONTEXT);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // test output (should have sorted all rows in reverse order)
        rowIt = resultTable[0].iterator();
        assertTrue(rowIt.hasNext());
        k = dimension - 1;
        while (rowIt.hasNext()) {
            RowKey rk = rowIt.next().getKey();
            int ic = Integer.parseInt(rk.getString());
            assertEquals(k, ic);
            k--;
        }
        assertFalse(rowIt.hasNext());
        m_snm.reset();

    }

    /**
     * This method produces a unit matrix -<code>DataTable</code>.
     * 
     * @param dimension of the matrix
     * @return Unit matrix
     */
    public DefaultTable generateUnitMatrixTable(final int dimension) {
        // generate Column names and types
        String[] columnNames = new String[dimension];
        for (int i = 0; i < dimension; i++) {
            columnNames[i] = "col" + i;
        }
        DataType[] columnTypes = new DataType[dimension];
        for (int i = 0; i < dimension; i++) {
            columnTypes[i] = IntCell.TYPE;
        }

        DataRow[] unitmatrix = new DataRow[dimension];
        for (int i = 0; i < dimension; i++) {
            DataCell[] myRow = new DataCell[dimension];
            for (int j = 0; j < dimension; j++) {
                if (i == j) {
                    myRow[j] = new IntCell(1);
                } else {
                    myRow[j] = new IntCell(0);
                }
            }
            DataRow temprow = new DefaultRow(Integer.toString(i), myRow);
            unitmatrix[i] = temprow;
        }
        return new DefaultTable(unitmatrix, columnNames, columnTypes);
    }

}
