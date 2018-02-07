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
 * -------------------------------------------------------------------
 *
 */
package org.knime.expressions.node.formulas;

import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.expressions.ExpressionConverterUtils;

/**
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
final class FormulasNodeDialog extends NodeDialogPane {

	/* Column identifiers that are shown in the table. */
	private static final String[] COLUMN_IDENTIFIERS = new String[] { "Type", "Output Column", "Formula" };

	private static final int TYPE_COLUMN = 0;
	private static final int NAME_COLUMN = 1;
	private static final int EXPRESSION_COLUMN = 2;

	/* Table holding the column names and the expressions. */
	private final JTable m_table;
	private DefaultTableModel m_tableModel;

	/* Buttons to edit the table. */
	private final JButton m_addButton;
	private final JButton m_editButton;
	private final JButton m_removeButton;
	private final JButton m_removeAllButton;
	private final JButton m_moveUpButton;
	private final JButton m_moveDownButton;
	private final JButton m_copyButton;

	/* Used to update the editor dialog. */
	private DataTableSpec m_tableSpecs;

	/* Listener to disable and enable buttons according to empty table. */
	private final TableModelListener m_tableListener = new TableModelListener() {

		@Override
		public void tableChanged(TableModelEvent e) {
			m_editButton.setEnabled(true);
			m_removeAllButton.setEnabled(true);
			m_removeButton.setEnabled(true);
			m_copyButton.setEnabled(true);
			m_moveDownButton.setEnabled(true);
			m_moveUpButton.setEnabled(true);

			if (m_tableModel.getRowCount() == 0) {
				m_editButton.setEnabled(false);
				m_moveDownButton.setEnabled(false);
				m_moveUpButton.setEnabled(false);
				m_removeAllButton.setEnabled(false);
				m_removeButton.setEnabled(false);
				m_copyButton.setEnabled(false);
			} else if (m_tableModel.getRowCount() == 1) {
				m_moveDownButton.setEnabled(false);
				m_moveUpButton.setEnabled(false);
			} else if (m_table.getSelectedRow() == 0) {
				m_moveUpButton.setEnabled(false);
			} else if (m_table.getSelectedRow() == m_table.getRowCount() - 1) {
				m_moveDownButton.setEnabled(false);
			}
		}
	};

	/* ActionListener used for the buttons. */
	private final ActionListener m_buttonListener = new ActionListener() {
		private DataType m_defaultType;

		@Override
		public void actionPerformed(ActionEvent e) {
			/* Stop editing the cell if any button is clicked on. */
			if (m_table.getCellEditor() != null) {
				m_table.getCellEditor().stopCellEditing();
			}

			if (e.getSource().equals(m_addButton)) {

				/* Searches for default data type 'String' */
				if (m_defaultType == null) {
					for (DataType type : ExpressionConverterUtils.possibleTypes()) {
						if (type.toString().equals("String")) {
							m_defaultType = type;
						}
					}
				}

				/* Add row at end and select it. */
				m_tableModel.addRow(new Object[] { m_defaultType, "Col" + m_table.getRowCount(), "0" });
				m_table.getSelectionModel().setSelectionInterval(m_table.getRowCount() - 1, m_table.getRowCount() - 1);
			} else if (e.getSource().equals(m_editButton)) {
				/* Open the edit dialog. */

				// figure out the parent to be able to make the dialog modal
				Frame f = null;
				Container c = getPanel().getParent();
				while (c != null) {
					if (c instanceof Frame) {
						f = (Frame) c;
						break;
					}
					c = c.getParent();
				}

				FormulasExpressionEditorDialog m_editDialog = new FormulasExpressionEditorDialog(f, FormulasNodeDialog.this);
				m_editDialog.setModal(true);
				m_editDialog.setLocationRelativeTo(getPanel());
				m_editDialog.updateSnippet(m_tableSpecs, getAvailableFlowVariables());
				m_editDialog.setVisible(true);
			} else if (e.getSource().equals(m_copyButton)) {
				/* Copy selected row and insert it after the selected one. */
				m_tableModel.getValueAt(m_table.getSelectedRow(), 0);
				Object[] row = new Object[3];
				row[NAME_COLUMN] = m_tableModel.getValueAt(m_table.getSelectedRow(), NAME_COLUMN);
				row[EXPRESSION_COLUMN] = m_tableModel.getValueAt(m_table.getSelectedRow(), EXPRESSION_COLUMN);
				row[TYPE_COLUMN] = m_tableModel.getValueAt(m_table.getSelectedRow(), TYPE_COLUMN);

				m_tableModel.insertRow(m_table.getSelectedRow() + 1, row);
			} else if (e.getSource().equals(m_removeButton)) {
				/* Remove the selected row and select the previous row. */
				int index = m_table.getSelectedRow();
				m_tableModel.removeRow(index);

				index = index - 1 < 0 ? 0 : index - 1;
				m_table.getSelectionModel().setSelectionInterval(index, index);
			} else if (e.getSource().equals(m_removeAllButton)) {
				/* Remove all rows by simply creating a new table model. */
				m_tableModel = new DefaultTableModel();
				m_tableModel.setColumnIdentifiers(COLUMN_IDENTIFIERS);
				m_table.setModel(m_tableModel);

				m_tableModel.addTableModelListener(m_tableListener);
				m_tableListener.tableChanged(null);
				
				JComboBox<DataType> comboBox = new JComboBox<>(ExpressionConverterUtils.possibleTypes());

				m_table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(comboBox));	
			} else if (e.getSource().equals(m_moveUpButton)) {
				/* Move the selected row up. */
				m_tableModel.moveRow(m_table.getSelectedRow(), m_table.getSelectedRow(), m_table.getSelectedRow() - 1);
				m_table.getSelectionModel().setSelectionInterval(m_table.getSelectedRow() - 1,
						m_table.getSelectedRow() - 1);
			} else if (e.getSource().equals(m_moveDownButton)) {
				/* Move the selected row down. */
				m_tableModel.moveRow(m_table.getSelectedRow(), m_table.getSelectedRow(), m_table.getSelectedRow() + 1);
				m_table.getSelectionModel().setSelectionInterval(m_table.getSelectedRow() + 1,
						m_table.getSelectedRow() + 1);
			}
		}
	};

	/**
	 * Constructor to create a new NodeDialog.
	 */
	public FormulasNodeDialog() {
		m_tableModel = new DefaultTableModel();
		m_tableModel.setColumnIdentifiers(COLUMN_IDENTIFIERS);
		m_table = new JTable(m_tableModel);
		m_table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_tableModel.addTableModelListener(m_tableListener);

		/* Listener to disable move up/down depending on the selected row. */
		m_table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (m_table.getSelectedRow() == 0 && m_table.getRowCount() > 1) {
					m_moveUpButton.setEnabled(false);
					m_moveDownButton.setEnabled(true);
				} else if (m_table.getSelectedRow() == m_table.getRowCount() - 1 && m_table.getRowCount() > 1) {
					m_moveDownButton.setEnabled(false);
					m_moveUpButton.setEnabled(true);
				} else if (m_table.getRowCount() > 1) {
					m_moveUpButton.setEnabled(true);
					m_moveDownButton.setEnabled(true);
				}
			}
		});

		/*
		 * Add a combobox containing the available knime data types. Obtain String as
		 * the default type.
		 */
		JComboBox<DataType> comboBox = new JComboBox<>(ExpressionConverterUtils.possibleTypes());

		m_table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(comboBox));

		// figure out the parent to be able to make the dialog modal
		Frame f = null;
		Container c = getPanel().getParent();
		while (c != null) {
			if (c instanceof Frame) {
				f = (Frame) c;
				break;
			}
			c = c.getParent();
		}

		m_addButton = new JButton("Add");
		m_editButton = new JButton("Edit...");
		m_removeButton = new JButton("Remove");
		m_removeAllButton = new JButton("Remove All");
		m_moveUpButton = new JButton("Move Up");
		m_moveDownButton = new JButton("Move Down");
		m_copyButton = new JButton("Copy");

		m_editButton.setEnabled(false);
		m_moveDownButton.setEnabled(false);
		m_moveUpButton.setEnabled(false);
		m_removeAllButton.setEnabled(false);
		m_removeButton.setEnabled(false);
		m_copyButton.setEnabled(false);

		m_addButton.addActionListener(m_buttonListener);
		m_editButton.addActionListener(m_buttonListener);
		m_removeButton.addActionListener(m_buttonListener);
		m_removeAllButton.addActionListener(m_buttonListener);
		m_moveUpButton.addActionListener(m_buttonListener);
		m_moveDownButton.addActionListener(m_buttonListener);
		m_copyButton.addActionListener(m_buttonListener);

		initLayout();
	}

	/**
	 * Initialize the dialog consisting of two parts: the table containing the
	 * expression + column, and the buttons.
	 */
	private void initLayout() {
		JPanel mainPanel = new JPanel(new GridBagLayout());
		GridBagConstraints constraint = new GridBagConstraints();

		constraint.gridx = 0;
		constraint.gridy = 0;
		constraint.weightx = 1;
		constraint.weighty = 1;
		constraint.fill = GridBagConstraints.BOTH;

		mainPanel.add(new JScrollPane(m_table), constraint);

		/* Use simple GridLayout as the buttons are in one line. */
		GridLayout grid = new GridLayout(7, 1);
		grid.setVgap(5);
		JPanel subPanel = new JPanel(grid);

		subPanel.add(m_addButton);
		subPanel.add(m_editButton);
		subPanel.add(m_copyButton);
		subPanel.add(m_removeButton);
		subPanel.add(m_removeAllButton);
		subPanel.add(m_moveUpButton);
		subPanel.add(m_moveDownButton);

		constraint.gridx++;
		constraint.weightx = 0;
		constraint.weighty = 0;
		constraint.insets = new Insets(5, 5, 5, 5);
		constraint.fill = GridBagConstraints.NONE;
		constraint.anchor = GridBagConstraints.PAGE_START;

		mainPanel.add(subPanel, constraint);

		this.addTab("Formula", mainPanel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		/* Stop possible editing cell. */
		if (m_table.getCellEditor() != null) {
			m_table.getCellEditor().stopCellEditing();
		}

		FormulasNodeConfiguration configuration = new FormulasNodeConfiguration();

		Vector<?> table = m_tableModel.getDataVector();

		/*
		 * Transpose the matrix in such a way that the rows of the table are stored as
		 * columns. This makes it easier to save the expression table. First row
		 * contains the column names and the second row contains the expressions.
		 */
		String[][] tableContents = new String[COLUMN_IDENTIFIERS.length - 1][table.size()];
		DataType[] types = new DataType[table.size()];

		for (int i = 0; i < table.size(); i++) {
			Vector<?> row = (Vector<?>) table.get(i);

			tableContents[EXPRESSION_COLUMN - 1][i] = row.get(EXPRESSION_COLUMN).toString();
			tableContents[NAME_COLUMN - 1][i] = row.get(NAME_COLUMN).toString();
			types[i] = (DataType) row.get(TYPE_COLUMN);
		}

		configuration.setExpressionTable(tableContents);
		configuration.setDataTypes(types);
		configuration.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {
		FormulasNodeConfiguration configuration = new FormulasNodeConfiguration();

		configuration.loadSettingsIntoDialog(settings);

		String[][] expressions = configuration.getExpressionTable();
		DataType[] types = configuration.getDataTypes();

		/*
		 * Transpose the expression table s.t. first column contains column names and
		 * second column contains expressions.
		 */
		if (expressions.length > 0) {
			m_tableModel = new DefaultTableModel();
			m_tableModel.setColumnIdentifiers(COLUMN_IDENTIFIERS);
			m_table.setModel(m_tableModel);
			m_table.getColumnModel().getColumn(TYPE_COLUMN)
					.setCellEditor(new DefaultCellEditor(new JComboBox<>(ExpressionConverterUtils.possibleTypes())));

			m_tableModel.addTableModelListener(m_tableListener);
			m_tableListener.tableChanged(null);

			for (int i = 0; i < expressions[0].length; i++) {
				m_tableModel.addRow(new Object[] { types[i], expressions[0][i], expressions[1][i] });
			}

			m_table.getSelectionModel().setSelectionInterval(0, 0);
		}

		m_tableSpecs = specs[0];
	}

	/**
	 * Sets the EditDialog invisible, re-enables the focus for the NodeDialog, and
	 * inserts the given expression at the selected row.
	 * 
	 * @param expression
	 *            Expression to be set.
	 */
	void okEditDialog(String expression) {
		m_tableModel.setValueAt(expression, m_table.getSelectedRow(), EXPRESSION_COLUMN);
	}

	/**
	 * @return table containing the column names and expressions
	 */
	JTable getTable() {
		return m_table;
	}

	/**
	 * 
	 * @return index of the type column.
	 */
	public static int getTypeColumn() {
		return TYPE_COLUMN;
	}

	/**
	 * 
	 * @return index of the name column.
	 */
	public static int getNameColumn() {
		return NAME_COLUMN;
	}

	/**
	 * 
	 * @return index of the expression column.
	 */
	public static int getExpressionColumn() {
		return EXPRESSION_COLUMN;
	}
}
