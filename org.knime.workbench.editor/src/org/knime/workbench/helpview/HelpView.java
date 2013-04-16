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
 * History
 *   30.09.2005 (ohl): created
 */
package org.knime.workbench.helpview;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.part.ViewPart;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.util.DynamicNodeDescriptionCreator;

/**
 * View displaying the description of the selected nodes. The description is
 * provided by the node's factory.
 *
 * @author ohl, University of Konstanz
 */

public class HelpView extends ViewPart implements ISelectionListener,
    LocationListener {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            HelpView.class);


    private Browser m_browser;

    private FallbackBrowser m_text;

    private boolean m_isFallback;

    private IStructuredSelection m_lastSelection;

    /**
     * the constructor.
     */
    public HelpView() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus() {
        if (m_browser != null) {
            m_browser.setFocus();
        } else {
            m_text.setFocus();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        getViewSite().getPage().addSelectionListener(this);
        try {
            m_text = null;
            m_browser = new Browser(parent, SWT.NONE);
            m_browser.addLocationListener(this);
            // add us as listeners of the page selection
            m_browser.setText("");
            m_isFallback = false;
        } catch (SWTError e) {
            NodeLogger.getLogger(getClass()).warn(
                    "No html browser for node description available.", e);
            m_browser = null;
            m_text = new FallbackBrowser(parent,
                    SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
            m_isFallback = true;
        }
    }

    /**
     * The method updating the content of the browser. Depending on the type of
     * the selected part(s) it will retrieve the node(s) description and set it
     * in the browser.
     *
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IWorkbenchPart part,
            final ISelection selection) {

//        assert m_browser != null; // we only register if we have a browser

        if (m_browser != null && m_browser.isDisposed()) {
            // if someone closed it, unregister as selection listener
            // TODO same for text
            getViewSite().getPage().removeSelectionListener(this);
            return;
        }

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSel = (IStructuredSelection)selection;
            // we do not clear our content if nothing is selected.
            if (structSel.size() < 1 || structSel.equals(m_lastSelection)) {
                return;
            }
            m_lastSelection = structSel;

            // we display the full desciption only if a single node is selected
            boolean useSingleLine;
            if ((structSel.size() > 1)
                    || (structSel.getFirstElement() instanceof Category)) {
                useSingleLine = true;
            } else {
                useSingleLine = false;
            }

            // construct the html page to display
            final StringBuilder content = new StringBuilder();
            if (useSingleLine) {
                // add the prefix to make it a html page
                content.append("<html><head>");
                content.append("<meta http-equiv=\"content-type\" " +
                        "content=\"text/html; charset=UTF-8\"></meta>");
                // include stylesheet
                content.append("<style>");
                content.append(DynamicNodeDescriptionCreator.instance().getCss());
                content.append("</style>");
                content.append("</head><body><dl>");
            }
            // Keep a list of already displayed objects (this works as long as
            // the selected items come in an ordered way. Ordered with item
            // containing other selected items coming before the items
            // contained. For the tree view in the repository this is the case.
            HashSet<String> ids = new HashSet<String>();

            for (Iterator<?> selIt = structSel.iterator(); selIt.hasNext();) {
                Object sel = selIt.next();
                if (sel instanceof Category) {
                    // its a category in the node repository, display a list of
                    // contained nodes
                    Category cat = (Category)sel;
                    if (!ids.contains(cat.getID())) {
                        ids.add(cat.getID());
                        DynamicNodeDescriptionCreator.instance()
                            .addDescription(cat, content, ids);
                    }
                } else if (sel instanceof NodeTemplate) {
                    // its a node selected in the repository
                    NodeTemplate templ = (NodeTemplate)sel;
                    if (!ids.contains(templ.getID())) {
                        ids.add(templ.getID());
                        DynamicNodeDescriptionCreator.instance()
                            .addDescription(templ, useSingleLine,
                                    content);

                    }
                } else if (sel instanceof NodeContainerEditPart) {
                    // if multiple nodes in the editor are selected we should
                    // not show description for the same node (if used multiple
                    // times) twice. We store the node name in the set.
                    NodeContainer nc = ((NodeContainerEditPart)sel)
                            .getNodeContainer();
                    if (!ids.contains(nc.getName())) {
                        ids.add(nc.getName());
                        DynamicNodeDescriptionCreator.instance()
                        .addDescription(nc, useSingleLine, content);
                    }
                } else if (sel instanceof MetaNodeTemplate) {
                    // TODO: add support for MetaNodeTemplates and get the
                    // description out of them
                    NodeContainer manager
                        = ((MetaNodeTemplate)sel).getManager();
                    DynamicNodeDescriptionCreator.instance()
                        .addDescription(manager,
                                useSingleLine, content);
                }
            }
            if (useSingleLine) {
                // finish the html
                content.append("</dl></body></html>");
            }

            if (m_browser != null) {
                // FG: must always be invoked in SWT UI thread
                m_browser.getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        m_browser.setText(content.toString());
                    }
                });
            } else if (m_isFallback) {
                m_text.getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        m_text.setText(content.toString());
                    }
                });
            }
        }
        // Object first = ((IStructuredSelection)selection).getFirstElement();
        // if (first instanceof Word) {
        // label.setText(((Word)first).toString());
        // }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changed(final LocationEvent event) {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changing(final LocationEvent event) {
        if (!event.location.startsWith("about:")) {
            IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench()
                .getBrowserSupport();
            try {
                IWebBrowser browser = browserSupport.getExternalBrowser();
                browser.openURL(new URL(event.location));
                event.doit = false;
            } catch (PartInitException ex) {
                LOGGER.error(ex.getMessage(), ex);
            } catch (MalformedURLException ex) {
                LOGGER.warn("Cannot open URL '" + event.location + "'", ex);
                // just ignore it and let the default handle this case
            }
        }
    }
}
