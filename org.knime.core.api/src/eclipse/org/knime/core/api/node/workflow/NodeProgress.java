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
 * ------------------------------------------------------------------------
 */
package org.knime.core.api.node.workflow;

import java.text.NumberFormat;

/**
 * Contained in a {@link NodeProgressEvent} which is fired when the progress
 * information has changed, either the progress (value between 0 and 1 or
 * <code>null</code>), or the progress message (could also be
 * <code>null</code>).
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class NodeProgress {

    /** Keeps the progress value. */
    private final Double m_progress;

    /** Keeps the progress information. */
    private final String m_message;

    /**
     * Create a progress event based on progress value and message. Both
     * arguments can be <code>null</code>.
     *
     * @param progress The progress value or <code>null</code>.
     * @param message The progress message or <code>null</code>.
     */
    public NodeProgress(final Double progress, final String message) {
        m_progress = progress;
        m_message = message;
    }

    /**
     * If the progress value has changed (more correctly is not
     * <code>null</code>).
     *
     * @return <code>true</code> if the progress value is not
     *         <code>null</code>.
     */
    public boolean hasProgress() {
        return m_progress != null;
    }

    /**
     * Current progress value or null.
     *
     * @return current progress value between 0 and 1, or <code>null</code>.
     */
    public Double getProgress() {
        return m_progress;
    }

    /**
     * Current progress message or <code>null</code>.
     *
     * @return current progress message or <code>null</code>.
     */
    public String getMessage() {
        return m_message;
    }

    /**
     * If the progress message has changed (more correctly is not
     * <code>null</code>).
     *
     * @return <code>true</code> if the progress message is not
     *         <code>null</code>.
     */
    public boolean hasMessage() {
        return m_message != null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Progress " + (m_progress == null ? "<none>"
                : NumberFormat.getPercentInstance().format(m_progress))
                + "; message: " + (m_message == null ? "<none>" : m_message);
    }

}
