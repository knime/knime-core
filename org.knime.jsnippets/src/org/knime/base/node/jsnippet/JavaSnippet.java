/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   01.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import org.eclipse.jdt.internal.compiler.tool.EclipseFileObject;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.knime.base.node.jsnippet.JavaFieldSettings.OutCol;
import org.knime.base.node.jsnippet.JavaFieldSettingsList.OutColList;
import org.knime.base.node.jsnippet.expression.AbstractJSnippet;
import org.knime.base.node.jsnippet.expression.Cell;
import org.knime.base.node.jsnippet.expression.ColumnException;
import org.knime.base.node.jsnippet.expression.FlowVariableException;
import org.knime.base.node.jsnippet.expression.Type;
import org.knime.base.node.jsnippet.expression.TypeException;
import org.knime.base.node.jsnippet.guarded.GuardedDocument;
import org.knime.base.node.jsnippet.guarded.GuardedSection;
import org.knime.base.node.jsnippet.ui.JSnippetParser;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.FileUtil;

/**
 *
 * @author Heiko Hofer
 */
public final class JavaSnippet {
    static final String GUARDED_IMPORTS = "imports";
    static final String GUARDED_FIELDS = "fields";
    static final String GUARDED_BODY_START = "bodyStart";
    static final String GUARDED_BODY_END = "bodyEnd";

    public static final String VERSION_1_X = "version 1.x";

    private static File jSnippetJar;
    private String[] m_jarFiles;
    // caches the jSnippetJar and the jarFiles.
    private File[] m_jarFileCache;

    private JavaFileObject m_snippet;

    private GuardedDocument m_document;
    // true when the document has changed and the m_snippet is not up to date.
    private boolean m_dirty;

    private JSnippetParser m_parser;

    private JavaSnippetSettings m_settings;
    private JavaSnippetFields m_fields;


    private File m_tempClassPathDir;

    /**
     *
     */
    public JavaSnippet() {
    	m_fields = new JavaSnippetFields();
    }


    /**
     * @param settings
     */
    public void setSettings(final JavaSnippetSettings settings) {
        m_settings = settings;
   		m_fields = settings.getJavaSnippetFields();
   		m_jarFiles = settings.getJarFiles();
        init();
    }

    private void init() {
        if (null != m_document) {
            initDocument(m_document);
        }
    }


    /**
     * @param settings
     */
    public JavaSnippetSettings getSettings() {
        updateSettings();
        return m_settings;
    }

    private void updateSettings() {
        try {
            GuardedDocument doc = getDocument();
            m_settings.setScriptImports(doc.getTextBetween(GUARDED_IMPORTS,
                    GUARDED_FIELDS));
            m_settings.setScriptFields(doc.getTextBetween(GUARDED_FIELDS,
                    GUARDED_BODY_START));
            m_settings.setScriptBody(doc.getTextBetween(GUARDED_BODY_START,
                    GUARDED_BODY_END));
        } catch (BadLocationException e) {
            // this should never happen
            throw new IllegalStateException(e);
        }
        // update input fields
		m_settings.setJavaSnippetFields(m_fields);
		// update jar files
		m_settings.setJarFiles(m_jarFiles);
    }


    /**
     * Get the jar files to be added to the class path.
     * @return the jar files for the class path
     * @throws IOException when a file could not be loaded
     */
    File[] getClassPath() throws IOException {
        // use cached list if present
        if (null != m_jarFileCache) {
            return m_jarFileCache;
        }
        // Add lock since jSnippetJar is used across all JavaSnippets
        synchronized (this) {
            if (jSnippetJar == null) {
                jSnippetJar = createJSnippetJarFile();
            }
        }

        if (null != m_jarFiles && m_jarFiles.length > 0) {
            List<File> jarFiles = new ArrayList<File>();
            jarFiles.add(jSnippetJar);
            for (int i = 0; i < m_jarFiles.length; i++) {
                try {
                    jarFiles.add(JavaSnippetUtil.toFile(m_jarFiles[i]));
                } catch (InvalidSettingsException e) {
                    // jar file does not exist
                    // TODO how to react
                }
            }
            m_jarFileCache = jarFiles.toArray(new File[jarFiles.size()]);
        } else {
            m_jarFileCache = new File[] {jSnippetJar};
        }
        return m_jarFileCache;
    }

    /**
     * Get compilation units used by the JavaSnippetCompiler.
     * @return the files to compile
     * @throws IOException When files cannot be created.
     */
    public Iterable<? extends JavaFileObject> getCompilationUnits()
        throws IOException {

        if (m_snippet == null) {
            m_snippet = createJSnippetFile();
        } else {
            if (m_dirty) {
                Writer out = m_snippet.openWriter();
                try {
                    try {
                        Document doc = getDocument();
                        out.write(doc.getText(0, doc.getLength()));
                        m_dirty = false;
                    } catch (BadLocationException e) {
                        // this should never happen.
                        throw new IllegalStateException(e);
                    }
                } finally {
                    out.close();
                }
            }
        }
        return Collections.singletonList(m_snippet);
    }

