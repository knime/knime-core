/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.workbench.editor2.actions;

import static org.knime.core.ui.wrapper.Wrapper.unwrap;
import static org.knime.core.ui.wrapper.Wrapper.wraps;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.AbstractNodeView.ViewableModel;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.wizard.AbstractWizardNodeView;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.action.InteractiveWebViewsResult.SingleInteractiveWebViewResult;
import org.knime.core.ui.node.workflow.InteractiveWebViewsResultUI.SingleInteractiveWebViewResultUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.SubNodeContainerUI;
import org.knime.core.ui.wrapper.NodeContainerWrapper;
import org.knime.core.ui.wrapper.SingleInteractiveWebViewResultWrapper;
import org.knime.js.core.JSCorePlugin;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WizardNodeView;

/**
 * Action to open an interactive web view of a node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 3.3
 */
public final class OpenInteractiveWebViewAction extends Action {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(OpenInteractiveWebViewAction.class);

    private static final String CHROMIUM_BROWSER = "org.knime.ext.seleniumdrivers.multios.ChromiumWizardNodeView";
    private static final String CHROME_BROWSER = "org.knime.ext.seleniumdrivers.multios.ChromeWizardNodeView";

    private final SingleInteractiveWebViewResultUI m_webViewForNode;
    private final NodeContainerUI m_nodeContainer;
    private final boolean m_singleTitle;


    /**
     * New action to open an interactive node view.
     *
     * @param nodeContainer The NC for the view, might not the NodeContainer for the model contained in the view arg
     * @param webViewForNode The view for the node (note, for {@link org.knime.core.node.workflow.SubNodeContainer}
     *        this is the content of a contained node.
     */
    public OpenInteractiveWebViewAction(final NodeContainerUI nodeContainer,
        final SingleInteractiveWebViewResultUI webViewForNode) {
        m_nodeContainer = CheckUtils.checkArgumentNotNull(nodeContainer);
        m_webViewForNode = CheckUtils.checkArgumentNotNull(webViewForNode);
        m_singleTitle = nodeContainer instanceof SubNodeContainerUI;
    }

    /**
     * New action to open an interactive node view.
     *
     * @param nodeContainer The NC for the view, might not the NodeContainer for the model contained in the view arg
     * @param webViewForNode The view for the node (note, for {@link org.knime.core.node.workflow.SubNodeContainer} this
     *            is the content of a contained node.
     *
     * @deprecated use
     *             {@link OpenInteractiveWebViewAction#OpenInteractiveWebViewAction(NodeContainerUI, SingleInteractiveWebViewResultUI)}
     *             instead.
     */
    @Deprecated
    public OpenInteractiveWebViewAction(final NodeContainer nodeContainer,
        final SingleInteractiveWebViewResult webViewForNode) {
        this(NodeContainerWrapper.wrap(nodeContainer), SingleInteractiveWebViewResultWrapper.wrap(webViewForNode));
    }

    @Override
    public boolean isEnabled() {
        return m_nodeContainer.getNodeContainerState().isExecuted();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/openInteractiveView.png");
    }

    @Override
    public String getToolTipText() {
        return "Opens interactive node view: " + m_webViewForNode.getViewName();
    }

    @Override
    public String getText() {
        String text = m_webViewForNode.getViewName();
        if (!m_singleTitle) {
            return "Interactive View: " + text;
        } else {
            return text;
        }
    }

