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
 * ---------------------------------------------------------------------
 *
 * History
 *   05.12.2011 (meinl): created
 */
package org.knime.workbench.ui.wrapper;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.repository.util.DynamicNodeDescriptionCreator;

/**
 * This class is the additional help window that users can open inside a node's
 * dialog.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class HelpWindow extends Window implements LocationListener {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(HelpWindow.class);

    private static final Point SIZE = new Point(400, 500);

    /** Singleton instance of the help window. */
    public static final HelpWindow instance = new HelpWindow();

    private Browser m_browser;

    private HelpWindow() {
        super(new Shell(Display.getCurrent(), SWT.SHELL_TRIM));
        setBlockOnOpen(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createContents(final Composite parent) {
        GridData gd = new GridData(GridData.FILL_BOTH);
        m_browser = new Browser(parent, SWT.EMBEDDED | SWT.FILL);
        m_browser.setLayoutData(gd);
        m_browser.addLocationListener(this);
        m_browser.setText("");
        return m_browser;
    }

    /**
     * Show the description of the given node in this window. The window is
     * <b>not</b> opened automatically, thus you may need to call
     * {@link HelpWindow#open()} before.
     *
     * @param nc the node for which the description should be shown
     */
    public void showDescriptionForNode(final NodeContainer nc) {
        final StringBuilder content = new StringBuilder();
        DynamicNodeDescriptionCreator.instance().addDescription(nc, false,
                content);

        m_browser.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                HelpWindow.this.getShell().setText(
                        "Node description for " + nc.getDisplayLabel());
                m_browser.setText(content.toString());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Point getInitialSize() {
        return SIZE;
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
        // make sure links are opened in an external browser
        if (!event.location.startsWith("about:")) {
            IWorkbenchBrowserSupport browserSupport =
                    PlatformUI.getWorkbench().getBrowserSupport();
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
