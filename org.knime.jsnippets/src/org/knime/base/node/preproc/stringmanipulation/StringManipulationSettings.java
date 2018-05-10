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
 * ------------------------------------------------------------------------
 *
 * History
 *   30.09.2011 (hofer): created
 */
package org.knime.base.node.preproc.stringmanipulation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.knime.base.node.preproc.stringmanipulation.manipulator.Manipulator;
import org.knime.base.node.util.ManipulatorProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.ext.sun.nodes.script.calculator.ColumnCalculator;
import org.knime.ext.sun.nodes.script.expression.Expression;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingSettings;
import org.knime.ext.sun.nodes.script.settings.JavaSnippetType;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * The settings for the string manipulation node.
 *
 * @author Heiko Hofer
 */
public class StringManipulationSettings {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(StringManipulationSettings.class);
    /** NodeSettings key for the expression. */
    private static final String CFG_EXPRESSION = "expression";

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

    /** NodeSettings key how to treat missing values. */
    private static final String CFG_INSERT_MISSING_AS_NULL =
        "insert_missing_as_null";

    private String m_expression;
    private Class<?> m_returnType;

    private String m_colName;
    private boolean m_isReplace;
    /** Only important for dialog: Test the syntax of the snippet code
     * when the dialog closes, bug fix #1229. */
    private boolean m_isTestCompilationOnDialogClose = true;

    /** if true any missing value in the (relevant) input will result
     * in a "missing" result. */
    private boolean m_insertMissingAsNull = false;

    private JavaScriptingSettings m_javaScriptingSettings;

