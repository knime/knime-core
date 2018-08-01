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
 * ------------------------------------------------------------------------
 *
 * History
 *   May 26, 2011 (wiswedel): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.gef.commands.Command;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.WorkflowManagerWrapper;
import org.knime.core.ui.wrapper.Wrapper;

/**
 * Abstract super class for KNIME related commands. It holds a reference to
 * the host workflow on which this command is applied.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public abstract class AbstractKNIMECommand extends Command {

   private WorkflowManagerUI m_hostWFM;

   /** @param hostWFM The host workflow, must not be null. */
    public AbstractKNIMECommand(final WorkflowManager hostWFM) {
        if (hostWFM == null) {
            throw new IllegalArgumentException("Argument must not be null.");
        } else {
            m_hostWFM = WorkflowManagerWrapper.wrap(hostWFM);
        }
    }

    /**
     * If this constructor is used, the {@link #getHostWFM()}-method will return <code>null</code> and the
     * {@link #getHostWFMUI()} must be used instead.
     *
     * @param hostWFM The host UI(!) workflow, must not be null.
     */
    public AbstractKNIMECommand(final WorkflowManagerUI hostWFM) {
        if (hostWFM == null) {
            throw new IllegalArgumentException("Argument must not be null.");
        }
        m_hostWFM = hostWFM;
    }

    /**
     * @return the hostWFM, not null unless this command is disposed or the derived command works on the
     *         {@link WorkflowManagerUI}.
     */
    protected final WorkflowManager getHostWFM() {
        return m_hostWFM == null ? null : Wrapper.unwrapWFMOptional(m_hostWFM).orElse(null);
    }

    /**
     * @return the hostWFMUI, not null unless the command is disposed.
     */
    protected final WorkflowManagerUI getHostWFMUI() {
        return m_hostWFM;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canExecute() {
        return m_hostWFM != null && !m_hostWFM.isWriteProtected();
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        super.dispose();
        m_hostWFM = null;
    }

    /**
     * Opens a warning dialog when something went wrong while trying to run a command.
     *
     * @param title the dialog title
     * @param message the actual message
     */
    protected static void openDialog(final String title, final String message) {
        Display.getDefault().syncExec(() -> {
            final Shell shell = Display.getDefault().getActiveShell();
            MessageBox mb = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
            mb.setText(title);
            mb.setMessage(message);
            mb.open();
        });
    }
}
