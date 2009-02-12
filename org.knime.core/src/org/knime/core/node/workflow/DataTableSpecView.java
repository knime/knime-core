/* This source code, its documentation and all appendant files
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
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JPanel;

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
import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableView;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DataTableSpecView extends JPanel {
    
//    private static final NodeLogger LOGGER =
//        NodeLogger.getLogger(DataTableSpecView.class);


    private final TableView m_specView;
    
    private final DataTableSpec m_tableSpec;

    /** Updates are synchronized on this object. Declaring the methods
     * as synchronized (i.e. using "this" as mutex) does not work as swing
     * also acquires locks on this graphical object.
     */
    private final Object m_updateLock = new Object();
    
    
    /**
     * 
     * @param tableSpec data table spec to display
     */
    public DataTableSpecView(final DataTableSpec tableSpec) {
        m_tableSpec = tableSpec;


        setLayout(new BorderLayout());
        setBackground(NodeView.COLOR_BACKGROUND);

        m_specView = new TableView();
        m_specView.setShowIconInColumnHeader(false);
        // in the data view our columns are all of type string. Don't show that.
        // Users confuse it with the type of their table.
        // m_dataView.getHeaderTable().setShowTypeInfo(false);
        m_specView.getHeaderTable().setShowColorInfo(false);

        setName("DataTableSpec");
        add(m_specView);
  
        updateDataTableSpec();
    }
    

   
    

    
    /**
     * Sets a new DataTableSpec to display.
     *
     * @param newTableSpec The new data table spec (or null) to display in the
     *            view.
     */
    private void updateDataTableSpec() {
        synchronized (m_updateLock) {
            m_specView.setDataTable(createTableSpecTable(m_tableSpec));
            // display the number of columns in the upper left corner
            if (m_tableSpec != null) {
                String title = createWindowTitle(m_tableSpec.getNumColumns());
                m_specView.getHeaderTable().setColumnName(title);
                setName("Spec - " + title);
            } else {
                m_specView.getHeaderTable().setColumnName("");
                setName("No Spec");
            }
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
    
    
    private static String createWindowTitle(final int numOfCols) {
        return "Column" + (numOfCols > 1 ? "s: " : ": ") + numOfCols;
    }

    
}
