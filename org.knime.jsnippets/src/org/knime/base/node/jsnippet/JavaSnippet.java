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
 *   01.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet;

import static org.knime.base.node.jsnippet.guarded.JavaSnippetDocument.GUARDED_BODY_END;
import static org.knime.base.node.jsnippet.guarded.JavaSnippetDocument.GUARDED_BODY_START;
import static org.knime.base.node.jsnippet.guarded.JavaSnippetDocument.GUARDED_FIELDS;
import static org.knime.base.node.jsnippet.guarded.JavaSnippetDocument.GUARDED_IMPORTS;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.compiler.tool.EclipseFileObject;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.knime.base.node.jsnippet.expression.Abort;
import org.knime.base.node.jsnippet.expression.AbstractJSnippet;
import org.knime.base.node.jsnippet.expression.Cell;
import org.knime.base.node.jsnippet.expression.ColumnException;
import org.knime.base.node.jsnippet.expression.FlowVariableException;
import org.knime.base.node.jsnippet.expression.Type;
import org.knime.base.node.jsnippet.expression.TypeException;
import org.knime.base.node.jsnippet.guarded.JavaSnippetDocument;
import org.knime.base.node.jsnippet.template.JavaSnippetTemplate;
import org.knime.base.node.jsnippet.type.ConverterUtil;
import org.knime.base.node.jsnippet.ui.JSnippetParser;
import org.knime.base.node.jsnippet.util.FlowVariableRepository;
import org.knime.base.node.jsnippet.util.JSnippet;
import org.knime.base.node.jsnippet.util.JavaFieldList;
import org.knime.base.node.jsnippet.util.JavaFieldList.OutColList;
import org.knime.base.node.jsnippet.util.JavaSnippetCompiler;
import org.knime.base.node.jsnippet.util.JavaSnippetFields;
import org.knime.base.node.jsnippet.util.JavaSnippetSettings;
import org.knime.base.node.jsnippet.util.JavaSnippetUtil;
import org.knime.base.node.jsnippet.util.ValidationReport;
import org.knime.base.node.jsnippet.util.field.InCol;
import org.knime.base.node.jsnippet.util.field.InVar;
import org.knime.base.node.jsnippet.util.field.JavaField;
import org.knime.base.node.jsnippet.util.field.OutCol;
import org.knime.base.node.jsnippet.util.field.OutVar;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.convert.datacell.ArrayToCollectionConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.CollectionConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.util.ClassUtil;
import org.knime.core.data.convert.util.MultiParentClassLoader;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.rsyntaxtextarea.guarded.GuardedDocument;
import org.knime.core.node.util.rsyntaxtextarea.guarded.GuardedSection;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.FileUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * The java snippet which can be controlled by changing the settings, fields and jar-files to be included or by changing
 * the contents of the snippets document. The document is a java class which is compiled for execution.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("restriction")
public final class JavaSnippet implements JSnippet<JavaSnippetTemplate>, Closeable {

    /**
     * Check whether a given source version sufficiently matches a certain target version.
     *
     * @param source Version that should match <code>target</code>
     * @param target Version that should be matched by <code>source</code>
     * @return true if source is not {@link Version#emptyVersion}, major versions are equal and the source minor/micro
     *         version is greater or equal to the required minor/minor version.
     */
    public static boolean versionMatches(final Version source, final Version target) {
        return !source.equals(Version.emptyVersion) && source.getMajor() == target.getMajor()
            && source.compareTo(target) >= 0;
    }

    private static class SnippetCache {
        private String m_snippetCode;

        private Class<? extends AbstractJSnippet> m_snippetClass;

        private boolean m_hasCustomFields;

        void invalidate() {
            m_snippetCode = null;
            m_snippetClass = null;
        }

        boolean isValid(final Document snippetDoc) {
            try {
                String currentCode = snippetDoc.getText(0, snippetDoc.getLength());
                return Objects.equals(currentCode, m_snippetCode);
            } catch (BadLocationException ex) {
                return false;
            }
        }

        boolean hasCustomFields() {
            return m_hasCustomFields;
        }

        void update(final Document snippetDoc, final Class<? extends AbstractJSnippet> snippetClass,
            final JavaSnippetSettings settings) {
            try {
                m_snippetCode = snippetDoc.getText(0, snippetDoc.getLength());
                m_snippetClass = snippetClass;

                m_hasCustomFields = false;
                JavaSnippetFields systemFields = settings.getJavaSnippetFields();
                for (Field f : snippetClass.getDeclaredFields()) {
                    if (!isSystemField(systemFields.getInColFields(), f.getName())
                        && !isSystemField(systemFields.getOutColFields(), f.getName())
                        && !isSystemField(systemFields.getInVarFields(), f.getName())
                        && !isSystemField(systemFields.getOutVarFields(), f.getName())) {
                        m_hasCustomFields = true;
                        break;
                    }

                }
            } catch (BadLocationException ex) {
                // ignore and clear cache
                m_snippetCode = null;
                m_snippetClass = null;
            }
        }

        private static boolean isSystemField(final JavaFieldList<? extends JavaField> list, final String fieldName) {
            for (JavaField f : list) {
                if (f.getJavaName().equals(fieldName)) {
                    return true;
                }
            }
            return false;
        }

        Class<? extends AbstractJSnippet> getSnippetClass() {
            return m_snippetClass;
        }
    }

    /** Identifier for row index (starting with 0). */
    public static final String ROWINDEX = "ROWINDEX";

    /** Identifier for row ID. */
    public static final String ROWID = "ROWID";

    /** Identifier for row count. */
    public static final String ROWCOUNT = "ROWCOUNT";

    /** The version 1.x of the java snippet. */
    public static final String VERSION_1_X = "version 1.x";

    private static File jSnippetJar;

    private String[] m_jarFiles = new String[0];

