/*
 * ------------------------------------------------------------------------
 *
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
 *   29.06.2015 (koetter): created
 */
package org.knime.base.node.jsnippet.util;

import java.io.File;
import java.io.IOException;

import javax.swing.text.Document;
import javax.tools.JavaFileObject;

import org.apache.commons.io.FileUtils;
import org.fife.ui.rsyntaxtextarea.parser.Parser;


/**
 *
 * <p>This class might change and is not meant as public API.
 * @author Tobias Koetter, KNIME.com
 * @param <T> the {@link JSnippetTemplate}
 * @since 2.12
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface JSnippet<T extends JSnippetTemplate> {

    /**
     * Create a template for this snippet.
     * @param metaCategory the meta category of the template
     * @return the template with a new uuid.
     */
    public T createTemplate(Class<?> metaCategory);

    /**
     * Set the system fields in the java snippet.
     * @param fields the fields to set
     */
    public void setJavaSnippetFields(JavaSnippetFields fields);

    /**
     * Get the document with the code of the snippet.
     * @return the document
     */
    public Document getDocument();

    /**
     * Get the parser for the snippet's document.
     * @return the parser
     */
    public Parser getParser();

    /**
     * Get the jar files to be added to the class path.
     * @return the jar files for the class path, never <code>null</code>
     * @throws IOException when a file could not be loaded
     */
    public File[] getClassPath() throws IOException;

    /**
     * Get the jar files required at runtime for this snippet.
     *
     * This is usually a subset of @ref {@link #getClassPath()}.
     * @return the jar files for the class path, never <code>null</code>
     * @throws IOException when a file could not be loaded
     */
    default public File[] getRuntimeClassPath() throws IOException {
        return getClassPath();
    }

    /**
     * Get the jar files required at compiletime for this snippet.
     *
     * This is usually a subset of @ref {@link #getClassPath()}.
     * @return the jar files for the class path, never <code>null</code>
     * @throws IOException when a file could not be loaded
     */
    default public File[] getCompiletimeClassPath() throws IOException {
        return getClassPath();
    }

    /**
     * Get compilation units used by the JavaSnippetCompiler.
     * @return the files to compile
     * @throws IOException When files cannot be created.
     */
     Iterable<? extends JavaFileObject> getCompilationUnits() throws IOException;

     /** Get the path to the temporary directory of this java snippet.
      * @return the path to the temporary directory
      */
    public File getTempClassPath();

    /**
     * Return true when this snippet is the creator and maintainer of the
     * given source.
     * @param source the source
     * @return if this snippet is the given source
     */
    public boolean isSnippetSource(JavaFileObject source);

    /** Additional build path entries. Non-empty for standard java snipppet that also supports new in-output types
     * driven by {@link org.knime.core.data.convert.java.DataCellToJavaConverterRegistry}.
     * @return A non-null array (default implementation returns empty array)
     * @since 3.3
     */
    default public File[] getAdditionalBuildPaths() {
        return FileUtils.EMPTY_FILE_ARRAY;
    }

}