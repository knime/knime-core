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

import java.util.Arrays;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.expressions.core.DefaultScriptRowInput;
import org.knime.expressions.core.Expressions;
import org.knime.expressions.core.FunctionScript;
import org.knime.expressions.core.ScriptRowInput;
import org.knime.expressions.core.exceptions.ScriptCompilationException;
import org.knime.expressions.core.exceptions.ScriptExecutionException;
import org.knime.expressions.core.exceptions.ScriptParseException;

/**
 * {@link CellFactory} for expressions. Allows to run multiple expressions and
 * outputs the result of each expression in its own {@link DataCell}.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
public class MultiExpressionCellFactory extends AbstractCellFactory {

	private final FunctionScript<ScriptRowInput, DataCell>[] m_functions;
	private long m_rowindex = 1;
	private final long m_rowCount;
	private final NodeLogger m_logger;
	private final String[] m_expressions;
	private final boolean[] m_shownWarningMessage;

	@SuppressWarnings("unchecked")
	public MultiExpressionCellFactory(DataColumnSpec[] inSpec, DataColumnSpec[] originalSpec, String[] expressions,
			DataType[] resultTypes, Map<String, FlowVariable> flowVariables, long rowCount, NodeLogger nodeLogger) {
		super(inSpec);

		if (resultTypes.length != expressions.length) {
			throw new IllegalArgumentException(
					"Number of provided resulting data types is not equal to the number of provided expressions.");
		}

		this.m_rowCount = rowCount;
		this.m_logger = nodeLogger;
		this.m_expressions = expressions;

		this.m_shownWarningMessage = new boolean[expressions.length];
		Arrays.fill(m_shownWarningMessage, false);

		FlowVariable[] variables = new FlowVariable[flowVariables.size()];

		flowVariables.values().toArray(variables);

		try {
			m_functions = new FunctionScript[m_expressions.length];

			for (int i = 0; i < expressions.length; i++) {

				m_functions[i] = Expressions.wrap(expressions[i], originalSpec, variables, resultTypes[i]);
			}
		} catch (ScriptParseException | ScriptCompilationException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataCell[] getCells(DataRow row) {
		DataCell[] returnCells = new DataCell[m_functions.length];

		for (int i = 0; i < m_functions.length; i++) {
			DefaultScriptRowInput rowInput = new DefaultScriptRowInput(row, m_rowindex, m_rowCount);

			try {
				returnCells[i] = m_functions[i].apply(rowInput);
			} catch (ScriptExecutionException e) {
				returnCells[i] = new MissingCell("");

				if (!m_shownWarningMessage[i]) {
					m_logger.warn("An error occurred in the expression: \n"// + e.getMessage() + " \n\nexpression:\n"
							+ m_expressions[i]);

					m_shownWarningMessage[i] = true;
				}
			}
		}

		m_rowindex++;

		return returnCells;
	}
}