    /** Saves current parameters to settings object.
     * @param settings To save to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_EXPRESSION, m_expression);
        settings.addString(CFG_COLUMN_NAME, m_colName);
        settings.addBoolean(CFG_IS_REPLACE, m_isReplace);
        String rType = m_returnType != null ? m_returnType.getName() : null;
        settings.addBoolean(
                CFG_TEST_COMPILATION, m_isTestCompilationOnDialogClose);
        settings.addBoolean(CFG_INSERT_MISSING_AS_NULL, m_insertMissingAsNull);
        settings.addString(CFG_RETURN_TYPE, rType);
    }

    /** Loads parameters in NodeModel.
     * @param settings To load from.
     * @throws InvalidSettingsException If incomplete or wrong.
     */
    public void loadSettingsInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_expression = settings.getString(CFG_EXPRESSION);
        m_colName = settings.getString(CFG_COLUMN_NAME);
        m_isReplace = settings.getBoolean(CFG_IS_REPLACE);
        if (!m_isReplace && (m_colName == null || m_colName.length() == 0)) {
            throw new InvalidSettingsException("Column name must not be empty");
        }
        String returnType = settings.getString(CFG_RETURN_TYPE, null);
        m_returnType = null == returnType ? null
                : getClassForReturnType(returnType);
        // this setting is not available in 1.2.x
        m_isTestCompilationOnDialogClose =
            settings.getBoolean(CFG_TEST_COMPILATION, true);
        // added in v2.3
        m_insertMissingAsNull  =
            settings.getBoolean(CFG_INSERT_MISSING_AS_NULL, false);
        // only discards previous JavaScriptSettings
        discard();
    }

    /** Loads parameters in Dialog.
     * @param settings To load from.
     * @param spec Spec of input table.
     */
    public void loadSettingsInDialog(final NodeSettingsRO settings,
            final DataTableSpec spec) {
        m_expression = settings.getString(CFG_EXPRESSION, "");
        String returnType = settings.getString(CFG_RETURN_TYPE, null);
        try {
            m_returnType = null == returnType ? null
                    : getClassForReturnType(returnType);
        } catch (InvalidSettingsException e) {
            m_returnType = null;
        }
        String defaultColName = "new column";
        m_colName = settings.getString(CFG_COLUMN_NAME, defaultColName);
        m_isReplace = settings.getBoolean(CFG_IS_REPLACE, false);
        m_isTestCompilationOnDialogClose =
            settings.getBoolean(CFG_TEST_COMPILATION, true);
        // added in v2.3
        m_insertMissingAsNull  =
            settings.getBoolean(CFG_INSERT_MISSING_AS_NULL, false);
        // only discards previous JavaScriptingSettings
        discard();
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
        m_returnType = null;
    }

    /**
     * @return the returnType
     */
    public Class<?> getReturnType() {
        return m_returnType;
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

    /** The column spec of the generated column.
     * @return The col spec.
     * @throws InvalidSettingsException If settings are inconsistent.
     */
    public DataColumnSpec getNewColSpec() throws InvalidSettingsException {
        Class<?> returnType = getReturnType();
        String colName = getColName();
        boolean isArrayReturn = false;
        DataType type = null;
        for (JavaSnippetType<?, ?, ?> t : JavaSnippetType.TYPES) {
            if (t.getJavaClass(false).equals(returnType)) {
                type = t.getKNIMEDataType(isArrayReturn);
            }
        }
        if (type == null) {
            throw new InvalidSettingsException("Illegal return type: "
                    + returnType.getName());
        }
        return new DataColumnSpecCreator(colName, type).createSpec();
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

    private Class<?> determineReturnType(final String expression)
        throws InvalidSettingsException {
        if (expression.isEmpty()) {
            throw new InvalidSettingsException(
                    "Empty expressions are not supported.");
        }
        int endIndex = StringUtils.indexOf(expression, '(');
        if (endIndex < 0) {
            throw new InvalidSettingsException(getAmbigiousReturnTypeMessage());
        }
        String function = expression.substring(0, endIndex);

        StringManipulatorProvider provider =
            StringManipulatorProvider.getDefault();
        // Add StringManipulators to the imports
        Collection<Manipulator> manipulators =
            provider.getManipulators(ManipulatorProvider.ALL_CATEGORY);
        Class<?> returnType = null;
        for (Manipulator manipulator : manipulators) {
            if (function.equals(manipulator.getName())) {
                returnType = manipulator.getReturnType();
            }
        }
        if (null == returnType) {
            throw new InvalidSettingsException(getAmbigiousReturnTypeMessage());
        }
        return returnType;

    }

    private String getAmbigiousReturnTypeMessage() {
        return "Ambiguous return type! "
        + "Use 'string()' or another function from the \"Convert Type\" "
        + "category to specify the return type.";
    }

    /** Discards the compiled expressions (e.g. temporary .java files, closing URL class loaders).
     * @since 3.6
     */
    public void discard() {
        if (m_javaScriptingSettings != null) {
            m_javaScriptingSettings.discard();
            m_javaScriptingSettings = null;
        }
    }

    /**
     * Create settings to be used by {@link ColumnCalculator} in order
     * to execute the expression.
     *
     * @return settings java scripting settings
     * @throws InvalidSettingsException when settings are not correct
     * @since 3.6
     */
    public JavaScriptingSettings getJavaScriptingSettings()
        throws InvalidSettingsException {
        if (m_javaScriptingSettings == null) {
            // determine return type
            m_returnType = null == m_returnType
                ? determineReturnType(StringUtils.strip(m_expression))
                : m_returnType;

            JavaScriptingSettings s = new JavaScriptingSettings(null);
            s.setArrayReturn(false);
            s.setColName(this.getColName());
            s.setExpression("return " + this.getExpression() + ";");
            s.setExpressionVersion(Expression.VERSION_2X);
            s.setHeader("");
            s.setInsertMissingAsNull(this.isInsertMissingAsNull());
            Bundle bundle = FrameworkUtil.getBundle(this.getClass());
            try {
                List<String> includes = new ArrayList<String>();
                URL snippetIncURL = FileLocator.find(bundle,
                        new Path("/lib/snippet_inc"), null);
                File includeDir = new File(
                        FileLocator.toFileURL(snippetIncURL).getPath());
                for (File includeJar : includeDir.listFiles()) {
                    if (includeJar.isFile()
                            && includeJar.getName().endsWith(".jar")) {
                        includes.add(includeJar.getPath());
                        LOGGER.debug("Include jar file: "
                                + includeJar.getPath());
                    }
                }
                StringManipulatorProvider provider =
                    StringManipulatorProvider.getDefault();
                includes.add(provider.getJarFile().getAbsolutePath());
                includes.add(FileLocator.getBundleFile(FrameworkUtil.getBundle(StringUtils.class)).getAbsolutePath());
                s.setJarFiles(includes.toArray(new String[includes.size()]));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot locate necessary libraries due to I/O problem: " + e.getMessage(),
                    e);
            }
            s.setReplace(this.isReplace());
            s.setReturnType(m_returnType.getName());
            s.setTestCompilationOnDialogClose(
                    this.isTestCompilationOnDialogClose());
            List<String> imports = new ArrayList<String>();
            // Use defaults imports
            imports.addAll(Arrays.asList(Expression.getDefaultImports()));
            StringManipulatorProvider provider =
                StringManipulatorProvider.getDefault();
            // Add StringManipulators to the imports
            Collection<Manipulator> manipulators =
                provider.getManipulators(ManipulatorProvider.ALL_CATEGORY);
            for (Manipulator manipulator : manipulators) {
                String toImport = manipulator.getClass().getName();
                imports.add("static " + toImport + ".*");
            }
            s.setImports(imports.toArray(new String[imports.size()]));
            m_javaScriptingSettings = s;
        }
        return m_javaScriptingSettings;
    }

}
