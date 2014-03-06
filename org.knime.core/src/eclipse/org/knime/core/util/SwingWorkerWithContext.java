/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 * ---------------------------------------------------------------------
 *
 * Created on 25.06.2013 by thor
 */
package org.knime.core.util;

import java.util.List;

import javax.swing.SwingWorker;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContext;

/**
 * This is an extension of {@link SwingWorker} that ensures that a {@link NodeContext} is set when
 * {@link #doInBackground()}, {@link #process(List)}, or {@link #done()} are called. The {@link NodeContext} is taken
 * from the creator of this object, not from the caller of {@link #execute()}.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @param <T> the result type returned by this {@code SwingWorker's} {@code doInBackground} and {@code get} methods
 * @param <V> the type used for carrying out intermediate results by this {@code SwingWorker's} {@code publish} and
 *            {@code process} methods
 * @since 2.8
 */
public abstract class SwingWorkerWithContext<T, V> extends SwingWorker<T, V> {
    private static final NodeLogger logger = NodeLogger.getLogger(SwingWorkerWithContext.class);

    private final NodeContext m_nodeContext;

    /**
     * Creates a new swing worker that saves the current {@link NodeContext} for use in the abstract methods.
     */
    public SwingWorkerWithContext() {
        m_nodeContext = NodeContext.getContext();
        if (m_nodeContext == null) {
            logger
                .debug("Unnecessary usage of SwingWorkerWithContext because no context is available", new Exception());
        }
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * <p>
     * Note that this method is executed only once.
     *
     * <p>
     * Note: this method is executed in a background thread.
     *
     *
     * @return the computed result
     * @throws Exception if unable to compute a result
     * @see SwingWorker#doInBackground
     */
    protected abstract T doInBackgroundWithContext() throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    protected final T doInBackground() throws Exception {
        NodeContext.pushContext(m_nodeContext);
        try {
            return doInBackgroundWithContext();
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /**
     * Receives data chunks from the {@code publish} method asynchronously on the <i>Event Dispatch Thread</i>.
     *
     * <p>
     * Please refer to the {@link #publish} method for more details.
     *
     * @param chunks intermediate results to process
     *
     * @see #publish
     * @see SwingWorker#process(List)
     *
     */
    protected void processWithContext(final List<V> chunks) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void process(final List<V> chunks) {
        NodeContext.pushContext(m_nodeContext);
        try {
            processWithContext(chunks);
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /**
     * Executed on the <i>Event Dispatch Thread</i> after the {@code doInBackground}
     * method is finished. The default
     * implementation does nothing. Subclasses may override this method to
     * perform completion actions on the <i>Event Dispatch Thread</i>. Note
     * that you can query status inside the implementation of this method to
     * determine the result of this task or whether this task has been cancelled.
     *
     * @see #doInBackground
     * @see #isCancelled()
     * @see #get
     * @see SwingWorker#done
     */
    protected void doneWithContext() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void done() {
        NodeContext.pushContext(m_nodeContext);
        try {
            doneWithContext();
        } finally {
            NodeContext.removeLastContext();
        }
    }

}
