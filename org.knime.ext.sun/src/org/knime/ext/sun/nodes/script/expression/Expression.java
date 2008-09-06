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
package org.knime.ext.sun.nodes.script.expression;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.ext.sun.nodes.script.JavaScriptingSettings;

import com.sun.tools.javac.Main;


/**
 * An expression contains the java code snippet including imports, header,
 * method definition etc. Once generated, an ExpressionInstance is available
 * using the {@link #getInstance()} method on which calculations can be carried
 * out.
 */
public class Expression implements Serializable {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(Expression.class);
    
    /** Source of a field. */
    public enum FieldType {
        /** Represents a column value. */ 
        Column,
        /** Represents a scope variable. */
        Variable,
        /** Represents a table column, e.g. total row count. */
        TableConstant
    }

    /**
     * Snippet code may contain the row number as parameter, it will be written
     * as "$$ROWNUMBER$$" (quotes excluded).
     */
    public static final String ROWINDEX = "ROWNUMBER";

    /**
     * Snippet code may contain the row key as parameter, it will be written as
     * "$$ROWKEY$$" (quotes excluded).
     */
    public static final String ROWKEY = "ROWKEY";

    /** These imports are put in the import section of the source file. */
    private static final Collection<String> IMPORTS = Arrays
            .asList(new String[]{"java.text.*", "java.util.*", "java.io.*",
                    "java.net.*", "java.util.regex.*"});

    /** Temp file to use. Must end with .java, passed in constructor. */
    private final File m_javaFile;

    /** Created class file, will be created and deleted in this class. */
    private File m_classFile;

    /**
     * Properties, i.e. fields in the source file with their corresponding
     * class.
     */
    private final Map<InputField, ExpressionField> m_fieldMap;

    /** The compiled class for the instance of the expression. */
    private final Class<?> m_compiled;

    /**
     * Constructor for an expression with fields.
     * 
     * @param body the expression body (Java)
     * @param fieldMap the property-names mapped to types, i.e. class name of
     *             the field that is available to the evaluated expression
     * @param rType the return type of the expression (i.e. Integer.class,
     *            Double.class, String.class)
     * @param tempFile the temp file to write the java source to
     * @throws CompilationFailedException when the expression couldn't be
     *             compiled
     * @throws IllegalArgumentException if the map contains <code>null</code>
     *             elements
     */
    public Expression(final String body, 
            final Map<InputField, ExpressionField> fieldMap,
            final Class<?> rType, final File tempFile)
            throws CompilationFailedException {
        if (!tempFile.getName().endsWith(".java")) {
            throw new IllegalArgumentException("No java file: "
                    + tempFile.getName());
        }
        m_javaFile = tempFile;
        m_fieldMap = fieldMap;
        m_compiled = createClass(body, rType);
    }

