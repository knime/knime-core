/* --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 * History
 *   03.08.2005 (ohl): created
 *   08.05.2006(sieb, ohl): reviewed 
 */
package de.unikn.knime.core.node;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JTabbedPane;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.StringType;
import de.unikn.knime.core.data.def.DefaultRow;
import de.unikn.knime.core.data.def.DefaultStringCell;
import de.unikn.knime.core.data.def.DefaultTable;
import de.unikn.knime.core.node.property.hilite.HiLiteHandler;
import de.unikn.knime.core.node.tableview.TableView;

/**
 * Implements a view to inspect the data, tablespec and other stuff currently
 * stored in an output port.
 * 
 * @author Peter Ohl, University of Konstanz
 */
final class DataOutPortView extends NodeOutPortView {

    private final JTabbedPane m_tabs;
    
    private final TableView m_specView;

    private final TableView m_hiliteView;

    private final TableView m_dataView;

    private final TableView m_propsView;

    private String m_nodeName;

    private String m_portName;

    /**
     * A view showing the data stored in the specified ouput port.
     * 
     * @param nodeName The name of the node the inspected port belongs to
     * @param portName The name of the port to view data from. Will appear in
     *            the title of the frame.
     * 
     */
    DataOutPortView(final String nodeName, final String portName) {
        super(createWindowTitle(nodeName, portName, null));

        m_nodeName = nodeName;
        m_portName = portName;
        
        Container cont = getContentPane();
        cont.setLayout(new BorderLayout());
        cont.setBackground(NodeView.COLOR_BACKGROUND);

        m_tabs = new JTabbedPane();
        m_specView = new TableView();
        m_hiliteView = new TableView();
        m_dataView = new TableView();
        m_propsView = new TableView();
        m_dataView.getHeaderTable().setShowColorInfo(false);
        m_specView.getHeaderTable().setShowColorInfo(false);
        m_hiliteView.getHeaderTable().setShowColorInfo(false);
        m_propsView.getHeaderTable().setShowColorInfo(false);
        m_tabs.addTab("Data", m_dataView);
        m_tabs.addTab("DataTableSpec", m_specView);
        m_tabs.addTab("Annotated Props", m_propsView);
        m_tabs.addTab("HighLightHdlr", m_hiliteView);

        m_tabs.setBackground(NodeView.COLOR_BACKGROUND);
        cont.add(m_tabs, BorderLayout.CENTER);
    }

    /**
     * Sets a new DataTable to display.
     * 
     * @param newDataTable The new data table (or null) to display in the view.
     */
    void updateDataTable(final DataTable newDataTable) {
        m_dataView.setDataTable(newDataTable);
    }

    /**
     * Sets a new DataTableSpec to display.
     * 
     * @param newTableSpec The new data table spec (or null) to display in the
     *            view.
     */
    void updateDataTableSpec(final DataTableSpec newTableSpec) {
        m_specView.setDataTable(createTableSpecTable(newTableSpec));
        m_propsView.setDataTable(createPropsTable(newTableSpec));
        // display the number of columns in the upper left corner
        if (newTableSpec != null) {
            int numOfCols = newTableSpec.getNumColumns();
            m_specView.getHeaderTable().setColumnName(
                    "" + numOfCols + " Column" + (numOfCols > 1 ? "s" : ""));
            m_propsView.getHeaderTable().setColumnName("Property Key");
            setTitle(createWindowTitle(m_nodeName, m_portName, 
                    newTableSpec.getName()));
        } else {
            m_specView.getHeaderTable().setColumnName("");
            m_propsView.getHeaderTable().setColumnName("");
            setTitle(createWindowTitle(m_nodeName, m_portName, null)); 
        }
    }
    

    /**
     * Sets a new HiLiteHandler to dislplay.
     * 
     * @param newHilitHdlr The new hilite handler to display in the view.
     */
    void updateHiliteHandler(final HiLiteHandler newHilitHdlr) {
        m_hiliteView.setDataTable(createHiLiteTable(newHilitHdlr));
    }

