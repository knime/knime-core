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

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Opens and closes the dialog after the workflow has been executed. Exception that occurred are reported as failures.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class WorkflowDialogsTest extends WorkflowTest {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowDialogsTest.class);

    WorkflowDialogsTest(final String workflowName, final IProgressMonitor monitor, final WorkflowTestContext context) {
        super(workflowName, monitor, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        result.startTest(this);

        try {
            checkDialogs(result, m_context.getWorkflowManager());
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
        return "check dialogs";
    }

    private void checkDialogs(final TestResult result, final WorkflowManager wfm) throws InterruptedException,
            InvocationTargetException {
        for (final NodeContainer node : wfm.getNodeContainers()) {
            if (m_context.isPreExecutedNode(node)) {
                continue;
            }

            if (node instanceof SingleNodeContainer) {
                if (((SingleNodeContainer)node).isInactive()) {
                    LOGGER.debug("Skipping dialog of node " + ((SingleNodeContainer)node).getName()
                            + " because the node is inactive");
                } else {
                    LOGGER.debug("Opening dialog of node " + ((SingleNodeContainer)node).getName());
                    final AtomicReference<Exception> exRef = new AtomicReference<Exception>();
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            final JFrame testFrame = new JFrame("Dialog for " + node.getName());
                            try {
                                NodeDialogPane dlg = node.getDialogPaneWithSettings();
                                testFrame.getContentPane().add(dlg.getPanel());
                                testFrame.pack();
                                testFrame.setVisible(true);
                                NodeSettings settings = new NodeSettings("bla");
                                dlg.finishEditingAndSaveSettingsTo(settings);
                                dlg.callOnClose();
                            } catch (Exception ex) {
                                exRef.set(ex);
                            } finally {
                                testFrame.dispose();
                            }
                        }
                    });
                    if (exRef.get() != null) {
                        String msg =
                                "Dialog of node '" + node.getNameWithID() + "' has thrown an "
                                        + exRef.get().getClass().getSimpleName() + ": " + exRef.get().getMessage();
                        AssertionFailedError error = new AssertionFailedError(msg);
                        error.initCause(exRef.get());
                        result.addFailure(this, error);
                    }
                }
            } else if (node instanceof WorkflowManager) {
                checkDialogs(result, (WorkflowManager)node);
            } else {
                throw new IllegalStateException("Unknown node container type: " + node.getClass());
            }
        }
    }
}
