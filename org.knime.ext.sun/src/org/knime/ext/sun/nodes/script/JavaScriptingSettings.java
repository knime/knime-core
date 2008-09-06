/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   Sep 6, 2008 (wiswedel): created
 */
package org.knime.ext.sun.nodes.script;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.ext.sun.nodes.script.expression.Expression;

/**
 * Settings proxy used by dialog and model implementation. 
 * @author Bernd Wwiswedel, University of Konstanz
 */
public final class JavaScriptingSettings {
    
    /** NodeSettings key for the expression. */
    private static final String CFG_EXPRESSION = "expression";

    /** NodeSettings key for the expression. */
    private static final String CFG_EXPRESSION_VERSION = "expression_version";
    
    /** NodeSettings key which column is to be replaced or appended. */
    private static final String CFG_COLUMN_NAME = "replaced_column";

    /** NodeSettings key is replace or append column? */
    private static final String CFG_IS_REPLACE = "append_column";

    /** NodeSettings key for the return type of the expression. */
    private static final String CFG_RETURN_TYPE = "return_type";

    /** NodeSettings key whether to check for compilation problems when
     * dialog closes (not used in the nodemodel, though). */
    private static final String CFG_TEST_COMPILATION =
        "test_compilation_on_dialog_close";

    private String m_expression;
    private Class<?> m_returnType;
    private String m_colName;
    private boolean m_isReplace;
    /** Only important for dialog: Test the syntax of the snippet code
     * when the dialog closes, bug fix #1229. */
    private boolean m_isTestCompilationOnDialogClose = true;
    private int m_expressionVersion = Expression.VERSION_2X;

    /** Saves current parameters to settings object. 
     * @param settings To save to.
     */
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_EXPRESSION, m_expression);
        settings.addString(CFG_COLUMN_NAME, m_colName);
        settings.addBoolean(CFG_IS_REPLACE, m_isReplace);
        String rType = m_returnType != null ? m_returnType.getName() : null;
        settings.addBoolean(
                CFG_TEST_COMPILATION, m_isTestCompilationOnDialogClose);
        settings.addString(CFG_RETURN_TYPE, rType);
        settings.addInt(CFG_EXPRESSION_VERSION, m_expressionVersion);
    }

    /** Loads parameters in NodeModel. 
     * @param settings To load from.
     * @throws InvalidSettingsException If incomplete or wrong.
     */
    protected void loadSettingsInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_expression = settings.getString(CFG_EXPRESSION);
        m_colName = settings.getString(CFG_COLUMN_NAME);
        m_isReplace = settings.getBoolean(CFG_IS_REPLACE);
        String returnType = settings.getString(CFG_RETURN_TYPE);
        m_returnType = getClassForReturnType(returnType);
        // this setting is not available in 1.2.x
        m_isTestCompilationOnDialogClose =
            settings.getBoolean(CFG_TEST_COMPILATION, true);
        m_expressionVersion = settings.getInt(
                CFG_EXPRESSION_VERSION, Expression.VERSION_1X);
    }
    
    /** Loads parameters in Dialog.
     * @param settings To load from.
     * @param spec Spec of input table.
     */
    protected void loadSettingsInDialog(final NodeSettingsRO settings,
            final DataTableSpec spec) {
        m_expression = settings.getString(CFG_EXPRESSION, "");
        String r = settings.getString(CFG_RETURN_TYPE, Double.class.getName());
        try {
            m_returnType = getClassForReturnType(r);
        } catch (InvalidSettingsException e) {
            m_returnType = Double.class;
        }
        String defaultColName = "new column";
        m_colName = settings.getString(CFG_COLUMN_NAME, defaultColName);
        m_isReplace = settings.getBoolean(CFG_IS_REPLACE, false);
        m_isTestCompilationOnDialogClose = 
            settings.getBoolean(CFG_TEST_COMPILATION, true);
        m_expressionVersion = settings.getInt(CFG_EXPRESSION_VERSION, 1);
    }

    /**
     * @return the expression
     */
    public String getExpression() {
        return m_expression;
    }

    /**
     * @param expression the expression to set
     */
    void setExpression(final String expression) {
        m_expression = expression;
    }

    /**
     * @return the returnType
     */
    public Class<?> getReturnType() {
        return m_returnType;
    }

    /**
     * @param className Name of the return class, for instance java.lang.String
     * @throws InvalidSettingsException if invalid class name.
     */
    void setReturnType(final String className) 
        throws InvalidSettingsException {
        m_returnType = getClassForReturnType(className);
    }

    /**
     * @return the colName
     */
    public String getColName() {
        return m_colName;
    }

    /**
     * @param colName the colName to set
     */
    void setColName(final String colName) {
        m_colName = colName;
    }

    /**
     * @return the isReplace
     */
    public boolean isReplace() {
        return m_isReplace;
    }

    /**
     * @param isReplace the isReplace to set
     */
    void setReplace(final boolean isReplace) {
        m_isReplace = isReplace;
    }

    /**
     * @return the isTestCompilationOnDialogClose
     */
    public boolean isTestCompilationOnDialogClose() {
        return m_isTestCompilationOnDialogClose;
    }

    /**
     * @param isTestCompilationOnDialogClose Flag to set
     */
    void setTestCompilationOnDialogClose(
            final boolean isTestCompilationOnDialogClose) {
        m_isTestCompilationOnDialogClose = isTestCompilationOnDialogClose;
    }
    
    /**
     * @return the expressionVersion
     */
    public int getExpressionVersion() {
        return m_expressionVersion;
    }
    
    /**
     * @param expressionVersion the expressionVersion to set
     */
    void setExpressionVersion(final int expressionVersion) {
        m_expressionVersion = expressionVersion;
    }

    /**
     * Get the class associated with returnType.
     *
     * @param returnType <code>Double.class.getName()</code>
     * @return the associated class
     * @throws InvalidSettingsException if the argument is invalid
     */
    static Class<?> getClassForReturnType(final String returnType)
            throws InvalidSettingsException {
        if (Integer.class.getName().equals(returnType)) {
            return Integer.class;
        } else if (Double.class.getName().equals(returnType)) {
            return Double.class;
        } else if (String.class.getName().equals(returnType)) {
            return String.class;
        } else {
            throw new InvalidSettingsException("Not a valid return type: "
                    + returnType);
        }
    }


}
