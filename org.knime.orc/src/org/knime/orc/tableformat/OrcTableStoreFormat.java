/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 14, 2016 (wiswedel): created
 */
package org.knime.orc.tableformat;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.storage.AbstractTableStoreFormat;
import org.knime.core.data.container.storage.AbstractTableStoreReader;
import org.knime.core.data.container.storage.AbstractTableStoreWriter;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;

/**
 *
 * @author wiswedel
 */
public final class OrcTableStoreFormat extends AbstractTableStoreFormat {

    static final Map<DataType, OrcKNIMEType> SUPPORTED_TYPES_MAP;

    static {
        SUPPORTED_TYPES_MAP = new HashMap<>();
        SUPPORTED_TYPES_MAP.put(StringCell.TYPE, OrcKNIMEType.STRING);
        SUPPORTED_TYPES_MAP.put(DoubleCell.TYPE, OrcKNIMEType.DOUBLE);
        SUPPORTED_TYPES_MAP.put(IntCell.TYPE, OrcKNIMEType.LONG);
        SUPPORTED_TYPES_MAP.put(LongCell.TYPE, OrcKNIMEType.BYTE_ARRAY);
    }

    /** {@inheritDoc} */
    @Override
    public boolean accept(final DataTableSpec spec) {
        return spec.stream().map(c -> c.getType()).allMatch(t -> SUPPORTED_TYPES_MAP.containsKey(t));
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsBlobs() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public AbstractTableStoreWriter createWriter(final File binFile, final DataTableSpec spec, final int bufferID,
        final boolean writeRowKey) throws IOException {
        return new OrcTableStoreWriter(binFile, spec, bufferID, writeRowKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractTableStoreWriter createWriter(final OutputStream output, final DataTableSpec spec, final int bufferID,
        final boolean writeRowKey) throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException("Writing to stream not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractTableStoreReader createReader(final File binFile, final DataTableSpec spec, final NodeSettingsRO settings,
        final int bufferID, final Map<Integer, ContainerTable> tblRep, final int version,
        final boolean isReadRowKey) throws IOException, InvalidSettingsException {
        return new OrcTableStoreReader(binFile, spec, settings, bufferID, tblRep, version, isReadRowKey);
    }

}
