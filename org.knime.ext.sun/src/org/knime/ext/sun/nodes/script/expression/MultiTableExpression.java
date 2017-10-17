/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.ext.sun.nodes.script.compile.CompilationFailedException;
import org.knime.ext.sun.nodes.script.compile.InMemorySourceJavaFileObject;
import org.knime.ext.sun.nodes.script.compile.JavaCodeCompiler;
import org.knime.ext.sun.nodes.script.expression.Expression.ExpressionField;
import org.knime.ext.sun.nodes.script.expression.Expression.FieldType;
import org.knime.ext.sun.nodes.script.expression.Expression.InputField;
import org.knime.ext.sun.nodes.script.multitable.MultiSpecHandler;
import org.knime.ext.sun.nodes.script.settings.JavaSnippetType;
import org.knime.ext.sun.nodes.script.settings.MultiTableJavaScriptingSettings;


/**
 * An expression contains the java code snippet including imports, header,
 * method definition etc. Once generated, an ExpressionInstance is available
 * using the {@link #getInstance()} method on which calculations can be carried
 * out.
 * @since 3.5
 */
public final class MultiTableExpression {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(MultiTableExpression.class);

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

    /** The compiled class for the instance of the expression. */
    private final Class<? extends AbstractSnippetExpression>
        m_abstractExpression;

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
    private MultiTableExpression(final String body,
            final Map<InputField, ExpressionField> fieldMap,
            final MultiTableJavaScriptingSettings settings)
            throws CompilationFailedException {
        m_fieldMap = fieldMap;
        m_abstractExpression = createClass(body, settings);
    }


    /**
     * Get collection of default imports.
     * @return the list of default imports.
     */
    public static String[] getDefaultImports() {
        return DEFAULT_IMPORTS;
    }

    /* Called from the constructor. */
    @SuppressWarnings("unchecked")
    private Class<? extends AbstractSnippetExpression> createClass(
            final String body, final MultiTableJavaScriptingSettings settings)
            throws CompilationFailedException {
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
            ensureTempClassPathExists();
        } catch (IOException e1) {
            throw new CompilationFailedException(
                    "Unable to copy required class path files", e1);
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

        JavaCodeCompiler compiler = new JavaCodeCompiler();
        compiler.setSources(new InMemorySourceJavaFileObject(name, source));
        compiler.setClasspaths(classPathFiles);
        compiler.compile();
        ClassLoader loader = compiler.createClassLoader(
                compiler.getClass().getClassLoader());
        try {
            return (Class<? extends AbstractSnippetExpression>)
                loader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new CompilationFailedException(
                    "Could not load generated class", e);
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
                    m_abstractExpression.newInstance(), m_fieldMap);
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
    private static synchronized void ensureTempClassPathExists()
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
                InputStream inStream = loader.getResourceAsStream(
                        cl.getName().replace('.', '/') + ".class");
                OutputStream outStream = new FileOutputStream(classFile);
                FileUtil.copy(inStream, outStream);
                inStream.close();
                outStream.close();
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
    public static MultiTableExpression compile(final MultiTableJavaScriptingSettings settings,
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
                        FieldSpecs specs = getFieldSpecs(s, ver);
                        if (specs != null) {
                            inputFieldName = specs.getInputFieldName();
                            inputFieldType = specs.getInputFieldType();
                            expFieldName = specs.getExpFieldName();
                            expFieldClass = specs.getExpFieldClass();
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
                    s = s.replace(Character.toString((char)tokType), "\\" + (char)tokType);
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
                            throw new InvalidSettingsException("No such column: "
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
        return new MultiTableExpression(body, nameValueMap, settings);
    }

    private static FieldSpecs getFieldSpecs(final String s, final int ver) {
        String expFieldName;
        Class<?> expFieldClass;
        String inputFieldName;
        FieldType inputFieldType;
        if ((MultiSpecHandler.LEFT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWNUMBER).equals(s)
                && ver == VERSION_1X
                || (MultiSpecHandler.LEFT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWINDEX).equals(s)
                && ver != VERSION_1X) {
            expFieldName = MultiSpecHandler.LEFT_PREFIX + ROWINDEX;
            expFieldClass = Integer.class;
            inputFieldName = MultiSpecHandler.LEFT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWINDEX;
            inputFieldType = FieldType.TableConstant;
        } else if ((MultiSpecHandler.RIGHT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWNUMBER).equals(s)
                && ver == VERSION_1X
                || (MultiSpecHandler.RIGHT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWINDEX).equals(s)
                && ver != VERSION_1X) {
            expFieldName = MultiSpecHandler.RIGHT_PREFIX + ROWINDEX;
            expFieldClass = Integer.class;
            inputFieldName = MultiSpecHandler.RIGHT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWINDEX;
            inputFieldType = FieldType.TableConstant;

        } else if ((MultiSpecHandler.LEFT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWKEY).equals(s)
                && ver == VERSION_1X
                || (MultiSpecHandler.LEFT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWID).equals(s)
                && ver != VERSION_1X) {
            expFieldName = MultiSpecHandler.LEFT_PREFIX + ROWID;
            expFieldClass = String.class;
            inputFieldName = MultiSpecHandler.LEFT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWID;
            inputFieldType = FieldType.TableConstant;
        } else if ((MultiSpecHandler.RIGHT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWKEY).equals(s)
                && ver == VERSION_1X
                || (MultiSpecHandler.RIGHT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWID).equals(s)
                && ver != VERSION_1X) {
            expFieldName = MultiSpecHandler.RIGHT_PREFIX + ROWID;
            expFieldClass = String.class;
            inputFieldName = MultiSpecHandler.RIGHT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWID;
            inputFieldType = FieldType.TableConstant;

        } else if ((MultiSpecHandler.LEFT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWCOUNT).equals(s)) {
            expFieldName = MultiSpecHandler.LEFT_PREFIX + ROWCOUNT;
            expFieldClass = Integer.class;
            inputFieldName = MultiSpecHandler.LEFT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWCOUNT;
            inputFieldType = FieldType.TableConstant;
        } else if ((MultiSpecHandler.RIGHT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWCOUNT).equals(s)) {
            expFieldName = MultiSpecHandler.RIGHT_PREFIX + ROWCOUNT;
            expFieldClass = Integer.class;
            inputFieldName = MultiSpecHandler.RIGHT_PREFIX + MultiSpecHandler.PREFIX_SEPARATOR + ROWCOUNT;
            inputFieldType = FieldType.TableConstant;
        } else {
            return null;
        }
        return new FieldSpecs(inputFieldName, inputFieldType, expFieldName, expFieldClass);
    }


    private static class FieldSpecs {
        private String m_inputFieldName;
        private FieldType m_inputFieldType;
        private String m_expFieldName;
        private Class<?> m_expFieldClass;

        public FieldSpecs(final String inputFieldName, final FieldType inputFieldType,
            final String expFieldName, final Class<?> expFieldClass) {
            m_inputFieldName = inputFieldName;
            m_inputFieldType = inputFieldType;
            m_expFieldName = expFieldName;
            m_expFieldClass = expFieldClass;
        }
        public String getInputFieldName() {
            return m_inputFieldName;
        }
        public FieldType getInputFieldType() {
            return m_inputFieldType;
        }
        public String getExpFieldName() {
            return m_expFieldName;
        }
        public Class<?> getExpFieldClass() {
            return m_expFieldClass;
        }
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
