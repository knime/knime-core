/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.node;

import java.util.Optional;

import org.knime.core.internal.MessageAwareException;
import org.knime.core.node.message.Message;

/**
 * This exception is used in the
 * {@link org.knime.core.node.ExecutionMonitor} when a node's
 * execution has been canceled. If the
 * {@link org.knime.core.node.NodeModel} ask the
 * {@link org.knime.core.node.NodeProgressMonitor} if a cancel is
 * requested, this method will throw this exception which then leads the
 * process to terminate.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
@SuppressWarnings("serial")
public class CanceledExecutionException extends Exception implements MessageAwareException {

    /**
     * A message that is returned by {@link #getKNIMEMessage()} if not otherwise set during cancelation.
     * @noreference This field is not intended to be referenced by clients.
     * @since 5.5
     */
    public static final Message DEFAULT_CANCEL_MESSAGE = Message.fromSummary("Execution canceled");

    private final Message m_knimeMessage; // NOSONAR (serialization)

    /**
     * Creates a new exception of this type with an error message.
     */
    public CanceledExecutionException() {
        super();
        m_knimeMessage = DEFAULT_CANCEL_MESSAGE;
    }

    /**
     * Constructs an <code>CancelExecutionException</code> with the specified
     * detail message.
     *
     * Use a helpful message here as it will be displayed to the user, and it is
     * the only hint ones gets to correct the problem.
     *
     * @param s a detail message about the cancelation
     */
    public CanceledExecutionException(final String s) {
        super(s);
        m_knimeMessage = Optional.ofNullable(s).map(Message::fromSummary).orElse(DEFAULT_CANCEL_MESSAGE);
    }

    /**
     * Constructs an <code>CancelExecutionException</code> with the specified
     * detail message.
     *
     * @param message a detail message about the cancelation
     * @since 5.5
     */
    public CanceledExecutionException(final Message message) {
        super(message.getSummary());
        m_knimeMessage = message;
    }

    /**
     * A message that was possibly set during construction, might explain details of cancellation (resource shortage).
     * {@inheritDoc}
     */
    @Override
    public Message getKNIMEMessage() {
        return m_knimeMessage;
    }

    /**
     * A function that can be used to check whether execution has been canceled.
     * @author Carl Witt, KNIME AG, Zurich, Switzerland
     * @since 4.2
     */
    @FunctionalInterface
    public interface CancelChecker {
        /**
         * Check whether the execution was canceled. If so, throw an according exception.
         * @throws CanceledExecutionException
         */
        void checkCanceled() throws CanceledExecutionException;

        /**
         * Provides a default value for the polling interval parameter of
         * {@link #checkCanceledPeriodically(ExecutionContext, long)}
         *
         * @param exec see {@link #checkCanceledPeriodically(ExecutionContext, long)}
         * @return an object that periodically tests the execution context for cancelation
         */
        public static CancelChecker checkCanceledPeriodically(final ExecutionContext exec) {
            return checkCanceledPeriodically(exec, 5000);
        }

        /**
         * Create a cancel checker that skips every n-th call to {@link #checkCanceled()}.
         *
         * @param exec the context to check for cancellation
         * @param skipEveryNInvocations check cancellation only on every n-th invocation of {@link #checkCanceled()} to
         *            save some time
         * @return an object that periodically tests the execution context for cancellation
         */
        public static CancelChecker checkCanceledPeriodically(final ExecutionContext exec,
            final long skipEveryNInvocations) {
            return new CancelChecker() {
                long m_skipNext = skipEveryNInvocations;

                @Override
                public void checkCanceled() throws CanceledExecutionException {
                    m_skipNext--;
                    if (m_skipNext < 0) {
                        exec.checkCanceled();
                        m_skipNext = skipEveryNInvocations;
                    }
                }
            };
        }

        /**
         * Create a cancel checker that skips every n-th call to to {@link #checkCanceled()} and updates progress on the
         * given execution context on every non-skipped call of {@link #checkCanceled()}.
         *
         * @param exec the context to check for cancellation
         * @param executeEveryNthInvocation check cancellation only on every n-th invocation of {@link #checkCanceled()}
         *            to save some time. If you want every call to the cancel checker to be executed, pass a value of 1
         *            or less.
         * @param targetInvocations the total number of invocations of the cancel checker.
         * @return an object that periodically tests the execution context for cancellation
         */
        public static CancelChecker checkCanceledPeriodicallyWithProgress(final ExecutionContext exec,
            final long executeEveryNthInvocation, final long targetInvocations) {
            return new CancelChecker() {
                /**
                 * Ignore the next m_skipNext calls to checkCanceled()
                 */
                long m_skipNext = executeEveryNthInvocation;

                /**
                 * The number of times checkCanceled() was called on this object.
                 */
                double m_invocations = 0;

                /**
                 * The number of invocations of checkCanceled() that is considered 100% progress.
                 */
                final long m_maxInvocations = targetInvocations;

                @Override
                public void checkCanceled() throws CanceledExecutionException {
                    m_invocations++;
                    m_skipNext--;
                    if (m_skipNext < 0) {
                        if (m_maxInvocations != 0) {
                            exec.getProgressMonitor().setProgress(m_invocations / m_maxInvocations);
                        }
                        exec.checkCanceled();
                        m_skipNext = executeEveryNthInvocation;
                    }
                }
            };
        }
    }
}
