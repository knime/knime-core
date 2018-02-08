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
package org.knime.expressions.base.node.formulas;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.knime.base.node.util.JSnippetPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.expressions.base.node.ExpressionPanelFactory;

/**
 * Dialog that holds a JSnippetPanel to edit expressions in a more advanced way.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
final class FormulasExpressionEditorDialog extends JDialog {

	/**
	 * Default serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	private final JButton m_okButton;
	private final JButton m_cancelButton;
	private final FormulasNodeDialog m_parent;
	private final JSnippetPanel m_snippetPanel;

	/* ActionListener used to re-enable focus of the parent dialog. */
	private final ActionListener m_buttonListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource().equals(m_okButton)) {
				m_parent.okEditDialog(m_snippetPanel.getExpression());
			}
			dispose();
		}
	};

	/**
	 * Creates a new EditDialog.
	 * 
	 * @param parent
	 *            parent of the EditDialog. Used to access functions to re-enable
	 *            focus of parent and access the table.
	 */
	public FormulasExpressionEditorDialog(Frame par, FormulasNodeDialog parent) {
		super(par);
		m_parent = parent;
		m_snippetPanel = ExpressionPanelFactory.createExpressionPanel();
		m_okButton = new JButton("ok");
		m_cancelButton = new JButton("cancel");
		m_okButton.addActionListener(m_buttonListener);
		m_cancelButton.addActionListener(m_buttonListener);

		initLayout();

		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.pack();
	}

	/**
	 * Initializes the layout consisting of the JSnippetPanel on the top and two
	 * buttons at the bottom.
	 */
	private void initLayout() {
		this.setTitle("Formula Builder");
		// this.setAlwaysOnTop(true);
		this.setLayout(new GridBagLayout());

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.fill = GridBagConstraints.BOTH;

		this.add(m_snippetPanel, constraints);

		constraints.fill = GridBagConstraints.NONE;
		constraints.gridy++;
		constraints.weightx = 0;
		constraints.weighty = 0;
		constraints.anchor = GridBagConstraints.LINE_END;
		constraints.insets = new Insets(5, 5, 5, 5);

		GridLayout grid = new GridLayout(1, 2);
		grid.setHgap(5);
		JPanel buttonPanel = new JPanel(grid);
		buttonPanel.add(m_okButton);
		buttonPanel.add(m_cancelButton);

		this.add(buttonPanel, constraints);
	}

	/**
	 * Updates the content of the JSnippetPanel with new content.
	 * 
	 * @param specs
	 *            data table spec of the input table
	 * @param flowVariables
	 *            map containing all available flow variables
	 */
	void updateSnippet(DataTableSpec specs, Map<String, FlowVariable> flowVariables) {
		m_snippetPanel.update("", specs, flowVariables);
		this.pack();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setVisible(boolean visible) {
		/*
		 * Set the expression of the snippet panel to the expression that is shown in
		 * the table.
		 */
		if (visible) {
			final int selectedRow = m_parent.getTable().getSelectedRow();
			m_snippetPanel.setExpressions(m_parent.getTable().getModel()
					.getValueAt(selectedRow, FormulasNodeDialog.getExpressionColumn()).toString());
		}
		
		super.setVisible(visible);
	}
}