    private JavaFileObject m_snippet;

    private File m_snippetFile;

    private GuardedDocument m_document;

    // true when the document has changed and the m_snippet is not up to date.
    private boolean m_dirty;

    private JSnippetParser m_parser;

    private JavaSnippetSettings m_settings;

    private JavaSnippetFields m_fields;

    private final File m_tempClassPathDir;

    private NodeLogger m_logger;

    private final SnippetCache m_snippetCache = new SnippetCache();

    /* ClassLoader used to load the compiled JavaSnippet class */
    private URLClassLoader m_classLoader = null;

    /**
     * Create a new snippet.
     */
    public JavaSnippet() {
        m_fields = new JavaSnippetFields();
        File tempDir;
        try {
            tempDir = FileUtil.createTempDir("knime_javasnippet");
        } catch (IOException ex) {
            NodeLogger.getLogger(getClass())
                .error("Could not create temporary directory for Java Snippet: " + ex.getMessage(), ex);
            // use the standard temp directory instead
            tempDir = new File(KNIMEConstants.getKNIMETempDir());
        }
        m_tempClassPathDir = tempDir;
    }

    /**
     * {@inheritDoc}
     *
     * Closes the URLClassLoader used to load the compiled Java Snippet, if open.
     */
    @Override
    public void close() {
        if (m_classLoader != null) {
            // The class loader may still have opened some jar files which lie in
            // temporary directories, because downloaded from an external URL.
            try {
                m_classLoader.close();
                m_classLoader = null;
            } catch (IOException e) {
                LOGGER.warn("Could not close Java Snippet URLClassLoader.", e);
            }
        }
    }

    /**
     * Create a new snippet with the given settings.
     *
     * @param settings the settings
     */
    public synchronized void setSettings(final JavaSnippetSettings settings) {
        m_settings = settings;
        setJavaSnippetFields(settings.getJavaSnippetFields());
        setJarFiles(settings.getJarFiles());
        init();
    }

    private void init() {
        if (null != m_document) {
            initDocument(m_document);
        }
    }

    /**
     * Get the updated settings java snippet.
     *
     * @return the settings
     */
    public JavaSnippetSettings getSettings() {
        updateSettings();
        return m_settings;
    }

    private void updateSettings() {
        try {
            final GuardedDocument doc = getDocument();
            m_settings.setScriptImports(doc.getTextBetween(GUARDED_IMPORTS, GUARDED_FIELDS));
            m_settings.setScriptFields(doc.getTextBetween(GUARDED_FIELDS, GUARDED_BODY_START));
            m_settings.setScriptBody(doc.getTextBetween(GUARDED_BODY_START, GUARDED_BODY_END));
        } catch (BadLocationException e) {
            // this should never happen
            throw new IllegalStateException(e);
        }
        // update input fields
        m_settings.setJavaSnippetFields(m_fields);
        // update jar files
        m_settings.setJarFiles(m_jarFiles);
    }

    @Override
    public File[] getClassPath() throws IOException {
        final List<File> additionalBuildPath = Arrays.asList(getAdditionalBuildPaths());
        final ArrayList<File> jarFiles = new ArrayList<>();

        jarFiles.addAll(Arrays.asList(getRuntimeClassPath()));
        jarFiles.addAll(additionalBuildPath);

        return jarFiles.toArray(new File[jarFiles.size()]);
    }

    @Override
    public File[] getCompiletimeClassPath() throws IOException {
        final List<File> additionalBuildPath = Arrays.asList(getAdditionalBuildPaths());
        final List<File> additionalBundlesPath = Arrays.asList(getAdditionalBundlesPaths(false));
        final ArrayList<File> jarFiles = new ArrayList<>();

        jarFiles.addAll(Arrays.asList(getRuntimeClassPath()));
        jarFiles.addAll(additionalBuildPath);
        jarFiles.addAll(additionalBundlesPath);

        return jarFiles.toArray(new File[jarFiles.size()]);
    }

