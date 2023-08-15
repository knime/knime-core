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
 *   Jan 13, 2023 (wiswedel): created
 */
package org.knime.core.node;

import org.knime.core.internal.MessageAwareException;
import org.knime.core.node.message.Message;
import org.knime.core.node.util.CheckUtils;

/**
 * Exception thrown by node implementations during {@link NodeModel#execute(BufferedDataTable[], ExecutionContext) node
 * execution}. The method declaration allows implementations to throw <i>any</i> type of exception but implementations
 * should prefer this class in order to support rich {@link Message messages}.
 *
 * @author Bernd Wiswedel, KNIME GmbH
 * @since 5.0
 */
@SuppressWarnings("serial")
public final class KNIMEException extends Exception implements MessageAwareException {

    private final Message m_message; // NOSONAR non-transient string

    /**
     * Generic exception declaration. Clients should prefer to create an exception via one of the
     * {@link #of(Message, Throwable) of} methods.
     *
     * @param message Forwarded to super.
     * @param cause The cause
     * @see Exception#Exception(String, Throwable)
     */
    public KNIMEException(final String message, final Throwable cause) {
        this(extractMessageFrom(message, cause), message, cause);
    }

    private KNIMEException(final Message knimeMessage, final String message, final Throwable cause) {
        super(message, cause);
        m_message = knimeMessage;
    }

    /**
     * Generic exception declaration. Clients should prefer to create an exception via one of the
     * {@link #of(Message) of} methods.
     *
     * @param message Forwarded to super.
     * @see Exception#Exception(String)
     */
    public KNIMEException(final String message) {
        this(extractMessageFrom(message, null), message);
    }

    private KNIMEException(final Message knimeMessage, final String message) {
        super(message);
        m_message = knimeMessage;
    }

    @Override
    public Message getKNIMEMessage() {
        return m_message;
    }

    private static Message extractMessageFrom(final String message, final Throwable cause) {
        if (message != null) {
            return Message.fromSummary(message);
        }
        if (cause != null && cause.getMessage() != null) {
            return Message.fromSummary(cause.getMessage());
        }
        return Message.fromSummary("unknown reason");
    }

    /**
     * Create an exception from a non-null {@link Message}.
     *
     * @param message the KNIME message
     * @return A new exception.
     */
    public static KNIMEException of(final Message message) {
        CheckUtils.checkArgumentNotNull(message);
        return new KNIMEException(message, message.getSummary());
    }

    /**
     * Create an exception from a non-null {@link Message} and a cause.
     *
     * @param message the KNIME message
     * @param cause the cause
     * @return A new exception.
     */
    public static KNIMEException of(final Message message, final Throwable cause) {
        CheckUtils.checkArgumentNotNull(message);
        return new KNIMEException(message, message.getSummary(), cause);
    }

    /**
     * Wraps this exception to unchecked {@link RuntimeException} so that existing API can be kept unchanged. Method
     * should really not exist but is a compromise to changing 500+ nodes.
     * @return An unchecked exception with <code>this</code> as cause.
     */
    public KNIMERuntimeException toUnchecked() {
        return new KNIMERuntimeException();
    }

    /**
     * A runtime exception derived from a {@link KNIMEException}. It only exists to not break APIs which really
     * should have thrown a {@link KNIMEException} but these APIs were defined much before that existed.
     */
    public final class KNIMERuntimeException extends RuntimeException implements MessageAwareException {

        KNIMERuntimeException() {
            super(KNIMEException.this.getMessage(), KNIMEException.this);
        }

        @Override
        public synchronized KNIMEException getCause() {
            return (KNIMEException)super.getCause();
        }

        @Override
        public Message getKNIMEMessage() {
            return getCause().getKNIMEMessage();
        }
    }

}
