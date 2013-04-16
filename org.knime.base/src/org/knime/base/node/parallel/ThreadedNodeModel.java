/*  
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 
 * History
 *   Mar 13, 2006 (thor): created
 */
package org.knime.base.node.parallel;

import org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel;
import org.knime.base.node.parallel.builder.ThreadedTableBuilderNodeModel;
import org.knime.core.node.KNIMEConstants;

/**
 * This model is an extension of the AbstractParallelNodeModel that uses a sub
 * pool of the global KNIME thread pool. The default maximum number of threads
 * for each of these nodes is {@link #DEFAULT_MAX_THREAD_COUNT} which is
 * currently set to the number of CPUs + 1 (this is only a rule of thumb,
 * nothing more). The maximum number can be adjusted by calling
 * {@link #setMaxThreads(int)} but the real maximum is still determined by the
 * global thread pool.
 * 
 * @deprecated Use either the {@link ThreadedColAppenderNodeModel} if you want
 *             to add columns to the input tables and the number of rows stays
 *             the same, or the {@link ThreadedTableBuilderNodeModel} if you
 *             want to build a completely new table.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
@Deprecated
public abstract class ThreadedNodeModel extends AbstractParallelNodeModel {
    /** The default maximum number of threads for each threaded node. */
    public static final int DEFAULT_MAX_THREAD_COUNT =
            Runtime.getRuntime().availableProcessors() + 1;

    /**
     * Creates a new AbstractParallelNodeModel.
     * 
     * @param nrDataIns The number of {@link org.knime.core.data.DataTable}
     *            elements expected as inputs.
     * @param nrDataOuts The number of {@link org.knime.core.data.DataTable}
     *            objects expected at the output.
     * @param chunkSize the default number of rows in the chunked
     *            {@link org.knime.core.data.DataTable}s
     */
    public ThreadedNodeModel(final int nrDataIns, final int nrDataOuts,
            final int chunkSize) {
        super(nrDataIns, nrDataOuts, chunkSize,
                KNIMEConstants.GLOBAL_THREAD_POOL
                        .createSubPool(DEFAULT_MAX_THREAD_COUNT));
    }

    /**
     * Sets the maximum number of threads that may be used by this node.
     * 
     * @param count the maximum thread count
     */
    public void setMaxThreads(final int count) {
        m_workers.setMaxThreads(count);
    }
}