    /**
     * @return
     */
    private JavaFileObject createJSnippetFile() throws IOException {
        m_tempClassPathDir = FileUtil.createTempDir("knime_javasnippet");
        m_tempClassPathDir.deleteOnExit();
        File jSnippetFile = new File(m_tempClassPathDir.getPath()
                + File.separator + "JSnippet.java");
        FileOutputStream fos = new FileOutputStream(jSnippetFile);
        OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8");
        try {
            try {
                Document doc = getDocument();
                out.write(doc.getText(0, doc.getLength()));
            } catch (BadLocationException e) {
                // this should never happen.
                throw new IllegalStateException(e);
            }
        } finally {
            out.close();
        }

        return new EclipseFileObject("JSnippet", jSnippetFile.toURI(),
                Kind.SOURCE, Charset.forName("UTF-8"));
    }


    public boolean isSnippetSource(final JavaFileObject source) {
        return null != m_snippet ? source.equals(m_snippet) : false;
    }


    /**
     * Give jar file with all *.class files returned by
     * getManipulators(ALL_CATEGORY).
     *
     * @return file object of a jar file with all compiled manipulators
     * @throws IOException if jar file cannot be created
     */
    private File createJSnippetJarFile() throws IOException {

            File tempClassPathDir = FileUtil
                    .createTempDir("knime_javasnippet");
            tempClassPathDir.deleteOnExit();
            File m_jarFile = new File(tempClassPathDir.getPath()
                    + File.separator + "jsnippet.jar");
            m_jarFile.deleteOnExit();
            FileOutputStream out = new FileOutputStream(m_jarFile);
            JarOutputStream jar = new JarOutputStream(out);

            Collection<Object> classes = new ArrayList<Object>();
            classes.add(AbstractJSnippet.class);
            classes.add(Cell.class);
            classes.add(ColumnException.class);
            classes.add(FlowVariableException.class);
            classes.add(Type.class);
            classes.add(TypeException.class);




            // create tree structure for classes
            DefaultMutableTreeNode root = createTree(classes);
            try {
                createJar(root, jar, null);
            } catch (IOException ioe) {
                throw ioe;
            } finally {
                jar.close();
                out.close();
            }


        return m_jarFile;
    }

