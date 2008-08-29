/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.ext.sun.nodes.script;

import java.io.File;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.ext.sun.nodes.script.expression.CompilationFailedException;
import org.knime.ext.sun.nodes.script.expression.Expression;
import org.knime.ext.sun.nodes.script.expression.Expression.ExpressionField;
import org.knime.ext.sun.nodes.script.expression.Expression.FieldType;
import org.knime.ext.sun.nodes.script.expression.Expression.InputField;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JavaScriptingNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(JavaScriptingNodeModel.class);

    /** NodeSettings key for the expression. */
    protected static final String CFG_EXPRESSION = "expression";

    /** NodeSettings key which column is to be replaced or appended. */
    protected static final String CFG_COLUMN_NAME = "replaced_column";

    /** NodeSettings key is replace or append column? */
    protected static final String CFG_IS_REPLACE = "append_column";

    /** NodeSettings key for the return type of the expression. */
    protected static final String CFG_RETURN_TYPE = "return_type";

    /** NodeSettings key whether to check for compilation problems when
     * dialog closes (not used in the nodemodel, though). */
    protected static final String CFG_TEST_COMPILATION =
        "test_compilation_on_dialog_close";

    private String m_expression;

    /* the compiled version is stored because it is expensive to create it. Do
     * not rely on its existence!
     */
    private Expression m_compiledExpression = null;

   /* The input table spec at the time the above expression was compiled
    */
    private DataTableSpec m_inputSpec = null;

    private Class<?> m_returnType;

    private String m_colName;

    private boolean m_isReplace;

    /** Only important for dialog: Test the syntax of the snippet code
     * when the dialog closes, bug fix #1229. */
    private boolean m_isTestCompilationOnDialogClose = true;

    private File m_tempFile;

    /** One input, one output. */
    public JavaScriptingNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_EXPRESSION, m_expression);
        settings.addString(CFG_COLUMN_NAME, m_colName);
        settings.addBoolean(CFG_IS_REPLACE, m_isReplace);
        String rType = m_returnType != null ? m_returnType.getName() : null;
        settings.addBoolean(
                CFG_TEST_COMPILATION, m_isTestCompilationOnDialogClose);
        settings.addString(CFG_RETURN_TYPE, rType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getString(CFG_EXPRESSION);
        settings.getString(CFG_COLUMN_NAME);
        settings.getBoolean(CFG_IS_REPLACE);
        String returnType = settings.getString(CFG_RETURN_TYPE);
        getClassForReturnType(returnType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_expression = settings.getString(CFG_EXPRESSION);
        // after we got a new expression delete the compiled version of it.
        m_compiledExpression = null;
        m_inputSpec = null;

        m_colName = settings.getString(CFG_COLUMN_NAME);
        m_isReplace = settings.getBoolean(CFG_IS_REPLACE);
        String returnType = settings.getString(CFG_RETURN_TYPE);
        m_returnType = getClassForReturnType(returnType);
        // this setting is not available in 1.2.x
        m_isTestCompilationOnDialogClose =
            settings.getBoolean(CFG_TEST_COMPILATION, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        ColumnRearranger c = createColumnRearranger(inSpec);
        BufferedDataTable o = exec.createColumnRearrangeTable(
                inData[0], c, exec);
        return new BufferedDataTable[]{o};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_expression == null) {
            throw new InvalidSettingsException("No expression has been set.");
        }
        ColumnRearranger c = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{c.createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec)
            throws InvalidSettingsException {
        try {
            if ((m_compiledExpression == null)
                    || (!m_inputSpec.equalStructure(spec))) {
                // if the spec changes, we need to re-compile the expression
                if (m_tempFile == null) {
                    m_tempFile = createTempFile();
                }
                m_compiledExpression =
                    compile(m_expression, spec, m_returnType, m_tempFile);
                m_inputSpec = spec;
            }
            assert m_inputSpec != null;
            DataColumnSpec newColSpec = getNewColSpec();
            ColumnCalculator cc = new ColumnCalculator(this, newColSpec);
            ColumnRearranger result = new ColumnRearranger(spec);
            if (m_isReplace) {
                result.replace(cc, m_colName);
            } else {
                result.append(cc);
            }
            return result;
        } catch (Exception e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
    }
    
    /** Reads a variable from this node model. Calls for instance
     * {@link #peekScopeVariableDouble(String)}.
     * @param name The name of variable.
     * @param type Type of variable.
     * @return The value
     */
    Object readVariable(final String name, final Class<?> type) {
        if (Integer.class.equals(type)) {
            return peekScopeVariableInt(name);
        } else if (Double.class.equals(type)) {
            return peekScopeVariableDouble(name);
        } else if (String.class.equals(type)) {
            return peekScopeVariableString(name);
        } else {
            throw new RuntimeException("Invalid variable class: " + type);
        }
    }
    
    /**
     * @return the returnType
     */
    Class<?> getReturnType() {
        return m_returnType;
    }

    /**
     * @return the compiledExpression
     */
    Expression getCompiledExpression() {
        return m_compiledExpression;
    }
    
    /**
     * @return the inputSpec
     */
    DataTableSpec getInputSpec() {
        return m_inputSpec;
    }
    
    /** Creates an returns a temp java file, for which no .class file exists
     * yet.
     * @return The temporary java file to use.
     * @throws IOException If that fails for whatever reason.
     */
    static File createTempFile() throws IOException {
        // what follows: create a temp file, check if the corresponding
        // class file exists and if so, generate the next temp file
        // (we can't use a temp file, for which a class file already exists).
        while (true) {
            File tempFile = File.createTempFile("Expression", ".java");
            File classFile = getAccompanyingClassFile(tempFile);
            if (classFile.exists()) {
                tempFile.delete();
            } else {
                tempFile.deleteOnExit();
                return tempFile;
            }
        }
    }

    /** Determine the class file name of the javaFile argument. Needed to
     * check if temp file is ok and on exit (to delete all traces of this node).
     * @param javaFile The file name of java file
     * @return The class file (may not exist (yet)).
     */
    static File getAccompanyingClassFile(final File javaFile) {
        if (javaFile == null || !javaFile.getName().endsWith(".java")) {
            throw new IllegalArgumentException("Can't determine class file for"
                    + " non-java file: " + javaFile);
        }
        File parent = javaFile.getParentFile();
        String prefixName = javaFile.getName().substring(
                0, javaFile.getName().length() - ".java".length());
        String classFileName = prefixName + ".class";
        return new File(parent, classFileName);
    }

    private DataColumnSpec getNewColSpec() throws InvalidSettingsException {
        DataType cellReturnType;
        if (m_returnType.equals(Integer.class)) {
            cellReturnType = IntCell.TYPE;
        } else if (m_returnType.equals(Double.class)) {
            cellReturnType = DoubleCell.TYPE;
        } else if (m_returnType.equals(String.class)) {
            cellReturnType = StringCell.TYPE;
        } else {
            throw new InvalidSettingsException("Illegal return type: "
                    + m_returnType.getName());
        }
        return new DataColumnSpecCreator(m_colName, cellReturnType)
                .createSpec();
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

    /**
     * Tries to compile the given expression as entered in the dialog with the
     * current spec.
     *
     * @param expression the expression from dialog or settings
     * @param spec the spec
     * @param rType the return type, e.g. <code>Integer.class</code>
     * @param tempFile the file to use
     * @return the java expression
     * @throws CompilationFailedException if that fails
     * @throws InvalidSettingsException if settings are missing
     */
    static Expression compile(final String expression,
            final DataTableSpec spec, final Class<?> rType, final File tempFile)
            throws CompilationFailedException, InvalidSettingsException {
        Map<InputField, ExpressionField> nameValueMap = 
            new HashMap<InputField, ExpressionField>();
        StringBuffer correctedExp = new StringBuffer();
        StreamTokenizer t = new StreamTokenizer(new StringReader(expression));
        t.resetSyntax();
        t.wordChars(0, 0xFF);
        t.ordinaryChar('/');
        t.eolIsSignificant(false);
        t.slashSlashComments(true);
        t.slashStarComments(true);
        t.quoteChar('\'');
        t.quoteChar('"');
        t.quoteChar('$');
        int tokType;
        int variableIndex = 0;
        boolean isNextTokenSpecial = false;
        try {
            while ((tokType = t.nextToken()) != StreamTokenizer.TT_EOF) {
                final String expFieldName;
                final Class<?> expFieldClass;
                final FieldType inputFieldType;
                final String inputFieldName;
                switch (tokType) {
                case StreamTokenizer.TT_WORD:
                    String s = t.sval;
                    if (isNextTokenSpecial) {
                        if (ColumnCalculator.ROWINDEX.equals(s)) {
                            expFieldName = ColumnCalculator.ROWINDEX;
                            expFieldClass = Integer.class;
                            inputFieldName = ColumnCalculator.ROWINDEX;
                            inputFieldType = FieldType.TableConstant;
                        } else if (ColumnCalculator.ROWKEY.equals(s)) {
                            expFieldName = ColumnCalculator.ROWKEY;
                            expFieldClass = String.class;
                            inputFieldName = ColumnCalculator.ROWKEY;
                            inputFieldType = FieldType.TableConstant;
                        } else if (s.startsWith("{") && s.endsWith("}")) {
                            String var = s.substring(1, s.length() - 1);
                            if (var.length() == 0) {
                                throw new InvalidSettingsException(
                                        "Empty variable string at line "
                                        + t.lineno());
                            }
                            switch (var.charAt(0)) {
                            case 'I': expFieldClass = Integer.class; break;
                            case 'D': expFieldClass = Double.class; break;
                            case 'S': expFieldClass = String.class; break;
                            default:
                                throw new InvalidSettingsException(
                                        "Invalid type identifier for variable "
                                        + "in line " + t.lineno() + ": " 
                                        + var.charAt(0));
                            }
                            var = var.substring(1);
                            if (var.length() == 0) {
                                throw new InvalidSettingsException(
                                        "Empty variable identifier in line " 
                                        + t.lineno());
                            }
                            expFieldName = "variable_" + (variableIndex++);
                            inputFieldName = var;
                            inputFieldType = FieldType.Variable;
                        } else {
                            throw new InvalidSettingsException(
                                    "Invalid special identifier: " + s
                                    + " (at line " + t.lineno() + ")");
                        }
                        InputField inputField = 
                            new InputField(inputFieldName, inputFieldType);
                        ExpressionField expField =
                            new ExpressionField(expFieldName, expFieldClass);
                        nameValueMap.put(inputField, expField);
                        correctedExp.append(expFieldName);
                    } else {
                        correctedExp.append(s);
                    }
                    break;
                case '/':
                    correctedExp.append((char)tokType);
                    break;
                case '\'':
                case '"':
                    if (isNextTokenSpecial) {
                        throw new InvalidSettingsException(
                                "Invalid special identifier: " + t.sval
                                + " (at line " + t.lineno() + ")");
                    }
                    correctedExp.append((char)tokType);
                    s = t.sval.replace(Character.toString('\\'), "\\\\");
                    s = s.replace(Character.toString('\n'), "\\n");
                    s = s.replace(Character.toString('\r'), "\\r");
                    // escape quote characters
                    s = s.replace(Character.toString((char)tokType),
                            "\\" + (char)tokType);
                    correctedExp.append(s);
                    correctedExp.append((char)tokType);
                    break;
                case '$':
                    if ("".equals(t.sval)) {
                        isNextTokenSpecial = !isNextTokenSpecial;
                    } else {
                        s = t.sval;
                        int colIndex = spec.findColumnIndex(s);
                        if (colIndex < 0) {
                            throw new InvalidSettingsException(
                                    "No such column: "
                                    + s + " (at line " + t.lineno() + ")");
                        }
                        inputFieldName = s;
                        inputFieldType = FieldType.Column;
                        expFieldName = 
                            ColumnCalculator.createColField(colIndex);
                        DataType colType =
                            spec.getColumnSpec(colIndex).getType();
                        correctedExp.append(expFieldName);
                        boolean isArray = colType.isCollectionType();
                        if (isArray) {
                            colType = colType.getCollectionElementType();
                        }
                        if (colType.isCompatible(IntValue.class)) {
                            if (isArray) {
                                expFieldClass = int[].class;
                            } else {
                                expFieldClass = Integer.class;
                                correctedExp.append(".intValue()");
                            }
                        } else if (colType.isCompatible(DoubleValue.class)) {
                            if (isArray) {
                                expFieldClass = double[].class;
                            } else {
                                expFieldClass = Double.class;
                                correctedExp.append(".doubleValue()");
                            }
                        } else {
                            if (isArray) {
                                expFieldClass = String[].class;
                            } else {
                                expFieldClass = String.class;
                            }
                        }
                        InputField inputField = 
                            new InputField(inputFieldName, inputFieldType);
                        ExpressionField expField =
                            new ExpressionField(expFieldName, expFieldClass);
                        nameValueMap.put(inputField, expField);
                    }
                    break;
                default:
                    throw new IllegalStateException(
                            "Unexpected type in tokenizer: "
                            + tokType + " (at line " + t.lineno() + ")");
                }
            }
        } catch (IOException e) {
            throw new InvalidSettingsException(
                    "Unable to tokenize expression string", e);

        }
        return new Expression(correctedExp.toString(), nameValueMap, rType,
                tempFile);
    }

    /** Attempts to delete temp files.
     * {@inheritDoc} */
    @Override
    protected void finalize() throws Throwable {
        try {
            if ((m_tempFile != null) && m_tempFile.exists()) {
                if (!m_tempFile.delete()) {
                    LOGGER.warn("Unable to delete temp file "
                            + m_tempFile.getAbsolutePath());
                }
                File classFile = getAccompanyingClassFile(m_tempFile);
                if (classFile.exists() && !classFile.delete()) {
                    LOGGER.warn("Unable to delete temp class file "
                            + classFile.getAbsolutePath());
                }
            }
        } finally {
            super.finalize();
        }
    }
}
