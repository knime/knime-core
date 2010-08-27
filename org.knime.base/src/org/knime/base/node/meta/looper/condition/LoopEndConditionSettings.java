/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   02.09.2008 (thor): created
 */
package org.knime.base.node.meta.looper.condition;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * This class holds the settings for the condition loop tail node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopEndConditionSettings {
    /** All comparison operators the user can choose from in the dialog. */
    public enum Operator {
        /** Numeric greater than. */
        GT(">"),
        /** Numeric lower than. */
        LT("<"),
        /** Numeric greater than or equal. */
        GE(">="),
        /** Numeric lower than or equal. */
        LE("<="),
        /** Numeric or string unequal. */
        NE("!="),
        /** Numeric or string equal. */
        EQ("=");

        private final String m_represent;

        private Operator(final String represent) {
            m_represent = represent;
        }

        private Operator() {
            m_represent = null;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            if (m_represent != null) {
                return m_represent;
            }
            return super.toString();
        }
    }

    private String m_variableName;

    private Type m_variableType;

    private Operator m_operator;

    private String m_value;

    private boolean m_addLastRows = true;

    private boolean m_addLastRowsOnly = false;

    private boolean m_addIterationColumn = true;

    /**
     * Returns if the rows from the loop's last iteration should be added to the
     * output table or not.
     *
     * @return <code>true</code> if the rows should be added,
     *         <code>false</code> otherwise
     */
    public boolean addLastRows() {
        return m_addLastRows;
    }

    /**
     * Sets if only the rows from the loop's last iteration should be added
     * to the output table or not.
     *
     * @param b <code>true</code> if the only the last rows should be added,
     *         <code>false</code> otherwise
     */
    public void addLastRowsOnly(final boolean b) {
        m_addLastRowsOnly = b;
    }

    /**
     * Returns if only the rows from the loop's last iteration should be added
     * to the output table or not.
     *
     * @return <code>true</code> if the only the last rows should be added,
     *         <code>false</code> otherwise
     */
    public boolean addLastRowsOnly() {
        return m_addLastRowsOnly;
    }

    /**
     * Sets if the rows from the loop's last iteration should be added to the
     * output table or not.
     *
     * @param b <code>true</code> if the rows should be added,
     *            <code>false</code> otherwise
     */
    public void addLastRows(final boolean b) {
        m_addLastRows = b;
    }

    /**
     * Sets if a column containing the iteration number should be appended to
     * the output table.
     *
     * @param add <code>true</code> if a column should be added,
     *            <code>false</code> otherwise
     */
    public void addIterationColumn(final boolean add) {
        m_addIterationColumn = add;
    }

    /**
     * Returns if a column containing the iteration number should be appended to
     * the output table.
     *
     * @return <code>true</code> if a column should be added,
     *            <code>false</code> otherwise
     */
    public boolean addIterationColumn() {
        return m_addIterationColumn;
    }

    /**
     * Returns the flow variable's name which is checked in each iteration.
     *
     * @return the variable's name
     */
    public String variableName() {
        return m_variableName;
    }

    /**
     * Returns the flow variable's type which is checked in each iteration.
     *
     * @return the variable's type
     */
    public Type variableType() {
        return m_variableType;
    }

    /**
     * Sets the flow variable which is checked in each iteration.
     *
     * @param var the flow variable
     */
    public void variable(final FlowVariable var) {
        if (var == null) {
            m_variableName = null;
            m_variableType = null;
        } else {
            m_variableName = var.getName();
            m_variableType = var.getType();
        }
    }

    /**
     * Returns the operator that should be used in the comparison between
     * variable and value.
     *
     * @return the comparison operator
     */
    public Operator operator() {
        return m_operator;
    }

    /**
     * Sets the operator that should be used in the comparison between variable
     * and value.
     *
     * @param op the comparison operator
     */
    public void operator(final Operator op) {
        m_operator = op;
    }

    /**
     * Returns the value the flow variable should be compared with. It can
     * either be a number or a string, depending on the flow variables type.
     *
     * @return the value
     */
    public String value() {
        return m_value;
    }

    /**
     * Sets the value the flow variable should be compared with. It can either
     * be a number or a string, depending on the flow variables type.
     *
     * @param s the value
     */
    public void value(final String s) {
        m_value = s;
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_variableName = settings.getString("variableName");
        String s = settings.getString("variableType");
        if (s == null) {
            m_variableType = null;
        } else {
            m_variableType = Type.valueOf(s);
        }
        m_value = settings.getString("value");

        s = settings.getString("operator");
        if (s == null) {
            m_operator = null;
        } else {
            m_operator = Operator.valueOf(s);
        }

        m_addLastRows = settings.getBoolean("addLastRows");
        m_addLastRowsOnly = settings.getBoolean("addLastRowsOnly");
        m_addIterationColumn = settings.getBoolean("addIterationColumn", true);
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_variableName = settings.getString("variableName", null);
        m_value = settings.getString("value", "");

        String s = settings.getString("variableType", null);
        if (s == null) {
            m_variableType = null;
        } else {
            m_variableType = Type.valueOf(s);
        }

        s = settings.getString("operator", Operator.EQ.name());
        if (s == null) {
            m_operator = null;
        } else {
            m_operator = Operator.valueOf(s);
        }

        m_addLastRows = settings.getBoolean("addLastRows", true);
        m_addLastRowsOnly = settings.getBoolean("addLastRowsOnly", false);
        m_addIterationColumn = settings.getBoolean("addIterationColumn", true);
    }

    /**
     * Writes the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("variableName", m_variableName);
        if (m_variableType == null) {
            settings.addString("variableType", null);
        } else {
            settings.addString("variableType", m_variableType.name());
        }
        settings.addString("value", m_value);
        if (m_operator == null) {
            settings.addString("operator", null);
        } else {
            settings.addString("operator", m_operator.name());
        }
        settings.addBoolean("addLastRows", m_addLastRows);
        settings.addBoolean("addLastRowsOnly", m_addLastRowsOnly);
        settings.addBoolean("addIterationColumn", m_addIterationColumn);
    }
}
