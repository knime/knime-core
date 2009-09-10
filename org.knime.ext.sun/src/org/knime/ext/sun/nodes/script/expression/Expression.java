/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject.Kind;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.TimestampValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.ext.sun.nodes.script.JavaScriptingSettings;

import com.sun.tools.javac.api.JavacTool;


/**
 * An expression contains the java code snippet including imports, header,
 * method definition etc. Once generated, an ExpressionInstance is available
 * using the {@link #getInstance()} method on which calculations can be carried
 * out.
 */
public final class Expression {
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

    /** Version "1" of the expression, used in KNIME 1.xx (no "return" 
     * statement). */
    public static final int VERSION_1X = 1;
    /** Version "2" of the expression, used in KNIME 2.0 (with "return" 
     * statement). */
    public static final int VERSION_2X = 2;

    /** Identifier for row index (starting with 0), since 2.0. */
    public static final String ROWINDEX = "ROWINDEX";
    /** Identifier for row index (starting with 0), deprecated as of 2.0. */
    public static final String ROWNUMBER = "ROWNUMBER";

    /** Identifier for row ID, since 2.0. */
    public static final String ROWID = "ROWID";
    /** Identifier for row ID, deprecated as of 2.0. */
    public static final String ROWKEY = "ROWKEY";
    
    /** Identifier for row count. */
    public static final String ROWCOUNT = "ROWCOUNT";
    
    /** An unique id to ensure uniqueness of class names (per vm). */
    private static final AtomicInteger COUNTER = new AtomicInteger();

    /** These imports are put in the import section of the source file. */
    private static final Collection<String> IMPORTS = Arrays
            .asList(new String[]{"java.text.*", "java.util.*", "java.io.*",
                    "java.net.*", "java.util.regex.*"});

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
     * @param settings To get information from, e.g. desired return type
     * @throws CompilationFailedException when the expression couldn't be
     *             compiled
     * @throws IllegalArgumentException if the map contains <code>null</code>
     *             elements
     */
    private Expression(final String body, 
            final Map<InputField, ExpressionField> fieldMap,
            final JavaScriptingSettings settings)
            throws CompilationFailedException {
        m_fieldMap = fieldMap;
        m_compiled = createClass(body, settings);
    }

