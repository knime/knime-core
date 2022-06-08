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
 *   Jun 8, 2022 (wiswedel): created
 */
package org.knime.core.data.container;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.knime.core.data.DataCell;
import org.knime.core.data.container.BlobDataCell.BlobAddress;

/**
 *
 * Map of blob addresses that were copied into a {@link Buffer}. It maps the original address to the new address (having
 * m_bufferID as owner). Copying is necessary if we copy from or to a buffer with id "-1" or this buffer is
 * instructed to copy all blobs (important for loop end nodes). If we didn't use such a map, we wouldn't notice if any
 * one blob gets copied into this buffer multiple times ... we would copy each time it is added, which is bad.
 *
 * This class used to be a simple HashMap but was enriched as part of AP-19046 in order to deal with Blobs that are
 * added to a Buffer from two different table, both of which are external to the workflow (buffer ID "-1").
 *
 * @author Bernd Wiswedel, KNIME, Konstanz, Germany
 */
final class BlobAddressCopyCache {

    private final Map<BlobAddressSource, BlobAddress> m_map = new HashMap<>();

    /**
     * Get the blob address that was a blob with the given argument was mapped to previously, or null if no such mapping
     * exists.
     *
     * @param blobAddress The source address (where this blob was added from)
     * @param cellSupplier The supplier that is only evaluated if the blobs is external to the workflow.
     * @return The mapping blob address, or null if no such mapping exists.
     */
    BlobAddress get(final BlobAddress blobAddress, final Supplier<DataCell> cellSupplier) {
        return m_map.get(new BlobAddressSource(blobAddress, cellSupplier));
    }

    /**
     * Register a new mapping. Arguments semantics, see get-method.
     */
    BlobAddress put(final BlobAddress blobAddress, final Supplier<DataCell> cellSupplier, final BlobAddress value) {
        return m_map.put(new BlobAddressSource(blobAddress, cellSupplier), value);
    }

    /** The blob address + the identity hash code as "heuristic" for duplicated blobs. The identity hash code is used
     * for differentation of two blobs added with the same address (e.g. buffer id = -1, column = 0, index-incolumn = 0)
     * but originating from two different buffers. (This is the case when using the "Table Reader" node reading two
     * '.table' files at the same time.
     * It's only a heuristic but a very strong one.
     */
    private static final class BlobAddressSource {
        private final BlobAddress m_blobAddress;
        private final int m_identityHashCode;

        BlobAddressSource(final BlobAddress blobAddress, final Supplier<DataCell> cellSupplier) {
            m_blobAddress = blobAddress;
            int identityHashCode = blobAddress.getBufferID() == -1 ? System.identityHashCode(cellSupplier.get()) : -1;
            m_identityHashCode = identityHashCode;
        }

        @Override
        public int hashCode() {
            return 31 * m_blobAddress.hashCode() + m_identityHashCode; // constant stolen from Arrays.hashCode()
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            BlobAddressSource bas = (BlobAddressSource)obj;
            if (!Objects.equals(m_blobAddress, bas.m_blobAddress)) {
                return false;
            }
            return Objects.equals(m_identityHashCode, bas.m_identityHashCode);
        }

        @Override
        public String toString() {
            return String.format("Blob Address: %s, Identity Hash Code: %s", m_blobAddress,
                m_blobAddress.getBufferID() == -1 ? Integer.toString(m_identityHashCode) : "<irrelevant>");
        }

    }

}
