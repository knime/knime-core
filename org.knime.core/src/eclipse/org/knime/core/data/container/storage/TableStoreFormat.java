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
 * History
 *   Mar 14, 2016 (wiswedel): created
 */
package org.knime.core.data.container.storage;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;

/** Implemented by additional table storage formats, collected via extension point.
 *
 * @author wiswedel
 * @since 3.6
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public interface TableStoreFormat {

    /**
     * Sub element in config that keeps the list of cell class information (used to be a plain array).
     */
    static final String CFG_CELL_CLASSES = "table.datacell.classes";

    /** Class name of data cell. */
    static final String CFG_CELL_SINGLE_CLASS = "class";

    /** Element type if a cell represents a collection. */
    static final String CFG_CELL_SINGLE_ELEMENT_TYPE = "collection.element.type";

    /** @return non-blank short name shown in the preference page, e.g. "Default" or "Column Store (Apache XYZ)".*/
    public String getName();

    /** @return file name suffix for temp files, e.g. 'bin' or 'orc'. */
    public String getFilenameSuffix();

    /** Check if this format can write a table with the argument spec.
     * @param spec non-null spec that is about to be written
     * @return true if possible, false if not (will log a message and then use the fallback format)
     */
    public boolean accepts(final DataTableSpec spec);

    /** Create a new writer instance. No argument is null.
     * @param binFile where to write to (new file).
     * @param spec The spec
     * @param writeRowKey If to also write the row key (false for appending columsn tables)
     * @return A writer instance.
     * @throws IOException Any type of I/O problem.
     */
    public AbstractTableStoreWriter createWriter(final File binFile, final DataTableSpec spec,
        boolean writeRowKey) throws IOException;

    /** Similar to {@link #createWriter(File, DataTableSpec, boolean)} but writing to a stream. This is an optional
     * operation. If not supported the content will be written to a temp file and then the content is copied into the
     * stream.
     * @param output ..
     * @param spec ..
     * @param writeRowKey ...
     * @return The writer
     * @throws IOException ...
     * @throws UnsupportedOperationException If not supported by the implementation
     */
    // TODO really needed?
    public AbstractTableStoreWriter createWriter(final OutputStream output, final DataTableSpec spec,
        boolean writeRowKey) throws IOException, UnsupportedOperationException;

    /** Creates a reader that is able to read the file previously written by the writer.
     * @param binFile where to read from
     * @param spec The spec matching the content of the file.
     * @param settings The settings (written by
     *          {@link AbstractTableStoreWriter#writeMetaInfoAfterWrite(org.knime.core.node.NodeSettingsWO)})
     * @param tblRep For looking up blobs
     * @param version The version as defined in the Buffer class
     * @param isReadRowKey If file contains the row key.
     * @return The reader object
     * @throws IOException Any type of I/O problem.
     * @throws InvalidSettingsException If the settings aren't expected as per the corresponding write method.
     */
    public AbstractTableStoreReader createReader(final File binFile, final DataTableSpec spec,
        final NodeSettingsRO settings, final Map<Integer, ContainerTable> tblRep, int version,
        boolean isReadRowKey) throws IOException, InvalidSettingsException;

    /**
     * The (internal) version used to write the format. The value is {@link #validateVersion(String) validated} during
     * reading.
     *
     * @return the version string that is persisted
     */
    public String getVersion();

    /**
     * Validates the version string that was saved along with the data.
     *
     * @param versionString The non-null version
     * @return true if the version is 'known' and readable, false otherwise.
     */
    public boolean validateVersion(final String versionString);

}
