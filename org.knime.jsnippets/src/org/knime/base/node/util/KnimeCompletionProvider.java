/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   21.03.2012 (meinl): created
 */
package org.knime.base.node.util;

import java.util.ArrayList;
import java.util.List;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * A provider for auto-completion that adds completions for column names and
 * flow variables.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.6
 */
public abstract class KnimeCompletionProvider extends DefaultCompletionProvider {
    private final List<Completion> m_columnCompletions =
            new ArrayList<Completion>();

    private final List<Completion> m_flowVariableCompletions =
            new ArrayList<Completion>();

    /**
     * Escapes a column name, e.g. by delimiting it with "$".
     *
     * @param colName the column's name
     * @return the escaped column name
     */
    public abstract String escapeColumnName(String colName);

    /**
     * Escapes a column name, e.g. by delimiting it with "${" and "$}".
     *
     * @param varName the flow variables names
     * @return the escaped variable name
     */
    public abstract String escapeFlowVariableName(String varName);

    /**
     * Set columns that should be shown in the code completion box.
     *
     * @param columns the columns
     */
    public void setColumns(final Iterable<DataColumnSpec> columns) {
        if (m_columnCompletions.size() > 0) {
            for (Completion c : m_columnCompletions) {
                removeCompletion(c);
            }
        }
        m_columnCompletions.clear();
        for (DataColumnSpec column : columns) {
            m_columnCompletions
                    .add(new BasicCompletion(this, escapeColumnName(column
                            .getName()), column.getType().toString(),
                            "The column " + column.getName() + "."));
        }
        addCompletions(m_columnCompletions);
    }

    /**
     * Set flow variables that should be shown in the code completion box.
     *
     * @param variables flow variables
     */
    public void setFlowVariables(final Iterable<FlowVariable> variables) {
        if (m_flowVariableCompletions.size() > 0) {
            for (Completion c : m_flowVariableCompletions) {
                removeCompletion(c);
            }
        }
        m_flowVariableCompletions.clear();
        for (FlowVariable var : variables) {
            String typeChar;
            switch (var.getType()) {
                case DOUBLE:
                    typeChar = "D";
                    break;
                case INTEGER:
                    typeChar = "I";
                    break;
                case STRING:
                    typeChar = "S";
                    break;
                default:
                    return;
            }
            m_flowVariableCompletions.add(new BasicCompletion(this, "$${"
                    + typeChar + var.getName() + "}$$", var.getType()
                    .toString(), "The flow variable " + var.getName() + "."));
        }
        addCompletions(m_flowVariableCompletions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isValidChar(final char ch) {
        // FIXME: this is not perfect, since columns can contain any unicode
        // character. The solution would be to add semantics to
        // CompletionProvider::getParameterizedCompletions(...).
        // for flow variables
        if (!m_flowVariableCompletions.isEmpty() && (ch == '{' || ch == '}')) {
            return true;
        } else {
            // $ is needed for KNIME specials (columns, flowvariables)
            return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$';
        }
    }
}
