/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.util;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableStringConverter;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.util.filter.NameFilterPanel;
import org.knime.core.node.util.filter.NameFilterTableModel;

/**
 * Panel is used to select/filter a certain number of columns.
 *
 * <p>
 * You can add a property change listener to this class that is notified when the include list changes.
 *
 * @deprecated A new column filter panel with more options is available in
 *             {@link org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel}
 * @author Thomas Gabriel, University of Konstanz
 */
@Deprecated
@SuppressWarnings("serial")
public class ColumnFilterPanel extends JPanel {

    /** Settings key for the excluded columns. */
    public static final String INCLUDED_COLUMNS = "included_columns";

    /** Settings key for the excluded columns. */
    public static final String EXCLUDED_COLUMNS = "excluded_columns";

    /** Include list. */
    private final JTable m_inclTable;

    /** Include model. */
    @SuppressWarnings("rawtypes")
    private final NameFilterTableModel m_inclMdl;

    /** Include sorter. */
    @SuppressWarnings("rawtypes")
    private final TableRowSorter<NameFilterTableModel> m_inclSorter;

    /** Include cards. */
    private final JPanel m_inclCards;

    /** Include table placeholder. */
    private final TablePlaceholder m_inclTablePlaceholder;

    /** Exclude list. */
    private final JTable m_exclTable;

    /** Exclude model. */
    @SuppressWarnings("rawtypes")
    private final NameFilterTableModel m_exclMdl;

    /** Exclude sorter. */
    @SuppressWarnings("rawtypes")
    private final TableRowSorter<NameFilterTableModel> m_exclSorter;

    /** Include cards. */
    private final JPanel m_exclCards;

    /** Include table placeholder. */
    private final TablePlaceholder m_exclTablePlaceholder;

    /** Remove all button. */
    private final JButton m_remAllButton;

    /** Remove button. */
    private final JButton m_remButton;

    /** Add all button. */
    private final JButton m_addAllButton;

    /** Add button. */
    private final JButton m_addButton;

    /** Search Field in include list. */
    private final JTextField m_inclSearchField;

    /** Search Field in exclude list. */
    private final JTextField m_exclSearchField;

    /** Border of the include panel, keep it so we can change the title. */
    private final TitledBorder m_includeBorder;

    /** Border of the include panel, keep it so we can change the title. */
    private final TitledBorder m_excludeBorder;

    private List<ChangeListener> m_listeners;

    /** Constants for updating different parts of the UI */
    private static final String ID_CARDLAYOUT_PLACEHOLDER = "PLACEHOLDER";

    private static final String ID_CARDLAYOUT_LIST = "LIST";

    /** Text to be displayed in the filter as a placeholder */
    private static final String FILTER = "Filter";

    /**
     * Placeholder being displayed instead of table when search matches no items.
     *
     * @author Johannes Schweig
     */
    private class TablePlaceholder extends JLabel {
        final String m_entryString;

        TablePlaceholder(final String entryString) {
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.TOP);
            setFont(getFont().deriveFont(Font.ITALIC));
            setForeground(Color.GRAY);
            m_entryString = entryString;
        }

        /**
         * Updates the label's text if there are no entries to be displayed.
         *
         */
        private void updateTextEmpty() {
            setText("No " + m_entryString + " in this list");
        }

