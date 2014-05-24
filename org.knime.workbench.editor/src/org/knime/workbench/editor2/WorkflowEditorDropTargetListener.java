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

package org.knime.workbench.editor2;

import java.net.URL;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.knime.core.node.ContextAwareNodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.repository.util.ContextAwareNodeFactoryMapper;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public abstract class WorkflowEditorDropTargetListener
        <T extends CreationFactory> extends AbstractTransferDropTargetListener {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowEditorFileDropTargetListener.class);


    private final T m_factory;

    /**
     * @param viewer the edit part viewer this drop target listener is attached
     *            to
     */
    protected WorkflowEditorDropTargetListener(final EditPartViewer viewer,
            final T factory) {
        super(viewer);
        m_factory = factory;
    }

    /**
     * @param url the URL of the file
     * @return a node factory creating a node that is registered for handling
     *      this type of file
     */
    protected ContextAwareNodeFactory<NodeModel> getNodeFactory(final URL url) {
        String path = url.getPath();
        Class<? extends ContextAwareNodeFactory> clazz = ContextAwareNodeFactoryMapper.getNodeFactory(path);
        if (clazz == null) {
            LOGGER.warn("No node factory is registered for handling "
                    + " \"" + path + "\"");
            return null;
        }
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            LOGGER.error("Can't create node " + clazz.getName() + ".", e);
        } catch (IllegalAccessException e) {
            LOGGER.error(e);
        }
        return null;
    }

    /**
     * @param event the drop target event
     * @return the first dragged resource or null if the event contains a
     *      resource that is not of type {@link ContentObject}
     */
    protected ContentObject getDragResources(final DropTargetEvent event) {
        LocalSelectionTransfer transfer = (LocalSelectionTransfer)getTransfer();
        ISelection selection = transfer.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection)selection;
            Object firstElement = ss.getFirstElement();
            if (firstElement instanceof ContentObject) {
                return (ContentObject)firstElement;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected Request createTargetRequest() {
        CreateRequest request = new CreateRequest();
        request.setFactory(m_factory);
        return request;
    }

    /** {@inheritDoc} */
    @Override
    protected void updateTargetRequest() {
        ((CreateRequest)getTargetRequest()).setLocation(getDropLocation());
    }

    /**
     * @return the creation factory
     */
    protected T getFactory() {
        return m_factory;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final DropTargetEvent event) {
        return getDragResources(event) != null;
    }


}
