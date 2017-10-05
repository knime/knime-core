/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Mar 14, 2016 (albrecht): created
 */
package org.knime.product.rcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.explorer.view.actions.OpenKNIMEArchiveFileAction;
import org.knime.workbench.explorer.view.actions.OpenKnimeUrlAction;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz Germany
 */
public class KNIMEOpenDocumentEventProcessor implements Listener {

    private ArrayList<String> m_filesToOpen = new ArrayList<String>(1);

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleEvent(final Event event) {
        if (event.text != null) {
            m_filesToOpen.add(event.text);
        }
    }

    /**
     * If there are files to open from an openFile-Event this method triggers the corresponding Actions, depending on
     * the type of files (knimeURL or knar/knwf).
     *
     */
    public void openFiles() {
        if (m_filesToOpen.isEmpty()) {
            return;
        }

        List<File> archiveFileList = new ArrayList<File>(1);
        List<String> urlFileList = new ArrayList<String>(1);
        for (String file : m_filesToOpen) {
            if (isKnimeUrl(file)) {
                urlFileList.add(file);
                continue;
            }
            File fileToOpen = new File(file);
            if (fileToOpen.exists() && fileToOpen.isFile() && fileToOpen.canRead()) {
                if (file.endsWith(".knimeURL")) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(fileToOpen))) {
                        String url = reader.readLine();
                        if (isKnimeUrl(url)) {
                            urlFileList.add(url);
                        }
                    } catch (IOException e) {
                        // TODO issue warning?
                    } finally {
                        fileToOpen.delete();
                    }
                } else {
                    archiveFileList.add(fileToOpen);
                }
            }
        }
        m_filesToOpen.clear();

        IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (!urlFileList.isEmpty()) {
            OpenKnimeUrlAction a = new OpenKnimeUrlAction(activePage, urlFileList);
            a.run();
        }

        if (!archiveFileList.isEmpty()) {
            OpenKNIMEArchiveFileAction a = new OpenKNIMEArchiveFileAction(activePage, archiveFileList);
            a.run();
        }
    }

    private boolean isKnimeUrl(final String url) {
        String protocol = "knime://";
        int length = protocol.length();
        if (url != null && !url.isEmpty() && url.length() > length) {
            if (url.substring(0, length).equalsIgnoreCase(protocol)) {
                return true;
            }
        }
        return false;
    }

}
