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
 * -------------------------------------------------------------------
 *
 */
package org.knime.ext.sun.nodes.script.expression;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.tools.JavaFileObject.Kind;

import org.eclipse.jdt.internal.compiler.tool.EclipseFileObject;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.ext.sun.nodes.script.compile.CompilationFailedException;
import org.knime.ext.sun.nodes.script.compile.JavaCodeCompiler;
import org.knime.ext.sun.nodes.script.compile.JavaCodeCompiler.JavaVersion;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingSettings;
import org.knime.ext.sun.nodes.script.settings.JavaSnippetType;


/**
 * An expression contains the java code snippet including imports, header,
 * method definition etc. Once generated, an ExpressionInstance is available
 * using the {@link #getInstance()} method on which calculations can be carried
 * out.
 */
public final class Expression implements AutoCloseable {
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
    private static final String[] DEFAULT_IMPORTS =
        new String[]{"java.text.*", "java.util.*", "java.io.*",
                     "java.net.*", "java.util.regex.*"};

    /** Contains the class files of {@link #REQUIRED_COMPILATION_UNITS}. */
    private static File tempClassPath;

    /** The list of classes that are required for compilation/execution (the
     * abstract super class). */
    private static final Class<?>[] REQUIRED_COMPILATION_UNITS = new Class[] {
        AbstractSnippetExpression.class,
        Abort.class
    };
    /**
     * Properties, i.e. fields in the source file with their corresponding
     * class.
     */
    private final Map<InputField, ExpressionField> m_fieldMap;

    /** The folder containing the generated .class files --- the class loader is pointed at it. */
    private final File m_instanceTempFolder;

    /** The compiled class for the instance of the expression. */
    private final Class<? extends AbstractSnippetExpression> m_abstractExpression;

    /** The class loader of the auto-generated {@link AbstractSnippetExpression}. Needs to be {@link #close() closed} */
    private final URLClassLoader m_abstractExpressionClassLoader;

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
    private Expression(final String body, final Map<InputField, ExpressionField> fieldMap,
        final JavaScriptingSettings settings) throws CompilationFailedException {
        m_fieldMap = fieldMap;
        Class<?> rType = settings.getReturnType();
        int version = settings.getExpressionVersion();
        boolean isArrayReturn = settings.isArrayReturn();
        String header = settings.getHeader();
        String source;
        String name = "Expression" + COUNTER.getAndIncrement();
        String[] imports = null != settings.getImports()
                ? settings.getImports()
                : getDefaultImports();
        // Generate the well known source of the Expression
        switch (version) {
        case VERSION_1X:
            source = generateSourceVersion1(name, body, rType, imports);
            break;
        case VERSION_2X:
            source = generateSourceVersion2(
                    name, body, header, rType, imports, isArrayReturn);
            break;
        default:
            throw new CompilationFailedException(
                    "Unknown snippet version number: " + version);
        }
        try {
            ensureStaticTempClassPathExists();
            m_instanceTempFolder = FileUtil.createTempDir(name.toLowerCase());
        } catch (IOException e1) {
            throw new CompilationFailedException("Unable to copy required class path files", e1);
        }
        File[] additionalJarFiles;
        try {
            additionalJarFiles = settings.getJarFilesAsFiles();
        } catch (InvalidSettingsException e1) {
            throw new CompilationFailedException(e1.getMessage(), e1);
        }
        File[] classPathFiles = new File[additionalJarFiles.length + 1];
        classPathFiles[0] = tempClassPath;
        System.arraycopy(additionalJarFiles, 0,
                classPathFiles, 1, additionalJarFiles.length);

        File instanceTempFile = new File(m_instanceTempFolder, name.concat(".java"));
        try {
            Files.write(instanceTempFile.toPath(), Collections.singleton(source));
        } catch (IOException e) {
            throw new CompilationFailedException(
                "Unable to write expression source to temp file \"" + instanceTempFile.getAbsolutePath() + "\"", e);
        }
        JavaCodeCompiler compiler = new JavaCodeCompiler(JavaVersion.JAVA_8, m_instanceTempFolder);
        EclipseFileObject snippetFile =
            new EclipseFileObject(name, instanceTempFile.toURI(), Kind.SOURCE, StandardCharsets.UTF_8);
        compiler.setSources(snippetFile);
        compiler.setClasspaths(classPathFiles);
        compiler.compile();
        m_abstractExpressionClassLoader = compiler.createClassLoader(compiler.getClass().getClassLoader());
        try {
            m_abstractExpression =
                    (Class<? extends AbstractSnippetExpression>)m_abstractExpressionClassLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new CompilationFailedException("Could not load generated class", e);
        }
    }


