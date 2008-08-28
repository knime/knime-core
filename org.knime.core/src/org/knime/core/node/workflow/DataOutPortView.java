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
 * History
 *   03.08.2005 (ohl): created
 *   08.05.2006(sieb, ohl): reviewed 
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ShapeHandler;
import org.knime.core.data.property.SizeHandler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.tableview.TableView;

/**
 * Implements a view to inspect the data, table spec and other stuff currently
 * stored in an output port.
 * 
 * @author Peter Ohl, University of Konstanz
 */
class DataOutPortView extends NodeOutPortView {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(DataOutPortView.class); 

    private final JTabbedPane m_tabs;

    private final TableView m_specView;

    private final TableView m_dataView;

    private final TableView m_propsView;
    
    private final JLabel m_busyLabel;

    private final String m_nodeName;

    private final String m_portName;

    private DataTable m_table;
    
    private DataTableSpec m_tableSpec;
    
    /** Updates are synchronized on this object. Declaring the methods
     * as synchronized (i.e. using "this" as mutex) does not work as swing
     * also acquires locks on this graphical object.
     */
    private final Object m_updateLock = new Object();
    
    /**
     * A view showing the data stored in the specified output port.
     * 
     * @param nodeName The name of the node the inspected port belongs to
     * @param portName The name of the port to view data from. Will appear in
     *            the title of the frame.
     * 
     */
    DataOutPortView(final String nodeName, final String portName) {
        super(createWindowTitle(nodeName, portName, null, null, null));

        m_table = null;
        m_tableSpec = null;
        
        m_nodeName = nodeName;
        m_portName = portName;

        Container cont = getContentPane();
        cont.setLayout(new BorderLayout());
        cont.setBackground(NodeView.COLOR_BACKGROUND);

        m_tabs = new JTabbedPane();
        m_specView = new TableView();
        m_specView.setShowIconInColumnHeader(false);
        m_dataView = new TableView();
        m_propsView = new TableView();
        m_propsView.setShowIconInColumnHeader(false);
        m_dataView.getHeaderTable().setShowColorInfo(false);
        // in the data view our columns are all of type string. Don't show that.
        // Users confuse it with the type of their table.
        // m_dataView.getHeaderTable().setShowTypeInfo(false);
        m_specView.getHeaderTable().setShowColorInfo(false);
        m_propsView.getHeaderTable().setShowColorInfo(false);

        m_tabs.addTab("DataTable", m_dataView);
        m_tabs.addTab("DataTableSpec", m_specView);
        m_tabs.addTab("DataColumnProperties", m_propsView);

        m_tabs.setBackground(NodeView.COLOR_BACKGROUND);
        
        m_busyLabel = new JLabel("Fetching data ...");
        cont.add(m_tabs, BorderLayout.CENTER);
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    void update(final PortObject table, final PortObjectSpec spec) {
        this.update((DataTable) table, (DataTableSpec) spec); 
    }
    
    /** 
     * Updates table and spec. This is executed in a newly created thread to
     * allow the view to pop up quickly. The view will show a label that
     * the data is being loaded while the set process is executing.
     * @param table The new table (may be null).
     * @param spec The new spec.
     */
    protected void update(final DataTable table, final DataTableSpec spec) {
        synchronized (m_updateLock) {
            m_table = table;
            m_tableSpec = spec;
            
            if (isVisible()) {
                showComponent(m_busyLabel);
                updateAll();
            }
        }
    }
    
    private void showComponent(final JComponent p) {
        Runnable run = new Runnable() {
            public void run() {
                Container contentPane = getContentPane();
                contentPane.removeAll();
                contentPane.add(p, BorderLayout.CENTER);
                updatePortView();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            run.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(run);
            } catch (InvocationTargetException ite) {
                LOGGER.warn("Exception while setting new component", ite);
            } catch (InterruptedException ie) {
                // do nothing here.
            }
        }
    }

    /**
     * Sets a new DataTable to display.
     * 
     * @param newDataTable The new data table (or null) to display in the view.
     */
    private void updateDataTable(final DataTable newDataTable) {
        synchronized (m_updateLock) {
            m_dataView.setDataTable(newDataTable);
            // display the number of rows in the upper left corner
            
            if (newDataTable instanceof BufferedDataTable) {
                // only BufferedTables have the row count set.
                BufferedDataTable bTable = (BufferedDataTable)newDataTable;
                int colCount = bTable.getDataTableSpec().getNumColumns();
                int rowCount = bTable.getRowCount();
                
                String header =
                    "" + rowCount + " Row" + (rowCount != 1 ? "s" : "") + ", "
                    + colCount + " Col" + (colCount != 1 ? "s" : "");
                m_dataView.getHeaderTable().setColumnName(header);
                // display the row count in the window title, too. 
                setTitle(createWindowTitle(m_nodeName, m_portName, 
                        newDataTable.getDataTableSpec().getName(), 
                        newDataTable.getDataTableSpec().getNumColumns(), 
                        bTable.getRowCount()));
                
            } else {
                m_dataView.getHeaderTable().setColumnName("");
                setTitle(createWindowTitle(
                        m_nodeName, m_portName, null, null, null));
                
            }
        }
    }

    /**
     * Sets a new DataTableSpec to display.
     * 
     * @param newTableSpec The new data table spec (or null) to display in the
     *            view.
     */
    private void updateDataTableSpec(final DataTableSpec newTableSpec) {
        synchronized (m_updateLock) {
            m_specView.setDataTable(createTableSpecTable(newTableSpec));
            m_propsView.setDataTable(createPropsTable(newTableSpec));
            // display the number of columns in the upper left corner
            if (newTableSpec != null) {
                int numOfCols = newTableSpec.getNumColumns();
                m_specView.getHeaderTable().setColumnName("" + numOfCols 
                        + " Column" + (numOfCols > 1 ? "s" : ""));
                m_propsView.getHeaderTable().setColumnName("Property Key");
                setTitle(createWindowTitle(m_nodeName, m_portName, newTableSpec
                        .getName(), newTableSpec.getNumColumns(), null));
            } else {
                m_specView.getHeaderTable().setColumnName("");
                m_propsView.getHeaderTable().setColumnName("");
                setTitle(createWindowTitle(m_nodeName, m_portName, null, null, 
                        null));
            }
        }
    }

    private DataTable createPropsTable(final DataTableSpec tSpec) {
        if (tSpec != null) {
            int numOfCols = tSpec.getNumColumns(); // output has as many cols
            String[] colNames = new String[numOfCols];
            DataType[] colTypes = new DataType[numOfCols];
            // colnames are the same as incoming, types are all StringTypes
            for (int c = 0; c < numOfCols; c++) {
                colNames[c] = tSpec.getColumnSpec(c).getName();
                colTypes[c] = StringCell.TYPE;
            }
            // get keys for ALL props in the table. Each will show in one row.
            HashSet<String> allKeys = new HashSet<String>();
            for (int c = 0; c < numOfCols; c++) {
                Enumeration<String> props =
                        tSpec.getColumnSpec(c).getProperties().properties();
                while (props.hasMoreElements()) {
                    allKeys.add(props.nextElement());
                }
            }

            DataContainer result =
                    new DataContainer(new DataTableSpec(colNames, colTypes));

            // now construct the rows we wanna display
            for (String key : allKeys) {
                DataCell[] cells = new DataCell[numOfCols];
                for (int c = 0; c < numOfCols; c++) {
                    String cellValue = "";
                    if (tSpec.getColumnSpec(c).getProperties()
                            .containsProperty(key)) {
                        cellValue =
                                tSpec.getColumnSpec(c).getProperties()
                                        .getProperty(key);
                    }
                    cells[c] = new StringCell(cellValue);
                }
                result.addRowToTable(new DefaultRow(key, cells));
            }
            result.close();
            return result.getTable();

        } else {
            DataContainer result =
                    new DataContainer(new DataTableSpec(
                            new String[]{"No outgoing table spec"},
                            new DataType[]{StringCell.TYPE}));
            result.close();
            return result.getTable();

        }

    }

    private DataTable createTableSpecTable(final DataTableSpec spec) {

        // first create a table header: names and types
        String[] names = null;
        DataType[] types = null;
        if (spec != null) {
            int numCols = spec.getNumColumns();
            names = new String[numCols];
            types = new DataType[numCols];
            for (int c = 0; c < numCols; c++) {
//                names[c] = "Col_" + c;
                names[c] = spec.getColumnSpec(c).getName();
                types[c] = StringCell.TYPE;
            }
        } else {
            names = new String[]{"No outgoing table spec"};
            types = new DataType[]{StringCell.TYPE};
        }

        DataContainer result =
                new DataContainer(new DataTableSpec(names, types));

        // now put the data we want to show in rows: name + type + each value
        if (spec != null) {

            addInfoRowsToDataContainer(result, spec);
            addPossValuesRowsToDataContainer(result, spec);

        }

        // create the new table
        result.close();
        return result.getTable();

    }

    private void addInfoRowsToDataContainer(final DataContainer result,
            final DataTableSpec spec) {
        assert spec != null;
        assert result != null;

        int numCols = spec.getNumColumns();

//        // 1st row: displays the name of each column
//        DataCell[] cols = new DataCell[numCols];
//        for (int c = 0; c < numCols; c++) {
//            cols[c] = new StringCell(spec.getColumnSpec(c).getName());
//        }
//        result.addRowToTable(new DefaultRow(new StringCell("<html><b>Name"),
//                cols));
        
        DataCell[] cols;
        
        // 2nd row: displays type of column
        cols = new DataCell[numCols];
        for (int c = 0; c < numCols; c++) {
            String typename = spec.getColumnSpec(c).getType().toString();
            cols[c] = new StringCell(typename);
        }
        result.addRowToTable(new DefaultRow("Column Type", cols));

        
        // 1st row: show the column number 
        cols = new DataCell[numCols];
        for (int c = 0; c < numCols; c++) {
            cols[c] = new StringCell("" + c);
        }
        result.addRowToTable(new DefaultRow("Column Index", cols));
        
        
        // 3rd row: shows who has a color handler set
        cols = new DataCell[numCols];
        for (int c = 0; c < numCols; c++) {
            if (spec.getColumnSpec(c).getColorHandler() == null) {
                cols[c] = new StringCell("");
            } else {
                // Display the String repr. of the ColorHdl
                ColorHandler cHdl = spec.getColumnSpec(c).getColorHandler();
                String colHdlStr = cHdl.toString();
                // add an instance unique ID
                colHdlStr += " (id=" + System.identityHashCode(cHdl) + ")";
                cols[c] = new StringCell(colHdlStr);
            }
        }
        result.addRowToTable(new DefaultRow("Color Handler", cols));

        // 4th row: shows who has a SizeHandler set
        cols = new DataCell[numCols];
        for (int c = 0; c < numCols; c++) {
            if (spec.getColumnSpec(c).getSizeHandler() == null) {
                cols[c] = new StringCell("");
            } else {
                SizeHandler sHdl = spec.getColumnSpec(c).getSizeHandler();
                String sHdlrStr = sHdl.toString();
                sHdlrStr += " (id=" + System.identityHashCode(sHdl) + ")";
                cols[c] = new StringCell(sHdlrStr);
            }
        }
        result
                .addRowToTable(new DefaultRow("Size Handler", cols));

        // 5th row: shows where the shape handler is attached to.
        cols = new DataCell[numCols];
        for (int c = 0; c < numCols; c++) {
            if (spec.getColumnSpec(c).getShapeHandler() == null) {
                cols[c] = new StringCell("");
            } else {
                ShapeHandler hdl = spec.getColumnSpec(c).getShapeHandler();
                String hdlrStr = hdl.toString();
                hdlrStr += " (id=" + System.identityHashCode(hdl) + ")";
                cols[c] = new StringCell(hdlrStr);
            }
        }
        result.addRowToTable(new DefaultRow("Shape Handler", cols));

        // 6th row: displays the lower bound of the domain
        cols = new DataCell[numCols];
        for (int c = 0; c < numCols; c++) {
            String boundText = "<undefined>";
            if (spec.getColumnSpec(c).getDomain().getLowerBound() != null) {
                boundText =
                        spec.getColumnSpec(c).getDomain().getLowerBound()
                                .toString();
            }
            cols[c] = new StringCell(boundText);
        }
        result.addRowToTable(new DefaultRow("Lower Bound", cols));

        // 7th row: shows the upper bound value of the domain
        cols = new DataCell[numCols];
        for (int c = 0; c < numCols; c++) {
            String boundText = "<undefined>";
            if (spec.getColumnSpec(c).getDomain().getUpperBound() != null) {
                boundText =
                        spec.getColumnSpec(c).getDomain().getUpperBound()
                                .toString();
            }
            cols[c] = new StringCell(boundText);
        }
        result.addRowToTable(new DefaultRow("Upper Bound", cols));
    }

    private void addPossValuesRowsToDataContainer(final DataContainer result,
            final DataTableSpec spec) {
        assert result != null;
        assert spec != null;

        int numCols = spec.getNumColumns();

        // from the 8th row: show the nominal values of that column. If any.
        // find out how many rows we need to create - enough for the column
        // with the most possible values
        int maxNumValues = 0;
        Iterator<DataCell> emptyIter = new ArrayList<DataCell>().iterator();
        Iterator<?>[] valueIter = new Iterator<?>[numCols];
        // store an iterator for _each_ column, remember the maximum value
        // set size
        for (int c = 0; c < numCols; c++) {
            Set<DataCell> values =
                    spec.getColumnSpec(c).getDomain().getValues();
            if (values != null) {
                valueIter[c] = values.iterator();
                if (values.size() > maxNumValues) {
                    maxNumValues = values.size();
                }
            } else {
                valueIter[c] = emptyIter;
            }
        }
        DataCell emptyStringCell = new StringCell("");

        for (int r = 0; r < maxNumValues; r++) {
            DataCell[] cols = new DataCell[numCols];
            for (int c = 0; c < numCols; c++) {
                if (!valueIter[c].hasNext()) {
                    cols[c] = emptyStringCell;
                } else {
                    // transform it into a string cell
                    cols[c] = new StringCell(valueIter[c].next().toString());
                }
            }
            result.addRowToTable(new DefaultRow("Value " + r, cols));
        }

    }

    private static String createWindowTitle(final String nodeName,
            final String portName, final String tableName,
            final Integer numOfCols, final Integer numOfRows) {
        StringBuilder result = new StringBuilder("");
        if (tableName != null) {
            result.append(nodeName + ", " + portName + ", Table: " + tableName);
        } else {
            result.append(nodeName + ", " + portName);
        }
        if (numOfCols != null) {
            result.append(", Cols: " + numOfCols);
        }
        if (numOfRows != null) {
            result.append(", Rows: " + numOfRows);
        }
        return result.toString();
    }
    
    private void updateAll() {
        /* Thread that executes the setDataTable method in the TableView.
         * The reason for that is that setting the table may require some
         * time as sometimes the entire data needs to be extracted from a
         * workspace archive to the temp directory. During that time, this
         * view will show label "Getting data..." and be more responsive.
         */
        Thread updateThread = new Thread() {
            @Override
            public void run() {
                try {
                    synchronized (m_updateLock) {
                        if (m_table != null) {
                            // although a data table is by definition is
                            // read only, the buffered data table may be clear
                            // when the node is reset; we acquire a lock here
                            // to block the intermediate clear() 
                            synchronized (m_table) {
                                updateDataTable(m_table);
                                updateDataTableSpec(m_tableSpec);
                                showComponent(m_tabs);
                            }
                        } else {
                            updateDataTable(null);
                            updateDataTableSpec(m_tableSpec);
                            showComponent(m_tabs);                        
                        }
                    }
                } catch (Exception ite) {
                    LOGGER.warn("Exception while setting table", ite);
                    showComponent(new JLabel(ite.getClass().getSimpleName()
                            + " while setting table, "
                            + "see log file for details"));
                }
            }
        };

        // this updates the table view and table spec view in a background 
        // process and replaces the busy label with the table views.
        updateThread.start();        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(final boolean b) {
        super.setVisible(b);
        if (b) {
            showComponent(m_busyLabel);
            updateAll();
        }
        
    }
}
