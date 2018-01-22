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
 *   25 Apr 2018 (albrecht): created
 */
package org.knime.core.node.interactive;

import java.util.Optional;
import java.util.UUID;

/**
 * A simple implementation of a {@link ViewResponseMonitor} which notifies the view of a failure in the execution
 * of a view request.
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @param <RES> the expected {@link ViewResponse} implementation corresponding to the issued {@link ViewRequest}
 * @since 3.6
 */
public class SimpleErrorViewResponse<RES extends ViewResponse> implements ViewResponseMonitor<RES> {

    private String m_id;
    private int m_requestSequence;
    private String m_errorMessage;

    /**
     * Creates a new simple response object containing only an error message with information about why a
     * view request execution failed.
     *
     * @param requestSequence the request sequence number for later identification
     * @param errorMessage an optional error message with details about the ocurred error
     */
    public SimpleErrorViewResponse(final int requestSequence, final String errorMessage) {
        m_requestSequence = requestSequence;
        m_errorMessage = errorMessage;
        m_id = UUID.randomUUID().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return m_id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRequestSequence() {
        return m_requestSequence;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Double> getProgress() {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getProgressMessage() {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExecutionStarted() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExecutionFinished() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RES> getResponse() {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResponseAvailable() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExecutionFailed() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(m_errorMessage);
    }

}
