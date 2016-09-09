/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 */
package org.knime.core.api.node.workflow;

import java.util.function.Supplier;


/**
 * Contained in a {@link ConnectionProgressEvent} which is fired when the
 * progress information has changed.
 */
public final class ConnectionProgress {

    private final boolean m_inProgress;

    private final Supplier<String> m_messageSupplier;

    /**
     * Create a progress event based on progress value and message.
     *
     * @param inProgress true if currently in-progress.
     * @param message the message to display (or <code>null</code> to display
     *            nothing)
     */
    public ConnectionProgress(final boolean inProgress, final String message) {
        this(inProgress, () -> message);
    }

    /**
     * Create a progress event based on progress value and message supplier. The supplier is only evaluated if
     * the event is processed by the UI (avoids storm of string creation, possibly synced on number formats).
     *
     * @param inProgress true if currently in-progress.
     * @param messageSupplier the message supplier to display (or <code>null</code> to display nothing)
     * @since 3.0
     */
    public ConnectionProgress(final boolean inProgress, final Supplier<String> messageSupplier) {
        m_inProgress = inProgress;
        m_messageSupplier = messageSupplier;
    }

    /**
     * @return whether we are currently in-progress. Within the UI, this
     * determines whether we show the lines as dashed or solid. (If dashed, the
     * lines are also animated, stepping with each event).
     */
    public boolean inProgress() {
        return m_inProgress;
    }

    /**
     * Returns the current progress message or <code>null</code> to display
     * nothing.
     *
     * @return current progress message or <code>null</code> to display nothing.
     */
    public String getMessage() {
        return m_messageSupplier.get();
    }

    /**
     * Returns whether there is currently a message to display.
     *
     * @return whether there is currently a message to display.
     */
    public boolean hasMessage() {
        return m_messageSupplier != null && m_messageSupplier.get() != null;
    }

}
