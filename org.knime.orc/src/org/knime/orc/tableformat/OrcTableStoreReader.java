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
 *   Mar 18, 2016 (wiswedel): created
 */
package org.knime.orc.tableformat;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.storage.AbstractTableStoreReader;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.orc.tableformat.OrcKNIMEUtil.OrcWriterBuilder;
import org.knime.orc.tableformat.OrcKNIMEUtil.PrestoDrivenRow;
import org.knime.orc.tableformat.OrcKNIMEUtil.PrestoDrivenRowIterator;

/**
 *
 * @author wiswedel
 */
final class OrcTableStoreReader extends AbstractTableStoreReader {

    private OrcWriterBuilder m_builder;

    /**
     * @param binFile
     * @param spec
     * @param settings
     * @param bufferID
     * @param tblRep
     * @param version
     * @param isReadRowKey
     * @throws InvalidSettingsException
     */
    public OrcTableStoreReader(final File binFile, final DataTableSpec spec, final NodeSettingsRO settings, final int bufferID,
        final Map<Integer, ContainerTable> tblRep, final int version, final boolean isReadRowKey) throws InvalidSettingsException {
        m_builder = new OrcWriterBuilder(binFile, isReadRowKey);
        m_builder.fromSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    public TableStoreCloseableRowIterator iterator() throws IOException {
        return new MyTableStoreCloseableRowIterator(m_builder.createRowIterator());
    }

    static class MyTableStoreCloseableRowIterator extends TableStoreCloseableRowIterator {

        private PrestoDrivenRowIterator m_prestoRowIterator;
        private PrestoDrivenRow m_prestoRow;

        /**
         * @param prestoRowIterator
         */
        MyTableStoreCloseableRowIterator(final PrestoDrivenRowIterator prestoRowIterator) {
            m_prestoRowIterator = prestoRowIterator;
            m_prestoRow = m_prestoRowIterator.next(null);
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return m_prestoRow != null;
        }

        /** {@inheritDoc} */
        @Override
        public DataRow next() {
            DataRow next = m_prestoRow;
            m_prestoRow = m_prestoRowIterator.next(null);
            return next;
        }

        /** {@inheritDoc} */
        @Override
        public boolean performClose() throws IOException {
            m_prestoRowIterator.close();
            return true;
        }

    }

}
