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
package org.knime.workbench.ui.navigator;

import java.net.URL;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.knime.core.api.node.workflow.INodeContainer;
import org.knime.core.util.JobManagerUtil;

/**
 *
 * @author Fabian Dill, KNIME.com AG
 */
public class JobManagerDecorator implements
        ILabelDecorator {

    private final CopyOnWriteArraySet<ILabelProviderListener>m_listeners =
            new CopyOnWriteArraySet<ILabelProviderListener>();



    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(final ILabelProviderListener listener) {
        m_listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLabelProperty(final Object element, final String property) {
        // TODO Auto-generated method stub
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(final ILabelProviderListener listener) {
        m_listeners.remove(listener);
    }

    @Override
    public Image decorateImage(final Image image, final Object element) {
        if (element instanceof IContainer) {
            INodeContainer cont = ProjectWorkflowMap.getWorkflow(
                    ((IContainer)element).getLocationURI());
            if (cont != null) {
                URL iconURL = JobManagerUtil.getJobManagerFactory(cont.findJobManagerUID()).getInstance().getIcon();
                if (iconURL != null) {
                    ImageDescriptor descr = ImageDescriptor.createFromURL(
                            iconURL);
                    return new DecorationOverlayIcon(image,
                            descr, IDecoration.TOP_RIGHT).createImage();
                }
            }
        }
        return image;
    }

    @Override
    public String decorateText(final String text, final Object element) {
        // TODO Auto-generated method stub
        return null;
    }



}
