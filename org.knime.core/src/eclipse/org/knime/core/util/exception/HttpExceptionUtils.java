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
 *   9 Dec 2022 (leon.wenzler): created
 */
package org.knime.core.util.exception;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

/**
 * All non-JAX-RS HTTP utilities, the core needs to know about. Also acts as a central HTTP utility provider, mainly for
 * knime-server-client.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 * @since 4.8
 */
public final class HttpExceptionUtils {

    /** We need this list because the server returns 500 instead of 400 in those error cases. */
    private static final String[] ERROR_MESSAGES = new String[]{"does not have an input resource",
        "does not have an input resource", "job wasn't loaded within", "valid parameter names"};

    /**
     * Maps HTTP responses codes to their corresponding java Exceptions. Uses {@link ResourceAccessException}.
     *
     * @param statusCode
     * @param errorMessage
     * @return ResourceAccessException
     */
    public static ResourceAccessException wrapException(final int statusCode, String errorMessage) {
        if (300 <= statusCode && statusCode <= 399) {
            errorMessage = StringUtils.isEmpty(errorMessage) ? String.format("Failed with status code %d", statusCode)
                : String.format("%d %s", statusCode, errorMessage);
            return new HttpResourceAccessException(errorMessage, statusCode);
        } else if (400 <= statusCode && statusCode <= 499) {
            errorMessage = String.format("%d %s", statusCode, errorMessage);
            return new ClientErrorAccessException(errorMessage, statusCode);
        } else if (500 <= statusCode && statusCode <= 599) {
            if (!isExceptionalErrorMessage(errorMessage)) {
                errorMessage = String.format("%d %s", statusCode, errorMessage);
            }
            return new ServerErrorAccessException(errorMessage, statusCode);
        }
        return new ResourceAccessException(
            String.format("Probably a coding problem, status code is neither 3xx, 4xx, nor 5xx. Error message: %s",
                errorMessage));
    }

    private static boolean isExceptionalErrorMessage(final String errorMessage) {
        return StringUtils.containsAny(errorMessage.toLowerCase(Locale.ENGLISH), ERROR_MESSAGES);
    }

    /**
     * Wraps an {@link IOException} (or probably mostly a {@link ResourceAccessException}) in a RuntimeException. Allows
     * for more flexible error handling.
     *
     * This method should be moved to the com.knime.server.nodes.callworkflow.HttpSupport when the class moved to an
     * accessible location!
     *
     * @param <T> type of the wrapped value
     * @param value supplier for the value
     * @return same value, but potentially captured IOException
     */
    public static <T> T returnWithRuntimeException(final RequestSupplier<T> value) {
        try {
            return value.getWithException();
        } catch (ResourceAccessException e) {
            throw RuntimeResourceAccessException.from(e);
        }
    }

    /**
     * Wraps an {@link IOException} (or probably mostly a {@link ResourceAccessException}) in a RuntimeException. Same
     * as {@link HttpExceptionUtils#returnWithRuntimeException(RequestSupplier)} but without return value.
     *
     * This method should be moved to the com.knime.server.nodes.callworkflow.HttpSupport when the class moved to an
     * accessible location!
     *
     * @param callee RequestThunk for side-effects
     */
    public static void callWithRuntimeException(final RequestThunk callee) {
        try {
            callee.call();
        } catch (ResourceAccessException e) {
            throw RuntimeResourceAccessException.from(e);
        }
    }

    /**
     * Equals the {@link Supplier} interface, except for the possibility of a ResourceAccessException being thrown at
     * the get method.
     *
     * @param <T>
     *
     * @author Leon Wenzler
     */
    @FunctionalInterface
    public interface RequestSupplier<T> {
        /**
         * Returns the supplied value, but a the method can throw an ResourceAccessException.
         *
         * @return supplied value
         * @throws ResourceAccessException
         */
        T getWithException() throws ResourceAccessException;
    }

    /**
     * RequestThunk is an interface for calls with side-effects. It can throw a ResourceAccessException.
     *
     * @author Leon Wenzler
     */
    @FunctionalInterface
    public interface RequestThunk {
        /**
         * Calls some method with side-effects but without a return value.
         *
         * @throws ResourceAccessException
         */
        void call() throws ResourceAccessException;
    }

    /**
     * Hides the constructor.
     */
    private HttpExceptionUtils() {
    }
}
