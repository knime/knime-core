/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Nov 11, 2022 (hornm): created
 */
package org.knime.core.webui;

import org.eclipse.swt.program.Program;
import org.knime.core.node.NodeLogger;

/**
 * Utily methods relevant for the web UI.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class WebUIUtil {

    /**
     * System property that controls whether external requests from within CEF browser instances are to be blocked or
     * not.
     */
    public static final String BLOCK_ALL_EXTERNAL_REQUESTS_SYS_PROP = "chromium.block_all_external_requests";

    private WebUIUtil() {
        // utility class
    }

    /**
     * Tries to open the given url in an external browser. But only if it's a http(s) url. Will also output respective
     * debug log messages.
     *
     * @param url
     * @param classForLogging
     *
     */
    public static void openURLInExternalBrowserAndAddToDebugLog(final String url, final Class<?> classForLogging) {
        if (url.startsWith("http")) {
            // open http-urls in the system browser
            if (Program.launch(url)) {
                NodeLogger.getLogger(classForLogging).debugWithFormat("Opening URL '%s' with external browser ...",
                    url);
            } else {
                NodeLogger.getLogger(classForLogging)
                    .error("Failed to open URL in external browser. The URL is: " + url);
            }
        } else {
            // don't do anything with other url-types (e.g. file-urls)
            NodeLogger.getLogger(classForLogging).debugWithFormat("URL '%s' can't be opened (not allowed).", url);
        }
    }

}
