/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * History
 *   19.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.util.ArrayList;
import java.util.List;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Testcase that open all views of all nodes in the workflow. Exception during opening a view are reported as failures.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class WorkflowOpenViewsTest extends WorkflowTest {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowOpenViewsTest.class);

    WorkflowOpenViewsTest(final String workflowName, final IProgressMonitor monitor, final WorkflowTestContext context) {
        super(workflowName, monitor, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        result.startTest(this);

        try {
            openViews(result, m_context.getWorkflowManager());
        } catch (Throwable t) {
            result.addError(this, t);
        } finally {
            result.endTest(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "open views (assertions " + (KNIMEConstants.ASSERTIONS_ENABLED ? "on" : "off") + ")";
    }

    private void openViews(final TestResult result, final WorkflowManager wfm) {
        for (NodeContainer node : wfm.getNodeContainers()) {
            if (node instanceof SingleNodeContainer) {
                for (int i = 0; i < node.getNrViews(); i++) {
                    try {
                        openView((SingleNodeContainer)node, i);
                    } catch (Exception ex) {
                        String msg =
                                "View " + i + " of node '" + node.getNameWithID() + "' has thrown a "
                                        + ex.getClass().getSimpleName() + " during open: " + ex.getMessage();
                        AssertionFailedError error = new AssertionFailedError(msg);
                        error.initCause(ex);
                        result.addFailure(this, error);
                    }
                }
                // test InteractiveNodeViews
                if (node.hasInteractiveView()) {
                    try {
                        openInteractiveView((SingleNodeContainer)node);
                    } catch (Exception ex) {
                        String msg =
                                "Interactive view of node '" + node.getNameWithID() + "' has thrown a "
                                        + ex.getClass().getSimpleName() + " during open: " + ex.getMessage();
                        AssertionFailedError error = new AssertionFailedError(msg);
                        error.initCause(ex);
                        result.addFailure(this, error);
                    }
                }
            } else if (node instanceof WorkflowManager) {
                openViews(result, (WorkflowManager)node);
            } else {
                throw new IllegalStateException("Unknown node container type: " + node.getClass());
            }
        }
    }

    private void openView(final SingleNodeContainer node, final int index) {
        // test NodeViews
        LOGGER.debug("opening view nr. " + index + " for node " + node.getName());
        final AbstractNodeView<? extends NodeModel> view = node.getView(index);
        // store the view in order to close is after the test finishes
        List<AbstractNodeView<? extends NodeModel>> l = m_context.getNodeViews().get(node);
        if (l == null) {
            l = new ArrayList<AbstractNodeView<? extends NodeModel>>(2);
            m_context.getNodeViews().put(node, l);
        }
        l.add(view);
        // open it now.
        ViewUtils.invokeAndWaitInEDT(new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                Node.invokeOpenView(view, "View #" + index);
            }
        });
    }

    private void openInteractiveView(final SingleNodeContainer node) {
        LOGGER.debug("opening interactive view for node " + node.getName());
        final AbstractNodeView<?> view = node.getInteractiveView();
        // open it now.
        ViewUtils.invokeAndWaitInEDT(new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                Node.invokeOpenView(view, "Interactive View");
            }
        });
    }
}
