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
 *   11.08.2011 (meinl): created
 */
package org.knime.product.rcp;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Simple action that opens a browser window with the tips and tricks page.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
class TipsAndTricksAction extends Action {
    private static final String TIPS_AND_TRICKS_URL =
            "http://tech.knime.org/tips-and-tricks";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(KNIMEApplicationWorkbenchAdvisor.class.getPackage()
                    .toString() + ".TipsAndTricks");

    private static final String ID = "KNIMETipsAndTricks";

    /**
     *
     */
    public TipsAndTricksAction() {
        super("&Tips and Tricks");
        setToolTipText("Opens the Tips&Tricks window");
        setId(ID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        openTipsAndTricks();
    }

    /**
     * Opens the tips and tricks window in an editor.
     */
    static void openTipsAndTricks() {
        try {
            IWebBrowser browser =
                    PlatformUI
                            .getWorkbench()
                            .getBrowserSupport()
                            .createBrowser(IWorkbenchBrowserSupport.AS_EDITOR,
                                    "TipsAndTricks", "Tips and Tricks", null);

            URL url = new URL(TIPS_AND_TRICKS_URL);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(500);
            try {
                conn.connect();
                conn.disconnect();
            } catch (IOException ex) {
                // timeout, unknown host, ...
                LOGGER.warn("Cannot connect to knime.org", ex);

                Path p = new Path("/intro/NoInternet.html");
                Bundle bundle =
                        FrameworkUtil.getBundle(TipsAndTricksAction.class);
                URL noInternetUrl = FileLocator.findEntries(bundle, p)[0];
                url = FileLocator.toFileURL(noInternetUrl);
            }
            browser.openURL(url);
        } catch (Exception ex) {
            LOGGER.error("Cannot open Tips&Tricks view", ex);
        }
    }
}