    /**
     * Get collection of default imports.
     * @return the list of default imports.
     */
    public static String[] getDefaultImports() {
        return DEFAULT_IMPORTS;
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
            return new ExpressionInstance(m_abstractExpression.newInstance(), m_fieldMap);
        } catch (IllegalAccessException iae) {
            LOGGER.error("Unexpected IllegalAccessException occurred", iae);
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
            final String body, final Class<?> rType,
            final String[] imports) {
        String copy = body;
        StringBuilder buffer = new StringBuilder(4096);

        /* Generate header */
        // Here comes the class
        for (String imp : imports) {
            buffer.append("import ");
            buffer.append(imp);
            buffer.append(";");
            buffer.append("\n");
        }

        for (Class<?> c : REQUIRED_COMPILATION_UNITS) {
            buffer.append("import ");
            buffer.append(c.getName());
            buffer.append(";");
            buffer.append("\n");
        }

        buffer.append("public class ").append(name);
        buffer.append(" extends ");
        buffer.append(AbstractSnippetExpression.class.getSimpleName());
        buffer.append(" {\n");
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
            final String[] imports, final boolean isArrayReturn) {
        StringBuilder buffer = new StringBuilder(4096);

        /* Generate header */
        // Here comes the class
        for (String imp : imports) {
            buffer.append("import ");
            buffer.append(imp);
            buffer.append(";");
            buffer.append("\n");
        }
        for (Class<?> c : REQUIRED_COMPILATION_UNITS) {
            buffer.append("import ");
            buffer.append(c.getName());
            buffer.append(";");
            buffer.append("\n");
        }

        buffer.append("public class ").append(name);
        buffer.append(" extends ");
        buffer.append(AbstractSnippetExpression.class.getSimpleName());
        buffer.append(" {\n");
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
        buffer.append("  @Override\n");
        buffer.append("  public ");
        buffer.append(rType.getName());
        buffer.append(isArrayReturn ? "[]" : "");
        buffer.append(" internalEvaluate() throws Abort {");
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
     * @throws IOException
     */
    private static synchronized void ensureStaticTempClassPathExists()
        throws IOException {
        // the temp directory may get deleted under Linux if it has not been
        // accessed for some time, see bug #3319
        if ((tempClassPath == null) || !tempClassPath.isDirectory()) {
            File tempClassPathDir =
                FileUtil.createTempDir("knime_javasnippet", new File(KNIMEConstants.getKNIMETempDir()));
            for (Class<?> cl : REQUIRED_COMPILATION_UNITS) {
                Package pack = cl.getPackage();
                File childDir = tempClassPathDir;
                for (String subDir : pack.getName().split("\\.")) {
                    childDir = new File(childDir, subDir);
                }
                if (!childDir.isDirectory() && !childDir.mkdirs()) {
                    throw new IOException("Could not create temporary directory for Java Snippet class files: "
                        + childDir.getAbsolutePath());
                }
                String className = cl.getSimpleName();
                className = className.concat(".class");
                File classFile = new File(childDir, className);
                ClassLoader loader = cl.getClassLoader();
                try (InputStream inStream = loader.getResourceAsStream(cl.getName().replace('.', '/') + ".class");
                        OutputStream outStream = new FileOutputStream(classFile)) {
                    FileUtil.copy(inStream, outStream);
                }
            }
            tempClassPath = tempClassPathDir;
        }
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
                            inputFieldName = var;
                            inputFieldType = FieldType.Variable;
                            // bug fix 2128 (handle multiple occurrences of var)
                            InputField tempField =
                                new InputField(inputFieldName, inputFieldType);
                            ExpressionField oldExpressionField =
                                nameValueMap.get(tempField);
                            if (oldExpressionField != null) {
                                expFieldName =
                                    oldExpressionField.getExpressionFieldName();
                            } else {
                                expFieldName = "variable_" + (variableIndex++);
                            }
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
                        JavaSnippetType<?, ?, ?> jst =
                            JavaSnippetType.findType(colType);
                        // e.g. Integer.class or Integer[].class
                        expFieldClass = jst.getJavaClass(isArray);
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

    /** Used by node implementation execution can be streamed. If row count is required we need to count first.
     * @return Whether row count is required during computation (field is used).
     * @since 3.2
     */
    public boolean usesRowCount() {
        return m_fieldMap.keySet().stream().anyMatch(f -> ROWCOUNT.equals(f.getColOrVarName()));
    }

    /** Similar to {@link #usesRowCount()} it checks whether the row index is used in the expression. If so, the
     * computation cannot be distributed.
     * @return that property.
     * @since 3.2
     */
    public boolean usesRowIndex() {
        return m_fieldMap.keySet().stream().anyMatch(f -> ROWINDEX.equals(f.getColOrVarName()));
    }

    @Override
    public void close() throws IOException {
        m_abstractExpressionClassLoader.close();
        FileUtil.deleteRecursively(m_instanceTempFolder);
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

        /** @return the expressionFieldName in java (prepended by __) */
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