    /* Called from the constructor. */
    private Class<?> createClass(final String body, final Class<?> rType)
            throws CompilationFailedException {

        String javaAbsoluteName;
        String javaClassName;
        String classAbsoluteName;
        try {
            String name = m_javaFile.getName();
            javaClassName = name.substring(0, name.length() - ".java".length());
            javaAbsoluteName = m_javaFile.getAbsolutePath();
            // Generate the well known source of the Expression
            String source = generateSource(javaClassName, body, rType);
            BufferedWriter w = new BufferedWriter(new FileWriter(m_javaFile));
            w.write(source);
            w.close();
        } catch (IOException ioe) {
            CompilationFailedException c = new CompilationFailedException(
                    "Unable to write java temp file.");
            c.initCause(ioe);
            throw c;
        }
        final StringWriter logString = new StringWriter();
        final PrintWriter log = new PrintWriter(logString);
        // compile the classes
        if (Main.compile(new String[]{javaAbsoluteName}, log) != 0) {
            throw new CompilationFailedException("Unable to compile \"" + body
                    + "\":\n" + logString.toString());
        }
        classAbsoluteName = javaAbsoluteName.substring(
                0, javaAbsoluteName.length() - ".java".length()) + ".class";
        m_classFile = new File(classAbsoluteName);
        assert (m_classFile.exists());
        m_classFile.deleteOnExit();
        Class<?> compiled;
        try {
            URL[] urls = new URL[]{m_classFile.getParentFile().toURI().toURL()};
            ClassLoader load = URLClassLoader.newInstance(urls);
            compiled = load.loadClass(javaClassName);
        } catch (MalformedURLException mue) {
            CompilationFailedException c = new CompilationFailedException(
                    "Can't determine URL of class file " + classAbsoluteName);
            c.initCause(mue);
            throw c;
        } catch (ClassNotFoundException cfe) {
            StringBuilder addInfo = new StringBuilder("(Class file ");
            addInfo.append(m_classFile.getAbsolutePath());
            if (m_classFile.exists()) {
                addInfo.append(" does exist - ");
                addInfo.append(m_classFile.length());
                addInfo.append(" bytes");
            } else {
                addInfo.append(" does not exist");
            }
            addInfo.append(")");
            CompilationFailedException c = new CompilationFailedException(
                    "Can't load class file " + classAbsoluteName
                    + addInfo.toString());
            c.initCause(cfe);
            throw c;
        }
        return compiled;
    }

    /**
     * Generates a new instance of the compiled class.
     * 
     * @return a new expression instance that wraps the compiled source
     * @throws InstantiationException if the compiled class can't be
     *             instantiated
     */
    public ExpressionInstance getInstance() throws InstantiationException {
        try {
            return new ExpressionInstance(
                    m_compiled.newInstance(), m_fieldMap);
        } catch (IllegalAccessException iae) {
            LOGGER.error("Unexpected IllegalAccessException occured", iae);
            throw new InternalError();
        }
    }
    
    /**
     * @return the fieldMap
     */
    public Map<InputField, ExpressionField> getFieldMap() {
        return m_fieldMap;
    }
    
    /*
     * Creates the source for given expression classname, body & properties.
     */
    private String generateSource(final String filename, final String body,
            final Class<?> rType) {
        String copy = body;
        StringBuilder buffer = new StringBuilder(4096);

        /* Generate header */
        // Here comes the class
        for (String imp : IMPORTS) {
            buffer.append("import ");
            buffer.append(imp);
            buffer.append(";");
            buffer.append("\n");
        }

        buffer.append("public class " + filename + " {");
        buffer.append("\n");
        buffer.append("\n");

        /* Add the source fields */
        for (ExpressionField type : m_fieldMap.values()) {
            buffer.append("  public ");
            buffer.append(type.getFieldClass().getSimpleName());
            buffer.append(" " + type.getExpressionFieldName() + ";");
            buffer.append("\n");
        }
        buffer.append("\n");

        /* Add body */
        String cast;
        if (rType.equals(Double.class)) {
            cast = "(double)(";
        } else if (rType.equals(Integer.class)) {
            cast = "(int)(";
        } else if (rType.equals(String.class)) {
            cast = "\"\" + (";
        } else {
            cast = "(" + rType.getName() + ")(";
        }

        // And the evaluation method
        buffer.append("  public Object internalEvaluate() {");
        buffer.append("\n");

        int instructions = copy.lastIndexOf(';');
        if (instructions >= 0) {
            buffer.append("    " + copy.substring(0, instructions + 1));
            copy = copy.substring(instructions + 1);
        }
        // .. and the (last and final) expression which is 'objectified'
        buffer.append("\n");
        buffer.append("    return objectify(" + cast + copy + "));\n");
        buffer.append("  }\n\n");
        buffer.append(OBJECTIVER);

        /* Add footer */
        buffer.append('}');
        buffer.append("\n");
        return buffer.toString();
    }
    
