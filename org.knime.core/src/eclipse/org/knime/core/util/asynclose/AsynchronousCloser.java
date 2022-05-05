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
 *   May 5, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.util.asynclose;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class that implements the {@link AsynchronousCloseable} logic.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <X> the type of exception the close method throws
 */
final class AsynchronousCloser<X extends Exception> implements AsynchronousCloseable<X> {

    private final AtomicBoolean m_open = new AtomicBoolean(true);

    private final CloseMethod<X> m_closeMethod;

    /**
     * Constructor for the closer.
     *
     * @param closeMethod method that should be called by {@link #close()} and {@link #asynchronousClose()}
     */
    AsynchronousCloser(final CloseMethod<X> closeMethod) {
        m_closeMethod = closeMethod;
    }

    @Override
    public final void close() throws X {
        if (m_open.getAndSet(false)) {
            // this method is called first potentially because the try-with resource is left due to an exception
            // therefore shutdown synchronously
            m_closeMethod.call();
        }
    }

    @Override
    public final Future<Void> asynchronousClose() throws X {
        if (m_open.getAndSet(false)) {
            // this method is called first, therefore the client intended asynchronous closing
            var executor = Executors.newSingleThreadExecutor();
            Future<Void> future = executor.submit(() -> {
                m_closeMethod.call();
                return null;
            });
            // shuts down the executor once the close task has been completed
            executor.shutdown();
            return future;
        } else {
            return AsynchronousCloseable.alreadyClosed();
        }
    }

}