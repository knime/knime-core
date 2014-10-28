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
 *   15.10.2014 (tibuch): created
 */
package org.knime.base.node.io.fixedwidthfr;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Tim-Oliver Buchholz
 */
public class FixedWidthFRTable implements DataTable, AutoCloseable {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(FixedWidthFRTable.class);

    private List<WeakReference<CloseableRowIterator>> m_iterators;

    private DataTableSpec m_tableSpec;

    private FixedWidthFRSettings m_nodeSettings;

    private ExecutionContext m_exec;

    /**
     * @param tableSpec the DataTableSpec
     * @param nodeSettings the current node settings
     * @param exec the execution context
     */
    public FixedWidthFRTable(final DataTableSpec tableSpec, final FixedWidthFRSettings nodeSettings,
        final ExecutionContext exec) {

        if (tableSpec == null || nodeSettings == null) {
            throw new IllegalArgumentException("Must specify non-null table spec and node settings for file table.");
        }
        m_iterators = new ArrayList<WeakReference<CloseableRowIterator>>();
        m_tableSpec = tableSpec;
        m_nodeSettings = nodeSettings;
        m_exec = exec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_tableSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloseableRowIterator iterator() {
        try {
            synchronized (m_iterators) {
                CloseableRowIterator i = createRowIterator(m_nodeSettings, m_tableSpec, m_exec);

                m_iterators.add(new WeakReference<CloseableRowIterator>(i));
                return i;

            }
        } catch (IOException ioe) {
            LOGGER.error("I/O Error occurred while trying to open a stream" + " to '"
                + m_nodeSettings.getFileLocation().toString() + "'.");
        }
        return null;
    }

    /**
     * @param nodeSettings the node settings
     * @param tableSpec the tableSpec
     * @param exec the execution context
     * @return a FixedWidthRowIterator
     * @throws IOException if buffered file reader can't be created
     */
    protected CloseableRowIterator createRowIterator(final FixedWidthFRSettings nodeSettings,
        final DataTableSpec tableSpec, final ExecutionContext exec) throws IOException {
        return new FixedWidthRowIterator(nodeSettings, tableSpec, exec);
    }

    /**
     * Call this before releasing the last reference to this table and all its iterators. It closes the underlying
     * source. Especially if the iterators don't run to the end of the table, it is required to call this method.
     * Otherwise the file handle is not released until the garbage collector cleans up. A call to <code>next()</code> on
     * any of the iterators of this table after disposing of the table has undefined behavior.
     */
    @Override
    public void close() {
        synchronized (m_iterators) {
            for (WeakReference<CloseableRowIterator> i : m_iterators) {
                CloseableRowIterator iter = i.get();
                if (iter != null) {
                    iter.close();
                }
            }
            m_iterators.clear();
        }
    }
}
