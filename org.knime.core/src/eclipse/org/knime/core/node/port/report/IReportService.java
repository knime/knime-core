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
 *   May 14, 2023 (wiswedel): created
 */
package org.knime.core.node.port.report;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.SubNodeContainer;

/**
 * Describes OSGi service enabling reporting outputs via Components/Subnodes.
 *
 * @author Bernd Wiswedel, KNIME
 * @noreference This interface is not intended to be referenced by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 5.1
 */
public interface IReportService {

    /**
     * Called after execution of a {@link SubNodeContainer} to fill the report output.
     * This method always suppresses exceptions and should return {@code null} if the
     * report generation failed.
     * <p>
     * In contrast, {@link #createOutputWithExceptions(SubNodeContainer, ExecutionMonitor)}
     * will consider the {@link ReportConfiguration#isFailOnReportError()} flag.
     * </p>
     *
     * @param subnode The component that is about to execute.
     * @param exec Progress/cancelation.
     * @return The port object to be output by the component's report output.
     */
    IReportPortObject createOutput(final SubNodeContainer subnode, final ExecutionMonitor exec);

    /**
     * Called after execution of a {@link SubNodeContainer} to fill the report output.
     *
     * @param subnode The component that is about to execute.
     * @param exec Progress/cancelation.
     * @return The port object to be output by the component's report output.
     * @throws CanceledExecutionException if the node execution was cancelled
     * @throws TimeoutException if the reporting page(s) are not initialized or generated in time
     * @throws ExecutionException if something goes wrong during the task of report generation
     * @throws InterruptedException if the thread is interrupted while report generation is in progress
     * @since 5.10
     */
    default IReportPortObject createOutputWithExceptions(final SubNodeContainer subnode, final ExecutionMonitor exec)
        throws CanceledExecutionException, TimeoutException, ExecutionException, InterruptedException {
        return createOutput(subnode, exec);
    }

    /**
     * Determines the spec of the report output.
     *
     * @param subnode The component.
     * @return the spec (currently a singleton).
     * @since 5.2
     */
    Optional<IReportPortObjectSpec> createOutputSpec(final SubNodeContainer subnode);

}
