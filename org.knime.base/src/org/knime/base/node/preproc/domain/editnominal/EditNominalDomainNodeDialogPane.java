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
 * ---------------------------------------------------------------------
 *
 * Created on Mar 17, 2013 by wiswedel
 */
package org.knime.base.node.preproc.domain.editnominal;

import static org.knime.core.node.util.DataColumnSpecListCellRenderer.isInvalid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.renderer.DefaultDataValueRenderer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfigurationRequestEvent;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfigurationRequestListener;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfiguredColumnDeterminer;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ListModifier;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.SearchedItemsSelectionMode;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * Dialog to node.
 *
 * @author Marcel Hanser
 */
final class EditNominalDomainNodeDialogPane extends NodeDialogPane {

    private static final String IGNORE_COLUMNS_NOT_PRESENT_IN_DATA = "If a configured columns is not present in data";

    private static final String IGNORE_COLUMNS_NOT_MATCHING_TYPES = "If a configured columns has an incompatible type";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(EditNominalDomainNodeDialogPane.class);

    private static final ColumnFilter STRING_VALUE_FILTER = new ColumnFilter() {

        @Override
        public boolean includeColumn(final DataColumnSpec name) {
            return StringCell.TYPE.equals(name.getType());
        }

        @Override
        public String allFilteredMsg() {
            return "";
        }
    };

    /**
     * The list of all columns.
     */
    private ColumnSelectionSearchableListPanel m_searchableListPanel;

    /**
     * The list of (maybe resorted) data cells.
     */
    private JList m_jlist;

    /**
     * The data model behind the {@link #m_jlist}.
     */
    private ListListModel<DataCell> m_currentSorting;

    /**
     * The table spec of the input table.
     */
    private DataTableSpec m_orgSpec;

    /**
     * The data column spec of the current selected column for resorting.
     */
    private DataColumnSpec m_currentColSpec;

    /**
     * The possible domain values of the current selected column. (Cached to fill them with an empty list on default)
     */
    private Set<DataCell> m_orgDomainCells;

    /**
     * True if anything changed on the sorting during the current column was selected.
     */
    private boolean m_somethingChanged = false;

    /**
     * The current configuration.
     */
    private EditNominalDomainConfiguration m_configuration;

    private JButton m_resetButton;

    private Map<String, JRadioButton> m_buttonMap = new HashMap<String, JRadioButton>();

    private ListModifier m_searchableListModifier;

    /** Inits members, does nothing else. */
    public EditNominalDomainNodeDialogPane() {
        createEditNominalDomainTab();
    }

