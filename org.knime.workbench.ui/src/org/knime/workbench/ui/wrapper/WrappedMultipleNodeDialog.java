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
 * History
 *   27.11.2012 (ohl): created
 */
package org.knime.workbench.ui.wrapper;

import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.node.workflow.NodeExecutorJobManagerDialogTab;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Wraps the settings that can be applied to multiple nodes.
 *
 * @author Peter Ohl, KNIME.com AG, Switzerland
 */
public class WrappedMultipleNodeDialog extends AbstractWrappedDialog {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WrappedMultipleNodeDialog.class);

    private final WorkflowManager m_parentMgr;

    private final NodeID[] m_nodes;

    private final NodeContainerSettings m_initValue;

    private final NodeExecutorJobManagerDialogTab m_dialogPane;

    /**
     * Creates the (application modal) dialog for a set of nodes.
     *
     * We'll set SHELL_TRIM - style to keep this dialog resizable. This is needed because of the odd "preferredSize"
     *  behavior (@see WrappedNodeDialog#getInitialSize()) (this comment is not necessarily applicable to this class;
     *  it appears to be the result of a copy and paste from WrappedNodeDialog)
     *
     * @param parentShell The parent shell
     * @param parentMgr The workflow manager containing the cited nodes
     * @param splitType A SplitType which i suppose is the lowest common denominator across all nodes.
     * @param nodes 1-N nodes whose dialogs are being wrapped.
     */
    public WrappedMultipleNodeDialog(final Shell parentShell, final WorkflowManager parentMgr,
                                     final SplitType splitType, final NodeID... nodes) {
        super(parentShell);

        m_parentMgr = parentMgr;
        m_nodes = nodes;
        m_initValue = m_parentMgr.getCommonSettings(nodes);
        m_dialogPane = new NodeExecutorJobManagerDialogTab(splitType);
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
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite)super.createDialogArea(parent);
        Color backgroundColor = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        area.setBackground(backgroundColor);

        String title = "Select Job Manager";
        if (m_nodes.length > 1) {
            title += " for " + m_nodes.length + " Nodes";
        }
        getShell().setText(title);
        m_wrapper = new Panel2CompositeWrapper(area, m_dialogPane, SWT.EMBEDDED);
        if (m_initValue != null) {
            m_dialogPane.loadSettings(m_initValue, null);
        }
        return area;
    }

    /**
     * Linux (GTK) hack: must explicitly invoke <code>getInitialSize()</code>.
     *
     * TODO it seems like WrappedNodeDialog is probably doing the better thing here - we should probably just move
     *  that implementation into AbstractWrappedDialog and get rid of this implementation.
     *
     * @see org.eclipse.jface.window.Window#create()
     */
    @Override
    public void create() {
        super.create();
        getShell().setSize(getInitialSize());
        this.finishDialogCreation();
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

        this.swtKeyListener = new KeyListener() {
            /** {@inheritDoc} */
            @Override
            public void keyReleased(final KeyEvent ke) {
                // TODO there's no point to this presently since we don't ever change the text
                if (ke.keyCode == SWT.MOD1) {
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
                // TODO this does not mimic the same behavior as found in WrappedNodeDialog to change the OK button text
                if ((ke.keyCode == SWT.CR) && ((ke.stateMask == SWT.MOD1) || (ke.stateMask == SWT.SHIFT + SWT.MOD1))) {
                    // force OK - Execute when CTRL/Command and ENTER is pressed
                    // open first out-port view if SHIFT is pressed
                    doOK(ke);
                }
            }
        };

        this.awtKeyListener = new KeyAdapter() {
            @Override
            public void keyReleased(final java.awt.event.KeyEvent ke) {
                // TODO there's no point to this presently since we don't ever change the text
                int menuAccelerator = (Platform.OS_MACOSX.equals(Platform.getOS())) ? java.awt.event.KeyEvent.VK_META
                                                                                    : java.awt.event.KeyEvent.VK_CONTROL;

                if (ke.getKeyCode() == menuAccelerator) {
                    getShell().getDisplay().asyncExec(() -> {
                        btnOK.setText("OK");
                    });
                }
            }

            @Override
            public void keyPressed(final java.awt.event.KeyEvent ke) {
                if (ke.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    // close dialog on ESC
                    getShell().getDisplay().asyncExec(() -> {
                        doCancel();
                    });
                }

                // TODO this does not mimic the same behavior as found in WrappedNodeDialog to change the OK button text
                int modifierKey = (Platform.OS_MACOSX.equals(Platform.getOS())) ? InputEvent.META_DOWN_MASK
                                                                                : InputEvent.CTRL_DOWN_MASK;
                if ((ke.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER)
                                    && ((ke.getModifiersEx() & modifierKey) != 0)) {
                    getShell().getDisplay().asyncExec(() -> {
                        // force OK - Execute when CTRL/Command and ENTER is pressed
                        if (doApply()) {
                            runOK();
                        }
                    });
                }
            }
        };

        if (!Platform.OS_MACOSX.equals(Platform.getOS())) {
            btnOK.addKeyListener(this.swtKeyListener);
            btnCancel.addKeyListener(this.swtKeyListener);
            m_wrapper.addKeyListener(this.swtKeyListener);
        }

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

    // these are extra height and width value since the parent dialog
    // does not return the right sizes for the underlying dialog pane
    private static final int EXTRA_WIDTH = 25;

    private static final int EXTRA_HEIGHT = 20;

    /**
     * This calculates the initial size of the dialog. As the wrapped AWT-Panel ("NodeDialogPane") sometimes just won't
     * return any useful preferred sizes this is kinda tricky workaround :-(
     *
     * TODO: It seems like either this one is more correct, or the one in WrappedNodeDialog is more correct, and
     *  whichever one it is should get their implementation moved up into AbstractWrappedDialog.
     *
     * {@inheritDoc}
     */
    @Override
    protected Point getInitialSize() {
        ViewUtils.invokeAndWaitInEDT(new Runnable() {
            @Override
            public void run() {
                m_dialogPane.doLayout();
            }
        });

        // underlying pane sizes
        int width = m_dialogPane.getPreferredSize().width;
        int height = m_dialogPane.getPreferredSize().height;

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
        m_wrapper.setSize(size);
        return size;
    }
}
