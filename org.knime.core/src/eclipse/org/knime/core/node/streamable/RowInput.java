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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 12, 2012 (wiswedel): created
 */
package org.knime.core.node.streamable;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowRead;

/**
 * A streamable data input.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 2.6
 */
public abstract class RowInput extends PortInput {

    /**
     * Returns a view of the row input as a {@link RowCursor}.
     * <p>
     * <b>Warnings:</b>
     * <ul>
     *   <li>The {@link InterruptibleRowCursor#canForward()} and {@link InterruptibleRowCursor#forward()} methods may
     *   throw an {@link InterruptedException} from an underlying call to {@link #poll()} even though they don't declare
     *   this checked exception.</li>
     *   <li>The returned row cursor caches one {@link DataRow} from the underlying row input to support the
     * {@link RowCursor#canForward()} operation.</li>
     * </ul>
     * @return adapter to use the row input as a row cursor
     *
     * @since 5.4
     */
    public InterruptibleRowCursor asCursor() {
        return new FallbackRowCursor();
    }

    /**
     * An adapter for {@link RowInput}s whose {@link #canForward()}, {@link #forward()}, and {@link #getNumColumns()}
     * methods may throw an undeclared {@link InterruptedException} from underlying calls
     * (e.g. to {@link RowInput#poll()}).
     *
     * @since 5.4
     *
     * @apiNote API still experimental. It might change in future releases of KNIME Analytics Platform.
     *
     * @noreference This interface is not intended to be referenced by clients.
     * @noextend This interface is not intended to be extended by clients.
     */
    public interface InterruptibleRowCursor extends RowCursor {
    }

    /**
     * Default implementation that just delegates to the {@link #poll()} method.
     */
    @SuppressWarnings("javadoc")
    private final class FallbackRowCursor implements InterruptibleRowCursor {

        private DataRow m_current;

        private final RowRead m_rowRead = RowRead.suppliedBy(() -> m_current, getNumColumns());

        private DataRow m_next;

        private boolean m_closed;

        private FallbackRowCursor() {
        }

        @Override
        public int getNumColumns() {
            return getDataTableSpec().getNumColumns();
        }

        /**
         * {@inheritDoc}
         *
         * @throws InterruptedException undeclared via {@link ExceptionUtils#rethrow(Throwable)}
         */
        @Override
        public boolean canForward() {
            if (m_closed) {
                return false;
            }
            if (m_next == null) {
                try {
                    m_next = poll();
                    if (m_next == null) {
                        m_closed = true;
                        return false;
                    }
                } catch (final InterruptedException ex) { // NOSONAR exception is rethrown
                    throw ExceptionUtils.asRuntimeException(ex);
                }

            }
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @throws InterruptedException undeclared via {@link ExceptionUtils#rethrow(Throwable)}
         */
        @Override
        public RowRead forward() {
            if (!canForward()) {
                return null;
            }
            m_current = m_next;
            m_next = null;
            return m_rowRead;
        }

        @Override
        public void close() {
            if (!m_closed) {
                RowInput.this.close();
                m_current = null;
                m_next = null;
                m_closed = true;
            }
        }
    }

    /**
     * The table spec of the input.
     *
     * @return The table spec of the input table/stream.
     */
    public abstract DataTableSpec getDataTableSpec();

    /**
     * Get the next row from the input stream. The call may block if upstream
     * nodes are still in the process of generating data. Client code could look
     * like this:
     *
     * <pre>
     * DataRow row;
     * while ((row = rowInput.poll()) != null) {
     *     // do something with row
     * }
     * rowInput.close();
     * </pre>
     *
     * @return The next row or null if the end of the stream has been reached.
     * @throws InterruptedException If canceled.
     */
    public abstract DataRow poll() throws InterruptedException;

    /** Indicates that no more input is needed. Upstream nodes may stop
     * generating data (unless there are other consumers). */
    public abstract void close();

}
