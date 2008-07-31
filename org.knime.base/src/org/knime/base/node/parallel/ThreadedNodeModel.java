/*  
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
