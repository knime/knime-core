/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   02.09.2008 (thor): created
 */
package org.knime.base.node.meta.looper.condition;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.ScopeVariable;
import org.knime.core.node.workflow.ScopeVariable.Type;

/**
 * This class holds the settings for the condition loop tail node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ConditionLoopTailSettings {
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

    private boolean m_addLastRows = false;

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
    public void variable(final ScopeVariable var) {
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
     * Sets the value the flow variable should be compared with. It can
     * either be a number or a string, depending on the flow variables type.
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

        m_addLastRows = settings.getBoolean("addLastRows", false);
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
    }
}