        /**
         * Updates the label's text if no matching entries are found.
         *
         * @param searchString term that was searched for
         * @param total total number of entries in the table
         */
        private void updateTextNothingFound(final String searchString, final int total) {
            // empty list/table
            String str = searchString;
            // shorten string if too long
            int max = 15;
            if (str.length() > max) {
                str = str.substring(0, max) + "...";
            }
            setText("<html>No " + m_entryString + " found matching<br>\"" + str + "\" (total: " + total + ")</html>");
        }

    }

    /**
     * MouseAdapter handling double clicks of elements in the table.
     *
     * @author Johannes Schweig
     */
    private class DoubleClickMouseAdapter extends MouseAdapter {
        private JTable m_table;

        DoubleClickMouseAdapter(final JTable table) {
            m_table = table;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseClicked(final MouseEvent e) {
            // if a double click occurs on one of the rows, these rows are moved to the other table
            if (e.getClickCount() == 2) {
                if (m_table.equals(m_inclTable)) {
                    onRemIt(m_inclTable.getSelectedRows());
                } else if (m_table.equals(m_exclTable)) {
                    onAddIt(m_exclTable.getSelectedRows());
                }
            }
            e.consume();
        }
    }

    /**
     * KeyAdapter of a table handling key strokes.
     *
     * @author Johannes Schweig
     */
    private class TableKeyAdapter extends KeyAdapter {
        private JTable m_table;

        TableKeyAdapter(final JTable table) {
            m_table = table;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyTyped(final KeyEvent e) {
            // find first entry starting with typed character
            String key = String.valueOf(e.getKeyChar());
            int index = -1;
            for (int i = 0; i < m_table.getRowCount(); i++) {
                String s = ((DataColumnSpec)m_table.getValueAt(i, 0)).getName();
                if (s.toLowerCase().startsWith(key)) {
                    index = i;
                    break;
                }
            }
            // if a entry is found, select it and scroll the view
            if (index != -1) {
                m_table.setRowSelectionInterval(index, index);
                m_table.scrollRectToVisible(new Rectangle(m_table.getCellRect(index, 0, true)));
            }
        }
    }

    /**
     * FocusAdapter that removes focus of a table if the other table is focused.
     *
     * @author Johannes Schweig
     */
    private class TableFocusAdapter extends FocusAdapter {
        private JTable m_table;

        /**
         * Initiates a TableFocusAdapter.
         *
         * @param table the table which focus is removed
         */
        TableFocusAdapter(final JTable table) {
            m_table = table;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void focusGained(final FocusEvent e) {
            // clears selection of one table if the other gains focus
            m_table.clearSelection();
        }
    }

    /**
     * KeyAdapter that updates the filter in the table if a new character is typed.
     *
     * @author Johannes Schweig
     */
    private class SearchKeyAdapter extends KeyAdapter {
        private JTable m_table;

        private JPanel m_cardsPanel;

        private TablePlaceholder m_tablePlaceholder;

        @SuppressWarnings("rawtypes")
        private TableRowSorter<NameFilterTableModel> m_tableRowSorter;

        private JTextField m_searchField;

        /**
         * Constructs a SearchKeyListener for a given table.
         *
         * @param table JTable that the search is filtering
         * @param cardsPanel JPanel with CardLayout holding the JTable and a TablePlaceholder
         * @param tablePlaceholder TablePlaceholder being displayed instead of the table
         * @param tableRowSorter
         */
        @SuppressWarnings("rawtypes")
        SearchKeyAdapter(final JTable table, final JPanel cardsPanel, final TablePlaceholder tablePlaceholder,
            final TableRowSorter<NameFilterTableModel> tableRowSorter, final JTextField searchField) {
            m_table = table;
            m_cardsPanel = cardsPanel;
            m_tablePlaceholder = tablePlaceholder;
            m_tableRowSorter = tableRowSorter;
            m_searchField = searchField;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyReleased(final KeyEvent e) {
            updateRowFilter(m_searchField.getText(), m_tableRowSorter);
            updateFilterView(m_table, m_cardsPanel, m_tablePlaceholder, m_searchField.getText());
        }
    }

    /**
     * FocusAdapter that updates the placeholder in the textfield on focus loss and gain.
     *
     * @author Johannes Schweig
     */
    private class SearchFocusAdapter extends FocusAdapter {
        private JTextField m_searchField;

        SearchFocusAdapter(final JTextField searchField) {
            m_searchField = searchField;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void focusGained(final FocusEvent e) {
            updateTextFieldPlaceholder(true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void focusLost(final FocusEvent e) {
            updateTextFieldPlaceholder(false);
        }

        /**
         * Shows or hides the placeholder text in the textfield
         *
         * @param focus true if focusGained, false if focusLost
         */
        private void updateTextFieldPlaceholder(final boolean focus) {
            String query = m_searchField.getText();
            if ((query.isEmpty() || query.equals(FILTER)) && focus) { //  no text or filter text and textfield gains focus -> hide placeholder
                m_searchField.setText("");
                m_searchField.setForeground(Color.BLACK);
                m_searchField.setFont(getFont().deriveFont(Font.PLAIN, 14f));
            } else if (query.isEmpty() && !focus) { // no text and textfield looses focus -> show placeholder
                m_searchField.setText(FILTER);
                m_searchField.setForeground(Color.GRAY);
                m_searchField.setFont(getFont().deriveFont(Font.ITALIC, 14f));
            }
        }

    }

    /**
     * ActionListener that triggers the moving of entries on button press.
     *
     * @author Johannes Schweig
     */
    private class MoveAllActionListener implements ActionListener {
        private JTable m_table;

        /**
         * Constructs a MoveAllActionListener for a table.
         *
         * @param table the table from which elements are moved
         * @param tableRowSorter the TableRowSorter filtering the elements in the table
         */
        MoveAllActionListener(final JTable table) {
            m_table = table;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            // if table is not filtered
            if (m_table.equals(m_inclTable)) {
                if (m_inclSorter.getRowFilter() == null) {
                    onRemAll();
                } else { //if table is filtered
                    int[] rows = IntStream.range(0, m_table.getRowCount()).toArray();
                    onRemIt(rows);
                }
            } else if (m_table.equals(m_exclTable)) {
                if (m_exclSorter.getRowFilter() == null) {
                    onAddAll(false);
                } else { //if table is filtered
                    int[] rows = IntStream.range(0, m_table.getRowCount()).toArray();
                    onAddIt(rows);
                }
            }
        }
    }

    /**
     * A border including an icon on the left surrounded with some padding
     *
     * @author Johannes Schweig
     */
    private class IconBorder extends AbstractBorder {

        // the icon to be drawn
        private Icon m_icon;
        // left and right padding of the image
        private final int PADDING = 2;

        IconBorder(final Icon icon) {
            m_icon = icon;

        }

        @Override
        public Insets getBorderInsets(final Component c, final Insets insets) {
            insets.left = m_icon.getIconWidth() + 2*PADDING;
            insets.right = insets.top = insets.bottom = 0;
            return insets;
        }

        @Override
        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
            m_icon.paintIcon(c, g, x + PADDING, y + (height - m_icon.getIconHeight()) / 2);
        }

    }

    /** Checkbox that enabled to keep all column in include list. */
    private final JCheckBox m_keepAllBox;

    /** List of DataCellColumnSpecss to keep initial ordering of DataCells. */
    private final LinkedHashSet<DataColumnSpec> m_order = new LinkedHashSet<DataColumnSpec>();

    private boolean m_showInvalidInclCols = false;

    private boolean m_showInvalidExclCols = false;

    /**
     * Class that filters all columns based on a given set of compatible <code>DataValue</code> classes.
     */
    public static class ValueClassFilter implements ColumnFilter {
        /**
         * Show only columns of types that are compatible to one of theses classes.
         */
        private final Class<? extends DataValue>[] m_filterClasses;

        /**
         * Creates a new value class filter.
         *
         * @param filterValueClasses all classes that are compatible with the type allowed in
         *            {@link #includeColumn(DataColumnSpec)}
         */
        @SuppressWarnings("unchecked")
        public ValueClassFilter(final Class<? extends DataValue>... filterValueClasses) {
            if (filterValueClasses == null || filterValueClasses.length == 0) {
                throw new NullPointerException("Classes must not be null");
            }
            final List<Class<? extends DataValue>> list = Arrays.asList(filterValueClasses);
            if (list.contains(null)) {
                throw new NullPointerException("List of value classes must not contain null elements.");
            }
            m_filterClasses = filterValueClasses;
        }

        /**
         * Checks if the given column type is included in the list of allowed types. If the list is empty, all types are
         * valid.
         *
         * @param cspec {@link ColumnFilterPanel} checked
         * @return true, if given column should be visible in column filter
         */
        @Override
        public final boolean includeColumn(final DataColumnSpec cspec) {
            for (final Class<? extends DataValue> cl : m_filterClasses) {
                if (cspec.getType().isCompatible(cl)) {
                    return true;
                }
            }
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public String allFilteredMsg() {
            return "No columns compatible with the specific column types.";
        }
    }

    /** The filter used to filter out/in valid column types. */
    private ColumnFilter m_filter;

    private final HashSet<DataColumnSpec> m_hideColumns = new HashSet<DataColumnSpec>();

    /**
     * Line border for include columns.
     */
    private static final Border INCLUDE_BORDER = BorderFactory.createLineBorder(new Color(0, 221, 0), 2);

    /**
     * Line border for exclude columns.
     */
    private static final Border EXCLUDE_BORDER = BorderFactory.createLineBorder(new Color(240, 0, 0), 2);

    private DataTableSpec m_spec;

    /**
     * Creates a new filter column panel with three component which are the include list, button panel to shift elements
     * between the two lists, and the exclude list. The include list then will contain all values to filter.
     *
     * @see #update(DataTableSpec, boolean, Collection)
     * @see #update(DataTableSpec, boolean, String[])
     *
     * @deprecated Use the constructor {@link #ColumnFilterPanel(boolean)} instead
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public ColumnFilterPanel() {
        this(DataValue.class);
    }

    /**
     * Creates a new filter column panel with three component which are the include list, button panel to shift elements
     * between the two lists, and the exclude list. The include list then will contain all values to filter.
     *
     * @param showKeepAllBox <code>true</code>, if an check box to keep all columns is shown
     *
     * @see #update(DataTableSpec, boolean, Collection)
     * @see #update(DataTableSpec, boolean, String[])
     */
    @SuppressWarnings("unchecked")
    public ColumnFilterPanel(final boolean showKeepAllBox) {
        this(showKeepAllBox, DataValue.class);
    }

    /**
     * Creates a new filter column panel with three component which are the include list, button panel to shift elements
     * between the two lists, and the exclude list. The include list then will contain all values to filter.
     *
     * @param filterValueClasses an array of type {@link DataValue} classes only allowed for selection. Will be check
     *            during update
     * @param showKeepAllBox true, if an check box to keep all columns is shown
     *
     * @see #update(DataTableSpec, boolean, Collection)
     * @see #update(DataTableSpec, boolean, String...)
     */
    @SuppressWarnings("unchecked")
    public ColumnFilterPanel(final boolean showKeepAllBox, final Class<? extends DataValue>... filterValueClasses) {
        this(showKeepAllBox, new ValueClassFilter(filterValueClasses));
    }

    /**
     * Creates a new filter column panel with three component which are the include list, button panel to shift elements
     * between the two lists, and the exclude list. The include list then will contain all values to filter.
     *
     * @param filterValueClasses an array of type {@link DataValue} classes only allowed for selection. Will be check
     *            during update
     *
     * @see #update(DataTableSpec, boolean, Collection)
     * @see #update(DataTableSpec, boolean, String...)
     *
     * @deprecated Use the constructor {@link #ColumnFilterPanel(boolean, Class...)} instead
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public ColumnFilterPanel(final Class<? extends DataValue>... filterValueClasses) {
        this(new ValueClassFilter(filterValueClasses));
    }

    /**
     * Creates a new filter column panel with three component which are the include list, button panel to shift elements
     * between the two lists, and the exclude list. The include list then will contain all values to filter.
     *
     * @param filter specifies valid column values. Will be check during update
     *
     * @see #update(DataTableSpec, boolean, Collection)
     * @see #update(DataTableSpec, boolean, String...)
     *
     * @deprecated Use the constructor {@link #ColumnFilterPanel(boolean, ColumnFilter)} instead
     */
    @Deprecated
    public ColumnFilterPanel(final ColumnFilter filter) {
        this(false, filter);
    }

    /**
     * Creates a new filter column panel with three component which are the include list, button panel to shift elements
     * between the two lists, and the exclude list. The include list then will contain all values to filter.
     *
     * @param showKeepAllBox true, if an check box to keep all columns is shown
     * @param filter specifies valid column values. Will be check during update
     *
     * @see #update(DataTableSpec, boolean, Collection)
     * @see #update(DataTableSpec, boolean, String...)
     */
    @SuppressWarnings("rawtypes")
    public ColumnFilterPanel(final boolean showKeepAllBox, final ColumnFilter filter) {
        m_filter = filter;
        // keeps buttons such add 'add', 'add all', 'remove', and 'remove all'
        final JPanel buttonPan = new JPanel();
        buttonPan.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        buttonPan.setLayout(new BoxLayout(buttonPan, BoxLayout.Y_AXIS));
        buttonPan.add(Box.createVerticalStrut(57));

        // JTables
        m_inclMdl = new NameFilterTableModel();
        m_inclTable = new JTable(m_inclMdl);
        m_exclMdl = new NameFilterTableModel();
        m_exclTable = new JTable(m_exclMdl);
        // include list
        // TableStringConverter
        TableStringConverter tableStringConverter = new TableStringConverter() {

            @Override
            public String toString(final TableModel model, final int row, final int column) {
                DataColumnSpec t = (DataColumnSpec)model.getValueAt(row, column);
                return t.getName();
            }
        };
        // include list
        m_inclTable.setShowGrid(false);
        m_inclTable.setFillsViewportHeight(true);
        m_inclTable.setTableHeader(null);
        m_inclTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_inclTable.setDefaultRenderer(Object.class, getTableCellRenderer());
        m_inclTable.addMouseListener(new DoubleClickMouseAdapter(m_inclTable));
        m_inclTable.addKeyListener(new TableKeyAdapter(m_inclTable));
        m_inclSorter = new TableRowSorter<NameFilterTableModel>(m_inclMdl);
        m_inclSorter.setStringConverter(tableStringConverter);
        m_inclTable.setRowSorter(m_inclSorter);
        m_inclTable.addFocusListener(new TableFocusAdapter(m_exclTable));
        final JScrollPane jspIncl = new JScrollPane(m_inclTable);
        jspIncl.setPreferredSize(new Dimension(250, 100));
        // setup cardlayout for display of placeholder on search returning no results
        m_inclCards = new JPanel(new CardLayout());
        m_inclCards.setBorder(new EmptyBorder(0, 8, 0, 8));
        m_inclTablePlaceholder = new TablePlaceholder("columns");
        m_inclCards.add(jspIncl, ID_CARDLAYOUT_LIST);
        m_inclCards.add(m_inclTablePlaceholder, ID_CARDLAYOUT_PLACEHOLDER);

        m_inclSearchField = new JTextField(FILTER, 8);
        m_inclSearchField.setPreferredSize(new Dimension(getPreferredSize().width, 24));
        m_inclSearchField.setBorder(new CompoundBorder(m_inclSearchField.getBorder(), new IconBorder(SharedIcons.FILTER.get())));
        m_inclSearchField.setForeground(Color.GRAY);
        m_inclSearchField.setFont(getFont().deriveFont(Font.ITALIC, 14f));
        m_inclSearchField.addKeyListener(
            new SearchKeyAdapter(m_inclTable, m_inclCards, m_inclTablePlaceholder, m_inclSorter, m_inclSearchField));
        m_inclSearchField.addFocusListener(new SearchFocusAdapter(m_inclSearchField));
        JPanel inclSearchPanel = new JPanel(new BorderLayout(8, 0));
        inclSearchPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        inclSearchPanel.add(m_inclSearchField, BorderLayout.CENTER);

        JPanel includePanel = new JPanel(new BorderLayout());
        m_includeBorder = BorderFactory.createTitledBorder(INCLUDE_BORDER, " Include ");
        includePanel.setBorder(m_includeBorder);
        includePanel.add(inclSearchPanel, BorderLayout.NORTH);
        includePanel.add(m_inclCards, BorderLayout.CENTER);

        // exclude list
        m_exclTable.setShowGrid(false);
        m_exclTable.setFillsViewportHeight(true);
        m_exclTable.setTableHeader(null);
        m_exclTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_exclTable.addMouseListener(new DoubleClickMouseAdapter(m_exclTable));
        m_exclTable.addKeyListener(new TableKeyAdapter(m_exclTable));
        // set renderer for items in the in- and exclude list
        m_inclTable.setDefaultRenderer(Object.class, getTableCellRenderer());
        m_exclTable.setDefaultRenderer(Object.class, getTableCellRenderer());
        m_exclSorter = new TableRowSorter<NameFilterTableModel>(m_exclMdl);
        m_exclSorter.setStringConverter(tableStringConverter);
        m_exclTable.setRowSorter(m_exclSorter);
        m_exclTable.addFocusListener(new TableFocusAdapter(m_inclTable));
        final JScrollPane jspExcl = new JScrollPane(m_exclTable);
        jspExcl.setPreferredSize(new Dimension(250, 100));
        // setup cardlayout for display of placeholder on search returning no results
        m_exclCards = new JPanel(new CardLayout());
        m_exclCards.setBorder(new EmptyBorder(0, 8, 0, 8));
        m_exclTablePlaceholder = new TablePlaceholder("columns");
        m_exclCards.add(jspExcl, ID_CARDLAYOUT_LIST);
        m_exclCards.add(m_exclTablePlaceholder, ID_CARDLAYOUT_PLACEHOLDER);

        m_exclSearchField = new JTextField(FILTER, 8);
        m_exclSearchField.setBorder(new CompoundBorder(m_exclSearchField.getBorder(), new IconBorder(SharedIcons.FILTER.get())));
        m_exclSearchField.setForeground(Color.GRAY);
        m_exclSearchField.setFont(getFont().deriveFont(Font.ITALIC, 14f));
        m_exclSearchField.addKeyListener(
            new SearchKeyAdapter(m_exclTable, m_exclCards, m_exclTablePlaceholder, m_exclSorter, m_exclSearchField));
        m_exclSearchField.addFocusListener(new SearchFocusAdapter(m_exclSearchField));

        JPanel exclSearchPanel = new JPanel(new BorderLayout(8, 0));
        exclSearchPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        exclSearchPanel.add(m_exclSearchField, BorderLayout.CENTER);

        JPanel excludePanel = new JPanel(new BorderLayout());
        m_excludeBorder = BorderFactory.createTitledBorder(EXCLUDE_BORDER, " Exclude ");
        excludePanel.setBorder(m_excludeBorder);
        excludePanel.add(exclSearchPanel, BorderLayout.NORTH);
        excludePanel.add(m_exclCards, BorderLayout.CENTER);

        // add 'keep all' checkbox
        if (showKeepAllBox) {
            m_keepAllBox = new JCheckBox("Always include all columns");
            m_keepAllBox.setToolTipText("If the set of input columns changes, " + "all columns stay included.");
            m_keepAllBox.addItemListener(new ItemListener() {
                /** {@inheritDoc} */
                @Override
                public void itemStateChanged(final ItemEvent ie) {
                    final boolean keepAll = m_keepAllBox.isSelected();
                    if (keepAll) {
                        onAddAll(false);
                    }
                    enabledComponents(!keepAll);
                }
            });
        } else {
            m_keepAllBox = null;
        }

        //buttons
        int width = 24;
        int height = width;
        int spacing = 12;
        m_addButton = new JButton(SharedIcons.ALT_ARROW_RIGHT.get());
        m_addButton.setMinimumSize(new Dimension(width, height));
        m_addButton.setToolTipText("Move the selected columns from the left to the right list.");
        buttonPan.add(m_addButton);
        m_addButton.addActionListener(e -> onAddIt(m_exclTable.getSelectedRows()));
        buttonPan.add(Box.createVerticalStrut(spacing));

        m_addAllButton = new JButton(SharedIcons.ALT_DOUBLE_ARROW_RIGHT.get());
        m_addAllButton.setMinimumSize(new Dimension(width, height));
        m_addAllButton.setToolTipText("Moves all visible columns from the left to the right list.");
        buttonPan.add(m_addAllButton);
        m_addAllButton.addActionListener(new MoveAllActionListener(m_exclTable));
        buttonPan.add(Box.createVerticalStrut(spacing));

        m_remButton = new JButton(SharedIcons.ALT_ARROW_LEFT.get());
        m_remButton.setMinimumSize(new Dimension(width, height));
        m_remButton.setToolTipText("Move the selected columns from the right to the left list.");
        buttonPan.add(m_remButton);
        m_remButton.addActionListener(e -> onRemIt(m_inclTable.getSelectedRows()));
        buttonPan.add(Box.createVerticalStrut(spacing));

        m_remAllButton = new JButton(SharedIcons.ALT_DOUBLE_ARROW_LEFT.get());
        m_remAllButton.setToolTipText("Moves all visible columns from the right to the left list.");
        m_remAllButton.setMinimumSize(new Dimension(width, height));
        buttonPan.add(m_remAllButton);
        m_remAllButton.addActionListener(new MoveAllActionListener(m_inclTable));
        buttonPan.add(Box.createVerticalStrut(20));
        buttonPan.add(Box.createGlue());

        // adds include, button, exclude component
        final JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        center.add(excludePanel);
        center.add(buttonPan);
        center.add(includePanel);
        final JPanel all = new JPanel();
        all.setLayout(new BoxLayout(all, BoxLayout.Y_AXIS));
        all.add(center);
        if (m_keepAllBox != null) {
            final JPanel keepAllPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            keepAllPanel.add(m_keepAllBox);
            all.add(keepAllPanel);
        }
        super.setLayout(new GridLayout(1, 1));
        super.add(all);
    }

    /**
     * Enables or disables all components on this panel. {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        if (m_keepAllBox != null) {
            m_keepAllBox.setEnabled(enabled);
        }
        final boolean newEnabled = enabled && !isKeepAllSelected();
        enabledComponents(newEnabled);
    }

    private void enabledComponents(final boolean newEnabled) {
        m_inclSearchField.setEnabled(newEnabled);
        m_exclSearchField.setEnabled(newEnabled);
        m_inclTable.setEnabled(newEnabled);
        m_exclTable.setEnabled(newEnabled);
        m_remAllButton.setEnabled(newEnabled);
        m_remButton.setEnabled(newEnabled);
        m_addAllButton.setEnabled(newEnabled);
        m_addButton.setEnabled(newEnabled);
        // change font color of jtable to gray when disabled
        if (newEnabled) {
            m_inclTable.setForeground(Color.BLACK);
            m_exclTable.setForeground(Color.BLACK);
        } else {
            m_inclTable.clearSelection();
            m_exclTable.clearSelection();
            m_inclTable.setForeground(Color.GRAY);
            m_exclTable.setForeground(Color.GRAY);
        }
    }

    /**
     * @param show set to <code>true</code> to show invalid exclude columns
     * @since 2.8
     */
    public void setShowInvalidExcludeColumns(final boolean show) {
        m_showInvalidExclCols = show;
    }

    /**
     * @param show set to <code>true</code> to show invalid include columns
     * @since 2.8
     */
    public void setShowInvalidIncludeColumns(final boolean show) {
        m_showInvalidInclCols = show;
    }

    /**
     * Adds a listener which gets informed whenever the column filtering changes.
     *
     * @param listener the listener
     */
    public void addChangeListener(final ChangeListener listener) {
        if (m_listeners == null) {
            m_listeners = new ArrayList<ChangeListener>();
        }
        m_listeners.add(listener);
    }

    /**
     * Removes the given listener from this filter column panel.
     *
     * @param listener the listener.
     */
    public void removeChangeListener(final ChangeListener listener) {
        if (m_listeners != null) {
            m_listeners.remove(listener);
        }
    }

    /**
     * Removes all column filter change listener.
     */
    public void removeAllColumnFilterChangeListener() {
        if (m_listeners != null) {
            m_listeners.clear();
        }
    }

    private void fireFilteringChangedEvent() {
        if (m_listeners != null) {
            for (final ChangeListener listener : m_listeners) {
                listener.stateChanged(new ChangeEvent(this));
            }
        }
    }

    /**
     * If the keep all box is visible and is selected.
     *
     * @return true, if keep all columns check box is selected, otherwise false
     */
    public final boolean isKeepAllSelected() {
        return m_keepAllBox != null && m_keepAllBox.isSelected();
    }

    /**
     * Sets a new selection for the keep all columns box.
     *
     * @param select true, if the box should be selected
     */
    public final void setKeepAllSelected(final boolean select) {
        if (m_keepAllBox != null) {
            m_keepAllBox.setSelected(select);
            enabledComponents(m_keepAllBox.isEnabled() && !select);
        }
    }

    /**
     * Called by the 'remove >>' button to exclude the selected elements from the include list.
     */
    @SuppressWarnings("unchecked")
    private void onRemIt(final int[] rows) {
        // add all selected elements from the include to the exclude list
        List<DataColumnSpec> o = new ArrayList<DataColumnSpec>();
        for (int i : rows) {
            o.add((DataColumnSpec)m_inclTable.getValueAt(i, 0));
        }
        final HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(o);
        List<DataColumnSpec> invalidSpecs = new LinkedList<DataColumnSpec>();
        for (Object obj : m_exclMdl) {
            DataColumnSpec spec = (DataColumnSpec)obj;
            if (m_showInvalidInclCols && DataColumnSpecListCellRenderer.isInvalid(spec)) {
                invalidSpecs.add(spec);
            }
            hash.add(spec);
        }
        boolean changed = false;
        for (DataColumnSpec spec : o) {
            changed |= m_inclMdl.remove(spec);
        }
        m_exclMdl.clear();
        for (final DataColumnSpec c : m_order) {
            if (hash.contains(c)) {
                m_exclMdl.add(c);
            }
        }
        if (!invalidSpecs.isEmpty()) {
            //add all remaining invalid specs at the end of the list
            for (DataColumnSpec invalidSpec : invalidSpecs) {
                m_exclMdl.add(invalidSpec);
            }
        }
        setKeepAllSelected(false);
        if (changed) {
            fireFilteringChangedEvent();
            updateFilterView(m_inclTable, m_inclCards, m_inclTablePlaceholder, m_inclSearchField.getText());
            updateFilterView(m_exclTable, m_exclCards, m_exclTablePlaceholder, m_exclSearchField.getText());
        }
    }

    /**
     * Called by the 'remove >>' button to exclude all elements from the include list.
     */
    @SuppressWarnings("unchecked")
    private void onRemAll() {
        boolean changed = !m_inclMdl.isEmpty();
        //perform the operation only if the exclude model contains at least one column to add
        final List<DataColumnSpec> invalidSpecs = new LinkedList<DataColumnSpec>();
        if (m_showInvalidExclCols) {
            for (Object obj : m_exclMdl) {
                final DataColumnSpec spec = (DataColumnSpec)obj;
                if (DataColumnSpecListCellRenderer.isInvalid(spec)) {
                    invalidSpecs.add(spec);
                }
            }
        }
        m_inclMdl.clear();
        m_exclMdl.clear();
        for (final DataColumnSpec c : m_order) {
            if (!m_hideColumns.contains(c)) {
                m_exclMdl.add(c);
            }
        }
        if (!invalidSpecs.isEmpty()) {
            //add all invalid specs at the end of the list
            for (DataColumnSpec invalidSpec : invalidSpecs) {
                m_exclMdl.add(invalidSpec);
            }
        }
        if (changed) {
            setKeepAllSelected(false);
            fireFilteringChangedEvent();
            updateFilterView(m_inclTable, m_inclCards, m_inclTablePlaceholder, m_inclSearchField.getText());
            updateFilterView(m_exclTable, m_exclCards, m_exclTablePlaceholder, m_exclSearchField.getText());
        }
    }

    /**
     * Called by the '<< add' button to include the selected elements from the exclude list.
     *
     * @param indices of rows to be added in table display order
     */
    @SuppressWarnings("unchecked")
    private void onAddIt(final int[] rows) {
        // add all selected elements from the exclude to the include list
        List<DataColumnSpec> o = new ArrayList<DataColumnSpec>();
        for (int i : rows) {
            o.add((DataColumnSpec)m_exclTable.getValueAt(i, 0));
        }
        final HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(o);
        List<DataColumnSpec> invalidSpecs = new LinkedList<DataColumnSpec>();
        for (Object obj : m_inclMdl) {
            final DataColumnSpec spec = (DataColumnSpec)obj;
            if (m_showInvalidInclCols && DataColumnSpecListCellRenderer.isInvalid(spec)) {
                invalidSpecs.add(spec);
            }
            hash.add(spec);
        }
        boolean changed = false;
        for (DataColumnSpec dcs : o) {
            changed |= m_exclMdl.remove(dcs);
        }
        m_inclMdl.clear();
        for (final DataColumnSpec c : m_order) {
            if (hash.contains(c)) {
                m_inclMdl.add(c);
            }
        }
        if (!invalidSpecs.isEmpty()) {
            //add all remaining invalid specs at the end of the list
            for (DataColumnSpec invalidSpec : invalidSpecs) {
                m_inclMdl.add(invalidSpec);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
            updateFilterView(m_inclTable, m_inclCards, m_inclTablePlaceholder, m_inclSearchField.getText());
            updateFilterView(m_exclTable, m_exclCards, m_exclTablePlaceholder, m_exclSearchField.getText());
        }
    }

    /**
     * Called by the '<< add all' button to include all elements from the exclude list.
     *
     * @param showInvalid <code>true</code> if the invalid columns should still be shown
     */
    @SuppressWarnings("unchecked")
    private void onAddAll(final boolean showInvalid) {
        boolean changed = !m_exclMdl.isEmpty();
        //perform the operation only if the exclude model contains at least one column to add
        final List<DataColumnSpec> invalidSpecs = new LinkedList<DataColumnSpec>();
        if (showInvalid && m_showInvalidInclCols) {
            for (Object obj : m_inclMdl) {
                final DataColumnSpec spec = (DataColumnSpec)obj;
                if (DataColumnSpecListCellRenderer.isInvalid(spec)) {
                    invalidSpecs.add(spec);
                }
            }
        }
        m_inclMdl.clear();
        m_exclMdl.clear();
        for (final DataColumnSpec c : m_order) {
            if (!m_hideColumns.contains(c)) {
                m_inclMdl.add(c);
            }
        }
        if (!invalidSpecs.isEmpty()) {
            //add all invalid specs at the end of the list
            for (DataColumnSpec invalidSpec : invalidSpecs) {
                m_inclMdl.add(invalidSpec);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
            updateFilterView(m_inclTable, m_inclCards, m_inclTablePlaceholder, m_inclSearchField.getText());
            updateFilterView(m_exclTable, m_exclCards, m_exclTablePlaceholder, m_exclSearchField.getText());
        }
    }

    /**
     * Updates this filter panel by removing all current selections from the include and exclude list. The include list
     * will contain all column names from the spec afterwards.
     *
     * @param spec the spec to retrieve the column names from
     * @param exclude the flag if <code>cells</code> contains the columns to exclude (otherwise include).
     * @param cells an array of data cells to either in- or exclude.
     */
    public void update(final DataTableSpec spec, final boolean exclude, final String... cells) {
        this.update(spec, exclude, Arrays.asList(cells));
    }

    /**
     * Updates this filter panel by removing all current selections from the include and exclude list. The include list
     * will contains all column names from the spec afterwards.
     *
     * @param spec the spec to retrieve the column names from
     * @param exclude the flag if <code>list</code> contains the columns to exclude otherwise include
     * @param list the list of columns to exclude or include
     */
    @SuppressWarnings("unchecked")
    public void update(final DataTableSpec spec, final boolean exclude, final Collection<String> list) {
        assert (spec != null && list != null);
        m_spec = spec;
        m_order.clear();
        m_inclMdl.clear();
        m_exclMdl.clear();
        m_hideColumns.clear();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            final DataColumnSpec cSpec = spec.getColumnSpec(i);
            if (!m_filter.includeColumn(cSpec)) {
                continue;
            }
            final String c = cSpec.getName();
            m_order.add(cSpec);
            if (isKeepAllSelected()) {
                m_inclMdl.add(cSpec);
            } else {
                if (exclude) {
                    if (list.contains(c)) {
                        m_exclMdl.add(cSpec);
                    } else {
                        m_inclMdl.add(cSpec);
                    }
                } else {
                    if (list.contains(c)) {
                        m_inclMdl.add(cSpec);
                    } else {
                        m_exclMdl.add(cSpec);
                    }
                }
            }
        }
        repaint();
    }

    /**
     * Updates this filter panel by removing all current selections from the include and exclude list. The include list
     * will contains all column names from the spec afterwards.
     *
     * @param spec the spec to retrieve the column names from
     * @param inclList the list of columns to include
     * @param exclList the list of columns to exclude
     * @since 2.8
     */
    public void update(final DataTableSpec spec, final Collection<String> inclList, final Collection<String> exclList) {
        update(spec, inclList, exclList, true);
    }

    /**
     * Updates this filter panel by removing all current selections from the include and exclude list. The include list
     * will contains all column names from the spec afterwards.
     *
     * @param spec the spec to retrieve the column names from
     * @param inclList the list of columns to include
     * @param exclList the list of columns to exclude
     * @param inclUnknown <code>true</code> if unknown columns should be added to the include list of the component
     *            otherwise they are added to the exclude list
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    public void update(final DataTableSpec spec, final Collection<String> inclList, final Collection<String> exclList,
        final boolean inclUnknown) {
        assert (spec != null && inclList != null && exclList != null);
        m_spec = spec;
        m_order.clear();
        final Set<String> exclCols = new HashSet<String>(exclList);
        final Set<String> inclCols = new HashSet<String>(inclList);
        m_inclMdl.clear();
        m_exclMdl.clear();
        m_hideColumns.clear();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            final DataColumnSpec cSpec = spec.getColumnSpec(i);
            if (!m_filter.includeColumn(cSpec)) {
                continue;
            }
            final String c = cSpec.getName();
            m_order.add(cSpec);
            if (isKeepAllSelected()) {
                m_inclMdl.add(cSpec);
                inclCols.remove(c);
            } else {
                if (exclCols.remove(c)) {
                    m_exclMdl.add(cSpec);
                } else if (inclCols.remove(c)) {
                    m_inclMdl.add(cSpec);
                } else if (inclUnknown) {
                    m_inclMdl.add(cSpec);
                } else {
                    m_exclMdl.add(cSpec);
                }
            }
        }
        addInvalidSpecs(m_showInvalidExclCols, spec, exclCols, m_exclMdl);
        addInvalidSpecs(m_showInvalidInclCols, spec, inclCols, m_inclMdl);
        repaint();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addInvalidSpecs(final boolean showInvalidCols, final DataTableSpec spec,
        final Set<String> colNames, final NameFilterTableModel model) {
        if (showInvalidCols && !colNames.isEmpty()) {
            for (String colName : colNames) {
                final DataColumnSpec invalidSpec = DataColumnSpecListCellRenderer.createInvalidSpec(colName);
                model.add(invalidSpec);
            }
        }
    }

    /**
     * Returns all columns from the exclude list.
     *
     * @return a set of all columns from the exclude list
     * @deprecated Use {@link #getExcludedColumnSet()} instead
     */
    @Deprecated
    public Set<String> getExcludedColumnList() {
        return getExcludedColumnSet();
    }

    /**
     * Returns all columns from the exclude list.
     *
     * @return a set of all columns from the exclude list
     */
    public Set<String> getExcludedColumnSet() {
        return getColumnList(m_exclMdl, false);
    }

    /**
     * Returns all invalid columns from the exclude list.
     *
     * @return a set of all invalid columns from the exclude list
     * @since 2.8
     */
    public Set<String> getInvalidExcludeColumnSet() {
        return getColumnList(m_exclMdl, true);
    }

    /**
     * Returns all columns from the include list.
     *
     * @return a list of all columns from the include list
     * @deprecated Use {@link #getIncludedColumnSet()} instead
     */
    @Deprecated
    public Set<String> getIncludedColumnList() {
        return getIncludedColumnSet();
    }

    /**
     * Returns all columns from the include list.
     *
     * @return a list of all columns from the include list
     */
    public Set<String> getIncludedColumnSet() {
        return getColumnList(m_inclMdl, false);
    }

    /**
     * Returns all invalid columns from the include list.
     *
     * @return a list of all invalid columns from the include list
     * @since 2.8
     */
    public Set<String> getInvalidIncludedColumnSet() {
        return getColumnList(m_inclMdl, true);
    }

    /**
     * Returns the data type for the given cell retrieving it from the initial {@link DataTableSpec}. If this name could
     * not found, return <code>null</code>.
     *
     * @param name the column name to get the data type for
     * @return the data type or <code>null</code>
     */
    public DataType getType(final String name) {
        for (final DataColumnSpec spec : m_order) {
            if (spec.getName().equals(name)) {
                return spec.getType();
            }
        }
        return null;
    }

    /**
     * Set the renderer that is used for both list in this panel.
     *
     * @param renderer the new renderer being used
     * @see JList#setCellRenderer(javax.swing.ListCellRenderer)
     * @deprecated JLists were replaced by JTables.
     */
    @SuppressWarnings("rawtypes")
    @Deprecated
    protected final void setListCellRenderer(final ListCellRenderer renderer) {
        //no-op
    }

    /**
     * Removes the given columns form either include or exclude list and notifies all listeners. Does not throw an
     * exception if the argument contains <code>null</code> elements or is not contained in any of the lists.
     *
     * @param columns the columns to remove
     */
    public final void hideColumns(final DataColumnSpec... columns) {
        boolean changed = false;
        for (final DataColumnSpec column : columns) {
            if (m_inclMdl.contains(column)) {
                m_hideColumns.add(column);
                changed |= m_inclMdl.remove(column);
            } else if (m_exclMdl.contains(column)) {
                m_hideColumns.add(column);
                changed |= m_exclMdl.remove(column);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }

    /**
     * Re-adds all remove/hidden columns to the exclude list.
     */
    @SuppressWarnings("unchecked")
    public final void resetHiding() {
        if (m_hideColumns.isEmpty()) {
            return;
        }
        // add all selected elements from the include to the exclude list
        final HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(m_hideColumns);
        for (Object obj : m_exclMdl) {
            hash.add(obj);
        }
        m_exclMdl.clear();
        for (final DataColumnSpec c : m_order) {
            if (hash.contains(c)) {
                m_exclMdl.add(c);
            }
        }
        m_hideColumns.clear();
    }

    /**
     * Sets the title of the include panel.
     *
     * @param title the new title
     */
    public final void setIncludeTitle(final String title) {
        m_includeBorder.setTitle(title);
    }

    /**
     * Sets the title of the exclude panel.
     *
     * @param title the new title
     */
    public final void setExcludeTitle(final String title) {
        m_excludeBorder.setTitle(title);
    }

    /**
     * Setter for the original "Remove All" button.
     *
     * @param text the new button title
     */
    public void setRemoveAllButtonText(final String text) {
        m_remAllButton.setText(text);
    }

    /**
     * Setter for the original "Add All" button.
     *
     * @param text the new button title
     */
    public void setAddAllButtonText(final String text) {
        m_addAllButton.setText(text);
    }

    /**
     * Setter for the original "Remove" button.
     *
     * @param text the new button title
     */
    public void setRemoveButtonText(final String text) {
        m_remButton.setText(text);
    }

    /**
     * Setter for the original "Add" button.
     *
     * @param text the new button title
     */
    public void setAddButtonText(final String text) {
        m_addButton.setText(text);
    }

    /**
     * Sets the internal used {@link ColumnFilter} to the given one and calls the
     * {@link #update(DataTableSpec, boolean, Collection)} method to update the column panel.
     *
     * @param filter the new {@link ColumnFilter} to use
     */
    public void setColumnFilter(final ColumnFilter filter) {
        m_filter = filter;
        if (m_spec == null) {
            //the spec is not available that's why we do not need to call
            //the update method
            return;
        }
        update(m_spec, true, getExcludedColumnSet());
    }

    /**
     * Displays a JTable if there is something to display, otherwise displays a TablePlaceholder.
     *
     * @param table the JTable being displayed instead of the TablePlaceholder
     * @param cardsPanel the JPanel holding the cards
     * @param tablePlaceholder the TablePlaceholder being displayed instead of the table
     * @param searchQuery the entered search query
     */
    private void updateFilterView(final JTable table, final JPanel cardsPanel, final TablePlaceholder tablePlaceholder,
        final String searchQuery) {
        CardLayout cl = (CardLayout)cardsPanel.getLayout();
        // if there are no entries in the list
        if (table.getModel().getRowCount() == 0) {
            tablePlaceholder.updateTextEmpty();
            cl.show(cardsPanel, ID_CARDLAYOUT_PLACEHOLDER);
        } else {
            // nothing found
            if (table.getRowCount() == 0) {
                tablePlaceholder.updateTextNothingFound(searchQuery, table.getModel().getRowCount());
                cl.show(cardsPanel, ID_CARDLAYOUT_PLACEHOLDER);
            } else {
                cl.show(cardsPanel, ID_CARDLAYOUT_LIST);
            }
        }
        // Resize table to fill the viewport or match the largest cell (if larger than viewport)
        TableColumn tableColumn = table.getColumnModel().getColumn(0);
        double maxCellWidth = 0;
        for (int row = 0; row < table.getRowCount(); row++) {
            Component c = getTableCellRenderer().getTableCellRendererComponent(table,
                table.getModel().getValueAt(row, 0), false, false, row, 0);
            double width = c.getPreferredSize().getWidth() + 8;
            // width of the largest cell
            maxCellWidth = Math.max(width, maxCellWidth);
        }
        // set width to either fill the viewport or to largest cell
        int scrollPaneWidth = table.getParent().getWidth();
        // FIXME width is 0 when dialog is first shown
        scrollPaneWidth = scrollPaneWidth == 0 ? 250 : scrollPaneWidth;

        // There is enough space for the cells to be fully displayed, column should fit available space
        if (scrollPaneWidth > maxCellWidth) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        } else { // cells are larger than the available space, set column width manually
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            tableColumn.setPreferredWidth(Math.max(scrollPaneWidth, (int)maxCellWidth));
        }
    }

    /**
     * @return a table cell renderer from items to be renderer in the filer
     * @since 3.6
     */
    private TableCellRenderer getTableCellRenderer() {
        return new DataColumnSpecTableCellRenderer();
    }

    /**
     * Updates the rowfilter of the corresponding table with a new query. If the query is empty, the rowfilter will be
     * set to null.
     *
     * @param table
     */
    @SuppressWarnings("rawtypes")
    private void updateRowFilter(final String searchQuery, final TableRowSorter<NameFilterTableModel> tableRowSorter) {
        // RowFilter is set to null if search field is empty
        RowFilter<NameFilterTableModel, Object> rf = null;
        if (!searchQuery.isEmpty()) {
            try {
                // by default perform case insensitive search
                // escape all regex characters [\^$.|?*+()
                rf = RowFilter.regexFilter("(?i)" + Pattern.quote(searchQuery));
            } catch (java.util.regex.PatternSyntaxException p) {
                return;
            }
        }
        tableRowSorter.setRowFilter(rf);
    }

    /**
     * Creates a resource URL for a file in the class's path
     *
     * @param file
     * @return resource URL
     */
    private URL getResourceUrl(final String file) {
        Package pack = NameFilterPanel.class.getPackage();
        String iconBase = pack.getName().replace(".", "/") + "/";
        return this.getClass().getClassLoader().getResource(iconBase + file);
    }

    /**
     * @param model The list from which to retrieve the elements
     * @param invalid if set to <code>true</code> only the invalid columns are retrieved if set to <code>false</code>
     *            only the valid columns are retrieved
     * @return a list of valid columns
     */
    @SuppressWarnings("rawtypes")
    private Set<String> getColumnList(final NameFilterTableModel model, final boolean invalid) {
        final Set<String> list = new LinkedHashSet<String>();
        for (int i = 0; i < model.getSize(); i++) {
            Object o = model.getElementAt(i);
            final DataColumnSpec spec = (DataColumnSpec)o;
            if (invalid == DataColumnSpecListCellRenderer.isInvalid(spec)) {
                String cell = spec.getName();
                list.add(cell);
            }
        }
        return list;
    }
}
