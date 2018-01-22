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
 *   6 Apr 2018 (albrecht): created
 */
package org.knime.core.node.interactive;

/**
 * This exception is used in {@link ViewRequestHandler} when an exception occurs
 * during the handling of a request or if a response can not be rendered.
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @since 3.6
 */
public class ViewRequestHandlingException extends Exception {

    /**
     * Constructs a new exception of this type with null as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.
     */
    public ViewRequestHandlingException() {
        super();
    }

    /**
     * Constructs a new exception of this type with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.
     * @param message the detail message. The detail message is saved for later retrieval by
     * the {@link #getMessage()} method.
     */
    public ViewRequestHandlingException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception of this type with the specified cause and a detail message of
     * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the class and detail message of
     * <tt>cause</tt>). This constructor is useful for exceptions that are little more than
     * wrappers for other throwables (for example, {@link java.security.PrivilegedActionException}).
     *
     * @param  cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     * (A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ViewRequestHandlingException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception of this type with the specified detail message and cause.
     * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param  message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     * (A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ViewRequestHandlingException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
