/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
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
 *   03.08.2005 (ohl): created on his birthday
 */
package de.unikn.knime.core.node;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.swing.JTabbedPane;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.RowKey;
import de.unikn.knime.core.data.StringType;
import de.unikn.knime.core.data.def.DefaultRow;
import de.unikn.knime.core.data.def.DefaultStringCell;
import de.unikn.knime.core.data.def.DefaultTable;
import de.unikn.knime.core.node.property.ColorAttr;
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

    private DataTable m_data;
    
    private final TableView m_specView;

    private final TableView m_propView;

    private final TableView m_attrView;

    private final TableView m_dataView;

    private final TableView m_propsView;

    private String m_nodeName;

    private String m_portName;

    /**
     * A view showing the stuff stored in the specified ouput port.
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
        m_propView = new TableView();
        m_attrView = new TableView();
        m_dataView = new TableView();
        m_propsView = new TableView();
        m_dataView.getHeaderTable().setShowColorInfo(false);
        m_specView.getHeaderTable().setShowColorInfo(false);
        m_propView.getHeaderTable().setShowColorInfo(false);
        m_attrView.getHeaderTable().setShowColorInfo(false);
        m_propsView.getHeaderTable().setShowColorInfo(false);
        m_tabs.addTab("Data", m_dataView);
        m_tabs.addTab("DataTableSpec", m_specView);
        m_tabs.addTab("Annotated Props", m_propsView);
        m_tabs.addTab("HighLightHdlr", m_propView);
        m_tabs.addTab("RowColorAttrs", m_attrView);

        m_tabs.setBackground(NodeView.COLOR_BACKGROUND);
        cont.add(m_tabs, BorderLayout.CENTER);
    }

    /**
     * Sets a new DataTable to display.
     * 
     * @param newDataTable The new data table (or null) to display in the view.
     */
    void updateDataTable(final DataTable newDataTable) {
        m_data = newDataTable;
        m_dataView.setDataTable(newDataTable);
        m_attrView.setDataTable(createRowAttrTable(newDataTable));
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
        m_propView.setDataTable(createHiLiteTable(newHilitHdlr));
    }

    private DataTable createHiLiteTable(final HiLiteHandler hiLiteHdl) {
        // for now we just display if null or if set.
        // otherwise we would have to register as listener and recreate
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

    private DataTable createRowAttrTable(final DataTable dTable) {
        if (dTable == null) {
            return new DefaultTable(new String[][]{new String[]{"No data"}});
        } else {
            return new RowAttrTable(dTable);
        }
    }

    /**
     * 
     * @author Peter Ohl, University of Konstanz
     */
    private final class RowAttrTable implements DataTable {

        private DataTable m_table;

        private DataTableSpec m_spec;

        /**
         * Creates a new Debugger table which contains rows showing the
         * attributes of the row key contained in the corresponding rows of the
         * table passed in.
         * 
         * @param debugTable the table to show attributes from.
         */
        private RowAttrTable(final DataTable debugTable) {
            m_table = debugTable;
            m_spec = null;
        }

        /**
         * @see de.unikn.knime.core.data.DataTable#getDataTableSpec()
         */
        public DataTableSpec getDataTableSpec() {
            if (m_spec == null) {
                if (m_table == null) {
                    m_spec = new DataTableSpec(
                            new DataCell[]{new DefaultStringCell("no table")},
                            new DataType[]{StringType.STRING_TYPE});
                } else {
                    m_spec = new DataTableSpec(new DataCell[]{
                            new DefaultStringCell("Color"),
                            new DefaultStringCell("Hilit"),
                            new DefaultStringCell("Select"),
                            new DefaultStringCell("HilitSlct"),
                            new DefaultStringCell("BordrCol"),
                            new DefaultStringCell("BordrHilit"),
                            new DefaultStringCell("BordrSel"),
                            new DefaultStringCell("BordrHilSel")},
                            new DataType[]{StringType.STRING_TYPE,
                                    StringType.STRING_TYPE,
                                    StringType.STRING_TYPE,
                                    StringType.STRING_TYPE,
                                    StringType.STRING_TYPE,
                                    StringType.STRING_TYPE,
                                    StringType.STRING_TYPE,
                                    StringType.STRING_TYPE});
                }
            }
            return m_spec;
        }

        /**
         * @see de.unikn.knime.core.data.DataTable#iterator()
         */
        public RowIterator iterator() {
            RowIterator iter = null;

            if (m_table != null) {
                iter = m_table.iterator();
            }

            return new RowAttrRowIterator(iter);
        }

    }

    /**
     * 
     * @author ohl, University of Konstanz
     */
    private final class RowAttrRowIterator extends RowIterator {

        private RowIterator m_iter;

        private boolean m_atEnd;

        /**
         * constructs a new iterator that will produce rows containing string
         * cells with information about the atrributes of the row keys produced
         * by the iterator passed in.
         * 
         * @param iter the iterator producing the rows to debug
         */
        private RowAttrRowIterator(final RowIterator iter) {
            m_iter = iter;
            m_atEnd = false;
        }

        /**
         * @see de.unikn.knime.core.data.RowIterator#hasNext()
         */
        public boolean hasNext() {
            if (m_iter == null) {
                return !m_atEnd;
            } else {
                return m_iter.hasNext();
            }
        }

        /**
         * @see de.unikn.knime.core.data.RowIterator#next()
         */
        public DataRow next() {

            if (m_iter == null) {
                if (m_atEnd) {
                    throw new NoSuchElementException(
                            "Row iterator proceeded beyond"
                                    + " the last element");
                }
                m_atEnd = true;
                return new DefaultRow(new RowKey(new DefaultStringCell(
                        "nullIter")), new DataCell[]{new DefaultStringCell(
                        "got no Iterator")});
            }

            DataRow thisRow = m_iter.next();
            // get Color for this row
            ColorAttr attr = m_data.getDataTableSpec().getRowColor(thisRow);
            RowKey key = thisRow.getKey();

            return new DefaultRow(key,
                    new DataCell[]{
                            new DefaultStringCell(cAttrToString(attr, false,
                                    false, false)),
                            new DefaultStringCell(cAttrToString(attr, false,
                                    true, false)),
                            new DefaultStringCell(cAttrToString(attr, false,
                                    false, true)),
                            new DefaultStringCell(cAttrToString(attr, false,
                                    true, true)),

                            new DefaultStringCell(cAttrToString(attr, true,
                                    false, false)),
                            new DefaultStringCell(cAttrToString(attr, true,
                                    true, false)),
                            new DefaultStringCell(cAttrToString(attr, true,
                                    false, true)),
                            new DefaultStringCell(cAttrToString(attr, true,
                                    true, true))});
        }

        private String cAttrToString(final ColorAttr cAttr,
                final boolean border, final boolean hilit, final boolean sel) {

            Color c = null;
            if (cAttr != null) {
                if (border) {
                    c = cAttr.getBorderColor(sel, hilit);
                } else {
                    c = cAttr.getColor(sel, hilit);
                }
            }
            if (c == null) {
                return "<null>";
            } else {
                return "r" + c.getRed() + "g" + c.getGreen() + "b"
                        + c.getBlue();
            }
        }
    }
}
