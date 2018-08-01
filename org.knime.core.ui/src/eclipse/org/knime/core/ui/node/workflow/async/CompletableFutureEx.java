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
 *   Aug 1, 2018 (hornm): created
 */
package org.knime.core.ui.node.workflow.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Wrapper for {@link CompletableFuture}s that additional re-throws a predefined exception on calling
 * {@link #getOrThrow()}.
 *
 * The exception to be re-thrown needs to be wrapped into a {@link CompletionException} in the actual code that the
 * future uses for execution.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @param <T> the type of the future result
 * @param <E> the exception type to be re-thrown
 */
public final class CompletableFutureEx<T, E extends Exception> {

    private final CompletableFuture<T> m_future;

    private final Class<E> m_exceptionClass;

    /**
     * @param future
     * @param exceptionClass
     */
    public CompletableFutureEx(final CompletableFuture<T> future, final Class<E> exceptionClass) {
        m_future = future;
        m_exceptionClass = exceptionClass;
    }

    /**
     * Same as {@link CompletableFuture#get()} but also potentially throws a predefined exception.
     *
     * @return the result
     * @throws E the predefined exception that is potentially thrown
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @SuppressWarnings("unchecked")
    public T getOrThrow() throws E, InterruptedException, ExecutionException {
        try {
            return m_future.get();
        } catch (ExecutionException e) {
            if (m_exceptionClass.isAssignableFrom(e.getCause().getClass())) {
                throw (E)e.getCause();
            } else {
                throw e;
            }
        }
    }

    /**
     * @return class of the exception that is potentially thrown from {@link #getOrThrow()}
     */
    public Class<E> getExceptionClass() {
        return m_exceptionClass;
    }
}