    private DataTable createHiLiteTable(final HiLiteHandler hiLiteHdl) {
        // for now we just display the pointer value.
        // Otherwise we would have to register as listener and recreate
        // the datatables completely each time something changes in the handlers
        DataCell[] names = {new DefaultStringCell("ClassName"),
                new DefaultStringCell("MemAddress"),
                new DefaultStringCell("fullString")};
        DataType[] types = {StringType.STRING_TYPE, StringType.STRING_TYPE,
                StringType.STRING_TYPE};
        DataRow[] rows = new DataRow[1];
        DataCell rowID = new DefaultStringCell("HiLiteHdlr");

        if (hiLiteHdl != null) {
            String fullname = hiLiteHdl.toString();
            String classname = hiLiteHdl.getClass().getName();
            classname = classname.substring(classname.lastIndexOf('.') + 1);
            String memAddress = hiLiteHdl.toString().substring(
                    hiLiteHdl.toString().indexOf('@'));
            rows[0] = new DefaultRow(rowID, new DataCell[]{
                    new DefaultStringCell(classname),
                    new DefaultStringCell(memAddress),
                    new DefaultStringCell(fullname)});
        } else {
            rows[0] = new DefaultRow(rowID, new DataCell[]{
                    new DefaultStringCell("<null>"),
                    new DefaultStringCell("<null>"),
                    new DefaultStringCell("<null>")});
        }

        return new DefaultTable(rows, names, types);

    }

    private DataTable createPropsTable(final DataTableSpec tSpec) {
        if (tSpec != null) {
            int numOfCols = tSpec.getNumColumns(); // output has as many cols
            DataCell[] colNames = new DataCell[numOfCols];
            DataType[] colTypes = new DataType[numOfCols];
            // colnames are the same than incoming, types are all StringTypes
            for (int c = 0; c < numOfCols; c++) {
                colNames[c] = tSpec.getColumnSpec(c).getName();
                colTypes[c] = StringType.STRING_TYPE;
            }
            // get keys for ALL props in the table. Each will show in one row.
            HashSet<String> allKeys = new HashSet<String>();
            for (int c = 0; c < numOfCols; c++) {
                Enumeration<String> props = tSpec.getColumnSpec(c)
                        .getProperties().properties();
                while (props.hasMoreElements()) {
                    allKeys.add(props.nextElement());
                }
            }

            // now construct the rows we wanna display
            DataRow[] rows = new DefaultRow[allKeys.size()];
            int rowIdx = 0;
            for (String key : allKeys) {
                DataCell rowID = new DefaultStringCell(key);
                DataCell[] cells = new DataCell[numOfCols];
                for (int c = 0; c < numOfCols; c++) {
                    String cellValue = "";
                    if (tSpec.getColumnSpec(c).getProperties()
                            .containsProperty(key)) {
                        cellValue = tSpec.getColumnSpec(c).getProperties()
                                .getProperty(key);
                    }
                    cells[c] = new DefaultStringCell(cellValue);
                }
                rows[rowIdx++] = new DefaultRow(rowID, cells);
            }

            return new DefaultTable(rows, colNames, colTypes);

        } else {
            return new DefaultTable(EMPTY_ROW,
                    new DataCell[]{new DefaultStringCell(
                            "No incoming table spec")},
                    new DataType[]{StringType.STRING_TYPE});
        }

    }

