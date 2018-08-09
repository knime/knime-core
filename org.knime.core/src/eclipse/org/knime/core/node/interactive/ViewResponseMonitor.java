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
 *   7 Apr 2018 (albrecht): created
 */
package org.knime.core.node.interactive;

import java.util.Optional;

/**
 * Interface for objects holding information about the execution status of a view request and
 * after successful execution the generated response object, or a possible error message otherwise.
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @param <RES> the actual class of the response implementation to be generated
 * @since 3.7
 * @noreference This interface is not intended to be referenced by clients.
 * @noinstantiate This interface is not intended to be instantiated by clients.
 */
public interface ViewResponseMonitor<RES extends ViewResponse> {

    /**
     * Returns a unique id for this monitor object, not null.
     * @return the unique id
     */
    public String getId();

    /**
     * @return the requestSequence
     */
    public int getRequestSequence();

    /**
     * The current progress value or if progress available.
     * @return Optional progress value between 0 and 1.
     */
    public Optional<Double> getProgress();

    /**
     * The current progress message if message available.
     * @return Optional progress message
     */
    public Optional<String> getProgressMessage();

    /**
     * Checks if the execution of the request was cancelled.
     * @return true if the execution is cancelled, false otherwise
     */
    public boolean isCancelled();

    /**
     * Returns whether or not an execution for a view request was started
     * @return true if the execution started, false otherwise
     */
    public boolean isExecutionStarted();

    /**
     * Returns whether or not the execution for a view request finished
     * @return true if the execution finished, false otherwise
     */
    public boolean isExecutionFinished();

    /**
     * Returns a response object if the response is available. The response might be empty
     * if the generation of the response is not yet complete or an error has occurred.
     * @return an optional response object
     */
    public Optional<RES> getResponse();

    /**
     * @return true if a response object is available, false otherwise
     */
    public boolean isResponseAvailable();

    /**
     * @return true if an error occurred during the generation of the response object, false otherwise
     */
    public boolean isExecutionFailed();

    /**
     * Returns an error message in case {@link #isExecutionFailed()} yields true and a message was available.
     * @return an optional error message
     */
    public Optional<String> getErrorMessage();
}
