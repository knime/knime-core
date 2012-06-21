/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * History
 *   27.06.2006 (sieb): created
 */
package org.knime.workbench.ui.navigator;

import java.net.URL;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter2;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.SingleNodeContainerPersistorVersion200;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Implements the label provider for the knime navigator. Mainly projects get
 * another image.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, KNIME.com AG
 */
public class KnimeResourceLabelProvider extends LabelProvider implements
        IColorProvider, IFontProvider {

//    private static final Image PROJECT = KNIMEUIPlugin.getDefault().getImage(
//            KNIMEUIPlugin.PLUGIN_ID, "icons/project_basic.png");

    /** Icon representing the executing state. */
    public static final Image EXECUTING = KNIMEUIPlugin.getDefault()
        .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_executing.png");
    /** Icon representing the executed state. */
    public static final Image EXECUTED = KNIMEUIPlugin.getDefault()
        .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_executed.png");
    /** Icon representing the configured state. */
    public static final Image CONFIGURED = KNIMEUIPlugin.getDefault()
        .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_configured.png");
    /** Icon representing a closed workflow. */
    public static final Image CLOSED_WORKFLOW = KNIMEUIPlugin.getDefault()
        .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_closed2.png");
    /** Error icon. */
    public static final Image ERROR = KNIMEUIPlugin.getDefault()
        .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_error.png");
    /** Icon representing a node in the resource navigator. */
    public static final Image NODE = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/node.png");
    /** Icon representing a workflow group in the resource navigator. */
    public static final Image WORKFLOW_GROUP
        = KNIMEUIPlugin.imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                "icons/wf_set.png").createImage();

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            KnimeResourceLableProvider.class);

    /** Path representation of the workflow file. */
    public static final Path WORKFLOW_FILE = new Path(
            WorkflowPersistor.WORKFLOW_FILE);

    /** Path representation of the meta info file. */
    public static final Path METAINFO_FILE = new Path(
            WorkflowPersistor.METAINFO_FILE);

    /** Path representation of the node settings file. */
    public static final Path NODE_FILE = new Path(
            SingleNodeContainerPersistorVersion200.SETTINGS_FILE_NAME);

    /**
     * Returns a workbench label provider that is hooked up to the decorator
     * mechanism.
     *
     * @return a new <code>DecoratingLabelProvider</code> which wraps a new
     *         <code>WorkbenchLabelProvider</code>
     */
    public static ILabelProvider getDecoratingWorkbenchLabelProvider() {
        return new DecoratingLabelProvider(new WorkbenchLabelProvider(),
                PlatformUI.getWorkbench().getDecoratorManager()
                        .getLabelDecorator());
    }

    /**
     * Listener that tracks changes to the editor registry and does a full
     * update when it changes, since many workbench adapters derive their icon
     * from the file associations in the registry.
     */
    private final IPropertyListener m_editorRegistryListener =
            new IPropertyListener() {
                @Override
                public void propertyChanged(final Object source,
                        final int propId) {
                    if (propId == IEditorRegistry.PROP_CONTENTS) {
                        fireLabelProviderChanged(new LabelProviderChangedEvent(
                                KnimeResourceLabelProvider.this));
                    }
                }
            };

    /**
     * Creates a new workbench label provider.
     */
    public KnimeResourceLabelProvider() {
        PlatformUI.getWorkbench().getEditorRegistry().addPropertyListener(
                m_editorRegistryListener);
    }


    /**
     * Returns an image descriptor that is based on the given descriptor, but
     * decorated with additional information relating to the state of the
     * provided object.
     *
     * Subclasses may reimplement this method to decorate an object's image.
     *
     * @param input The base image to decorate.
     * @param element The element used to look up decorations.
     * @return the resuling ImageDescriptor.
     * @see org.eclipse.jface.resource.CompositeImageDescriptor
     */
    protected ImageDescriptor decorateImage(final ImageDescriptor input,
            final Object element) {
        if (element instanceof IContainer) {
            NodeContainer cont = ProjectWorkflowMap.getWorkflow(
                    ((IContainer)element).getLocationURI());
            if (cont != null) {
                URL iconURL = cont.findJobManager().getIcon();
                if (iconURL != null) {
                    ImageDescriptor descr = ImageDescriptor.createFromURL(
                            iconURL);
                    return new DecorationOverlayIcon(input.createImage(),
                            descr, IDecoration.TOP_RIGHT);
                }
            }
        }
        return input;
    }


    /**
     * Returns a label that is based on the given label, but decorated with
     * additional information relating to the state of the provided object.
     *
     * Subclasses may implement this method to decorate an object's label.
     *
     * @param input The base text to decorate.
     * @param element The element used to look up decorations.
     * @return the resulting text
     */
    protected String decorateText(final String input, final Object element) {
        return input;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        PlatformUI.getWorkbench().getEditorRegistry().removePropertyListener(
                m_editorRegistryListener);
        // had to remove the disposal of the images in order to make deriving
        // label provider work otherwise these images were already disposed
        super.dispose();
    }

    /**
     * Returns the implementation of IWorkbenchAdapter for the given object.
     *
     * @param o the object to look up.
     * @return IWorkbenchAdapter or<code>null</code> if the adapter is not
     *         defined or the object is not adaptable.
     */
    protected final IWorkbenchAdapter getAdapter(final Object o) {
        if (!(o instanceof IAdaptable)) {
            return null;
        }
        return (IWorkbenchAdapter)((IAdaptable)o)
                .getAdapter(IWorkbenchAdapter.class);
    }

    /**
     * Returns the implementation of IWorkbenchAdapter2 for the given object.
     *
     * @param o the object to look up.
     * @return IWorkbenchAdapter2 or<code>null</code> if the adapter is not
     *         defined or the object is not adaptable.
     */
    protected final IWorkbenchAdapter2 getAdapter2(final Object o) {
        if (!(o instanceof IAdaptable)) {
            return null;
        }
        return (IWorkbenchAdapter2)((IAdaptable)o)
                .getAdapter(IWorkbenchAdapter2.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage(final Object element) {
        Image img = super.getImage(element);
        NodeContainer projectNode = null;
        if (element instanceof IContainer) {
            IContainer container = (IContainer)element;
            if (container.exists(WORKFLOW_FILE)) {
                // in any case a knime workflow or meta node (!)
                projectNode = ProjectWorkflowMap.getWorkflow(
                        container.getLocationURI());
                if (projectNode == null && !isMetaNode(container)) {
                    return CLOSED_WORKFLOW;
                }
            }
            if (projectNode == null) {
                if (isMetaNode(container)) {
                    return NODE;
                } else if (container.exists(METAINFO_FILE)) {
                    return WORKFLOW_GROUP;
                }
            }
        } else if (element instanceof NodeContainer) {
                projectNode = (NodeContainer)element;
        }
        if (projectNode != null) {
            if (projectNode instanceof WorkflowManager
                    // display state only for projects
                    // with this check only projects (
                    // direct children of the ROOT
                    // are displayed with state
                    && ((WorkflowManager)projectNode).getID()
                        .hasSamePrefix(WorkflowManager.ROOT.getID())) {
                if (projectNode.getNodeMessage().getMessageType().equals(
                        NodeMessage.Type.ERROR)) {
                    return ERROR;
                }
                switch (projectNode.getState()) {
                case EXECUTED:
                    return EXECUTED;
                case EXECUTING:
                case EXECUTINGREMOTELY:
                    return EXECUTING;
                case CONFIGURED:
                case IDLE:
                    return CONFIGURED;
                default:
                }
            } else {
                return NODE;
            }

        }
        return img;
    }

    private boolean isMetaNode(final IContainer container) {
        return (!(container instanceof IProject))
            && container.getParent().exists(WORKFLOW_FILE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText(final Object element) {

        if (element instanceof NodeContainer) {
            String output =  ((NodeContainer)element).getName()
                + " (#" + ((NodeContainer)element).getID().getIndex() + ")";
            // meta nodes are as object (workflow open) represented with ":"
            // then it can not be found
            return output.replace(":", "_");
        }
        // query the element for its label
        IWorkbenchAdapter adapter = getAdapter(element);
        if (adapter == null) {
            return ""; //$NON-NLS-1$
        }
        String label = adapter.getLabel(element);

        // return the decorated label
        return decorateText(label, element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getForeground(final Object element) {
        return getColor(element, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getBackground(final Object element) {
        return getColor(element, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Font getFont(final Object element) {
        IWorkbenchAdapter2 adapter = getAdapter2(element);
        if (adapter == null) {
            return null;
        }

        FontData descriptor = adapter.getFont(element);
        if (descriptor == null) {
            return null;
        }

        Font font = JFaceResources.getFontRegistry().get(descriptor.getName());
        if (font == null) {
            font = new Font(Display.getCurrent(), descriptor);
            JFaceResources.getFontRegistry().put(descriptor.getName(),
                    font.getFontData());
        }
        return font;
    }

    private Color getColor(final Object element, final boolean forground) {
        IWorkbenchAdapter2 adapter = getAdapter2(element);
        if (adapter == null) {
            return null;
        }
        RGB descriptor =
                forground ? adapter.getForeground(element) : adapter
                        .getBackground(element);
        if (descriptor == null) {
            return null;
        }

        Color color = JFaceResources.getColorRegistry().get(
                descriptor.toString());
        if (color == null) {
            color = new Color(Display.getCurrent(), descriptor);
            JFaceResources.getColorRegistry().put(
                    descriptor.toString(), color.getRGB());
        }
        return color;
    }
}
