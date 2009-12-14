/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   Sep 6, 2008 (wiswedel): created
 */
package org.knime.ext.sun.nodes.script;

import java.io.File;
import java.util.Date;

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
    private static final String CFG_HEADER = "header";

    /** NodeSettings key for the expression. */
    private static final String CFG_EXPRESSION_VERSION = "expression_version";
    
    /** NodeSettings key which column is to be replaced or appended. */
    private static final String CFG_COLUMN_NAME = "replaced_column";

    /** NodeSettings key is replace or append column? */
    private static final String CFG_IS_REPLACE = "append_column";

    /** NodeSettings key for the return type of the expression. */
    private static final String CFG_RETURN_TYPE = "return_type";

    /** NodeSettings key for whether the return type is an array (collection).*/
    private static final String CFG_IS_ARRAY_RETURN = "is_array_return";
    
    /** NodeSettings key for additional jar/zip files. */
    private static final String CFG_JAR_FILES = "java_libraries";
    
    /** NodeSettings key whether to check for compilation problems when
     * dialog closes (not used in the nodemodel, though). */
    private static final String CFG_TEST_COMPILATION =
        "test_compilation_on_dialog_close";

    private String m_expression;
    private String m_header; // added in 2.1
    private Class<?> m_returnType;
    private boolean m_isArrayReturn; // added in 2.1
    private String m_colName;
    private boolean m_isReplace;
    /** Only important for dialog: Test the syntax of the snippet code
     * when the dialog closes, bug fix #1229. */
    private boolean m_isTestCompilationOnDialogClose = true;
    private String[] m_jarFiles;
    private int m_expressionVersion = Expression.VERSION_2X;

    /** Saves current parameters to settings object. 
     * @param settings To save to.
     */
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_EXPRESSION, m_expression);
        settings.addString(CFG_HEADER, m_header);
        settings.addString(CFG_COLUMN_NAME, m_colName);
        settings.addBoolean(CFG_IS_REPLACE, m_isReplace);
        String rType = m_returnType != null ? m_returnType.getName() : null;
        settings.addBoolean(
                CFG_TEST_COMPILATION, m_isTestCompilationOnDialogClose);
        settings.addString(CFG_RETURN_TYPE, rType);
        settings.addBoolean(CFG_IS_ARRAY_RETURN, m_isArrayReturn);
        settings.addStringArray(CFG_JAR_FILES, m_jarFiles);
        settings.addInt(CFG_EXPRESSION_VERSION, m_expressionVersion);
    }

    /** Loads parameters in NodeModel. 
     * @param settings To load from.
     * @throws InvalidSettingsException If incomplete or wrong.
     */
    protected void loadSettingsInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_expression = settings.getString(CFG_EXPRESSION);
        m_header = settings.getString(CFG_HEADER, "");
        m_colName = settings.getString(CFG_COLUMN_NAME);
        m_isReplace = settings.getBoolean(CFG_IS_REPLACE);
        if (!m_isReplace && (m_colName == null || m_colName.length() == 0)) {
            throw new InvalidSettingsException("Column name must not be empty");
        }
        String returnType = settings.getString(CFG_RETURN_TYPE);
        m_returnType = getClassForReturnType(returnType);
        // this setting is not available in 1.2.x
        m_isTestCompilationOnDialogClose =
            settings.getBoolean(CFG_TEST_COMPILATION, true);
        m_isArrayReturn = settings.getBoolean(CFG_IS_ARRAY_RETURN, false);
        m_jarFiles = settings.getStringArray(CFG_JAR_FILES, (String[])null);
        for (String s : getJarFiles()) {
            if (!new File(s).isFile()) {
                throw new InvalidSettingsException("No such java library file: "
                        + s);
            }
        }
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
        m_header = settings.getString(CFG_HEADER, "");
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
        m_jarFiles = settings.getStringArray(CFG_JAR_FILES, (String[])null);
        m_isArrayReturn = settings.getBoolean(CFG_IS_ARRAY_RETURN, false);
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
     * @return the header
     */
    public String getHeader() {
        return m_header;
    }

    /**
     * @param header the header to set
     */
    void setHeader(final String header) {
        m_header = header;
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
     * @return the isArrayReturn
     */
    public boolean isArrayReturn() {
        return m_isArrayReturn;
    }

    /** 
     * @param isArrayReturn the isArrayReturn to set
     */
    void setArrayReturn(final boolean isArrayReturn) {
        m_isArrayReturn = isArrayReturn;
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
     * @return the jarFiles, never null
     */
    public String[] getJarFiles() {
        return m_jarFiles == null ? new String[0] : m_jarFiles; 
    }

    /**
     * @param jarFiles the jarFiles to set
     */
    void setJarFiles(final String[] jarFiles) {
        m_jarFiles = jarFiles;
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
        } else if (Date.class.getName().equals(returnType)) {
            return Date.class;
        } else if (String.class.getName().equals(returnType)) {
            return String.class;
        } else {
            throw new InvalidSettingsException("Not a valid return type: "
                    + returnType);
        }
    }


}