    /* Called from the constructor. */
    private Class<?> createClass(final String body, 
            final JavaScriptingSettings settings) 
            throws CompilationFailedException {
        Class<?> rType = settings.getReturnType();
        int version = settings.getExpressionVersion();
        boolean isArrayReturn = settings.isArrayReturn();
        String header = settings.getHeader();
        String source;
        String name = "Expression" + COUNTER.getAndIncrement();
        // Generate the well known source of the Expression
        switch (version) {
        case VERSION_1X:
            source = generateSourceVersion1(name, body, rType);
            break;
        case VERSION_2X:
            source = generateSourceVersion2(
                    name, body, header, rType, isArrayReturn);
            break;
        default:
            throw new CompilationFailedException(
                    "Unknown snippet version number: " + version);
        }
        ArrayList<String> compileArgs = new ArrayList<String>();
        String[] additionalJars = settings.getJarFiles();
        // first url is reserved for resulting class file
        URL[] classPath = new URL[additionalJars.length + 1];
        if (additionalJars.length > 0) {
            compileArgs.add("-classpath");
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < additionalJars.length; i++) {
                b.append(i > 0 ? ":"  : "");
                b.append(additionalJars[i]);
                File toFile = new File(additionalJars[i]);
                if (!toFile.exists()) {
                    throw new CompilationFailedException("Can't read file \""
                            + additionalJars[i] + "\"; invalid class path");
                }
                URL url;
                try {
                    url = toFile.toURI().toURL();
                } catch (MalformedURLException e) {
                    CompilationFailedException c = 
                        new CompilationFailedException("Can't convert class "
                            + "path file \"" + additionalJars[i] + "\" to URL");
                    c.initCause(e);
                    throw c;
                }
                classPath[i + 1] = url;
            }
            compileArgs.add(b.toString());
        }
        compileArgs.add("-nowarn");
        final StringWriter logString = new StringWriter();
        JavaCompiler compiler = JavacTool.create();
        DiagnosticCollector<JavaFileObject> digsCollector = 
            new DiagnosticCollector<JavaFileObject>();
        JavaFileObject jfo = new InMemoryJavaSourceFileObject(name, source);
        InMemoryJavaFileManager fileMgr = new InMemoryJavaFileManager(
                compiler.getStandardFileManager(digsCollector, null, null));
        CompilationTask compileTask = compiler.getTask(logString, fileMgr, 
                digsCollector, compileArgs, null, Collections.singleton(jfo));
        if (compileTask.call()) {
            try {
                return fileMgr.loadGeneratedClass(name);
            } catch (ClassNotFoundException e) {
                throw new CompilationFailedException(
                        "Could not load generated class", e);
            }
        } else {
            boolean hasDiagnostic = false;
            String[] sourceLines = getSourceLines(source);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("<<<< Expression Start >>>>");
                for (int i = 0; i < sourceLines.length; i++) {
                    LOGGER.debug((i + 1) + ": " + sourceLines[i]);
                }
                LOGGER.debug("<<<< Expression End >>>>");
            }
            StringBuilder b = new StringBuilder("Unable to compile expression");
            for (Diagnostic<? extends JavaFileObject> d 
                    : digsCollector.getDiagnostics()) {
                switch (d.getKind()) {
                case ERROR:
                    if (hasDiagnostic) {
                        b.append("\n"); // follow up error, insert empty line
                    }
                    hasDiagnostic = true;
                    int lineIndex = (int)(d.getLineNumber() - 1);
                    b.append("\nERROR at line ").append(lineIndex + 1);
                    b.append("\n").append(d.getMessage(null));
                    if (lineIndex - 1 >= 0 && lineIndex - 1 < source.length()) {
                        // previous line
                        b.append("\n  Line : ").append(lineIndex);
                        b.append("  ").append(sourceLines[lineIndex - 1]);
                    }
                    if (lineIndex >= 0 && lineIndex < source.length()) {
                        // error line
                        b.append("\n  Line : ").append(lineIndex + 1);
                        b.append("  ").append(sourceLines[lineIndex]);
                    }
                    break;
                default:
                    break;
                }
            }
            String errorOut = logString.toString();
            if (!hasDiagnostic) {
                b.append("\n").append(errorOut);
            } else {
                if (!errorOut.isEmpty()) {
                    LOGGER.debug("Error output of compilation: ");
                    LOGGER.debug(errorOut);
                }
            }
            throw new CompilationFailedException(b.toString());
        }
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
    private String generateSourceVersion1(final String name, 
            final String body, final Class<?> rType) {
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
    
        buffer.append("public class " + name + " {");
        buffer.append("\n");
        buffer.append("\n");
    
        /* Add the source fields */
        for (ExpressionField type : m_fieldMap.values()) {
            buffer.append("  public ");
            buffer.append(type.getFieldClass().getSimpleName());
            buffer.append(" " + type.getFieldNameInJava() + ";");
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

    /*
     * Creates the source for given expression classname, body & properties.
     */
    private String generateSourceVersion2(final String name, final String body,
            final String header, final Class<?> rType, 
            final boolean isArrayReturn) {
        StringBuilder buffer = new StringBuilder(4096);

        /* Generate header */
        // Here comes the class
        for (String imp : IMPORTS) {
            buffer.append("import ");
            buffer.append(imp);
            buffer.append(";");
            buffer.append("\n");
        }

        buffer.append("public class " + name + " {");
        buffer.append("\n");
        buffer.append("\n");

        /* Add the source fields */
        for (ExpressionField type : m_fieldMap.values()) {
            buffer.append("  public ");
            buffer.append(type.getFieldClass().getSimpleName());
            buffer.append(" " + type.getFieldNameInJava() + ";");
            buffer.append("\n");
        }
        buffer.append("\n");
        
        if (header != null && header.length() > 0) {
            buffer.append(header).append("\n");
        }

        /* Add body */
        // And the evaluation method
        buffer.append("  public ");
        buffer.append(rType.getName());
        buffer.append(isArrayReturn ? "[]" : "");
        buffer.append(" internalEvaluate() {");
        buffer.append("\n");

        buffer.append(body);

        buffer.append("\n");
        buffer.append("  }\n\n");

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
     * Get the field name, i.e. the name of the global variable. 
     * @param name The name of field (plain)
     * @return "__" + name;
     */
    public static String getJavaFieldName(final String name) {
        return "__" + name;
    }
    
    /**
     * Tries to compile the given expression as entered in the dialog with the
     * current spec.
     *
     * @param settings Contains node model settings, e.g. expression
     * @param spec the spec
     * @return the java expression
     * @throws CompilationFailedException if that fails
     * @throws InvalidSettingsException if settings are missing
     */
    public static Expression compile(final JavaScriptingSettings settings,
            final DataTableSpec spec)
            throws CompilationFailedException, InvalidSettingsException {
        String expression = settings.getExpression();
        int ver = settings.getExpressionVersion();
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
                        if (ROWNUMBER.equals(s) && ver == VERSION_1X
                                || ROWINDEX.equals(s) && ver != VERSION_1X) {
                            expFieldName = ROWINDEX;
                            expFieldClass = Integer.class;
                            inputFieldName = ROWINDEX;
                            inputFieldType = FieldType.TableConstant;
                        } else if (ROWKEY.equals(s) && ver == VERSION_1X
                                || ROWID.equals(s) && ver != VERSION_1X) {
                            expFieldName = ROWID;
                            expFieldClass = String.class;
                            inputFieldName = ROWID;
                            inputFieldType = FieldType.TableConstant;
                        } else if (ROWCOUNT.equals(s)) {
                            expFieldName = ROWCOUNT;
                            expFieldClass = Integer.class;
                            inputFieldName = ROWCOUNT;
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
                        correctedExp.append(getJavaFieldName(expFieldName));
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
                        correctedExp.append(getJavaFieldName(expFieldName));
                        boolean isArray = colType.isCollectionType();
                        if (isArray) {
                            colType = colType.getCollectionElementType();
                        }
                        if (colType.isCompatible(IntValue.class)) {
                            if (isArray) {
                                expFieldClass = Integer[].class;
                            } else {
                                expFieldClass = Integer.class;
                                correctedExp.append(".intValue()");
                            }
                        } else if (colType.isCompatible(DoubleValue.class)) {
                            if (isArray) {
                                expFieldClass = Double[].class;
                            } else {
                                expFieldClass = Double.class;
                                correctedExp.append(".doubleValue()");
                            }
                        } else if (colType.isCompatible(TimestampValue.class)) {
                            if (isArray) {
                                expFieldClass = Date[].class;
                            } else {
                                expFieldClass = Date.class;
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
        String body = correctedExp.toString();
        return new Expression(body, nameValueMap, settings);
    }

    private static final class InMemoryJavaFileManager 
        extends ForwardingJavaFileManager<JavaFileManager> {
        
        private final Map<String, InMemoryJavaClassFileObject> m_classMap;

        /**
         * @param delegate
         */
        protected InMemoryJavaFileManager(final JavaFileManager delegate) {
            super(delegate);
            m_classMap = new HashMap<String, InMemoryJavaClassFileObject>();
        }
        
        /** {@inheritDoc} */
        @Override
        public JavaFileObject getJavaFileForOutput(final Location location, 
                final String className, final Kind kind,
                final FileObject sibling) throws IOException {
            if (StandardLocation.CLASS_OUTPUT.equals(location) 
                    && JavaFileObject.Kind.CLASS.equals(kind)) {
                InMemoryJavaClassFileObject clazz = 
                    new InMemoryJavaClassFileObject(className);
                m_classMap.put(className, clazz);
                return clazz;
            } else {
                return super.getJavaFileForOutput(
                        location, className, kind, sibling);
            }
        }
        
        public Class<?> loadGeneratedClass(final String name) 
            throws ClassNotFoundException {
            InMemoryJavaClassFileObject classObject = m_classMap.get(name);
            if (classObject == null) {
                throw new RuntimeException("Couldn't locate generated class");
            }
            return classObject.loadGeneratedClass();
        }
    }
    
    /** {@link JavaFileObject} that keeps the source in memory (a string). */
    private static final class InMemoryJavaSourceFileObject 
    extends SimpleJavaFileObject {
        
        private final String m_source;
        
        /** Constructs new java file object.
         * @param name The name of the source file (no file is created though).
         * @param source The source content.
         */
        InMemoryJavaSourceFileObject(final String name, final String source) {
            super(URI.create("file:/" + name.replace('.', '/') 
                    + Kind.SOURCE.extension), Kind.SOURCE);
            m_source = source;
        }
        
        /** {@inheritDoc} */
        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors)
                throws IOException {
            return m_source; 
        }
    }
    
    /** {@link JavaFileObject} that keeps the generated class in memory. */
    private static final class InMemoryJavaClassFileObject 
    extends SimpleJavaFileObject {
        
        private ByteArrayOutputStream m_classStream;
        private final String m_name;
        
        /** Constructs new java class file object.
         * @param name The name of the class file.
         */
        InMemoryJavaClassFileObject(final String name) {
            super(URI.create("file:/" + name.replace('.', '/') 
                    + Kind.CLASS.extension), Kind.CLASS);
            m_name = name;
        }
        
        /** {@inheritDoc} */
        @Override
        public OutputStream openOutputStream() throws IOException {
            m_classStream = new ByteArrayOutputStream();
            return m_classStream;
        }
        
        /**
         * @return The class generated by this file object 
         *         (the expression instance)
         * @throws ClassNotFoundException If that's not possible for any reason.
         */
        public Class<?> loadGeneratedClass() throws ClassNotFoundException {
            return new ClassLoader(
                    Thread.currentThread().getContextClassLoader()) {
                /** {@inheritDoc} */
                @Override
                protected Class<?> findClass(final String name)
                        throws ClassNotFoundException {
                    if (name.equals(m_name)) {
                        byte[] bytes = m_classStream.toByteArray();
                        return defineClass(name, bytes, 0, bytes.length);
                    }
                    return super.findClass(name);
                }
            } .loadClass(m_name);
        }
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
        public String getFieldNameInJava() {
            return getJavaFieldName(m_expressionFieldName);
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
    
    /** Get the source code in an array, each array element representing the
     * corresponding row in the source code.
     * @param source The source code
     * @return The source code, split by line breaks.
     */
    private static String[] getSourceLines(final String source) {
        BufferedReader reader = new BufferedReader(new StringReader(source));
        List<String> result = new ArrayList<String>();
        String token;
        try {
            while ((token = reader.readLine()) != null) {
                result.add(token);
            }
            reader.close();
        } catch (IOException e) {
            LOGGER.fatal("Unexpected IOException while reading String", e);
        }
        return result.toArray(new String[result.size()]);
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