    private void createEditNominalDomainTab() {

        m_currentSorting = new ListListModel<DataCell>();
        m_searchableListPanel =
            new ColumnSelectionSearchableListPanel(SearchedItemsSelectionMode.SELECT_FIRST,
                new ConfiguredColumnDeterminer() {

                    @Override
                    public boolean isConfiguredColumn(final DataColumnSpec spec) {
                        return checkConfigured(spec);
                    }
                });

        m_searchableListPanel.addConfigurationRequestListener(new ConfigurationRequestListener() {

            @Override
            public void configurationRequested(final ConfigurationRequestEvent searchEvent) {
                switch (searchEvent.getType()) {
                    case DELETION:
                        DataColumnSpec selectedColumn = m_searchableListPanel.getSelectedColumn();
                        if (checkConfigured(selectedColumn)) {
                            removeConfiguration();
                        }
                        break;
                    case SELECTION:
                        DataColumnSpec newSpec = m_searchableListPanel.getSelectedColumn();

                        if (newSpec != null && newSpec != m_currentColSpec) {

                            storeCurrentList();

                            m_currentSorting.clear();

                            m_currentColSpec = newSpec;

                            addDataCellElements(m_currentColSpec);

                            if (isInvalid(m_currentColSpec)) {
                                m_resetButton.setText("Delete");
                            } else {
                                m_resetButton.setText("Reset");
                            }

                            m_somethingChanged = false;
                        }
                    default:
                        break;
                }
            }
        });

        // if something is changed in the currentSorting we set the save flag, which leads wo a storage
        m_currentSorting.addListDataListener(new ListDataListener() {

            @Override
            public void intervalRemoved(final ListDataEvent e) {
                m_somethingChanged = true;
            }

            @Override
            public void intervalAdded(final ListDataEvent e) {
                m_somethingChanged = true;
            }

            @Override
            public void contentsChanged(final ListDataEvent e) {
                m_somethingChanged = true;
            }
        });

        JScrollPane scrollPane = new JScrollPane();
        m_jlist = new JList(m_currentSorting);
        scrollPane.setViewportView(m_jlist);
        m_jlist.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        m_jlist.setCellRenderer(new HighlightSpecialCellsRenderer());
        m_jlist.setBorder(BorderFactory.createTitledBorder("Domain values"));

        Box buttonBox = Box.createVerticalBox();
        buttonBox.setBorder(new TitledBorder("Actions"));

        Component moveFirstButton = createButton("Move First", moveFirst());

        Dimension size = moveFirstButton.getPreferredSize();
        //  all buttons should have the same size
        buttonBox.add(createButton("Add", addStringCell(), size));
        buttonBox.add(createButton("Remove", removeStringCells(), size));
        buttonBox.add(createButton("A-Z", sortNames(SortOption.ASCENDING), size));
        buttonBox.add(createButton("Z-A", sortNames(SortOption.DESCENDING), size));
        buttonBox.add(moveFirstButton);
        buttonBox.add(createButton("Move Last", moveLast(), size));
        buttonBox.add(createButton("Up", moveUp(), size));
        buttonBox.add(createButton("Down", moveDown(), size));
        m_resetButton = createButton("Reset", reset(), size);
        buttonBox.add(m_resetButton);

        buttonBox.add(createLegend());
        //
        JPanel tabpanel = new JPanel(new BorderLayout());
        tabpanel.add(m_searchableListPanel, BorderLayout.WEST);

        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.add(scrollPane, BorderLayout.CENTER);
        jPanel.add(buttonBox, BorderLayout.EAST);

        JPanel radioGroups = new JPanel();
        radioGroups.setLayout(new BorderLayout());
        radioGroups.add(createRadioButtonGroup(IGNORE_COLUMNS_NOT_PRESENT_IN_DATA), BorderLayout.CENTER);
        radioGroups.add(createRadioButtonGroup(IGNORE_COLUMNS_NOT_MATCHING_TYPES), BorderLayout.SOUTH);

        tabpanel.add(radioGroups, BorderLayout.SOUTH);

        tabpanel.add(jPanel);

        addTab("Edit Domain Values", tabpanel);
    }

