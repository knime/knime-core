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
 *   22.07.2014 (thor): created
 */
package org.knime.workbench.ui.startup.view;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.startup.StartupMessage;

/**
 * Dialog that show a startup message. The message can contain hyperlinks.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class StartupMessageDialog extends Dialog {
    private final StartupMessage m_message;

    /**
     * Creates a new dialog
     *
     * @param parentShell the parent shell for this dialog
     * @param message the startup message that should be shown
     */
    public StartupMessageDialog(final Shell parentShell, final StartupMessage message) {
        super(parentShell);
        m_message = message;
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite container = (Composite)super.createDialogArea(parent);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 15;
        layout.marginHeight = 15;
        container.setLayout(layout);

        Label lblWarning = new Label(container, SWT.NONE);
        lblWarning.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));

        Dictionary<String, String> headers = m_message.getBundle().getHeaders();
        String plugin = headers.get("Bundle-Name");
        switch (m_message.getType()) {
            case StartupMessage.ERROR:
                lblWarning.setImage(Display.getCurrent().getSystemImage(SWT.ICON_ERROR));
                getShell().setText("Startup error from " + plugin);
                break;
            case StartupMessage.WARNING:
                lblWarning.setImage(Display.getCurrent().getSystemImage(SWT.ICON_WARNING));
                getShell().setText("Startup warning from " + plugin);
                break;
            default:
                lblWarning.setImage(Display.getCurrent().getSystemImage(SWT.ICON_INFORMATION));
                getShell().setText("Startup information from " + plugin);
        }

        Link textWithLink = new Link(container, SWT.NONE);
        GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd.widthHint = 379;
        textWithLink.setLayoutData(gd);
        textWithLink.setText(m_message.getMessage());
        textWithLink.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                try {
                    URI url = new URI(event.text);
                    Desktop.getDesktop().browse(url);
                } catch (IOException | URISyntaxException ex) {
                    NodeLogger.getLogger(StartupMessageDialog.this.getClass()).error(
                        "Could not open URL '" + event.text + "' in external browser: " + ex.getMessage(), ex);
                }
            }
        });

        return container;
    }

}