    /**
     * Get name of the field as it is used in the temp-java file.
     * 
     * @param col the number of the column
     * @return "col" + col
     */
    public static String createColField(final int col) {
        return "col" + col;
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
    public static Expression compile(final JavaScriptingSettings settings,
            final DataTableSpec spec, final File tempFile)
            throws CompilationFailedException, InvalidSettingsException {
        String expression = settings.getExpression();
        Class<?> rType = settings.getReturnType();
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
                        if (ROWINDEX.equals(s)) {
                            expFieldName = ROWINDEX;
                            expFieldClass = Integer.class;
                            inputFieldName = ROWINDEX;
                            inputFieldType = FieldType.TableConstant;
                        } else if (ROWKEY.equals(s)) {
                            expFieldName = ROWKEY;
                            expFieldClass = String.class;
                            inputFieldName = ROWKEY;
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
                        expFieldName = createColField(colIndex);
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
    
    /** Object that pairs the name of the field used in the temporarily created
     * java class with the class of that field. */
    public static final class ExpressionField {
        private final String m_expressionFieldName;
        private final Class<?> m_fieldClass;
        
        /** @param expressionFieldName The field name
         * @param fieldClass The class representing the field.
         */
        public ExpressionField(final String expressionFieldName,
                final Class<?> fieldClass) {
            if (expressionFieldName == null || fieldClass == null) {
                throw new NullPointerException("Arg must not be null");
            }
            m_expressionFieldName = expressionFieldName;
            m_fieldClass = fieldClass;
        }
        
        /** @return the expressionFieldName */
        public String getExpressionFieldName() {
            return m_expressionFieldName;
        }
        
        /** @return the field class. */
        public Class<?> getFieldClass() {
            return m_fieldClass;
        }
    }
    
    /** Pair of original column name or scope variable identifier with a 
     * enum type indicating the source. */
    public static final class InputField {
        
        private final String m_colOrVarName;
        private final FieldType m_fieldType;

        /** @param colOrVarName The name of the (original!) field 
         * @param fieldType The type of the source.
         */
        public InputField(final String colOrVarName, 
                final FieldType fieldType) {
            if (colOrVarName == null || fieldType == null) {
                throw new NullPointerException("Arg is null");
            }
            m_colOrVarName = colOrVarName;
            m_fieldType = fieldType;
        }
        
        /** {@inheritDoc} */
        @Override
        public String toString() {
            return m_fieldType + " \"" + m_colOrVarName + "\"";
                
        }
        
        /**
         * @return the fieldType
         */
        public FieldType getFieldType() {
            return m_fieldType;
        }
        
        /**
         * @return the colOrVarName
         */
        public String getColOrVarName() {
            return m_colOrVarName;
        }
        
        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return m_colOrVarName.hashCode() + m_fieldType.hashCode();
        }
        
        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof InputField)) {
                return false;
            }
            InputField f = (InputField)obj;
            return f.m_fieldType.equals(m_fieldType) 
                && f.m_colOrVarName.equals(m_colOrVarName);
        }
    }

    /** String containing the objectivy method for compilation. */
    private static final String OBJECTIVER = 
        "  protected final Byte objectify(byte b) {\n"
            + "    return new Byte(b);\n"
            + "  }\n\n"
            + "  protected final Character objectify(char c) {\n"
            + "    return new Character(c);\n"
            + "  }\n\n"
            + "  protected final Double objectify(double d) {\n"
            + "    return new Double(d);\n"
            + "  }\n\n"
            + "  protected final Float objectify(float d) {\n"
            + "    return new Float(d);\n"
            + "  }\n\n"
            + "  protected final Integer objectify(int i) {\n"
            + "    return new Integer(i);\n"
            + "  }\n\n"
            + "  protected final Long objectify(long l) {\n"
            + "    return new Long(l);\n"
            + "  }\n\n"
            + "  protected final Object objectify(Object o) {\n"
            + "    return o;\n"
            + "  }\n\n"
            + "  protected final Short objectify(short s) {\n"
            + "    return new Short(s);\n"
            + "  }\n\n"
            + "  protected final Boolean objectify(boolean b) {\n"
            + "    return new Boolean(b);\n" + "  }\n\n";

}
