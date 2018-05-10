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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 6, 2008 (wiswedel): created
 */
package org.knime.ext.sun.nodes.script.settings;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.ext.sun.nodes.script.compile.CompilationFailedException;
import org.knime.ext.sun.nodes.script.expression.Expression;

/**
 * Settings proxy used by dialog and model implementation.
 * @author Bernd Wiswedel, University of Konstanz
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

    /** NodeSettings key how to treat missing values. */
    private static final String CFG_INSERT_MISSING_AS_NULL =
        "insert_missing_as_null";

    /** NodeSettings key for imports used by the snippet. */
    private static final String CFG_IMPORTS = "imports";

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

    /** if true any missing value in the (relevant) input will result
     * in a "missing" result. */
    private boolean m_insertMissingAsNull = false;

    /** The compiled version is stored because it is expensive to create it. Do
     * not rely on its existence! */
    private Expression m_compiledExpression = null;
    /** Spec to compiled expression. */
    private DataTableSpec m_inputSpec;

    private final JavaScriptingCustomizer m_customizer;

    /** Imports used by the snippet. */
    private String[] m_imports;


    /** New settings for given customizer. */
    public JavaScriptingSettings(final JavaScriptingCustomizer customizer) {
        m_customizer = customizer;
    }

    /** Saves current parameters to settings object.
     * @param settings To save to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_EXPRESSION, m_expression);
        settings.addString(CFG_HEADER, m_header);
        settings.addString(CFG_COLUMN_NAME, m_colName);
        settings.addBoolean(CFG_IS_REPLACE, m_isReplace);
        String rType = m_returnType != null ? m_returnType.getName() : null;
        settings.addBoolean(
                CFG_TEST_COMPILATION, m_isTestCompilationOnDialogClose);
        settings.addBoolean(CFG_INSERT_MISSING_AS_NULL, m_insertMissingAsNull);
        settings.addString(CFG_RETURN_TYPE, rType);
        settings.addBoolean(CFG_IS_ARRAY_RETURN, m_isArrayReturn);
        settings.addStringArray(CFG_JAR_FILES, m_jarFiles);
        settings.addInt(CFG_EXPRESSION_VERSION, m_expressionVersion);
        settings.addStringArray(CFG_IMPORTS, m_imports);

    }

    /** Loads parameters in NodeModel.
     * @param settings To load from.
     * @throws InvalidSettingsException If incomplete or wrong.
     */
    public void loadSettingsInModel(final NodeSettingsRO settings)
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
        // added in v2.3
        m_insertMissingAsNull  =
            settings.getBoolean(CFG_INSERT_MISSING_AS_NULL, false);
        m_isArrayReturn = settings.getBoolean(CFG_IS_ARRAY_RETURN, false);
        m_jarFiles = settings.getStringArray(CFG_JAR_FILES, (String[])null);
        m_expressionVersion = settings.getInt(
                CFG_EXPRESSION_VERSION, Expression.VERSION_1X);
        // added in v2.5
        m_imports = settings.getStringArray(CFG_IMPORTS, (String[])null);
    }

    /** Loads parameters in Dialog.
     * @param settings To load from.
     * @param spec Spec of input table.
     */
    public void loadSettingsInDialog(final NodeSettingsRO settings,
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
        // added in v2.3
        m_insertMissingAsNull  =
            settings.getBoolean(CFG_INSERT_MISSING_AS_NULL, false);
        m_jarFiles = settings.getStringArray(CFG_JAR_FILES, (String[])null);
        m_isArrayReturn = settings.getBoolean(CFG_IS_ARRAY_RETURN, false);
        m_expressionVersion = settings.getInt(CFG_EXPRESSION_VERSION, 1);
        // added in v2.5
        m_imports = settings.getStringArray(CFG_IMPORTS, (String[])null);
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
    public void setExpression(final String expression) {
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
    public void setHeader(final String header) {
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
    public void setReturnType(final String className)
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
    public void setArrayReturn(final boolean isArrayReturn) {
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
    public void setColName(final String colName) {
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
    public void setReplace(final boolean isReplace) {
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
    public void setTestCompilationOnDialogClose(
            final boolean isTestCompilationOnDialogClose) {
        m_isTestCompilationOnDialogClose = isTestCompilationOnDialogClose;
    }

    /** @return the insertMissingAsNull */
    public boolean isInsertMissingAsNull() {
        return m_insertMissingAsNull;
    }

    /** @param insertMissingAsNull the insertMissingAsNull to set */
    public void setInsertMissingAsNull(final boolean insertMissingAsNull) {
        m_insertMissingAsNull = insertMissingAsNull;
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
    public void setExpressionVersion(final int expressionVersion) {
        m_expressionVersion = expressionVersion;
    }

    /**
     * @return the jarFiles, never null
     */
    public String[] getJarFiles() {
        return m_jarFiles == null ? new String[0] : m_jarFiles;
    }

    /** Get jar files as file objects. Will also handle the case where the
     * file is specified via URL (file:/...)
     * @return The registered jar files in a File[]
     * @throws InvalidSettingsException If any file is not present.
     */
    public File[] getJarFilesAsFiles() throws InvalidSettingsException {
        String[] jarLocations = getJarFiles();
        File[] resultFiles = new File[jarLocations.length];
        for (int i = 0; i < jarLocations.length; i++) {
            resultFiles[i] = toFile(jarLocations[i]);
        }
        return resultFiles;
    }

    /**
     * Get list of imports or null if defaults should be kept.
     * @return list of imports
     */
    public String[] getImports() {
        return m_imports;
    }

    /**
     * Set list of imports or null if default imports should be used.
     * @param imports the list of imports
     */
    public void setImports(final String[] imports) {
        m_imports = imports;
    }

    /** Convert jar file location to File. Also accepts file in URL format
     * (e.g. local drop files as URL).
     * @param location The location string.
     * @return The file to the location
     * @throws InvalidSettingsException if argument is null, empty or the file
     * does not exist.
     */
    public static final File toFile(final String location)
        throws InvalidSettingsException {
        if (location == null || location.length() == 0) {
            throw new InvalidSettingsException(
                    "Invalid (empty) jar file location");
        }
        File result;
        if (location.startsWith("file:/")) {
            try {
                URL fileURL = new URL(location);
                result = new File(fileURL.toURI());
            } catch (Exception e) {
                throw new InvalidSettingsException("Can't read file "
                        + "URL \"" + location + "\"; invalid class path", e);
            }
        } else {
            result = new File(location);
        }
        if (!result.exists()) {
            throw new InvalidSettingsException("Can't read file \""
                    + location + "\"; invalid class path");
        }
        return result;
    }

    /**
     * @param jarFiles the jarFiles to set
     */
    public void setJarFiles(final String[] jarFiles) {
        m_jarFiles = jarFiles;
    }

    /** Set input data, compile expression if necessary.
     * @param spec The input spec.
     * @throws CompilationFailedException If compilation fails.
     * @throws InvalidSettingsException If settins are incorrect.
     */
    public void setInputAndCompile(final DataTableSpec spec)
        throws CompilationFailedException, InvalidSettingsException {
        if ((m_compiledExpression == null) || (!spec.equalStructure(m_inputSpec))) {
            discard();
            // if the spec changes, we need to re-compile the expression
            m_compiledExpression = Expression.compile(this, spec);
            m_inputSpec = spec;
        }
    }

    /** Cleans up the old {@link Expression} object that is kept as member. Called when node is discarded or a new
     * expression is to be compiled.
     * @since 3.6 */
    public void discard() {
        if (m_compiledExpression != null) {
            try {
                m_compiledExpression.close();
            } catch (IOException ioe) {
                NodeLogger.getLogger(getClass()).warn("Unable to clean up old Expression object", ioe);
            }
            m_compiledExpression = null;
        }
    }

    /** @return the inputSpec */
    public DataTableSpec getInputSpec() {
        return m_inputSpec;
    }

    /** Get the compiled expression or null.
     * @return the compiledExpression
     * @see #setInputAndCompile(DataTableSpec)
     */
    public Expression getCompiledExpression() {
        return m_compiledExpression;
    }

    /** The column spec of the generated column.
     * @return The col spec.
     * @throws InvalidSettingsException If settings are inconsistent.
     */
    public DataColumnSpec getNewColSpec() throws InvalidSettingsException {
        Class<?> returnType = getReturnType();
        String colName = getColName();
        boolean isArrayReturn = isArrayReturn();
        DataType type = null;
        for (JavaSnippetType<?, ?, ?> t : JavaSnippetType.TYPES) {
            if (t.getJavaClass(false).equals(returnType)) {
                type = t.getKNIMEDataType(isArrayReturn);
            }
        }
        CheckUtils.checkSettingNotNull(type, "Illegal return type: %s", returnType.getName());
        if (isReplace()) {
            return new DataColumnSpecCreator(colName, type).createSpec();
        } else {
            return new UniqueNameGenerator(m_inputSpec).newColumn(colName, type);
        }
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
        } else if (Boolean.class.getName().equals(returnType)) {
            return Boolean.class;
        } else if (Long.class.getName().equals(returnType)) {
            return Long.class;
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
