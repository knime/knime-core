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
 *   Mar 18, 2016 (wiswedel): created
 */
package org.knime.orc.tableformat;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.storage.AbstractTableStoreWriter;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.orc.tableformat.OrcKNIMEUtil.OrcKNIMEWriter;
import org.knime.orc.tableformat.OrcKNIMEUtil.OrcWriterBuilder;

/**
 *
 * @author wiswedel
 */
final class OrcTableStoreWriter extends AbstractTableStoreWriter {

    private final File m_binFile;
    private final OrcKNIMEWriter m_orcKNIMEWriter;
    private final NodeSettings m_orcSettings;


    /**
     * @param binFile
     * @param spec
     * @param writeRowKey
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public OrcTableStoreWriter(final File binFile, final DataTableSpec spec, final boolean writeRowKey) throws IllegalArgumentException, IOException {
        super(spec, writeRowKey);
        m_binFile = binFile;
        m_binFile.delete();
        OrcWriterBuilder builder = new OrcWriterBuilder(binFile, writeRowKey);
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);
            String name = i + "-" + colSpec.getName().replaceAll("[^a-zA-Z]", "-");
            builder.addField(name, OrcTableStoreFormat.SUPPORTED_TYPES_MAP.get(colSpec.getType()));
        }
        m_orcKNIMEWriter = builder.create();
        NodeSettings s = new NodeSettings("orc-settings");
        builder.writeSettings(s);
        m_orcSettings = s;
    }

    /** {@inheritDoc} */
    @Override
    public void writeRow(final DataRow row) throws IOException {
        m_orcKNIMEWriter.addRow(row);
    }

    /** {@inheritDoc} */
    @Override
    public void writeMetaInfoAfterWrite(final NodeSettingsWO settings) {
        m_orcSettings.copyTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        m_orcKNIMEWriter.close();
    }

}
