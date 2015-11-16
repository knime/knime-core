/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   14.10.2015 (Adrian Nembach): created
 */

package org.knime.base.node.preproc.rank;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.knime.base.node.preproc.rank.RankNodeModel.RankMode;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.core.node.util.DataColumnSpecTableCellRenderer;

/**
 * <code>NodeDialog</code> for the "Rank" Node. This node ranks the input data based on the selected ranking field and
 * ranking mode
 *
 *
 * @author Adrian Nembach, KNIME GmbH Konstanz;
 *         Ferry Abt, KNIME GmbH Konstanz
 */
public class RankNodeDialog extends NodeDialogPane {

    private SettingsModelStringArray m_rankColsModel =
            RankNodeModel.createRankColumnsModel();

    private SettingsModelStringArray m_groupColsModel =
            RankNodeModel.createGroupColumnsModel();

    private SettingsModelStringArray m_rankOrderModel =
            RankNodeModel.createRankOrderModel();

    private SettingsModelString m_rankMode = RankNodeModel.createRankModeModel();

    private final SettingsModelString m_rankOutColName = RankNodeModel.createRankOutColNameModel();

    private final SettingsModelBoolean m_retainRowOrder = RankNodeModel.createRetainRowOrderModel();

    private final SettingsModelBoolean m_rankAsLong = RankNodeModel.createRankAsLongModel();

    private JTextField m_outColNameTextField;

    private JCheckBox m_retainOrderCheckBox;

    private JCheckBox m_rankAsLongCheckBox;

    private JTable m_rankJTable;

    private DefaultTableModel m_rankTableModel;

    private JComboBox<DataColumnSpec> m_rankColEditor;

    private DefaultComboBoxModel<DataColumnSpec> m_rankColEditorModel;

    private JTable m_groupJTable;

    private DefaultTableModel m_groupTableModel;

    private JComboBox<DataColumnSpec> m_groupColEditor;

    private DefaultComboBoxModel<DataColumnSpec> m_groupColEditorModel;

    private ButtonGroup m_modusGroup;

    private boolean m_availableEdited;

    private LinkedList<DataColumnSpec> m_groupCols = new LinkedList<DataColumnSpec>();

    private LinkedList<DataColumnSpec> m_availableCols = new LinkedList<DataColumnSpec>();

    private LinkedList<DataColumnSpec> m_rankCols = new LinkedList<DataColumnSpec>();

    private LinkedList<DataColumnSpec> m_rankEditorItems = new LinkedList<DataColumnSpec>();

    private LinkedList<DataColumnSpec> m_groupEditorItems = new LinkedList<DataColumnSpec>();