    /**
     * @return
     */
    private ActionListener reset() {
        return new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                removeConfiguration();
            }
        };
    }

    /**
     *
     */
    private void removeConfiguration() {
        if (m_currentColSpec != null) {
            m_currentSorting.clear();
            m_configuration.removeSorting(m_currentColSpec.getName());

            if (m_searchableListPanel.isAdditionalColumn(m_currentColSpec)) {
                m_searchableListModifier.removeAdditionalColumn(m_currentColSpec.getName());
                m_currentColSpec = null;
            } else {
                Set<DataCell> values = m_currentColSpec.getDomain().getValues();
                m_currentSorting.addAll(values == null ? Collections.<DataCell> emptySet() : values);
                m_currentSorting.add(EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL);
            }
            m_searchableListPanel.repaint();

            // Reset also removed incoming chages
            m_somethingChanged = false;
        }
    }

    /**
     * @return
     */
    private ActionListener moveDown() {
        return new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                int[] selectedIndices = m_jlist.getSelectedIndices();
                if (selectedIndices.length > 0) {
                    boolean overflows = (selectedIndices[selectedIndices.length - 1] == m_currentSorting.size() - 1);
                    moveItems(selectedIndices, 1, overflows, 0, true);
                }
            }
        };
    }

    /**
     * @return
     */
    private ActionListener moveUp() {
        return new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                int[] selectedIndices = m_jlist.getSelectedIndices();
                if (selectedIndices.length > 0) {
                    boolean overflows = (selectedIndices[0] == 0);
                    int overflowOffset = m_currentSorting.size() - selectedIndices.length;
                    moveItems(selectedIndices, -1, overflows, overflowOffset, false);
                }
            }
        };
    }

    /**
     * @param selectedIndices
     * @param step
     * @param overflows
     * @param boundOffset
     */
    private void moveItems(final int[] selectedIndices, final int step, //
        final boolean overflows, final int boundOffset, final boolean addToVisibleIndices) {

        TreeMap<Integer, DataCell> toAdd = new TreeMap<Integer, DataCell>();

        int offset = 0;
        for (int i : selectedIndices) {
            toAdd.put(i, m_currentSorting.remove(i - offset++));
        }

        if (!overflows) {
            for (Map.Entry<Integer, DataCell> entry : toAdd.entrySet()) {
                m_currentSorting.add(entry.getKey() + step, entry.getValue());
            }
            m_jlist.setSelectedIndices(addOffset(selectedIndices, step));
            m_jlist.ensureIndexIsVisible(selectedIndices[addToVisibleIndices ? selectedIndices.length - 1 : 0]);
        } else {
            m_currentSorting.addAll(boundOffset, toAdd.values());
            m_jlist.setSelectedIndices(createAscendingIntArray(boundOffset, selectedIndices.length));
            m_jlist.ensureIndexIsVisible(addToVisibleIndices ? boundOffset : boundOffset + selectedIndices.length - 1);
        }

    }

    /**
     * @return
     */
    private ActionListener moveLast() {
        return new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                int[] selectedIndices = m_jlist.getSelectedIndices();
                insertAtPosition(selectedIndices, m_currentSorting.size() - selectedIndices.length, true);
            }
        };
    }

    /**
     * @return
     */
    private ActionListener moveFirst() {
        return new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                insertAtPosition(m_jlist.getSelectedIndices(), 0, false);
            }
        };
    }

    /**
     * @return
     */
    private ActionListener addStringCell() {
        return new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                String s =
                    (String)JOptionPane.showInputDialog(EditNominalDomainNodeDialogPane.this.getPanel(), "Value: ",
                        "Add Data Cell", JOptionPane.PLAIN_MESSAGE, null, null, "");

                if (s != null && !s.isEmpty() && m_currentColSpec != null) {
                    int index = m_jlist.getSelectedIndex() == -1 ? 0 : m_jlist.getSelectedIndex();
                    StringCell stringCell = new StringCell(s);

                    int lastIndexOf = m_currentSorting.lastIndexOf(stringCell);
                    if (lastIndexOf != -1) {
                        JOptionPane.showMessageDialog(EditNominalDomainNodeDialogPane.this.getPanel(),
                            String.format("Value: '%s' does already exist at index: %d", s, lastIndexOf));
                    } else {
                        m_currentSorting.add(index, stringCell);
                        m_configuration.addCreatedValue(m_currentColSpec.getName(), stringCell);
                        m_jlist.setSelectedIndices(new int[]{index});
                        LOGGER.info("created new value: " + s);
                    }
                }
            }
        };
    }

    /**
     * @return
     */
    private ActionListener removeStringCells() {
        return new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                int[] selectedIndices = m_jlist.getSelectedIndices();

                if (selectedIndices.length > 0) {
                    int offset = 0;
                    for (int i : selectedIndices) {
                        DataCell dataCell = m_currentSorting.get(i - offset);
                        // we can only remove cells which are not part of the input table domain.
                        if (!m_orgDomainCells.contains(dataCell)
                            && !EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(dataCell)) {
                            m_currentSorting.remove(i - offset++);
                        }
                    }
                }
            }
        };
    }

    private ActionListener sortNames(final SortOption b) {
        return new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                Collections.sort(m_currentSorting, b);
                m_jlist.ensureIndexIsVisible(0);
            }
        };
    }

    private void insertAtPosition(final int[] selectedIndices, final int offsets, //
        final boolean addLegthToVisibleIndex) {
        if (selectedIndices.length > 0) {
            List<DataCell> toAdd = new ArrayList<DataCell>(selectedIndices.length);

            int offset = 0;
            for (int i : selectedIndices) {
                toAdd.add(m_currentSorting.remove(i - offset++));
            }

            m_currentSorting.addAll(offsets, toAdd);

            m_jlist.setSelectedIndices(createAscendingIntArray(offsets, selectedIndices.length));
            m_jlist.ensureIndexIsVisible(addLegthToVisibleIndex ? offsets + selectedIndices.length - 1 : offsets);
        }
    }

    private static int[] addOffset(final int[] selectedIndices, final int offset) {
        for (int i = 0; i < selectedIndices.length; i++) {
            selectedIndices[i] = selectedIndices[i] + offset;
        }
        return selectedIndices;
    }

    private static int[] createAscendingIntArray(final int offset, final int length) {
        int[] toReturn = new int[length];
        for (int i = 0; i < length; i++) {
            toReturn[i] = offset + i;
        }
        return toReturn;
    }

    private boolean checkConfigured(final DataColumnSpec value) {
        return m_configuration.isConfiguredColumn(value.getName());
    }

    /**
     * Saving and restoring.
     **/
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        storeCurrentList();

        m_configuration.setIgnoreNotExistingColumns(m_buttonMap.get(IGNORE_COLUMNS_NOT_PRESENT_IN_DATA).isSelected());
        m_configuration.setIgnoreWrongTypes(m_buttonMap.get(IGNORE_COLUMNS_NOT_MATCHING_TYPES).isSelected());
        m_configuration.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        try {
            if (specs[0].getNumColumns() == 0) {
                throw new NotConfigurableException("No data at input.");
            }
            m_orgSpec = specs[0];

            m_currentSorting.clear();

            m_configuration = new EditNominalDomainConfiguration();
            m_configuration.loadConfigurationInDialog(settings);

            if (m_configuration.isIgnoreNotExistingColumns()) {
                m_buttonMap.get(IGNORE_COLUMNS_NOT_PRESENT_IN_DATA).doClick();
            }
            if (m_configuration.isIgnoreWrongTypes()) {
                m_buttonMap.get(IGNORE_COLUMNS_NOT_MATCHING_TYPES).doClick();
            }

            m_searchableListModifier = m_searchableListPanel.update(m_orgSpec, STRING_VALUE_FILTER);
            addColumnSpecsForNotExistingColumns();
        } catch (Error e) {
            e.printStackTrace();
        }
    }

    private void storeCurrentList() {
        if (m_currentColSpec != null && m_currentSorting != null && !m_currentSorting.isEmpty() && m_somethingChanged) {
            m_configuration.setSorting(m_currentColSpec.getName(), m_currentSorting);
        }
    }

    /**
     * Adds the cells of the columnSpec and potential sorted but not more existing DataCells from the stored
     * configuration.
     *
     * @param columnSpec
     */
    private void addDataCellElements(final DataColumnSpec columnSpec) {

        List<DataCell> sorting = m_configuration.getSorting(columnSpec.getName());

        Set<DataCell> values = columnSpec.getDomain().getValues();

        m_orgDomainCells = values == null ? Collections.<DataCell> emptySet() : Collections.unmodifiableSet(values);

        if (sorting == null) {
            // there does not exist any sorting, so we just add the original cells and return.
            if (m_orgDomainCells != null) {
                for (DataCell cell : m_orgDomainCells) {
                    m_currentSorting.add(cell);
                }
            }
            m_currentSorting.add(EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL);
        } else {
            // determine the difference set between the original cells and the stored configuration.
            // Add them at the UNKOWN_VALUE_CELL position.
            Set<DataCell> diff = EditNominalDomainNodeModel.diff(m_orgDomainCells, sorting);

            for (DataCell cell : sorting) {
                //  add the new cells at the position of the UNKOWN_VALUES_CELL
                if (EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(cell)) {
                    for (DataCell diffCell : diff) {
                        m_currentSorting.add(diffCell);
                    }
                }
                // and add all cells so or so
                m_currentSorting.add(cell);
            }
        }
    }

    private void addColumnSpecsForNotExistingColumns() {
        Set<String> configuredColumns = m_configuration.getConfiguredColumns();

        for (String s : configuredColumns) {
            // include the given cell name or if the table has a wrong type.
            if (m_orgSpec.getColumnSpec(s) == null || !StringCell.TYPE.equals(m_orgSpec.getColumnSpec(s).getType())) {
                DataColumnSpec invalidSpec = DataColumnSpecListCellRenderer.createInvalidSpec(s, StringCell.TYPE);
                m_searchableListModifier.addAdditionalColumn(invalidSpec);
            }
        }
    }

    /**
     * Internal sorting options.
     *
     * @author Marcel Hanser
     */
    private enum SortOption implements Comparator<DataCell> {
        ASCENDING {

            @Override
            public int compare(final DataCell o1, final DataCell o2) {
                if (EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(o1)) {
                    return 1;
                } else if (EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(o2)) {
                    return -1;
                }
                return StringCell.TYPE.getComparator().compare(o1, o2);
            }

        },
        DESCENDING {

            @Override
            public int compare(final DataCell o1, final DataCell o2) {
                if (EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(o1)) {
                    return 1;
                } else if (EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(o2)) {
                    return -1;
                }
                return StringCell.TYPE.getComparator().compare(o2, o1);
            }

        }
    }

    /**
     * Highlights the {@link EditNominalDomainConfiguration#UNKNOWN_VALUES_CELL} cell, self created cells and cells
     * which do not exist in the input table.
     *
     * @author Marcel Hanser
     */
    @SuppressWarnings("serial")
    private class HighlightSpecialCellsRenderer extends DefaultDataValueRenderer {
        /**
         *
         */
        private static final String UNKNOWN_CELL_TEXT = "<any unknown new value>";

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList list, final Object value, final int index,
            final boolean isSelected, final boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            assert (c == this);

            if (EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(value)) {
                setForeground(Color.GRAY);
                setText(UNKNOWN_CELL_TEXT);
            } else if (!m_orgDomainCells.contains(value)) {
                // either a created one or an unknown one.
                if (m_configuration.isCreatedValue(m_currentColSpec.getName(), (DataCell)value)) {
                    setForeground(Color.GREEN);
                } else {
                    setForeground(Color.RED);
                }
            }

            return this;
        }
    }

    /**
     * UI creation methods.
     *
     **/
    private Component createLegend() {
        Box buttonBox = Box.createVerticalBox();
        buttonBox.add(createLabel("Created value", Color.GREEN));
        buttonBox.add(createLabel("Sorted value <br/>which does not exist", Color.RED));
        buttonBox.setBorder(BorderFactory.createTitledBorder("Legend"));
        return buttonBox;
    }

    private Component createLabel(final String text, final Color color) {
        JLabel label =
            new JLabel(String.format(
                "<html><font border='1' bgcolor='rgb(%d,%d,%d)'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>: %s</html>",
                color.getRed(), color.getGreen(), color.getBlue(), text));
        return label;
    }

    private JButton createButton(final String string, final ActionListener sortNames, final Dimension preferredSize) {
        JButton toReturn = new JButton(string);
        toReturn.addActionListener(sortNames);
        if (preferredSize != null) {
            toReturn.setMinimumSize(preferredSize);
            toReturn.setMaximumSize(preferredSize);
        }
        return toReturn;
    }

    private JButton createButton(final String string, final ActionListener actionListener) {
        return createButton(string, actionListener, null);
    }

    private JPanel createRadioButtonGroup(final String string) {
        return createRadioButtonGroup(string, "Fail", "Ignore column                   "
            + "                              ");
    }

    private JPanel createRadioButtonGroup(final String string, final String first, final String second) {
        JPanel toReturn = new JPanel(new GridLayout(2, 1));
        toReturn.setBorder(BorderFactory.createTitledBorder(string));
        JRadioButton firstBut = new JRadioButton(first);
        JRadioButton secondBut = new JRadioButton(second);

        m_buttonMap.put(string, secondBut);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(firstBut);
        buttonGroup.add(secondBut);

        firstBut.setSelected(true);

        toReturn.add(firstBut);
        toReturn.add(secondBut);

        return toReturn;
    }

}
