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
package org.knime.expressions.base.node;

import org.knime.base.node.util.JSnippetPanel;
import org.knime.base.node.util.KnimeCompletionProvider;
import org.knime.base.node.util.ManipulatorProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * {@link JSnippetPanel} that is modified to use delimiters defined by the
 * {@link ExpressionCompletionProvider}.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public class ExpressionPanel extends JSnippetPanel {

	/**
	 * Standard serial version.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * See
	 * {@link JSnippetPanel#JSnippetPanel(ManipulatorProvider, KnimeCompletionProvider)}
	 */
	public ExpressionPanel(ManipulatorProvider manipulatorProvider, KnimeCompletionProvider completionProvider) {
		super(manipulatorProvider, completionProvider);
	}

	/**
	 * See
	 * {@link JSnippetPanel#JSnippetPanel(ManipulatorProvider, KnimeCompletionProvider, boolean)}
	 */
	public ExpressionPanel(ManipulatorProvider manipulatorProvider, KnimeCompletionProvider completionProvider,
			boolean showColumns) {
		super(manipulatorProvider, completionProvider, showColumns);
	}

	/**
	 * See
	 * {@link JSnippetPanel#JSnippetPanel(ManipulatorProvider, KnimeCompletionProvider, boolean, boolean)}
	 */
	public ExpressionPanel(ManipulatorProvider manipulatorProvider, KnimeCompletionProvider completionProvider,
			boolean showColumns, boolean showFlowVariables) {
		super(manipulatorProvider, completionProvider, showColumns, showFlowVariables);
	}

	/**
	 * Listener method that is called when a double click or an enter key stroke
	 * occurred on the flow variable list.
	 * 
	 * @param selected
	 *            Object that has been selected.
	 */
	@Override
	protected void onSelectionInVariableList(final Object selected) {
		if (selected instanceof FlowVariable) {
			FlowVariable v = (FlowVariable) selected;
			String enter = this.getCompletionProvider().escapeFlowVariableName(v.getName());
			this.getExpEdit().replaceSelection(enter);
			this.getFlowVarsList().clearSelection();
			this.getExpEdit().requestFocus();
		}
	}

	/**
	 * Listener method that is called when a double click or an enter key stroke
	 * occurred on the column list.
	 * 
	 * @param selected
	 *            Object that has been selected.
	 */
	@Override
	protected void onSelectionInColumnList(final Object selected) {
		if (selected != null) {
			String enter;
			if (selected instanceof String) {
				enter = ExpressionCompletionProvider.getEscapeExpressionStartSymbol() + selected
						+ ExpressionCompletionProvider.getEscapeExpressionEndSymbol();
			} else {
				DataColumnSpec colSpec = (DataColumnSpec) selected;
				String name = colSpec.getName().replace("$", "\\$");
				enter = this.getCompletionProvider().escapeColumnName(name);
			}
			this.getExpEdit().replaceSelection(enter);
			this.getColList().clearSelection();
			this.getExpEdit().requestFocus();
		}
	}
}
