/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jan 29, 2017 (ferry): created
 */
package org.knime.core.util;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author ferry
 * @since 3.3
 */
public class DesktopUtil {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(DesktopUtil.class);

    /**
     * Opens a file using the system-default program (determined by extension). Logs to <b>INFO</b> if successful,
     * otherwise to <b>WARN</b>.
     *
     * @param file to be opened
     * @return true if file was opened successfully
     * @since 3.3
     */
    public static boolean open(final File file) {
        RunnableFuture<Boolean> openFileRunnable = new RunnableFuture<Boolean>() {

            private boolean m_successfull;

            @Override
            public void run() {
                String progName = Program.findProgram(FilenameUtils.getExtension(file.toString())).getName();
                m_successfull = Program.launch(file.toString());
                if (m_successfull) {
                    LOGGER.info(file + " opened with " + progName);
                } else {
                    LOGGER.warn("Couldn't open " + file + " with " + progName);
                }
            }

            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public Boolean get() {
                return m_successfull;
            }

            @Override
            public Boolean get(final long timeout, final TimeUnit unit) {
                return m_successfull;
            }
        };
        Display.getDefault().syncExec(openFileRunnable);
        boolean result = false;
        try {
            result = openFileRunnable.get();
        } catch (InterruptedException | ExecutionException ex) {
            // Does not happen because the runnable was executed via syncExec.
        }
        return result;
    }

    /**
     * Opens a URL using the system default program for .html/.htm files. If this fails the external browser that is
     * registered in Eclipse is used.
     *
     * @param url to be opened
     * @throws URISyntaxException
     * @since 3.3
     */
    public static void browse(final URL url) throws URISyntaxException {
        final String location = url.toURI().toString();
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                //try a normal launch
                if (!Program.launch(location)) {
                    //failed -> Go over the BrowserSupport
                    try {
                        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
                    } catch (PartInitException e) {
                        LOGGER.warn("Could not open Browser at location \"" + url.toString() + "\"");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

}