    /**
     * New pane for configuring Rank node dialog. This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected RankNodeDialog() {

        addTab("Options", initPanel());
    }

    private JPanel initPanel() {
        JPanel jp = new JPanel();
        jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));

        Box rankColSelection = Box.createHorizontalBox();

        // add column list
        Box rankCols = initRankTableBox();
        rankColSelection.add(rankCols);

        // add buttons
        Box rankButtonBox = initRankButtonBox();
        rankColSelection.add(rankButtonBox);

        Box groupColSelection = Box.createHorizontalBox();

        groupColSelection.add(initGroupTableBox());
        groupColSelection.add(initGroupButtonBox());

        Box otherOptions = Box.createHorizontalBox();
        otherOptions.add(Box.createHorizontalGlue());
        Box modusBox = Box.createHorizontalBox();
        modusBox.setBorder(new TitledBorder("Ranking Mode"));
        modusBox.add(new JLabel("Mode:  "));
        Box radioPanel = Box.createVerticalBox();
        ButtonGroup modusGroup = new ButtonGroup();
        m_modusGroup = modusGroup;

        for (RankMode rankMode : RankMode.values()) {
            JRadioButton radio = new JRadioButton(rankMode.toString());
            modusGroup.add(radio);
            radioPanel.add(radio);
            if (m_rankMode.getStringValue().equals(radio.getText())) {
                radio.setSelected(true);
            }
        }
        modusBox.add(radioPanel);
        otherOptions.add(modusBox);
        otherOptions.add(Box.createHorizontalGlue());

        otherOptions.add(initOtherOptionsBox());
        otherOptions.add(Box.createHorizontalGlue());

        jp.add(rankColSelection);
        jp.add(groupColSelection);
        jp.add(otherOptions);
        otherOptions.revalidate();
        otherOptions.repaint();

        return jp;
    }

    private Box initOtherOptionsBox() {
        Box box = Box.createVerticalBox();
        box.setBorder(new TitledBorder("Other Options"));

        Box textBox = Box.createHorizontalBox();
        m_outColNameTextField = new JTextField(10);
        m_outColNameTextField.setMaximumSize(m_outColNameTextField.getPreferredSize());
        JLabel textBoxLabel = new JLabel("Name of Rank Attribute ");

        Dimension labelSize = textBoxLabel.getPreferredSize();
        textBoxLabel.setMaximumSize(labelSize);
        textBoxLabel.setMinimumSize(labelSize);

        textBox.add(textBoxLabel);
        textBox.add(m_outColNameTextField);

        Box checkBox = Box.createHorizontalBox();
        m_retainOrderCheckBox = new JCheckBox();
        JLabel checkBoxLabel = new JLabel("Retain Row Order");
        checkBoxLabel.setMaximumSize(labelSize);
        checkBoxLabel.setMinimumSize(labelSize);
        checkBoxLabel.setPreferredSize(labelSize);
        checkBox.add(checkBoxLabel);
        checkBox.add(m_retainOrderCheckBox);

        Box rankAsLongCheckBox = Box.createHorizontalBox();
        m_rankAsLongCheckBox = new JCheckBox();
        JLabel rankAsLongCheckBoxLabel = new JLabel("Rank as Long");
        rankAsLongCheckBoxLabel.setMaximumSize(labelSize);
        rankAsLongCheckBoxLabel.setMinimumSize(labelSize);
        rankAsLongCheckBoxLabel.setPreferredSize(labelSize);
        rankAsLongCheckBox.add(rankAsLongCheckBoxLabel);
        rankAsLongCheckBox.add(m_rankAsLongCheckBox);

        box.add(textBox);
        box.add(checkBox);
        box.add(rankAsLongCheckBox);

        return box;
    }

    private Box initRankTableBox() {
        Box columnTableBox = Box.createVerticalBox();
        columnTableBox.setBorder(new TitledBorder("Ranking Attributes"));

        m_rankTableModel = new DefaultTableModel(new String[]{"Attribute", "Order"}, 0);

        DefaultTableColumnModel columnModel = new DefaultTableColumnModel();

        m_rankJTable = new JTable(m_rankTableModel, columnModel);
        m_rankJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_rankJTable.setCellSelectionEnabled(false);
        m_rankJTable.setColumnSelectionAllowed(false);
        m_rankJTable.getTableHeader().setReorderingAllowed(false);
        m_rankJTable.setRowSelectionAllowed(true);

        TableColumn colColumn = new TableColumn();
        colColumn.setHeaderValue("Column");
        colColumn.setCellRenderer(new DataColumnSpecTableCellRenderer());
        m_rankColEditorModel = new DefaultComboBoxModel<DataColumnSpec>();
        m_rankColEditor = new JComboBox<DataColumnSpec>(m_rankColEditorModel);
        m_rankColEditor.setRenderer(new DataColumnSpecListCellRenderer());

        m_rankColEditor.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                // TODO Auto-generated method stub
                if (!m_availableEdited) {
                    DataColumnSpec colSpec = (DataColumnSpec)e.getItem();
                    int state = e.getStateChange();
                    if (state == ItemEvent.SELECTED && m_rankJTable.isEditing()) {
                        m_availableCols.remove(colSpec);
                        removeItemGroupColEditor(colSpec);
                        //                        removeItemRankColEditor(colSpec);
                        m_rankCols.add(colSpec);
                    } else if (state == ItemEvent.DESELECTED && m_rankJTable.isEditing()) {
                        m_rankCols.remove(colSpec);
                        if (!m_rankCols.contains(colSpec)) {
                            add2AvailableList(colSpec);
                            addItemGroupColEditor(colSpec);
                        }
                    }
                }
            }

        });


        colColumn.setCellEditor(new DefaultCellEditor(m_rankColEditor));

        TableColumn orderColumn = new TableColumn(1);
        orderColumn.setHeaderValue("Order");
        // combo box for order column
        JComboBox<String> orderColumnEditor = new JComboBox<String>();
        orderColumnEditor.addItem("Ascending");
        orderColumnEditor.addItem("Descending");
        orderColumn.setCellEditor(new DefaultCellEditor(orderColumnEditor));

        columnModel.addColumn(colColumn);
        columnModel.addColumn(orderColumn);

        JScrollPane scrollPane = new JScrollPane(m_rankJTable);
        scrollPane.setPreferredSize(m_rankJTable.getPreferredSize());

        columnTableBox.add(scrollPane);
        columnTableBox.setSize(scrollPane.getSize());
        return columnTableBox;
    }

    private Box initGroupTableBox() {
        Box groupTableBox = Box.createVerticalBox();
        groupTableBox.setBorder(new TitledBorder("Grouping Attributes"));

        m_groupTableModel = new DefaultTableModel(new String[]{"Attribute"}, 0);
        m_groupJTable = new JTable(m_groupTableModel);
        m_groupJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableColumn column = m_groupJTable.getColumn("Attribute");
        column.setCellRenderer(new DataColumnSpecTableCellRenderer());

        m_groupColEditorModel = new DefaultComboBoxModel<DataColumnSpec>();
        m_groupColEditor = new JComboBox<DataColumnSpec>(m_groupColEditorModel);
        m_groupColEditor.setRenderer(new DataColumnSpecListCellRenderer());
        m_groupColEditor.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (!m_availableEdited) {
                    DataColumnSpec colSpec = (DataColumnSpec)e.getItem();
                    int state = e.getStateChange();
                    if (state == ItemEvent.SELECTED && m_groupJTable.isEditing()) {
                        m_availableCols.remove(colSpec);
                        removeItemRankColEditor(colSpec);
                        m_groupCols.add(colSpec);
                    } else if (state == ItemEvent.DESELECTED && m_groupJTable.isEditing()) {
                        m_groupCols.remove(colSpec);
                        if (!m_groupCols.contains(colSpec)) {
                            add2AvailableList(colSpec);
                            addItemRankColEditor(colSpec);
                        }
                    }
                }
            }

        });
        column.setCellEditor(new DefaultCellEditor(m_groupColEditor));

        JScrollPane scrollPane = new JScrollPane(m_groupJTable);
        scrollPane.setMinimumSize(m_rankJTable.getPreferredSize());
        scrollPane.setPreferredSize(m_rankJTable.getPreferredSize());
        groupTableBox.add(new JScrollPane(scrollPane));
        return groupTableBox;
    }

    private Box initRankButtonBox() {
        Box buttonBox = Box.createVerticalBox();
        buttonBox.setBorder(new TitledBorder("Actions"));

        JButton buttonAdd = new JButton("Add");
        JButton buttonRemove = new JButton("Remove");
        JButton buttonRemoveAll = new JButton("Remove All");
        JButton buttonUp = new JButton("Up");
        JButton buttonDown = new JButton("Down");

        Dimension allButtons = buttonRemoveAll.getPreferredSize();
        buttonAdd.setMinimumSize(allButtons);
        buttonAdd.setMaximumSize(allButtons);
        buttonRemove.setMinimumSize(allButtons);
        buttonRemove.setMaximumSize(allButtons);
        buttonRemoveAll.setMinimumSize(allButtons);
        buttonRemoveAll.setMaximumSize(allButtons);
        buttonUp.setMinimumSize(allButtons);
        buttonUp.setMaximumSize(allButtons);
        buttonDown.setMinimumSize(allButtons);
        buttonDown.setMaximumSize(allButtons);

        buttonBox.add(buttonAdd);
        buttonBox.add(buttonRemove);
        buttonBox.add(buttonRemoveAll);
        buttonBox.add(buttonUp);
        buttonBox.add(buttonDown);

        buttonAdd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {

                if (!m_availableCols.isEmpty()) {
                    if (m_rankJTable.isEditing()) {
                        m_rankJTable.getCellEditor().stopCellEditing();
                    }
                    DataColumnSpec colSpec = m_availableCols.peek();
                    Object[] row = new Object[]{colSpec, "Ascending"};
                    m_rankTableModel.addRow(row);
                    m_rankCols.add(colSpec);
                    m_availableCols.remove(colSpec);
                    removeItemGroupColEditor(colSpec);
                }
            }

        });

        buttonRemove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                int index = m_rankJTable.getSelectedRow();
                if (index == -1) {
                    index = m_rankJTable.getEditingRow();
                }
                if (index != -1) {
                    if (m_rankJTable.isEditing()) {
                        m_rankJTable.getCellEditor().stopCellEditing();
                    }

                    DataColumnSpec colSpec =
                        (DataColumnSpec)m_rankTableModel.getValueAt(index, 0);
                    m_rankTableModel.removeRow(index);
                    m_rankCols.remove(colSpec);
                    if (!m_rankCols.contains(colSpec)) {
                        add2AvailableList(colSpec);
                        addItemGroupColEditor(colSpec);
                    }
                }

            }

        });

        buttonRemoveAll.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                if (m_rankJTable.isEditing()) {
                    m_rankJTable.getCellEditor().stopCellEditing();
                }
                if (m_rankJTable.getRowCount() > 0) {
                    m_rankCols.clear();
                    for (int r = 0; r < m_rankTableModel.getRowCount(); r++) {
                        DataColumnSpec colSpec = (DataColumnSpec)m_rankTableModel.getValueAt(r, 0);
                        add2AvailableList(colSpec);
                        addItemGroupColEditor(colSpec);
                    }
                    m_rankTableModel.setRowCount(0);
                }
            }

        });

        buttonUp.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                int row = m_rankJTable.getSelectedRow();
                if (row == -1) {
                    row = m_rankJTable.getEditingRow();
                }

                if (row > 0) {
                    m_availableEdited = true;
                    if (m_rankJTable.isEditing()) {
                        m_rankJTable.getCellEditor().stopCellEditing();
                    }
                    m_rankTableModel.moveRow(row, row, row - 1);
                    m_availableEdited = false;
                }
            }

        });

        buttonDown.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                int row = m_rankJTable.getSelectedRow();
                if (row == -1) {
                    m_rankJTable.getEditingRow();
                }
                if (row != -1 && row < m_rankJTable.getRowCount() - 1) {
                    if (m_rankJTable.isEditing()) {
                        m_rankJTable.getCellEditor().stopCellEditing();
                    }
                    m_rankTableModel.moveRow(row, row, row + 1);
                }
            }
        });

        return buttonBox;
    }

    private Box initGroupButtonBox() {
        Box buttonBox = Box.createVerticalBox();
        buttonBox.setBorder(new TitledBorder("Actions"));

        JButton buttonAdd = new JButton("Add");
        JButton buttonRemove = new JButton("Remove");
        JButton buttonRemoveAll = new JButton("Remove All");
        JButton buttonUp = new JButton("Up");
        JButton buttonDown = new JButton("Down");

        Dimension allButtons = buttonRemoveAll.getPreferredSize();
        buttonAdd.setMinimumSize(allButtons);
        buttonAdd.setMaximumSize(allButtons);
        buttonRemove.setMinimumSize(allButtons);
        buttonRemove.setMaximumSize(allButtons);
        buttonRemoveAll.setMinimumSize(allButtons);
        buttonRemoveAll.setMaximumSize(allButtons);
        buttonUp.setMinimumSize(allButtons);
        buttonUp.setMaximumSize(allButtons);
        buttonDown.setMinimumSize(allButtons);
        buttonDown.setMaximumSize(allButtons);

        buttonBox.add(buttonAdd);
        buttonBox.add(buttonRemove);
        buttonBox.add(buttonRemoveAll);

        buttonAdd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {

                if (!m_availableCols.isEmpty()) {
                    if (m_groupJTable.isEditing()) {
                        m_groupJTable.getCellEditor().stopCellEditing();
                    }
                    DataColumnSpec colSpec = m_availableCols.peek();
                    Object[] row = new Object[]{colSpec, "Ascending"};
                    m_availableCols.remove(colSpec);
                    removeItemRankColEditor(colSpec);
                    m_groupTableModel.addRow(row);
                    m_groupCols.add(colSpec);
                }
            }

        });

        buttonRemove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                int index = m_groupJTable.getSelectedRow();
                if (index == -1) {
                    index = m_groupJTable.getEditingRow();
                }
                if (index != -1) {
                    if (m_groupJTable.isEditing()) {
                        m_groupJTable.getCellEditor().stopCellEditing();
                    }
                    DataColumnSpec colSpec =
                        (DataColumnSpec)m_groupTableModel.getValueAt(index, 0);
                    m_groupTableModel.removeRow(index);
                    m_groupCols.remove(colSpec);
                    if (!m_groupCols.contains(colSpec)) {
                        add2AvailableList(colSpec);
                        addItemRankColEditor(colSpec);
                    }
                }

            }
        });

        buttonRemoveAll.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                if (m_groupJTable.getRowCount() > 0) {
                    if (m_groupJTable.isEditing()) {
                        m_groupJTable.getCellEditor().stopCellEditing();
                    }
                    m_groupCols.clear();
                    for (int r = 0; r < m_groupTableModel.getRowCount(); r++) {
                        DataColumnSpec colSpec = (DataColumnSpec)m_groupTableModel.getValueAt(r, 0);
                        addColSpec2Available(colSpec);
                        addItemRankColEditor(colSpec);
                    }
                    m_groupTableModel.setRowCount(0);
                }
            }

        });
        return buttonBox;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

        String[] rankColNames = new String[m_rankTableModel.getRowCount()];
        String[] order = new String[m_rankTableModel.getRowCount()];
        for (int r = 0; r < m_rankTableModel.getRowCount(); r++) {
            rankColNames[r] = ((DataColumnSpec)m_rankTableModel.getValueAt(r, 0)).getName();
            order[r] = (String)m_rankTableModel.getValueAt(r, 1);
        }
        String[] groupColNames = new String[m_groupTableModel.getRowCount()];
        for (int r = 0; r < m_groupTableModel.getRowCount(); r++) {
            groupColNames[r] = ((DataColumnSpec)m_groupTableModel.getValueAt(r, 0)).getName();
        }

        // get selected mode
        Enumeration<AbstractButton> buttons = m_modusGroup.getElements();
        String modus = null;
        while (buttons.hasMoreElements()) {
            AbstractButton currentButton = buttons.nextElement();
            if (m_modusGroup.isSelected(currentButton.getModel())) {
                modus = currentButton.getText();
            }
        }

        m_rankMode.setStringValue(modus);

        m_rankColsModel.setStringArrayValue(rankColNames);
        m_rankOrderModel.setStringArrayValue(order);

        m_groupColsModel.setStringArrayValue(groupColNames);

        m_retainRowOrder.setBooleanValue(m_retainOrderCheckBox.isSelected());

        m_rankOutColName.setStringValue(m_outColNameTextField.getText());

        m_rankAsLong.setBooleanValue(m_rankAsLongCheckBox.isSelected());

        validateSettings(settings);

        // save settings models
        m_rankColsModel.saveSettingsTo(settings);
        m_rankOrderModel.saveSettingsTo(settings);
        m_groupColsModel.saveSettingsTo(settings);
        m_rankMode.saveSettingsTo(settings);
        m_rankOutColName.saveSettingsTo(settings);
        m_retainRowOrder.saveSettingsTo(settings);
        m_rankAsLong.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        // Check input spec
        if (specs[0] == null || specs[0].getNumColumns() == 0) {
            throw new NotConfigurableException("No input table found or no columns found in input table! "
                + "Please connect the node first or check input table.");
        }

        final DataTableSpec spec = specs[0];

        // load settings models
        try {
            m_rankColsModel.loadSettingsFrom(settings);
            m_rankOrderModel.loadSettingsFrom(settings);
            m_groupColsModel.loadSettingsFrom(settings);
            m_rankMode.loadSettingsFrom(settings);
            m_rankOutColName.loadSettingsFrom(settings);
            m_retainRowOrder.loadSettingsFrom(settings);
            m_rankAsLong.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        m_rankTableModel.setRowCount(0);
        String[] rankColNames = m_rankColsModel.getStringArrayValue();
        String[] order = m_rankOrderModel.getStringArrayValue();
        m_rankCols.clear();
        for (int i = 0; i < rankColNames.length; i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(rankColNames[i]);
            if (colSpec != null) {
                m_rankTableModel.addRow(new Object[]{colSpec, order[i]});
                m_rankCols.add(colSpec);
            }
        }

        m_groupTableModel.setRowCount(0);
        m_groupCols.clear();
        String[] groupColNames = m_groupColsModel.getStringArrayValue();
        for (int r = 0; r < groupColNames.length; r++) {
            DataColumnSpec colSpec = spec.getColumnSpec(groupColNames[r]);
            if (colSpec != null) {
                m_groupTableModel.addRow(new Object[]{colSpec});
                m_groupCols.add(colSpec);
            }
        }

        removeAllColSpecsFromAvailable();

        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);
            if (m_rankCols.contains(colSpec)) {
                addItemRankColEditor(colSpec);
            } else if (m_groupCols.contains(colSpec)) {
                addItemGroupColEditor(colSpec);
            } else {
                addColSpec2Available(colSpec);
            }
        }

        //select rank mode:
        Enumeration<AbstractButton> radios = m_modusGroup.getElements();
        JRadioButton modus = (JRadioButton)radios.nextElement();
        while (!modus.getText().equals(m_rankMode.getStringValue())) {
            modus.setSelected(false);
            modus = (JRadioButton)radios.nextElement();
        }
        modus.setSelected(true);

        // set retain order checkbox
        m_retainOrderCheckBox.setSelected(m_retainRowOrder.getBooleanValue());

        // set rank out col name text field
        m_outColNameTextField.setText(m_rankOutColName.getStringValue());

        // set rank as long checkbox
        m_rankAsLongCheckBox.setSelected(m_rankAsLong.getBooleanValue());

    }

    private void validateSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        String[] rankColsArray = m_rankColsModel.getStringArrayValue();
        Set<String> rankColsSet = new HashSet<String>(Arrays.asList(rankColsArray));
        if (rankColsSet.size() != rankColsArray.length) {
            throw new InvalidSettingsException("Duplicate attribute selection in ranking columns detected.");
        }

        String[] groupColsArray = m_groupColsModel.getStringArrayValue();
        Set<String> groupColsSet = new HashSet<String>(Arrays.asList(groupColsArray));
        if (groupColsSet.size() != groupColsArray.length) {
            throw new InvalidSettingsException("Duplicate attribute selection in grouping columns detected.");
        }

        if (m_rankOutColName.getStringValue().length() == 0) {
            throw new InvalidSettingsException("A name for the rank column must be provided.");
        }
    }

    private void addColSpec2Available(final DataColumnSpec colSpec) {
        add2AvailableList(colSpec);
        addItemRankColEditor(colSpec);
        addItemGroupColEditor(colSpec);
    }

    private void removeAllColSpecsFromAvailable() {
        m_availableEdited = true;
        m_availableCols.clear();
        m_rankColEditorModel.removeAllElements();
        m_rankEditorItems.clear();
        m_groupColEditorModel.removeAllElements();
        m_groupEditorItems.clear();
        m_availableEdited = false;
    }

    private void addItemRankColEditor(final DataColumnSpec colSpec) {
        m_availableEdited = true;
        if (!m_rankEditorItems.contains(colSpec) && !m_groupCols.contains(colSpec)) {
            m_rankColEditorModel.addElement(colSpec);
            m_rankEditorItems.add(colSpec);
        }
        m_availableEdited = false;
    }

    private void addItemGroupColEditor(final DataColumnSpec colSpec) {
        m_availableEdited = true;
        if (!m_groupEditorItems.contains(colSpec) && !m_rankCols.contains(colSpec)) {
            m_groupColEditorModel.addElement(colSpec);
            m_groupEditorItems.add(colSpec);
        }
        m_availableEdited = false;
    }

    private void removeItemRankColEditor(final DataColumnSpec colSpec) {
        m_availableEdited = true;
        m_rankColEditorModel.removeElement(colSpec);
        m_rankEditorItems.remove(colSpec);
        m_availableEdited = false;
    }

    private void removeItemGroupColEditor(final DataColumnSpec colSpec) {
        m_availableEdited = true;
        m_groupColEditorModel.removeElement(colSpec);
        m_groupEditorItems.remove(colSpec);
        m_availableEdited = false;
    }

    private void add2AvailableList(final DataColumnSpec colSpec) {
        if (!m_availableCols.contains(colSpec) && !m_rankCols.contains(colSpec) && !m_groupCols.contains(colSpec)) {
            m_availableCols.add(colSpec);
        }
    }
}
