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
 *   27.11.2012 (ohl): created
 */
package org.knime.workbench.ui.wrapper;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.node.workflow.NodeExecutorJobManagerDialogTab;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Wraps the settings that can be applied to multiple nodes.
 *
 * @author Peter Ohl, KNIME.com AG, Switzerland
 */
public class WrappedMultipleNodeDialog extends Dialog {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WrappedMultipleNodeDialog.class.getName());

    private Composite m_container;

    private final WorkflowManager m_parentMgr;

    private final NodeID[] m_nodes;

    private Panel2CompositeWrapper m_wrapper;

    private final NodeContainerSettings m_initValue;

    private final NodeExecutorJobManagerDialogTab m_dialogPane;

    private final AtomicBoolean m_dialogSizeComputed = new AtomicBoolean();

    private final static boolean enableMacOSXWorkaround = Boolean
            .getBoolean(KNIMEConstants.PROPERTY_MACOSX_DIALOG_WORKAROUND);

    /**
     * Creates the (application modal) dialog for a given node.
     *
     * We'll set SHELL_TRIM - style to keep this dialog resizable. This is needed because of the odd "preferredSize"
     * behavior (@see WrappedNodeDialog#getInitialSize())
     *
     * @param parentShell The parent shell
     * @param nodeContainer The node.
     * @throws NotConfigurableException if the dialog cannot be opened because of real invalid settings or if any
     *             pre-conditions are not fulfilled, e.g. no predecessor node, no nominal column in input table, etc.
     */
    public WrappedMultipleNodeDialog(final Shell parentShell, final WorkflowManager parentMgr,
                                     final SplitType splitType, final NodeID... nodes) {
        super(parentShell);
        this.setShellStyle(SWT.PRIMARY_MODAL | SWT.SHELL_TRIM);
        m_parentMgr = parentMgr;
        m_nodes = nodes;
        m_initValue = m_parentMgr.getCommonSettings(nodes);
        m_dialogPane = new NodeExecutorJobManagerDialogTab(splitType);
        if (enableMacOSXWorkaround) {
            // get underlying panel and do layout it
            final Display display = Display.getCurrent();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    m_dialogPane.doLayout();
                    m_dialogSizeComputed.set(true);
                    display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            // deliberately empty, this is only to wake up a
                            // potentially waiting SWT-thread in getInitialSize
                        }
                    });
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleShellCloseEvent() {
        // dialog window x'ed out
        doCancel();
        super.handleShellCloseEvent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int open() {
        // nothing special on open
        return super.open();
    }

    /**
     * Configure shell, create top level menu.
     *
     * {@inheritDoc}
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        Image img = KNIMEUIPlugin.getDefault().getImageRegistry().get("knime");
        newShell.setImage(img);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite)super.createDialogArea(parent);
        Color backgroundColor = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        area.setBackground(backgroundColor);
        m_container = new Composite(area, SWT.NONE);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        m_container.setLayout(gridLayout);
        m_container.setLayoutData(new GridData(GridData.FILL_BOTH));

        String title = "Select Job Manager";
        if (m_nodes.length > 1) {
            title += " for " + m_nodes.length + " Nodes";
        }
        getShell().setText(title);

        m_wrapper = new Panel2CompositeWrapper(m_container, m_dialogPane, SWT.EMBEDDED);
        m_wrapper.setLayoutData(new GridData(GridData.FILL_BOTH));
        m_wrapper.addFocusListener(new FocusAdapter() {
            /**
             * @param e focus event passed to the underlying AWT component
             */
            @Override
            public void focusGained(final FocusEvent e) {
                ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
                    @Override
                    public void run() {
                        m_wrapper.getAwtPanel().requestFocus();
                    }
                });
            }
        });
        if (m_initValue != null) {
            m_dialogPane.loadSettings(m_initValue, null);
        }
        return area;
    }

    /**
     * Linux (GTK) hack: must explicitly invoke <code>getInitialSize()</code>.
     *
     * @see org.eclipse.jface.window.Window#create()
     */
    @Override
    public void create() {
        super.create();
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().startsWith("linux")) {
            getShell().setSize(getInitialSize());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        // WORKAROUND: can't use IDialogConstants.OK_ID here, as this always
        // closes the dialog, regardless if the settings couldn't be applied.
        final Button btnOK = createButton(parent, IDialogConstants.NEXT_ID, IDialogConstants.OK_LABEL, false);
        final Button btnCancel = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

        ((GridLayout)parent.getLayout()).numColumns++;

        final KeyListener keyListener = new KeyListener() {
            /** {@inheritDoc} */
            @Override
            public void keyReleased(final KeyEvent ke) {
                if (ke.keyCode == SWT.CTRL) {
                    btnOK.setText("OK");
                }
            }

            /** {@inheritDoc} */
            @Override
            public void keyPressed(final KeyEvent ke) {
                if (ke.keyCode == SWT.ESC) {
                    // close dialog on ESC
                    doCancel();
                }
                if (ke.keyCode == SWT.CR) {
                    if (ke.stateMask == SWT.CTRL || ke.stateMask == SWT.SHIFT + SWT.CTRL) {
                        // force OK - Execute when CTRL and ENTER is pressed
                        // open first out-port view if SHIFT is pressed
                        doOK(ke);
                    }
                }
            }
        };

        btnOK.addKeyListener(keyListener);
        btnCancel.addKeyListener(keyListener);
        m_wrapper.addKeyListener(keyListener);

        // Register listeners that notify the content object, which
        // in turn notify the dialog about the particular event.
        btnOK.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent se) {
                // OK only
                doOK(se);
            }
        });
        btnCancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent se) {
                doCancel();
            }
        });

    }

    private void doCancel() {
        // delegate cancel&close event to underlying dialog pane
        buttonPressed(IDialogConstants.CANCEL_ID);
    }

    private void doOK(final KeyEvent ke) {
        // event.doit = false cancels the SWT selection event, so that the
        // dialog is not closed on errors.
        ke.doit = doApply();
        if (ke.doit) {
            runOK();
        }
    }

    private void doOK(final SelectionEvent se) {
        // event.doit = false cancels the SWT selection event, so that the
        // dialog is not closed on errors.
        se.doit = doApply();
        if (se.doit) {
            runOK();
        }
    }

    private void runOK() {
        buttonPressed(IDialogConstants.OK_ID);
    }

    private boolean doApply() {
        try {
            NodeContainerSettings newSettings = new NodeContainerSettings();
            m_dialogPane.saveSettings(newSettings);
            if (newSettings.equals(m_initValue) || (m_initValue == null && newSettings.getJobManager() == null)) {
                informNothingChanged();
            } else {
                m_parentMgr.applyCommonSettings(newSettings, m_nodes);
            }
            return true;
        } catch (InvalidSettingsException ise) {
            LOGGER.warn("failed to apply settings: " + ise.getMessage(), ise);
            showWarningMessage("Invalid settings:\n" + ise.getMessage());
            // SWT-AWT-Bridge doesn't properly repaint after dialog disappears
            m_dialogPane.repaint();
        } catch (Throwable t) {
            LOGGER.error("failed to apply settings: " + t.getMessage(), t);
            showErrorMessage(t.getClass().getSimpleName() + ": " + t.getMessage());
            // SWT-AWT-Bridge doesn't properly repaint after dialog disappears
            m_dialogPane.repaint();
        }
        return false;
    }

    /**
     * Shows the latest error message of the dialog in a MessageBox.
     *
     * @param message The error string.
     */
    private void showErrorMessage(final String message) {
        MessageBox box = new MessageBox(getShell(), SWT.ICON_ERROR);
        box.setText("Error");
        box.setMessage(message != null ? message : "(no message)");
        box.open();
    }

    /**
     * Shows the latest error message of the dialog in a MessageBox.
     *
     * @param message The error string.
     */
    private void showWarningMessage(final String message) {
        MessageBox box = new MessageBox(getShell(), SWT.ICON_WARNING);
        box.setText("Warning");
        box.setMessage(message != null ? message : "(no message)");
        box.open();
    }

    /**
     * Show an information dialog that the settings were not changed and therefore the settings are not reset (node
     * stays executed).
     */
    private void informNothingChanged() {
        MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_INFORMATION | SWT.OK);
        mb.setText("Settings were not changed.");
        mb.setMessage("The settings were not changed. " + "The node will not be reset.");
        mb.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean close() {
        return super.close();
    }

    // these are extra height and width value since the parent dialog
    // does not return the right sizes for the underlying dialog pane
    private static final int EXTRA_WIDTH = 25;

    private static final int EXTRA_HEIGHT = 20;

    /**
     * This calculates the initial size of the dialog. As the wrapped AWT-Panel ("NodeDialogPane") sometimes just won't
     * return any useful preferred sizes this is kinda tricky workaround :-(
     *
     * {@inheritDoc}
     */
    @Override
    protected Point getInitialSize() {
        if (enableMacOSXWorkaround) {
            // this and the code in the constructor is a workaround
            // for the nasty deadlock on MacOSX
            // see http://bimbug.inf.uni-konstanz.de/show_bug.cgi?id=3151
            while (!m_dialogSizeComputed.get()) {
                Display.getCurrent().sleep();
            }
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        m_dialogPane.doLayout();
                    }
                });
            } catch (InterruptedException ex) {
                LOGGER.info("Thread interrupted", ex);
            } catch (InvocationTargetException ex) {
                LOGGER.error("Error while determining dialog sizes", ex);
            }
        }

        // underlying pane sizes
        int width = m_dialogPane.getWidth();
        int height = m_dialogPane.getHeight();

        // button bar sizes
        int widthButtonBar = buttonBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        int heightButtonBar = buttonBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

        // init dialog sizes
        int widthDialog = super.getInitialSize().x;
        int heightDialog = super.getInitialSize().y;

        // we need to make sure that we have at least enough space for
        // the button bar (+ some extra space)
        width = Math.max(Math.max(widthButtonBar, widthDialog), width + widthDialog - widthButtonBar + EXTRA_WIDTH);
        height = Math.max(Math.max(heightButtonBar, heightDialog), height + heightDialog + EXTRA_HEIGHT);

        // set the size of the container composite
        Point size = new Point(width, height);
        m_container.setSize(size);
        return size;
    }
}
