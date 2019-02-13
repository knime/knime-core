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
 */
package org.knime.core.data.chunk;

import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.port.HasDataTableSpec;
import org.knime.core.node.port.PortObject;

/**
 * Access to chunks of data rows. This interface is used by {@link PortObject}s that have can provide their data as data
 * row chunks (such as the DB-port objects). Implementing port objects will need to provide a
 * {@link HasDataTableSpec}-spec, too. Since this is pending API, we refrained from introducing an extra interface to
 * emphasize this connection.
 *
 * Pending API! Will be re-visited with AP-11279.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 3.8
 * @noimplement This interface is not intended to be implemented by clients. Pending API!
 * @noreference This interface is not intended to be referenced by clients. Pending API!
 */
public interface DataRowChunks {

    /**
     * Returns the {@link DataTableSpec} object that specifies the structure of the data rows of each provided chunk.
     *
     * @return the DataTableSpec of the data row chunks
     */
    DataTableSpec getDataTableSpec();

    /**
     * Returns a chunk of data rows from the given row index and the maximum row count.
     *
     * @param from the index of the first row in the chunk (minimum 0)
     * @param count the number of rows in the chunk
     * @return the data rows of the chunk, the maximum number of rows is <code>count</code> or less (if the total number
     *         of rows has been exceed). Returns an empty list but never <code>null</code> if there are no rows
     *         available
     */
    List<DataRow> getChunk(long from, long count);
}
