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
 *   Feb 15, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.util.asynclose;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * A {@link Closeable} that doesn't throw a {@link Exception checked Exception} and has a separate method that allows to
 * an asynchronous close returning a future.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <X> the type of exception {@link #close()} throws
 */
public interface AsynchronousCloseable<X extends Exception> extends AutoCloseable {

    /**
     * Closes the resource asynchronously in another thread and returns a {@link Future} of the closing process. After a
     * call of {@link #asynchronousClose()}, {@link #close()} will take no effect and return immediately.
     *
     * @return the Future of the closing process
     * @throws X the type of exception that can be thrown
     */
    Future<Void> asynchronousClose() throws X;

    /**
     * {@inheritDoc}. Closes the resource synchronously, i.e. after {@link #close()} returns, the resource is completely
     * closed and all further calls to {@link #close()} or {@link #asynchronousClose()} wil take no effect.
     */
    @Override
    void close() throws X;

    /**
     * A void Callable with a generically typed exception.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @param <X> the type of exception the close method throws (use Runtime for unchecked exceptions)
     */
    @FunctionalInterface
    public interface CloseMethod<X extends Exception> {
        /**
         * Invokes the method.
         *
         * @throws X if the call fails
         */
        void call() throws X;
    }

    /**
     * Creates a closer object that implements the asynchronous close logic. Use this method if closing your resources
     * can throw a checked exception.
     *
     * @param <X> the type of checked exception thrown when invoking the closeMethod
     * @param closeMethod that closes your resources
     * @return a closer that implements the asynchronous close logic
     */
    public static <X extends Exception> AsynchronousCloseable<X>
        createAsynchronousCloser(final CloseMethod<X> closeMethod) {
        return new AsynchronousCloser<>(closeMethod);
    }

    /**
     * Utility method for classes implementing {@link #asynchronousClose()} in case there is nothing to close
     * asynchronously.
     *
     * @return a completed future
     */
    public static Future<Void> alreadyClosed() {
        return CompletableFuture.completedFuture(null);
    }

}