    private DataTable createTableSpecTable(final DataTableSpec spec) {

        // first create a table header: names and types
        DataCell[] names = null;
        DataType[] types = null;
        if (spec != null) {
            int numCols = spec.getNumColumns();
            names = new DataCell[numCols];
            types = new DataType[numCols];
            for (int c = 0; c < numCols; c++) {
                names[c] = new DefaultStringCell("Col_" + c);
                types[c] = StringType.STRING_TYPE;
            }
        } else {
            names = new DataCell[]{new DefaultStringCell("nothing")};
            types = new DataType[]{StringType.STRING_TYPE};
        }

        // now put the data we want to show in rows: name + type + each value
        DataRow[] rows = null;
        if (spec != null) {
            // find out how many rows we need to create - enough for the column
            // with the most possible values
            int maxNumValues = 0;
            int numCols = spec.getNumColumns();
            for (int c = 0; c < numCols; c++) {
                Set<DataCell> v = spec.getColumnSpec(c).getDomain()
                        .getValues();
                if ((v != null) && (v.size() > maxNumValues)) {
                    maxNumValues = v.size();
                }
            }
            int numRows = maxNumValues + 4;
            // + 4 for 'Name', 'Type', lower bound, and upper bound
            rows = new DataRow[numRows];

            // now, generate those rows:

            // Row[0]: first row displays the name of each column
            DataCell[] cols = new DataCell[numCols];
            for (int c = 0; c < numCols; c++) {
                cols[c] = new DefaultStringCell("<html><b>"
                        + spec.getColumnSpec(c).getName().toString());
            }
            rows[0] = new DefaultRow(new DefaultStringCell("<html><b>Name"),
                    cols);

            // Row[1]: second row displays type of column
            cols = new DataCell[numCols];
            for (int c = 0; c < numCols; c++) {
                String typename = spec.getColumnSpec(c).getType().getClass()
                        .getName();
                cols[c] = new DefaultStringCell("<html><b>"
                        + typename.substring(typename.lastIndexOf('.') + 1));
            }
            rows[1] = new DefaultRow(new DefaultStringCell("<html><b>Type"),
                    cols);
            // Row[2]: displays the lower bound of the domain
            cols = new DataCell[numCols];
            for (int c = 0; c < numCols; c++) {
                String boundText = "<null>";
                if (spec.getColumnSpec(c).getDomain().getLowerBound() != null) {
                    boundText = spec.getColumnSpec(c).getDomain()
                            .getLowerBound().toString();
                }
                cols[c] = new DefaultStringCell(boundText);
            }
            rows[2] = new DefaultRow(new DefaultStringCell(
                    "<html><b>lower bound"), cols);
            // Row[3]: shows the upper bound value of the domain
            cols = new DataCell[numCols];
            for (int c = 0; c < numCols; c++) {
                String boundText = "<null>";
                if (spec.getColumnSpec(c).getDomain().getUpperBound() != null) {
                    boundText = spec.getColumnSpec(c).getDomain()
                            .getUpperBound().toString();
                }
                cols[c] = new DefaultStringCell(boundText);
            }
            rows[3] = new DefaultRow(new DefaultStringCell(
                    "<html><b>upper bound"), cols);

            // from row 4: show the nominal values of that column. If any.
            Iterator<DataCell> emptyIter = new ArrayList<DataCell>().iterator();
            Iterator<?>[] valueIter = new Iterator<?>[numCols];
            // store an iterator for _each_ column
            for (int c = 0; c < numCols; c++) {
                Set<DataCell> values = spec.getColumnSpec(c).getDomain()
                        .getValues();
                if (values != null) {
                    valueIter[c] = values.iterator();
                } else {
                    valueIter[c] = emptyIter;
                }
            }
            DataCell emptyStringCell = new DefaultStringCell("");

            for (int r = 4; r < numRows; r++) {
                cols = new DataCell[numCols];
                for (int c = 0; c < numCols; c++) {
                    if (!valueIter[c].hasNext()) {
                        cols[c] = emptyStringCell;
                    } else {
                        // transform it into a string cell
                        cols[c] = new DefaultStringCell(valueIter[c].next()
                                .toString());
                    }
                }
                rows[r] = new DefaultRow(
                        new DefaultStringCell("Val_" + (r - 3)), cols);
            }

        } else {
            rows = EMPTY_ROW;
        }

        // create the new table
        return new DefaultTable(rows, names, types);

    }

    private static String createWindowTitle(final String nodeName, 
            final String portName, final String tableName) {
        if (tableName != null) {
            return nodeName + ", Port: " + portName + ", Table: " + tableName;
        } else {
            return nodeName + ", Port: " + portName;
        }
    }

    private static final DataRow[] EMPTY_ROW = new DataRow[]{new DefaultRow(
            new DefaultStringCell(""), new DataCell[]{new DefaultStringCell(
                    "<null>")})};

}
