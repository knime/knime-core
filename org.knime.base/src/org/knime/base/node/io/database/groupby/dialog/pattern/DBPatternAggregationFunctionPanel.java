/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 */
package org.knime.base.node.io.database.groupby.dialog.pattern;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.dialogutil.AbstractAggregationPanel;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionAndRowTableCellRenderer;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRowTableCellRenderer;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRowTableCellRenderer.ValueRenderer;
import org.knime.base.data.aggregation.dialogutil.BooleanCellRenderer;
import org.knime.base.node.io.database.groupby.dialog.DBAggregationFunctionProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.database.DatabaseUtility;
import org.knime.core.node.port.database.aggregation.AggregationFunctionProvider;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;


/**
 * This class creates the aggregation column panel that allows the user to
 * define the aggregation columns and their aggregation method.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class DBPatternAggregationFunctionPanel extends AbstractAggregationPanel<DBPatternAggregationFunctionTableModel,
DBPatternAggregationFunctionRow, String> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DBPatternAggregationFunctionPanel.class);

    private static final int REGEX_SIZE = 45;

    /**This field holds all columns of the input table.*/
    private final List<DataColumnSpec> m_avAggrColSpecs = new LinkedList<>();
    private final String m_key;

    private DBPatternAggregationFunctionRowTableCellEditor m_aggregationFunctionCellEditor;

    /**
     * {@inheritDoc}
     */
    @Override
    protected JPopupMenu createTablePopupMenu() {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem invalidRowsMenu = createInvalidRowsSelectionMenu();
        if (invalidRowsMenu != null) {
            menu.add(invalidRowsMenu);
            menu.addSeparator();
        }
        createColumnSelectionMenu(menu);
        menu.addSeparator();
        createAggregationSection(menu);
        return menu;
    }

    /**
     * Adds the column selection section to the given menu.
     * This section allows the user to select all rows that are compatible
     * to the chosen data type at once.
     * @param menu the menu to append the column selection section
     */
    private void createColumnSelectionMenu(final JPopupMenu menu) {
        //add the select all columns entry
        final JMenuItem selectAll = new JMenuItem("Select all columns");
        selectAll.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(final ActionEvent e) {
                getTable().selectAll();
            }
        });
        menu.add(selectAll);
    }

    /**
     * Adds the aggregation method section to the given menu.
     * This section allows the user to set an aggregation method for
     * all selected columns.
     * @param menu the menu to append the aggregation section
     */
    private void createAggregationSection(final JPopupMenu menu) {
        if (getSelectedRows().length <= 0) {
                final JMenuItem noneSelected = new JMenuItem("Select a column to change method");
                noneSelected.setEnabled(false);
                menu.add(noneSelected);
                return;
        }
        final List<DBAggregationFunction> methodList =
                getTableModel().getAggregationFunctionProvider().getFunctions(true);
        //we need no sub menu for a single group
        for (final DBAggregationFunction method : methodList) {
            final JMenuItem methodItem = new JMenuItem(method.getLabel());
            methodItem.setToolTipText(method.getDescription());
            methodItem.addActionListener(new ActionListener() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    changeAggregationMethod(method.getId());
                }
            });
            menu.add(methodItem);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Component createListComponent(final String title) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Component createButtonComponent() {
        final JButton addRegexButton = new JButton("Add");
        addRegexButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final List<DBPatternAggregationFunctionRow> rows = new LinkedList<>();
                rows.add(new DBPatternAggregationFunctionRow(".*", true,
                    getTableModel().getAggregationFunctionProvider().getDefaultFunction(StringCell.TYPE)));
                addRows(rows);
            }
        });
        final JButton removeSelectedButton = new JButton("Remove");
        removeSelectedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onRemIt();
            }
        });
        final JButton removeAllButton = new JButton("Remove all");
        removeAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onRemAll();
            }
        });
        final int width = Math.max(Math.max(addRegexButton.getPreferredSize().width,
            removeSelectedButton.getPreferredSize().width), removeAllButton.getPreferredSize().width);
        final Dimension dimension = new Dimension(width, 150);
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(dimension);
        panel.setMaximumSize(dimension);
        panel.setMinimumSize(dimension);
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.PAGE_START;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(addRegexButton, c);
        c.gridy++;
        c.weighty = 1;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(new JLabel(), c);
        c.weighty = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.gridy++;
        panel.add(removeSelectedButton, c);
        c.gridy++;
        c.weighty = 1;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(new JLabel(), c);
        c.weighty = 0;
        c.anchor = GridBagConstraints.PAGE_END;
        c.gridy++;
        panel.add(removeAllButton, c);
        return panel;
    }

    /**Constructor for class AggregationColumnPanel.
     * @param key the unique settings key
     *
     */
    public DBPatternAggregationFunctionPanel(final String key) {
        this(" Aggregation settings ", key);
    }

   /**
     * @param title the title of the border or <code>null</code> for no border
     * @param key the unique settings key
     */
    public DBPatternAggregationFunctionPanel(final String title, final String key) {
       super(title, " Available columns ", new DataColumnSpecListCellRenderer(),
           " To change multiple columns use right mouse click for context menu. ",
           new DBPatternAggregationFunctionTableModel());
       if (key == null) {
           throw new IllegalArgumentException("key must not be null");
       }
        m_key = key;
        getTableModel().setRootPanel(getComponentPanel());
   }

    /**
     * @param provider the {@link AggregationFunctionProvider}
     */
    public void setAggregationFunctionProvider(final AggregationFunctionProvider<DBAggregationFunction> provider) {
        getTableModel().setAggregationFunctionProvider(provider);
        m_aggregationFunctionCellEditor.setAggregationFunctionProvider(provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adaptTableColumnModel(final TableColumnModel columnModel) {
        columnModel.getColumn(0).setCellRenderer(
                new AggregationFunctionRowTableCellRenderer<>(new ValueRenderer<DBPatternAggregationFunctionRow>() {
                    @Override
                    public void renderComponent(final DefaultTableCellRenderer c,
                        final DBPatternAggregationFunctionRow row) {
                        c.setText(row.getInputPattern());
                    }
                }, true, "Double click to remove column. Right mouse click for context menu."));
        columnModel.getColumn(0).setCellEditor(new DBPatternTableCellEditor());
        columnModel.getColumn(1).setCellRenderer(
            new BooleanCellRenderer("Tick if the pattern is a regular expression"));
        columnModel.getColumn(1).setMinWidth(REGEX_SIZE);
        columnModel.getColumn(1).setMaxWidth(REGEX_SIZE);
        m_aggregationFunctionCellEditor = new DBPatternAggregationFunctionRowTableCellEditor(null);
        columnModel.getColumn(2).setCellEditor(m_aggregationFunctionCellEditor);
        columnModel.getColumn(2).setCellRenderer(new AggregationFunctionAndRowTableCellRenderer());
        columnModel.getColumn(0).setPreferredWidth(250);
        columnModel.getColumn(1).setPreferredWidth(150);
    }

    /**
     * Changes the aggregation method of all selected rows to the method
     * with the given label.
     * @param methodId the label of the aggregation method
     */
    protected void changeAggregationMethod(final String methodId) {
        final int[] selectedRows = getSelectedRows();
        AggregationFunctionProvider<DBAggregationFunction> provider = getTableModel().getAggregationFunctionProvider();
        if (provider != null) {
            getTableModel().setAggregationFunction(selectedRows, provider.getFunction(methodId));
            final Collection<Integer> idxs = new LinkedList<>();
            for (final int i : selectedRows) {
                idxs.add(Integer.valueOf(i));
            }
            updateSelection(idxs);
        } else {
            LOGGER.error("Aggregation function provider shouldn not be null");
        }
    }

    /**
     * Selects all rows that are compatible with the given type.
     * @param type the type to check for compatibility
     */
    protected void selectCompatibleRows(final DataType type) {
        final Collection<Integer> idxs = getTableModel().getCompatibleRowIdxs(type);
        updateSelection(idxs);
    }

    /**
     * Returns the number of rows that are compatible to the given type.
     * @param type the type to check for
     * @return the number of compatible rows
     */
    int noOfCompatibleRows(final DataType type) {
        return getTableModel().getCompatibleRowIdxs(type).size();
    }

    /**
     * @param settings the settings object to write to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        DBPatternAggregationFunctionRow.saveFunctions(settings, m_key, getTableModel().getRows());
    }

    /**
     * @param settings the settings to read from
     * @param dbIdentifier the database identifier
     * @param spec initializes the component
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final String dbIdentifier, final DataTableSpec spec)
                throws InvalidSettingsException {
        final DatabaseUtility utility = DatabaseUtility.getUtility(dbIdentifier);
        setAggregationFunctionProvider(new DBAggregationFunctionProvider(utility));
        initialize(spec, DBPatternAggregationFunctionRow.loadFunctions(settings, m_key, dbIdentifier, spec));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws InvalidSettingsException {
        final List<DBPatternAggregationFunctionRow> rows = getTableModel().getRows();
        for (final DBPatternAggregationFunctionRow row : rows) {
            DBAggregationFunction function = row.getFunction();
            try {
                function.validate();
            } catch (InvalidSettingsException e) {
                throw new InvalidSettingsException("Exception for pattern '" + row.getInputPattern()
                    + "': " + e.getMessage());
            }
            if (!row.isValid()) {
                throw new InvalidSettingsException("Row for pattern '" + row.getInputPattern() + "' is invalid.");
            }
        }

    }

    /**
     * Initializes the panel.
     * @param spec the {@link DataTableSpec} of the input table
     * @param colAggrs the {@link List} of {@link ColumnAggregator}s that are
     * initially used
     */
    public void initialize(final DataTableSpec spec, final List<DBPatternAggregationFunctionRow> colAggrs) {
        m_avAggrColSpecs.clear();
      //remove all invalid column aggregator
        final List<DBPatternAggregationFunctionRow> colAggrs2Use = new ArrayList<>(colAggrs.size());
        for (final DBPatternAggregationFunctionRow colAggr : colAggrs) {
            if (colAggr.isValid()) {
                colAggrs2Use.add(colAggr);
            }
        }
        initialize(Collections.EMPTY_LIST, colAggrs2Use, spec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DBPatternAggregationFunctionRow createRow(final String pattern) {
        final AggregationFunctionProvider<DBAggregationFunction> provider =
                getTableModel().getAggregationFunctionProvider();
        final DBAggregationFunction defaultFunction = provider.getDefaultFunction(StringCell.TYPE);
        return new DBPatternAggregationFunctionRow(".*", true, defaultFunction);
    }
}
