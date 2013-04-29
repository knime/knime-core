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
 * -------------------------------------------------------------------
 *
 */
package org.knime.workbench.editor2.actions;

import javax.swing.SwingUtilities;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.workbench.editor2.ImageRepository;

/**
 * Action to open an interactive view of a node.
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich, Switzerland
 * @since 2.8
 */
public class OpenInteractiveViewAction extends Action {
    private final NodeContainer m_nodeContainer;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(OpenInteractiveViewAction.class);

    /**
     * New action to open an interactive node view.
     *
     * @param nodeContainer The node
     */
    public OpenInteractiveViewAction(final NodeContainer nodeContainer) {
        m_nodeContainer = nodeContainer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/openInteractiveView.gif");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Opens interactive node view: " + m_nodeContainer.getInteractiveViewName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Interactive View: " + m_nodeContainer.getInteractiveViewName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOGGER.debug("Open Interactive Node View " + m_nodeContainer.getName());
        try {
            final String title = m_nodeContainer.getInteractiveViewName();
            Runnable runner = new Runnable() {
                @Override
                public void run() {
                    Node.invokeOpenView(m_nodeContainer.getInteractiveView(), title);
                }
            };
            // workaround for bug 2136: Schrodinger views must be opened
            // in non-AWT thread (unchecked call to SU.invokeAndWait)
            // This fix is to be reverted in future versions,
            // see bug 2137 for details
            boolean isSchrodinger = false;
            if (m_nodeContainer instanceof SingleNodeContainer) {
                SingleNodeContainer snc = (SingleNodeContainer)m_nodeContainer;
                String clName = snc.getNodeReferenceBug2136().
                    getFactory().getClass().getName();
                isSchrodinger = clName.startsWith(
                        "com.schrodinger.knime.node.");
            }
            if (isSchrodinger) {
                Display.getDefault().asyncExec(runner);
            } else {
                SwingUtilities.invokeLater(runner);
            }
        } catch (Throwable t) {
            MessageBox mb = new MessageBox(
                    Display.getDefault().getActiveShell(),
                    SWT.ICON_ERROR | SWT.OK);
            mb.setText("Interactive View cannot be opened");
            mb.setMessage("The interactive view cannot be opened for the "
                    + "following reason:\n" + t.getMessage());
            mb.open();
            LOGGER.error("The interactive view for node '"
                    + m_nodeContainer.getNameWithID() + "' has thrown a '"
                    + t.getClass().getSimpleName()
                    + "'. That is most likely an "
                    + "implementation error.", t);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "knime.open.interactive.view.action";
    }
}
