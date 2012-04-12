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
 *   Dec 31, 2009 (wiswedel): created
 */
package org.knime.ext.sun.nodes.script.compile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;

/** Utilizes {@link JavaCompiler} (by default the eclipse compiler)
 * to compile in memory java file objects.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class JavaCodeCompiler {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(JavaCodeCompiler.class);

    private File[] m_classpaths;
    private String[] m_additionalCompileArgs;
    private JavaFileObject[] m_sources;

    private InMemoryJavaFileManager m_fileMgr;

    private File m_sourceCodeDebugDir;

    /** @param classpaths the classpaths to set */
    public void setClasspaths(final File... classpaths) {
        m_classpaths = classpaths;
    }

    /** @return the classpaths */
    public File[] getClasspaths() {
        return m_classpaths;
    }

    /** Set a location to which the source code is written for debug purposes.
     * By default this location is <code>null</code>, i.e. everything happens
     * in memory.
     * @param sourceCodeDebugDir the sourceCodeDebugDir to set */
    public void setSourceCodeDebugDir(final File sourceCodeDebugDir) {
        m_sourceCodeDebugDir = sourceCodeDebugDir;
    }

    /** @return the sourceCodeDebugDir */
    public File getSourceCodeDebugDir() {
        return m_sourceCodeDebugDir;
    }

    /** @param additionalCompileArgs the additionalCompileArgs to set */
    public void setAdditionalCompileArgs(
            final String... additionalCompileArgs) {
        m_additionalCompileArgs = additionalCompileArgs;
    }

    /** @return the additionalCompileArgs */
    public String[] getAdditionalCompileArgs() {
        return m_additionalCompileArgs;
    }

    /** @param sources the sources to set */
    public void setSources(final JavaFileObject... sources) {
        m_sources = sources;
    }

    /** @return the sources */
    public JavaFileObject[] getSources() {
        return m_sources;
    }

    public void compile() throws CompilationFailedException {
        if (m_sources == null || m_sources.length == 0) {
            throw new CompilationFailedException("No sources set");
        }
        ArrayList<String> compileArgs = new ArrayList<String>();
        if (m_classpaths != null && m_classpaths.length > 0) {
            compileArgs.add("-classpath");
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < m_classpaths.length; i++) {
                if (i > 0) {
                    b.append(File.pathSeparatorChar);
                }
                b.append(m_classpaths[i]);
                File file = m_classpaths[i];
                String filePath = file.getAbsolutePath();
                if (!file.exists()) {
                    throw new CompilationFailedException("Can't read file \""
                            + filePath + "\"; invalid class path");
                }
            }
            compileArgs.add(b.toString());
        }
        compileArgs.add("-nowarn");
        if (m_additionalCompileArgs != null) {
            compileArgs.addAll(Arrays.asList(m_additionalCompileArgs));
        }
        final StringWriter logString = new StringWriter();
//        ServiceLoader<JavaCompiler> serviceLoader =
//            ServiceLoader.load(JavaCompiler.class);
        // the service loader sometimes didn't work in the RMI instance,
        // so we hard-code the compiler here.
        JavaCompiler compiler = new EclipseCompiler();
//        for (JavaCompiler c : serviceLoader) {
//            compiler = c;
//        }
//        if (compiler == null) {
//            throw new CompilationFailedException("Unable to find compiler");
//        }
        // compiler = com.sun.tools.javac.api.JavacTool.create();
        if (m_sourceCodeDebugDir != null) {
            try {
                File tmpDir = m_sourceCodeDebugDir;
                tmpDir.mkdir();
                for (JavaFileObject source : m_sources) {
                    CharSequence charContent = source.getCharContent(false);
                    File out = new File(tmpDir, source.getName());
                    out.getParentFile().mkdirs();
                    StringReader reader =
                        new StringReader(charContent.toString());
                    FileWriter writer = new FileWriter(out);
                    FileUtil.copy(reader, writer);
                    writer.close();
                    reader.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Unable to write source code to \""
                        + m_sourceCodeDebugDir.getAbsolutePath()
                        + "\": " + e.getMessage(), e);
            }
        }
        DiagnosticCollector<JavaFileObject> digsCollector =
            new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager stdFileMgr = compiler.getStandardFileManager(
                digsCollector, null, null);
        m_fileMgr = new InMemoryJavaFileManager(stdFileMgr);
        CompilationTask compileTask = compiler.getTask(logString, m_fileMgr,
                digsCollector, compileArgs, null, Arrays.asList(m_sources));
        if (!compileTask.call()) {
            boolean hasDiagnostic = false;
            StringBuilder b = new StringBuilder("Unable to compile expression");
            for (Diagnostic<? extends JavaFileObject> d
                    : digsCollector.getDiagnostics()) {
                switch (d.getKind()) {
                case ERROR:
                    String[] sourceLines = new String[0];
                    if (d.getSource() != null) {
                        JavaFileObject srcJavaFileObject = d.getSource();
                        String src;
                        if (srcJavaFileObject
                                instanceof InMemorySourceJavaFileObject) {
                            src = ((InMemorySourceJavaFileObject)
                                    srcJavaFileObject).getSource();
                        } else {
                            try {
                                src = srcJavaFileObject.getCharContent(
                                        false).toString();
                            } catch (IOException ioe) {
                                src = null;
                            }
                        }
                        if (src != null) {
                            sourceLines = getSourceLines(src);
                        }
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("<<<< Expression Start >>>>");
                            LOGGER.debug("<<<< " + srcJavaFileObject.getName()
                                    + " >>>>");
                            for (int i = 0; i < sourceLines.length; i++) {
                                LOGGER.debug((i + 1) + ": " + sourceLines[i]);
                            }
                            LOGGER.debug("<<<< Expression End >>>>");
                        }
                    }
                    if (hasDiagnostic) {
                        b.append("\n"); // follow up error, insert empty line
                    }
                    hasDiagnostic = true;
                    int lineIndex = (int)(d.getLineNumber() - 1);
                    b.append("\nERROR at line ").append(lineIndex + 1);
                    b.append("\n").append(d.getMessage(Locale.US));
                    int sourceLineCount = sourceLines.length;
                    if (lineIndex - 1 >= 0 && lineIndex - 1 < sourceLineCount) {
                        // previous line
                        b.append("\n  Line : ").append(lineIndex);
                        b.append("  ").append(sourceLines[lineIndex - 1]);
                    }
                    if (lineIndex >= 0 && lineIndex < sourceLineCount) {
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

    public ClassLoader createClassLoader(final ClassLoader parent)
        throws CompilationFailedException {
        if (m_fileMgr == null) {
            throw new IllegalStateException(
                    "Did not call compile() or compilation failed");
        }
        ClassLoader trueParent = parent;

        if (m_classpaths != null && m_classpaths.length > 0) {
            URL[] urls = new URL[m_classpaths.length];
            for (int i = 0; i < m_classpaths.length; i++) {
                try {
                    urls[i] = m_classpaths[i].toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new CompilationFailedException("Unable to retrieve "
                            + "URL from jar file \""
                            + m_classpaths[i].getAbsolutePath() + "\"", e);
                }
            }
            trueParent = URLClassLoader.newInstance(urls, parent);
        }
        return new ClassLoader(trueParent) {
            /** {@inheritDoc} */
            @Override
            protected Class<?> findClass(final String name)
                    throws ClassNotFoundException {
                byte[] byteCode = m_fileMgr.getClassByteCode(name);
                return defineClass(name, byteCode, 0, byteCode.length);
            }
        };
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

}
