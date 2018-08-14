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
 *   14 Aug 2018 (albrecht): created
 */
package org.knime.core.node.wizard;

import java.util.concurrent.CompletableFuture;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.interactive.ViewRequestHandler;
import org.knime.core.node.interactive.ViewRequestHandlingException;
import org.knime.core.node.interactive.ViewResponseMonitor;
import org.knime.core.node.workflow.NodeContext;

/**
 * Utility class to run view requests.
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @since 3.7
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class WizardViewRequestRunner {

    /**
     * Starts running a view request on the specified {@link ViewRequestHandler}.
     *
     * @param <REQ> the actual view request implementation
     * @param <RES> the actual view response implementation
     * @param handler the {@link ViewRequestHandler} which can handle the given view request
     * @param request the {@link WizardViewRequest} to run
     * @param exec an {@link ExecutionMonitor} to set progress and check for possible cancellation
     * @return a {@link ViewResponseMonitor} which contains status information about the execution of the
     * view request
     */
    public static <REQ extends WizardViewRequest, RES extends WizardViewResponse> ViewResponseMonitor<RES>
        run(final ViewRequestHandler<REQ, RES> handler, final REQ request, final ExecutionMonitor exec) {
        DefaultViewResponseMonitor<RES> monitor = new DefaultViewResponseMonitor<RES>(request.getSequence(), exec);
        CompletableFuture<RES> future = CompletableFuture.supplyAsync(() -> {
            NodeContext.pushContext(NodeContext.getContext());
            try {
                monitor.setExecutionStarted();
                return handler.handleRequest(request, exec);
            } catch (ViewRequestHandlingException | InterruptedException | CanceledExecutionException ex) {
                monitor.setExecutionFailed(ex);
                return null;
            } finally {
                NodeContext.removeLastContext();
            }
        });
        monitor.setFuture(future);
        future.thenAccept((response) -> {
            monitor.setResponse(response);
        });
        return monitor;
    }

}