    @Override
    public File[] getRuntimeClassPath() throws IOException {
        // Add lock since jSnippetJar is used across all JavaSnippets
        synchronized (JavaSnippet.class) {
            if (jSnippetJar == null || !jSnippetJar.exists()) {
                jSnippetJar = createJSnippetJarFile();
            }
        }

        final ArrayList<File> jarFiles = new ArrayList<>();
        jarFiles.add(jSnippetJar);

        for (final String jarFile : m_jarFiles) {
            try {
                jarFiles.add(JavaSnippetUtil.toFile(jarFile));
            } catch (InvalidSettingsException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        return jarFiles.toArray(new File[jarFiles.size()]);
    }

    @Override
    public Iterable<? extends JavaFileObject> getCompilationUnits() throws IOException {
        if (m_snippet == null || m_snippetFile == null || !m_snippetFile.exists()) {
            m_snippetFile = new File(m_tempClassPathDir, "JSnippet.java");

            // Note: this is a workaround for openWriter() not taking charset into account
            m_snippet = new EclipseFileObject("JSnippet", m_snippetFile.toURI(), Kind.SOURCE, StandardCharsets.UTF_8);
            m_dirty = true;
        }

        if (m_dirty) {
            try (final Writer out = new BufferedWriter(
                new OutputStreamWriter(m_snippet.openOutputStream(), StandardCharsets.UTF_8))) {
                try {
                    final Document doc = getDocument();
                    out.write(doc.getText(0, doc.getLength()));
                    m_dirty = false;
                } catch (BadLocationException e) {
                    // this should never happen.
                    throw new IllegalStateException(e);
                }
            }
        }

        return Collections.singletonList(m_snippet);
    }

    @Override
    public boolean isSnippetSource(final JavaFileObject source) {
        return null != m_snippet ? source.equals(m_snippet) : false;
    }

    @Override
    public File[] getAdditionalBuildPaths() {
        final Set<File> result = new LinkedHashSet<>();

        for (InCol colField : m_fields.getInColFields()) {
            final String factoryId = colField.getConverterFactoryId();
            final Collection<? extends File> buildPath = getBuildPathFromCache(factoryId);
            if (buildPath != null) {
                result.addAll(buildPath);
            }
        }

        for (OutCol colField : m_fields.getOutColFields()) {
            final String factoryId = colField.getConverterFactoryId();
            final Collection<? extends File> buildPath = getBuildPathFromCache(factoryId);
            if (buildPath != null) {
                result.addAll(buildPath);
            }
        }

        return result.toArray(new File[result.size()]);
    }

    /**
     * Get jar files required for compiling with the additional bundles.
     *
     * @param withDeps Whether to also resolve the dependencies of the bundles.
     * @return Array of .jar files.
     */
    public File[] getAdditionalBundlesPaths(final boolean withDeps) {
        final Set<Bundle> bundles = new LinkedHashSet<>();

        final LinkedBlockingQueue<Bundle> pending = new LinkedBlockingQueue<Bundle>();

        if (m_settings != null) {
            // Resolve bundle names to bundles
            Stream.of(m_settings.getBundles()).map(bname -> Platform.getBundle(bname.split(" ")[0]))
                .filter(b -> b != null).collect(Collectors.toCollection(() -> pending));
        }

        Bundle bundle = null;
        while ((bundle = pending.poll()) != null) {
            if (!bundles.add(bundle)) {
                continue;
            }

            if (withDeps) {
                // resolve dependencies
                final BundleWiring wiring = bundle.adapt(BundleWiring.class);
                final List<BundleWire> requiredWires = wiring.getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE);
                for (final BundleWire w : requiredWires) {
                    pending.add(w.getProviderWiring().getBundle());
                }
            }
        }

        final ArrayList<File> result = new ArrayList<>();
        for (final Bundle b : bundles) {
            try {
                result.add(FileLocator.getBundleFile(b));
            } catch (IOException e) {
                LOGGER.warn("Failed to get bundle file of \"" + b + "\"");
            }
        }

        return result.toArray(new File[result.size()]);
    }

    /**
     * Get ClassLoader to access the additional bundles at runtime.
     *
     * @return ClassLoader with access to all the jars of the additional bundles.
     */
    public List<ClassLoader> getAdditionalBundlesClassLoaders() {
        final ArrayList<ClassLoader> loaders = new ArrayList<>();
        if (m_settings != null) {
            // Resolve bundle names to bundles
            Stream.of(m_settings.getBundles()).map(bname -> Platform.getBundle(bname.split(" ")[0]))
                .filter(o -> o != null).map(b -> b.adapt(BundleWiring.class).getClassLoader())
                .filter(o -> o != null).collect(Collectors.toCollection(() -> loaders));
        }

        return loaders;
    }

    /**
     * Give jar file with all *.class files returned by getManipulators(ALL_CATEGORY).
     *
     * @return file object of a jar file with all compiled manipulators
     * @throws IOException if jar file cannot be created
     */
    private static File createJSnippetJarFile() throws IOException {
        Collection<Object> classes = new ArrayList<>();
        classes.add(AbstractJSnippet.class);
        classes.add(Abort.class);
        classes.add(Cell.class);
        classes.add(ColumnException.class);
        classes.add(FlowVariableException.class);
        classes.add(Type.class);
        classes.add(TypeException.class);
        classes.add(NodeLogger.class);
        classes.add(KNIMEConstants.class);

        // create tree structure for classes
        DefaultMutableTreeNode root = createTree(classes);

        File jarFile = FileUtil.createTempFile("jsnippet", ".jar", new File(KNIMEConstants.getKNIMETempDir()), true);
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile))) {
            createJar(root, jar, null);
        }
        return jarFile;
    }

    private static DefaultMutableTreeNode createTree(final Collection<? extends Object> classes) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("build");
        for (Object o : classes) {
            Class<?> cl = o instanceof Class ? (Class<?>)o : o.getClass();
            Package pack = cl.getPackage();
            DefaultMutableTreeNode curr = root;
            for (String p : pack.getName().split("\\.")) {
                DefaultMutableTreeNode child = getChild(curr, p);
                if (null == child) {
                    DefaultMutableTreeNode h = new DefaultMutableTreeNode(p);
                    curr.add(h);
                    curr = h;
                } else {
                    curr = child;
                }
            }
            curr.add(new DefaultMutableTreeNode(cl));
        }

        return root;
    }

    private static DefaultMutableTreeNode getChild(final DefaultMutableTreeNode curr, final String p) {
        for (int i = 0; i < curr.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)curr.getChildAt(i);
            if (child.getUserObject().toString().equals(p)) {
                return child;
            }
        }
        return null;
    }

    private static void createJar(final DefaultMutableTreeNode node, final JarOutputStream jar, final String path)
        throws IOException {
        Object o = node.getUserObject();
        if (o instanceof String) {
            // folders must end with a "/"
            String subPath = null == path ? "" : (path + (String)o + "/");
            if (path != null) {
                JarEntry je = new JarEntry(subPath);
                jar.putNextEntry(je);
                jar.flush();
                jar.closeEntry();
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
                createJar(child, jar, subPath);
            }
        } else {
            Class<?> cl = (Class<?>)o;
            String className = cl.getSimpleName();
            className = className.concat(".class");
            JarEntry entry = new JarEntry(path + className);
            jar.putNextEntry(entry);

            ClassLoader loader = cl.getClassLoader();
            InputStream inStream = loader.getResourceAsStream(cl.getName().replace('.', '/') + ".class");

            FileUtil.copy(inStream, jar);
            inStream.close();
            jar.flush();
            jar.closeEntry();

        }
    }

    @Override
    public GuardedDocument getDocument() {
        // Lazy initialization of the document
        if (m_document == null) {
            m_document = createDocument();
            m_document.addDocumentListener(new DocumentListener() {

                @Override
                public void removeUpdate(final DocumentEvent e) {
                    m_dirty = true;
                }

                @Override
                public void insertUpdate(final DocumentEvent e) {
                    m_dirty = true;
                }

                @Override
                public void changedUpdate(final DocumentEvent e) {
                    m_dirty = true;
                }
            });
            initDocument(m_document);
        }
        return m_document;
    }

    /** Create the document with the default skeleton. */
    private GuardedDocument createDocument() {
        return new JavaSnippetDocument("public void snippet() throws TypeException, ColumnException, Abort");
    }

    /** Initialize document with information from the settings. */
    private void initDocument(final GuardedDocument doc) {
        try {
            initGuardedSections(doc);
            if (null != m_settings) {
                doc.replaceBetween(GUARDED_IMPORTS, GUARDED_FIELDS, m_settings.getScriptImports());
                doc.replaceBetween(GUARDED_FIELDS, GUARDED_BODY_START, m_settings.getScriptFields());
                doc.replaceBetween(GUARDED_BODY_START, GUARDED_BODY_END, m_settings.getScriptBody());
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Initialize GUARDED_IMPORTS and GUARDED_FIELDS with information from the settings.
     */
    private void initGuardedSections(final GuardedDocument doc) {
        try {
            GuardedSection imports = doc.getGuardedSection(GUARDED_IMPORTS);
            imports.setText(createImportsSection());
            GuardedSection fields = doc.getGuardedSection(GUARDED_FIELDS);
            fields.setText(createFieldsSection());
        } catch (BadLocationException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Create the system variable (input and output) section of the snippet.
     */
    private String createFieldsSection() {
        StringBuilder out = new StringBuilder();
        out.append("// system variables\n");
        out.append("public class JSnippet extends AbstractJSnippet {\n");
        if (m_fields.getInColFields().size() > 0) {
            out.append("  // Fields for input columns\n");
            for (InCol field : m_fields.getInColFields()) {
                out.append("  /** Input column: \"");
                out.append(field.getKnimeName());
                out.append("\" */\n");
                appendFields(out, field);
            }
        }
        if (m_fields.getInVarFields().size() > 0) {
            out.append("  // Fields for input flow variables\n");
            for (InVar field : m_fields.getInVarFields()) {
                out.append("  /** Input flow variable: \"");
                out.append(field.getKnimeName());
                out.append("\" */\n");
                appendFields(out, field);
            }
        }
        out.append("\n");
        if (m_fields.getOutColFields().size() > 0) {
            out.append("  // Fields for output columns\n");
            for (OutCol field : m_fields.getOutColFields()) {
                out.append("  /** Output column: \"");
                out.append(field.getKnimeName());
                out.append("\" */\n");
                appendFields(out, field);
            }
        }
        if (m_fields.getOutVarFields().size() > 0) {
            out.append("  // Fields for output flow variables\n");
            for (OutVar field : m_fields.getOutVarFields()) {
                out.append("  /** Output flow variable: \"");
                out.append(field.getKnimeName());
                out.append("\" */\n");
                appendFields(out, field);
            }
        }

        out.append("\n");
        return out.toString();
    }

    /** Append field declaration to the string builder. */
    private void appendFields(final StringBuilder out, final JavaField f) {
        out.append("  public ");
        if (null != f.getJavaType()) {
            out.append(f.getJavaType().getSimpleName());
        } else {
            out.append("<invalid>");
        }

        out.append(" ");
        out.append(f.getJavaName());
        out.append(";\n");
    }

    /**
     * Create the imports section for the snippet's document.
     */
    private String createImportsSection() {
        StringBuilder imports = new StringBuilder();
        imports.append("// system imports\n");
        for (String s : getSystemImports()) {
            imports.append("import ");
            imports.append(s);
            imports.append(";\n");
        }
        imports.append("\n");

        // Some custom converters may allow custom input and output JavaTypes we need to import.
        final ArrayList<JavaField> fields = new ArrayList<>(m_fields.getInColFields());
        fields.addAll(m_fields.getOutColFields());
        fields.addAll(m_fields.getInVarFields());
        fields.addAll(m_fields.getOutVarFields());

        final Set<String> fieldImports = new LinkedHashSet<>();
        for (JavaField field : fields) {
            Class<?> fieldType = field.getJavaType();
            if (fieldType == null) {
                continue;
            }

            // Handle arrays of arrays of arrays of... AP-7012
            while (fieldType.isArray()) {
                fieldType = fieldType.getComponentType();
            }
            fieldType = ClassUtil.ensureObjectType(fieldType);

            // java.lang.* is imported by default, we do not need to import that again.
            if (!fieldType.getName().startsWith("java.lang")) {
                fieldImports.add(fieldType.getName());
            }
        }

        if (!fieldImports.isEmpty()) {
            imports.append("// imports for input and output fields\n");
        }

        for (String s : fieldImports) {
            imports.append("import ");
            imports.append(s);
            imports.append(";\n");
        }
        imports.append("\n");

        return imports.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Parser getParser() {
        // lazy initialization of the parser
        if (m_parser == null) {
            m_parser = new JSnippetParser(this);
        }
        return m_parser;
    }

    /**
     * Get the list of default imports. Override this method to append or modify this list.
     *
     * @return the list of default imports
     */
    protected String[] getSystemImports() {
        String pkg = "org.knime.base.node.jsnippet.expression";
        return new String[]{AbstractJSnippet.class.getName(), Abort.class.getName(), Cell.class.getName(),
            ColumnException.class.getName(), TypeException.class.getName(), "static " + pkg + ".Type.*"
            /* these need to be listed, even if unused, for backwards-compatibility */
            , "java.util.Date", "java.util.Calendar", "org.w3c.dom.Document"};
    }

    /**
     * Validate settings which is typically called in the configure method of a node.
     *
     * What is checked:
     * <ul>
     * <li>Whether converter factories matching the ids from the settings exist</li>
     * <li>Whether the code compiles</li>
     * <li>Whether columns required by input mappings still exist.</li>
     * </ul>
     *
     * @param spec the spec of the data table at the inport
     * @param flowVariableRepository the flow variables at the inport
     * @return the validation results
     */
    public ValidationReport validateSettings(final DataTableSpec spec,
        final FlowVariableRepository flowVariableRepository) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // check input fields
        for (final InCol field : m_fields.getInColFields()) {
            final int index = spec.findColumnIndex(field.getKnimeName());
            if (index < 0) {
                errors.add("The column \"" + field.getKnimeName() + "\" is not found in the input table.");
            } else {
                final DataColumnSpec colSpec = spec.getColumnSpec(index);
                final DataType type = colSpec.getType();

                if (!type.equals(field.getDataType())) {
                    // Input column type changed, try to find new converter
                    final Optional<?> factory = ConverterUtil.getConverterFactory(type, field.getJavaType());

                    if (factory.isPresent()) {
                        warnings.add(
                            "The type of the column \"" + field.getKnimeName() + "\" has changed but is compatible.");
                        field.setConverterFactory(type, (DataCellToJavaConverterFactory<?, ?>)factory.get());
                    } else {
                        errors.add("The type of the column \"" + field.getKnimeName() + "\" has changed.");
                    }
                }
            }

            if (!field.getConverterFactory().isPresent()) {
                errors.add(String.format("Missing converter for column '%s' to java field '%s' (converter id: '%s')",
                    field.getKnimeName(), field.getJavaName(), field.getConverterFactoryId()));
            }
        }

        // check input variables
        for (InVar field : m_fields.getInVarFields()) {
            FlowVariable var = flowVariableRepository.getFlowVariable(field.getKnimeName());
            if (var != null) {
                if (!var.getType().equals(field.getFlowVarType())) {
                    errors.add("The type of the flow variable \"" + field.getKnimeName() + "\" has changed.");
                }
            } else {
                errors.add("The flow variable \"" + field.getKnimeName() + "\" is not found in the input.");
            }
        }

        // check output fields
        for (OutCol field : m_fields.getOutColFields()) {
            if (field.getJavaType() == null) {
                errors.add("Java type could not be loaded. Providing plugin may be missing.");
            }

            int index = spec.findColumnIndex(field.getKnimeName());
            if (field.getReplaceExisting() && index < 0) {
                errors.add("The output column \"" + field.getKnimeName() + "\" is marked to be a replacement, "
                    + "but an input with this name does not exist.");
            }
            if (!field.getReplaceExisting() && index > 0) {
                errors.add("The output column \"" + field.getKnimeName() + "\" is marked to be new, "
                    + "but an input with this name does exist.");
            }
            if (!field.getConverterFactory().isPresent()) {
                errors.add(String.format("Missing converter for java field '%s' to column '%s' (converter id: '%s')",
                    field.getJavaName(), field.getKnimeName(), field.getConverterFactoryId()));
            }
        }

        // check output variables
        for (OutVar field : m_fields.getOutVarFields()) {
            FlowVariable var = flowVariableRepository.getFlowVariable(field.getKnimeName());
            if (field.getReplaceExisting() && var == null) {
                errors.add("The output flow variable \"" + field.getKnimeName() + "\" is marked to be a replacement, "
                    + "but an input with this name does not exist.");
            }
            if (!field.getReplaceExisting() && var != null) {
                errors.add("The output flow variable \"" + field.getKnimeName() + "\" is marked to be new, "
                    + "but an input with this name does exist.");
            }
        }

        // Check additional bundles
        for (final String bundleString : m_settings.getBundles()) {
            final String[] split = bundleString.split(" ");
            final String bundleName = split[0];
            if (split.length <= 1) {
                errors.add(String.format("Missing version for bundle \"%s\" in settings", bundleName));
                continue;
            }

            final Bundle[] bundles = Platform.getBundles(bundleName, null);
            if (bundles == null) {
                errors.add("Bundle \"" + bundleName + "\" required by this snippet was not found.");
                continue;
            }

            boolean bundleFound = false;
            final Version savedVersion = Version.parseVersion(split[1]);
            for (final Bundle bundle : bundles) {
                final Version installedVersion = bundle.getVersion();

                if (versionMatches(installedVersion, savedVersion)) {
                    bundleFound = true;
                    break;
                }
            }

            if (!bundleFound) {
                errors.add(String.format("No installed version of \"%s\" matched version range [%s, %d.0.0).",
                    bundleName, savedVersion, savedVersion.getMajor() + 1));
            }
        }

        try {
            // test if snippet compiles and if the file can be created
            // also checks additional jar files.
            createSnippetClass();
        } catch (Exception e) {
            errors.add(e.getMessage());
        } finally {
            close();
        }
        return new ValidationReport(errors.toArray(new String[errors.size()]),
            warnings.toArray(new String[warnings.size()]));
    }

    /**
     * Create the outspec of the java snippet node. This method is typically used in the configure of a node.
     *
     * @param spec the spec of the data table at the inport
     * @param flowVariableRepository the flow variables at the inport
     * @return the spec at the output
     * @throws InvalidSettingsException when settings are inconsistent with the spec or the flow variables at the inport
     */
    public DataTableSpec configure(final DataTableSpec spec, final FlowVariableRepository flowVariableRepository)
        throws InvalidSettingsException {
        DataTableSpec outSpec = createRearranger(spec, flowVariableRepository, -1, null).createSpec();
        // populate flowVariableRepository with new flow variables having
        // default values
        for (OutVar outVar : m_fields.getOutVarFields()) {
            FlowVariable flowVar = null;
            if (outVar.getFlowVarType().equals(org.knime.core.node.workflow.FlowVariable.Type.INTEGER)) {
                flowVar = new FlowVariable(outVar.getKnimeName(), -1);
            } else if (outVar.getFlowVarType().equals(org.knime.core.node.workflow.FlowVariable.Type.DOUBLE)) {
                flowVar = new FlowVariable(outVar.getKnimeName(), -1.0);
            } else {
                flowVar = new FlowVariable(outVar.getKnimeName(), "");
            }
            flowVariableRepository.put(flowVar);
        }
        return outSpec;
    }

    /**
     * Execute the snippet.
     *
     * @param table the data table at the inport
     * @param flowVariableRepository the flow variables at the inport
     * @param exec the execution context to report progress
     * @return the table for the output
     * @throws InvalidSettingsException when settings are inconsistent with the table or the flow variables at the input
     * @throws CanceledExecutionException when execution is canceled by the user
     */
    public BufferedDataTable execute(final BufferedDataTable table, final FlowVariableRepository flowVariableRepository,
        final ExecutionContext exec) throws CanceledExecutionException, InvalidSettingsException {
        final OutColList outFields = m_fields.getOutColFields();
        if (outFields.size() > 0) {
            final ColumnRearranger rearranger =
                createRearranger(table.getDataTableSpec(), flowVariableRepository, table.getRowCount(), exec);
            return exec.createColumnRearrangeTable(table, rearranger, exec);
        } else {
            final JavaSnippetCellFactory factory = new JavaSnippetCellFactory(this, table.getDataTableSpec(),
                flowVariableRepository, table.getRowCount(), exec);

            try {
                for (final DataRow row : table) {
                    exec.checkCanceled();
                    factory.getCells(row);
                }
            } finally {
                factory.afterProcessing();
            }
            return table;
        }
    }

    /**
     * The execution method when no input table is present. I.e. used by the java edit variable node.
     *
     * @param flowVariableRepository flow variables at the input
     * @param exec the execution context to report progress, may be null when this method is called from configure
     */
    public void execute(final FlowVariableRepository flowVariableRepository, final ExecutionContext exec) {
        DataTableSpec spec = new DataTableSpec();
        CellFactory factory = new JavaSnippetCellFactory(this, spec, flowVariableRepository, 1, exec);
        factory.getCells(new DefaultRow(RowKey.createRowKey(0L), new DataCell[0]));
    }

    /** The rearranger is the working horse for creating the output table. */
    ColumnRearranger createRearranger(final DataTableSpec spec, final FlowVariableRepository flowVariableRepository,
        final int rowCount, final ExecutionContext context) throws InvalidSettingsException {
        int offset = spec.getNumColumns();
        CellFactory factory = new JavaSnippetCellFactory(this, spec, flowVariableRepository, rowCount, context);
        ColumnRearranger c = new ColumnRearranger(spec);
        // add factory to the column rearranger
        c.append(factory);

        // define which new columns do replace others
        OutColList outFields = m_fields.getOutColFields();
        for (int i = outFields.size() - 1; i >= 0; i--) {
            OutCol field = outFields.get(i);
            int index = spec.findColumnIndex(field.getKnimeName());
            if (index >= 0) {
                if (field.getReplaceExisting()) {
                    c.remove(index);
                    c.move(offset + i - 1, index);
                } else {
                    throw new InvalidSettingsException(
                        "Field \"" + field.getJavaName() + "\" is configured to " + "replace no existing columns.");
                }
            }
        }

        return c;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public JavaSnippetTemplate createTemplate(final Class metaCategory) {
        JavaSnippetTemplate template = new JavaSnippetTemplate(metaCategory, getSettings());
        return template;
    }

    @Override
    public File getTempClassPath() {
        return m_tempClassPathDir;
    }

    /**
     * Get the system fields of the snippet.
     *
     * @return the fields
     */
    public JavaSnippetFields getSystemFields() {
        return m_fields;
    }

    @Override
    public void setJavaSnippetFields(final JavaSnippetFields fields) {
        m_fields = fields;
        if (null != m_document) {
            initGuardedSections(m_document);
        }
    }

    /**
     * Set the list of additional jar files to be added to the class path of the snippet.
     *
     * @param jarFiles the jar files
     */
    public void setJarFiles(final String[] jarFiles) {
        if (!Arrays.equals(m_jarFiles, jarFiles)) {
            m_jarFiles = jarFiles.clone();
            m_snippetCache.invalidate();
        }
    }

    /**
     * @param bundles
     */
    public void setAdditionalBundles(final String[] bundles) {
        if (!Arrays.equals(m_settings.getBundles(), bundles)) {
            m_settings.setBundles(bundles);
            m_snippetCache.invalidate();
        }
    }

    /**
     * Create the class file of the snippet.
     *
     * Creates a URLClassLoader which may open jar files referenced by the Java Snippet class. Make sure to match every
     * call to this method with a call to {@link #close()} in order for KNIME to be able to clean up .jar files that
     * were downloaded from URLs.
     *
     * @return the compiled snippet
     */
    @SuppressWarnings("unchecked")
    private synchronized Class<? extends AbstractJSnippet> createSnippetClass() {
        JavaSnippetCompiler compiler = new JavaSnippetCompiler(this);

        /* Recompile/Reload either if the code changed or the class loader has been closed since */
        if (m_classLoader != null && m_snippetCache.isValid(getDocument())) {
            if (!m_snippetCache.hasCustomFields()) {
                return m_snippetCache.getSnippetClass();
            }
        } else {
            // recompile
            m_snippetCache.invalidate();
            StringWriter log = new StringWriter();
            DiagnosticCollector<JavaFileObject> digsCollector = new DiagnosticCollector<>();
            CompilationTask compileTask = null;
            try {
                compileTask = compiler.getTask(log, digsCollector);
            } catch (IOException e) {
                throw new IllegalStateException("Compile with errors: " + e.getMessage(), e);
            }
            boolean success = compileTask.call();
            if (!success) {
                StringBuilder msg = new StringBuilder();
                msg.append("Compile with errors:\n");
                for (Diagnostic<? extends JavaFileObject> d : digsCollector.getDiagnostics()) {
                    boolean isSnippet = this.isSnippetSource(d.getSource());
                    if (isSnippet && d.getKind().equals(javax.tools.Diagnostic.Kind.ERROR)) {
                        long line = d.getLineNumber();
                        if (line != Diagnostic.NOPOS) {
                            msg.append("Error in line " + line + ": ");
                        } else {
                            msg.append("Error: ");
                        }
                        msg.append(d.getMessage(Locale.US));
                        msg.append('\n');
                    }
                }

                throw new IllegalStateException(msg.toString());
            }
        }

        try {
            close();

            final LinkedHashSet<ClassLoader> customTypeClassLoaders = new LinkedHashSet<>();
            customTypeClassLoaders.add(JavaSnippet.class.getClassLoader());

            for (final InCol col : m_fields.getInColFields()) {
                customTypeClassLoaders.addAll(getClassLoadersFor(col.getConverterFactoryId()));
            }
            for (final OutCol col : m_fields.getOutColFields()) {
                customTypeClassLoaders.addAll(getClassLoadersFor(col.getConverterFactoryId()));
            }

            /* Add class loaders of additional bundles */
            customTypeClassLoaders.addAll(getAdditionalBundlesClassLoaders());

            // remove core class loader:
            //   (a) it's referenced via JavaSnippet.class classloader and
            //   (b) it would collect buddies when used directly (see support ticket #1943)
            customTypeClassLoaders.remove(DataCellToJavaConverterRegistry.class.getClassLoader());

            final MultiParentClassLoader customTypeLoader = new MultiParentClassLoader(
                customTypeClassLoaders.stream().toArray(ClassLoader[]::new));

            // TODO (Next version bump) change return value of createClassLoader instead of cast
            m_classLoader = (URLClassLoader)compiler.createClassLoader(customTypeLoader);
            Class<? extends AbstractJSnippet> snippetClass =
                (Class<? extends AbstractJSnippet>)m_classLoader.loadClass("JSnippet");
            m_snippetCache.update(getDocument(), snippetClass, m_settings);
            return snippetClass;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load class file.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load jar files.", e);
        }
    }

    /**
     * Create an instance of the snippet.
     *
     * @return a snippet instance
     */
    public AbstractJSnippet createSnippetInstance() {
        Class<? extends AbstractJSnippet> jsnippetClass = createSnippetClass();
        AbstractJSnippet instance;
        try {
            instance = jsnippetClass.newInstance();
        } catch (InstantiationException e) {
            // cannot happen, but rethrow and close resources just in case
            close();
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            // cannot happen, but rethrow and close resources just in case
            close();
            throw new RuntimeException(e);
        }
        if (m_logger != null) {
            instance.attachLogger(m_logger);
        }
        return instance;
    }

    /**
     * Attach logger to be used by this java snippet instance.
     *
     * @param logger the node logger
     */
    public void attachLogger(final NodeLogger logger) {
        m_logger = logger;
    }

    @Override
    protected void finalize() throws Throwable {
        FileUtil.deleteRecursively(m_tempClassPathDir);

        if (m_classLoader != null) {
            LOGGER.debug("Java Snippet URLClassLoader was not closed properly.");
            close();
        }

        super.finalize();
    }

    // --- Classpath caching related methods and fields --- //
    private static final Map<String, Set<File>> CLASSPATH_CACHE = new LinkedHashMap<>();

    private static final Map<Class<?>, Set<File>> CLASSPATH_FOR_CLASS_CACHE = new LinkedHashMap<>();

    static final NodeLogger LOGGER = NodeLogger.getLogger(JavaSnippet.class);

    /**
     * Create the classpath for every java type of every converter factory registered in
     * {@link DataCellToJavaConverterRegistry} and {@link JavaToDataCellConverterRegistry}. Called during bundle
     * activation.
     */
    synchronized static void cacheCustomTypeClasspaths() {
        if (!CLASSPATH_CACHE.isEmpty()) {
            // this was already called.
            LOGGER.warn("Custom type classpaths were aleady cached.");
            return;
        }

        Set<DataCellToJavaConverterFactory<?, ?>> dcToJavaFactories = new LinkedHashSet<>();
        dcToJavaFactories.addAll(DataCellToJavaConverterRegistry.getInstance().getAllConverterFactories());
        dcToJavaFactories.addAll(ConverterUtil.getFactoriesForSourceType(DateAndTimeCell.TYPE));
        for (final DataCellToJavaConverterFactory<?, ?> factory : dcToJavaFactories) {
            final Class<?> javaType = factory.getDestinationType();
            CLASSPATH_CACHE.put(factory.getIdentifier(),
                resolveBuildPathForJavaType((javaType.isArray()) ? javaType.getComponentType() : javaType));
        }

        Set<JavaToDataCellConverterFactory<?>> javaToDCFactories = new LinkedHashSet<>();
        javaToDCFactories.addAll(JavaToDataCellConverterRegistry.getInstance().getAllConverterFactories());
        javaToDCFactories.addAll(ConverterUtil.getFactoriesForDestinationType(DateAndTimeCell.TYPE));
        for (JavaToDataCellConverterFactory<?> factory : javaToDCFactories) {
            final Class<?> javaType = factory.getSourceType();
            CLASSPATH_CACHE.put(factory.getIdentifier(),
                resolveBuildPathForJavaType((javaType.isArray()) ? javaType.getComponentType() : javaType));
        }

    }

    /**
     * Get the build path for a converter factory.
     *
     * @param converterFactoryId ID of the converter factory for which to get the build path
     * @return List of files required to compile with the java type of given converterFactoryId, or <code>null</code> if
     *         the converter factory Id doesn't have a build path and should not be provided.
     * @noreference This method is not intended to be referenced by clients.
     */
    public static Collection<? extends File> getBuildPathFromCache(final String converterFactoryId) {
        String id = converterFactoryId;
        while (id.startsWith(ArrayToCollectionConverterFactory.class.getName())) {
            id = id.substring(ArrayToCollectionConverterFactory.class.getName().length() + 1, id.length() - 1);
        }
        while (id.startsWith(CollectionConverterFactory.class.getName())) {
            id = id.substring(CollectionConverterFactory.class.getName().length() + 1, id.length() - 1);
        }
        final Collection<File> buildPath = CLASSPATH_CACHE.get(id);
        return buildPath;
    }

    /**
     * Get the class loaders required for a specific converter factory
     *
     * @param converterFactoryId ID of the converter factory
     * @return A list of class loaders required for given converter factory
     * @noreference This method is not intended to be referenced by clients.
     */
    private static Collection<ClassLoader> getClassLoadersFor(final String converterFactoryId) {
        final Optional<DataCellToJavaConverterFactory<?, ?>> factory =
            ConverterUtil.getDataCellToJavaConverterFactory(converterFactoryId);
        if (factory.isPresent()) {
            final ArrayList<ClassLoader> clsLoaders = new ArrayList<>(2);
            final ClassLoader sourceCL = factory.get().getSourceType().getClassLoader();
            if (sourceCL != null) {
                clsLoaders.add(sourceCL);
            }
            final ClassLoader destCL = factory.get().getDestinationType().getClassLoader();
            if (destCL != null) {
                clsLoaders.add(destCL);
            }
            return clsLoaders;
        } else {
            final Optional<JavaToDataCellConverterFactory<?>> factory2 =
                ConverterUtil.getJavaToDataCellConverterFactory(converterFactoryId);
            if (factory2.isPresent()) {
                final ClassLoader cl = factory2.get().getSourceType().getClassLoader();
                if (cl != null) {
                    return Collections.singleton(cl);
                }
            }
            return Collections.emptyList();
        }
    }

    /**
     * Get the {@link Bundle} which contains the given class.
     *
     * @param javaType Class for which to get the bundle.
     * @return the Bundle or <code>null</code>, if it is a built-in type.
     */
    public synchronized static Bundle resolveBundleForJavaType(final Class<?> javaType) {
        if (javaType.getClassLoader() instanceof ModuleClassLoader) {
            final ModuleClassLoader moduleClassLoader = (ModuleClassLoader)javaType.getClassLoader();
            return moduleClassLoader.getBundle();
        } else {
            return null;
        }
    }

    /**
     * Get file and jar urls required for compiling with given java type
     */
    private synchronized static Set<File> resolveBuildPathForJavaType(final Class<?> javaType) {
        if (javaType.isPrimitive()) {
            return Collections.emptySet();
        }
        final Set<File> javaTypeCache = CLASSPATH_FOR_CLASS_CACHE.get(javaType);
        if (javaTypeCache != null) {
            return javaTypeCache;
        }

        final Set<File> result = new LinkedHashSet<>();
        final Set<URL> urls = new LinkedHashSet<>();

        ClassUtil.streamForClassHierarchy(javaType).filter(c -> c.getClassLoader() instanceof ModuleClassLoader)
            .flatMap(c -> {
                final ModuleClassLoader moduleClassLoader = (ModuleClassLoader)c.getClassLoader();
                return Arrays.stream(moduleClassLoader.getClasspathManager().getHostClasspathEntries());
            }).forEach(entry -> {
                final BundleFile file = entry.getBundleFile();
                try {
                    final URL url = file.getBaseFile().toURI().toURL();
                    urls.add(url);
                } catch (MalformedURLException e) {
                    LOGGER.error("Could not resolve URL for bundle file \"" + file.toString()
                        + "\" while assembling build path for custom java type \"" + javaType.getName() + "\"");
                }
                result.add(file.getBaseFile());
            });

        /* Check whether the java snippet compiler can later find the class with this classpath */
        try (final URLClassLoader classpathClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]))) {
            classpathClassLoader.loadClass(javaType.getName());
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            LOGGER.error("Classpath for \"" + javaType.getName() + "\" could not be assembled.", e);
            return null; // indicate that this type should not be provided in java snippet
        } catch (IOException e) { // thrown by URLClassLoader.close()
            LOGGER.error("Unable to close classloader used for testing of custom type classpath.", e);
        }

        if (result.contains(null)) {
            throw new IllegalStateException("Couldn't assemble classpath for custom type \"" + javaType
                + "\", illegal <null> value in list:\n  " + result.stream()
                    .map(f -> f == null ? "<NULL>" : f.getAbsolutePath()).collect(Collectors.joining("\n  ")));
        }

        CLASSPATH_FOR_CLASS_CACHE.put(javaType, result);
        return result;
    }

    private String m_warningMessage = null;

    /**
     * Set the warning message. Used by {@link JavaSnippetCellFactory} to set the node's warning message.
     * @param message The message to set or <code>null</code> to clear the warning.
     */
    public synchronized void setWarningMessage(final String message) {
        m_warningMessage = message;
    }

    /**
     * Get warning message set by the {@link JavaSnippetCellFactory} if exceptions occurred during processing of on of
     * the input rows. Used to set the node's warning message.
     *
     * @return Warning message currently set on the snippet
     */
    public String getWarningMessage() {
        return m_warningMessage;
    }
}