    @Override
    public void run() {
        LOGGER.debug("Open Interactive Web Node View " + m_nodeContainer.getName());
        if(wraps(m_nodeContainer, NodeContainer.class)) {
            //in case we are in the 'old' world and UI-classes are not used
            //required objects need to be unwrapped
            NativeNodeContainer nativeNC =
                unwrap(m_webViewForNode, SingleInteractiveWebViewResult.class).getNativeNodeContainer();
            try {
                @SuppressWarnings("rawtypes")
                AbstractWizardNodeView view = null;
                NodeContext.pushContext(nativeNC);
                try {
                    NodeModel nodeModel = nativeNC.getNodeModel();
                    view = getConfiguredWizardNodeView(nodeModel);
                } finally {
                    NodeContext.removeLastContext();
                }
                view.setWorkflowManagerAndNodeID(nativeNC.getParent(), nativeNC.getID());
                final String title = m_webViewForNode.getViewName();
                Node.invokeOpenView(view, title, OpenViewAction.getAppBoundsAsAWTRec());
            } catch (Throwable t) {
                final MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_ERROR | SWT.OK);
                mb.setText("Interactive View cannot be opened");
                mb.setMessage("The interactive view cannot be opened for the following reason:\n" + t.getMessage());
                mb.open();
                LOGGER.error("The interactive view for node '" + nativeNC.getNameWithID() + "' has thrown a '"
                    + t.getClass().getSimpleName() + "'. That is most likely an implementation error.", t);
            }
        } else {
           //create view by using the UI-objects directly
           //TODO don't block the UI
           @SuppressWarnings("rawtypes")
           AbstractWizardNodeView view = getConfiguredWizardNodeView(m_webViewForNode.getModel());
           final String title = m_webViewForNode.getViewName();
           Node.invokeOpenView(view, title, OpenViewAction.getAppBoundsAsAWTRec());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static AbstractWizardNodeView getConfiguredWizardNodeView(final ViewableModel model) {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] configurationElements =
                registry.getConfigurationElementsFor("org.knime.core.WizardNodeView");

        Class<?> viewClass = null;
        String classString = JSCorePlugin.getDefault().getPreferenceStore().getString(JSCorePlugin.P_VIEW_BROWSER);
        if (StringUtils.isNotEmpty(classString)) {
            // try loading selected view
            viewClass = getViewClassByReflection(classString, configurationElements);
            if (viewClass == null) {
                LOGGER.error("JS view set in preferences (" + classString
                    + ") can't be loaded. Switching to default.");
            }
        }
        if (viewClass == null) {
            // try loading defaults
            viewClass = getViewClassByReflection(CHROMIUM_BROWSER, configurationElements);
            if (viewClass == null) {
                viewClass = getViewClassByReflection(CHROME_BROWSER, configurationElements);
                try {
                    Method isChromePresentM = viewClass.getMethod("isChromePresent");
                    boolean isChromePresent = (boolean)isChromePresentM.invoke(null);
                    if (!isChromePresent) {
                        // no Chrome found on system, defaulting to SWT browser
                        viewClass = null;
                    }
                } catch (Exception e) { /* do nothing */ }
            }
        }
        if (viewClass != null) {
            try {
                Method isEnabledM = viewClass.getMethod("isEnabled");
                boolean isEnabled = (boolean)isEnabledM.invoke(null);
                if (!isEnabled) {
                    LOGGER.error("JS view (" + classString
                        + ") is not available. Falling back to internal SWT browser.");
                    viewClass = null;
                }
            } catch (Exception e) { /*do nothing */ }
        }
        if (viewClass != null) {
            try {
                Constructor<?> constructor = viewClass.getConstructor(ViewableModel.class);
                return (AbstractWizardNodeView)constructor.newInstance(model);
            } catch (Exception e) {
                LOGGER.error("JS view can not be initialized. Falling back to internal SWT browser.");
            }
        }
        return new WizardNodeView(model);
    }

    private static Class<?> getViewClassByReflection(final String className, final IConfigurationElement[] confElements) {
        Class<?> viewClass = null;
        try {
            for (IConfigurationElement element : confElements) {
                if (className.equals(element.getAttribute("viewClass"))) {
                    viewClass = Platform.getBundle(element.getDeclaringExtension().getContributor().getName())
                        .loadClass(element.getAttribute("viewClass"));
                }
            }
        } catch (Exception e) { /* do nothing */}
        if (viewClass != null) {
            try {
                Method isEnabledM = viewClass.getMethod("isEnabled");
                boolean isEnabled = (boolean)isEnabledM.invoke(null);
                return isEnabled ? viewClass : null;
            } catch (Exception e) {
                return viewClass;
            }
        }
        return null;
    }

    @Override
    public String getId() {
        return "knime.open.interactive.web.view.action";
    }
}