    // TODO: This code is copied from StringManipulatorProvider create common code base
    private DefaultMutableTreeNode createTree(
            final Collection<? extends Object> classes) {
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

    // TODO: This code is copied from StringManipulatorProvider create common code base
    private DefaultMutableTreeNode getChild(final DefaultMutableTreeNode curr,
            final String p) {
        for (int i = 0; i < curr.getChildCount(); i++) {
            DefaultMutableTreeNode child =
                (DefaultMutableTreeNode)curr.getChildAt(i);
            if (child.getUserObject().toString().equals(p)) {
                return child;
            }
        }
        return null;
    }

    // TODO: This code is copied from StringManipulatorProvider create common code base
    private void createJar(final DefaultMutableTreeNode node,
            final JarOutputStream jar,
            final String path) throws IOException {
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
                DefaultMutableTreeNode child =
                    (DefaultMutableTreeNode) node.getChildAt(i);
                createJar(child, jar, subPath);
            }
        } else {
            Class<?> cl = (Class<?>)o;
            String className = cl.getSimpleName();
            className = className.concat(".class");
            JarEntry entry = new JarEntry(path + className);
            jar.putNextEntry(entry);

            ClassLoader loader = cl.getClassLoader();
            InputStream inStream = loader.getResourceAsStream(
                    cl.getName().replace('.', '/') + ".class");

            FileUtil.copy(inStream, jar);
            inStream.close();
            jar.flush();
            jar.closeEntry();

        }
    }


    /**
     * @return
     */
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


    private GuardedDocument createDocument() {
        GuardedDocument doc = new GuardedDocument(
                SyntaxConstants.SYNTAX_STYLE_JAVA);
        try {
            doc.addGuardedSection(
                    GUARDED_IMPORTS, doc.getLength());
            doc.insertString(doc.getLength(),
                    " \n", null);
            doc.addGuardedSection(
                    GUARDED_FIELDS, doc.getLength());
            doc.insertString(doc.getLength(),
                    " \n", null);
            GuardedSection bodyStart = doc.addGuardedSection(
                    GUARDED_BODY_START, doc.getLength());
            bodyStart.setText("// expression start\n"
                    + "  public void snippet() "
                    + "throws TypeException, ColumnException {\n");
            doc.insertString(doc.getLength(),
                    " \n", null);

            GuardedSection bodyEnd = doc.addGuardedFooterSection(
                    GUARDED_BODY_END, doc.getLength());
            bodyEnd.setText("// expression end\n"
                    + "    }\n"
                    + "}");
        } catch (BadLocationException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        return doc;
    }

    private void initDocument(final GuardedDocument doc) {
        try {
            initGuardedSections(doc);
            if (null != m_settings) {
                doc.replaceBetween(GUARDED_IMPORTS, GUARDED_FIELDS,
                        m_settings.getScriptImports());
                doc.replaceBetween(GUARDED_FIELDS, GUARDED_BODY_START,
                        m_settings.getScriptFields());
                doc.replaceBetween(GUARDED_BODY_START, GUARDED_BODY_END,
                        m_settings.getScriptBody());
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

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
     * @return
     */
    private String createFieldsSection() {
        StringBuilder out = new StringBuilder();
        out.append("// system variables\n");
        out.append("public class JSnippet extends AbstractJSnippet {\n");
        if (m_fields.getInColFields().size() > 0) {
        	out.append("  // Fields for input columns\n");
        	appendFields(out, m_fields.getInColFields().iterator());
        }
        if (m_fields.getInVarFields().size() > 0) {
        	out.append("  // Fields for input flow variables\n");
        	appendFields(out, m_fields.getInVarFields().iterator());
        }
        out.append("\n");
        if (m_fields.getOutColFields().size() > 0) {
        	out.append("  // Fields for output columns\n");
        	appendFields(out, m_fields.getOutColFields().iterator());
        }
        if (m_fields.getOutVarFields().size() > 0) {
        	out.append("  // Fields for output flow variables\n");
        	appendFields(out, m_fields.getOutVarFields().iterator());
        }

        out.append("\n");
        return out.toString();
    }

    private void appendFields(final StringBuilder out,
    		final Iterator<? extends JavaFieldSettings> fields) {
        while (fields.hasNext()) {
        	JavaFieldSettings f = fields.next();
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
    }

    /**
     * @return
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
        return imports.toString();
    }


    /**
     * @return
     */
    public Parser getParser() {
        // lazy initialization of the parser
        if (m_parser == null) {
            m_parser = new JSnippetParser(this);
        }
        return m_parser;
    }

    protected String[] getSystemImports() {
        String pkg = "org.knime.base.node.jsnippet.expression";
        return new String[] {AbstractJSnippet.class.getName()
                , Cell.class.getName()
                , ColumnException.class.getName()
                , TypeException.class.getName()
                , "static " + pkg + ".Type.*"
                , "java.util.Date"};
    }


    /**
     * @param spec
     * @param flowVariableRepository
     * @return
     */
    public DataTableSpec configure(final DataTableSpec spec,
            final FlowVariableRepository flowVariableRepository)
        throws InvalidSettingsException {
        return createRearranger(spec, flowVariableRepository, -1).createSpec();
    }


    /**
     * @param table
     * @param flowVariableRepository
     * @param exec
     * @return
     */
    public BufferedDataTable execute(
            final BufferedDataTable table,
            final FlowVariableRepository flowVariableRepository,
            final ExecutionContext exec) throws Exception {
        return exec.createColumnRearrangeTable(table,
                createRearranger(table.getDataTableSpec(),
                        flowVariableRepository, table.getRowCount()), exec);
    }

    /**
     * @param flowVarRepo
     * @param exec
     */
    public void execute(final FlowVariableRepository flowVariableRepository,
            final ExecutionContext exec) {
        DataTableSpec spec = new DataTableSpec();
        CellFactory factory = new JavaSnippetCellFactory(this, spec,
                flowVariableRepository, 1);
        factory.getCells(new DefaultRow(RowKey.createRowKey(0),
                new DataCell[0]));
    }

    private ColumnRearranger createRearranger(final DataTableSpec spec,
            final FlowVariableRepository flowVariableRepository,
            final int rowCount)
        throws InvalidSettingsException {
        int offset = spec.getNumColumns();
        CellFactory factory = new JavaSnippetCellFactory(this, spec,
                flowVariableRepository, rowCount);
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
                    throw new InvalidSettingsException("Field \""
                            + field.getJavaName() + "\" is configured to "
                            + "replace no existing columns.");
                }
            }
        }

        return c;
    }

    public File getTempClassPath() {
        return m_tempClassPathDir;
    }

	/**
	 * Get the system fields of the snippet.
	 * @return the fields
	 */
	public JavaSnippetFields getSystemFields() {
		return m_fields;
	}

	/**
	 * Set the system fields in the java snippet.
	 * @param fields the fields to set
	 */
	public void setJavaSnippetFields(final JavaSnippetFields fields) {
		m_fields = fields;
		initGuardedSections(m_document);
	}

	/**
	 * Set the list of additional jar files to be added to the class path
	 * of the snippet.
	 * @param jarFiles the jar files
	 */
	public void setJarFiles(final String[] jarFiles) {
	    m_jarFiles = jarFiles;
	    // reset cache
	    m_jarFileCache = null;
	}




}
