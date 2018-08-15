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
 *   09.02.2005 (georg): created
 */
package org.knime.workbench.ui.wrapper;

import static org.knime.core.ui.wrapper.Wrapper.unwrapNCOptional;
import static org.knime.core.ui.wrapper.Wrapper.wraps;
import static org.knime.workbench.ui.async.AsyncUtil.ncAsyncSwitchRethrow;

import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import javax.swing.JPanel;
import javax.swing.UIManager;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.NodeOutPortUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.node.workflow.async.AsyncWorkflowManagerUI;
import org.knime.core.util.SWTUtilities;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * JFace implementation of a dialog containing the wrapped panel from the original node dialog.
 *
 * @author Thomas Gabriel, University of Konstanz, Germany
 */
public class WrappedNodeDialog extends AbstractWrappedDialog {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WrappedNodeDialog.class);

    private final NodeContainerUI m_nodeContainer;

    private final NodeDialogPane m_dialogPane;

    private final Runnable m_writeProtectionChangedListener;

    private Button m_okButton;

    private Button m_applyButton;

    /**
     * Creates the (application modal) dialog for a given node.
     *
     * We'll set SHELL_TRIM - style to keep this dialog resizable. This is needed because of the odd "preferredSize"
     * behavior (@see WrappedNodeDialog#getInitialSize())
     *
     * @param parentShell The parent shell
     * @param nodeContainer The node
     * @throws NotConfigurableException if the dialog cannot be opened because of real invalid settings or if any
     *             pre-conditions are not fulfilled, e.g. no predecessor node, no nominal column in input table, etc.
     */
    public WrappedNodeDialog(final Shell parentShell, final NodeContainerUI nodeContainer) throws NotConfigurableException {
        super(parentShell);
        m_nodeContainer = nodeContainer;
        m_dialogPane =
            ncAsyncSwitchRethrow(nc -> nc.getDialogPaneWithSettings(), nc -> nc.getDialogPaneWithSettingsAsync(),
                nodeContainer, "Waiting for the dialog to open");

        if (m_nodeContainer.getParent() != null) {
            m_writeProtectionChangedListener = () -> updateWriteProtectedState();
            WorkflowManagerUI parent = m_nodeContainer.getParent();
            if(parent instanceof AsyncWorkflowManagerUI) {
                ((AsyncWorkflowManagerUI)parent).addWriteProtectionChangedListener(m_writeProtectionChangedListener);
                //TODO node state changed listener could be added, too, in order to update the write protected state
            }
        } else {
            m_writeProtectionChangedListener = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleShellCloseEvent() {
        // send cancel&close action to underlying dialog pane
        m_dialogPane.callOnCancel();
        m_dialogPane.callOnClose();
        super.handleShellCloseEvent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int open() {
        getParentShell().setEnabled(false);
        try {
            ViewUtils.invokeAndWaitInEDT(() -> {
                pushNodeContext();
            });
            pushNodeContext();
            try {
                m_dialogPane.onOpen();
                return super.open();
            } finally {
                removeNodeContext();
                ViewUtils.invokeAndWaitInEDT(() -> {
                    removeNodeContext();
                });
            }
        } finally {
            getParentShell().setEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean close() {
        boolean res = super.close();
        WorkflowManagerUI parent = m_nodeContainer.getParent();
        if (m_writeProtectionChangedListener != null && parent instanceof AsyncWorkflowManagerUI) {
            ((AsyncWorkflowManagerUI)parent).removeWriteProtectionChangedListener(m_writeProtectionChangedListener);
        }
        return res;
    }

    /**
     * Configure shell, create top level menu.
     *
     * {@inheritDoc}
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        final Menu menuBar = new Menu(newShell, SWT.BAR);
        newShell.setMenuBar(menuBar);
        final Menu menu = new Menu(newShell, SWT.DROP_DOWN);
        final MenuItem rootItem = new MenuItem(menuBar, SWT.CASCADE);
        rootItem.setText("File");
        rootItem.setAccelerator(SWT.MOD1 | 'F');
        rootItem.setMenu(menu);

        final FileDialog openDialog = new FileDialog(newShell, SWT.OPEN);
        final FileDialog saveDialog = new FileDialog(newShell, SWT.SAVE);

        final MenuItem itemLoad = new MenuItem(menu, SWT.PUSH);
        itemLoad.setText("Load Settings");
        itemLoad.setAccelerator(SWT.MOD1 | 'L');
        itemLoad.addSelectionListener(new SelectionAdapter() {
            /** {@inheritDoc} */
            @Override
            public void widgetSelected(final SelectionEvent e) {
                String file = openDialog.open();
                if (file != null) {
                    pushNodeContext();
                    try {
                        m_dialogPane.loadSettingsFrom(new FileInputStream(file));
                    } catch (IOException ioe) {
                        showErrorMessage(ioe.getMessage());
                    } catch (NotConfigurableException ex) {
                        showErrorMessage(ex.getMessage());
                    } finally {
                        removeNodeContext();
                    }
                }
            }
        });
        final MenuItem itemSave = new MenuItem(menu, SWT.PUSH);
        itemSave.setText("Save Settings");
        itemSave.setAccelerator(SWT.MOD1 | 'S');
        itemSave.addSelectionListener(new SelectionAdapter() {
            /** {@inheritDoc} */
            @Override
            public void widgetSelected(final SelectionEvent e) {
                String file = saveDialog.open();
                if (file != null) {
                    pushNodeContext();
                    try {
                        m_dialogPane.saveSettingsTo(new FileOutputStream(file));
                    } catch (IOException ioe) {
                        showErrorMessage(ioe.getMessage());
                        // SWT-AWT-Bridge doesn't properly
                        // repaint after dialog disappears
                        m_dialogPane.getPanel().repaint();
                    } catch (InvalidSettingsException ise) {
                        showErrorMessage("Invalid Settings\n"
                                + ise.getMessage());
                        // SWT-AWT-Bridge doesn't properly
                        // repaint after dialog disappears
                        m_dialogPane.getPanel().repaint();
                    } finally {
                        removeNodeContext();
                    }
                }
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite area = (Composite)super.createDialogArea(parent);
        final Color backgroundColor =
                Display.getCurrent()
                        .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        area.setBackground(backgroundColor);
        //area.setLayoutData(new GridData(GridData.FILL_BOTH));

        // create the dialogs' panel and pass it to the SWT wrapper composite
        getShell().setText("Dialog - " + m_nodeContainer.getDisplayLabel());

        final JPanel p = m_dialogPane.getPanel();
        m_wrapper = new Panel2CompositeWrapper(area, p, SWT.EMBEDDED);

        // Bug 6275: Explicitly set has size flag and layout.
        // This ensures that the wrapper component has the correct size.
        if (Platform.OS_WIN32.equals(Platform.getOS())) {
            resizeHasOccurred = true;
        }

        return area;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create() {
        super.create();
        final Point size;
        if (Platform.OS_MACOSX.equals(Platform.getOS())) {
            // For Mac OS X the size is already correct
            size = getShell().getSize();
        } else {
            // For other systems we calculate it
            size = getInitialSize();
        }

        final Rectangle knimeWindowBounds = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getBounds();
        // Middle point relative to the KNIME window
        final Point middle = new Point(knimeWindowBounds.width / 2, knimeWindowBounds.height / 2);
        // Absolute upper left point for the dialog
        final Point newLocation =
            new Point(middle.x - (size.x / 2) + knimeWindowBounds.x, middle.y - (size.y / 2) + knimeWindowBounds.y);
        getShell().setBounds(newLocation.x, newLocation.y, size.x, size.y);

        finishDialogCreation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        // WORKAROUND: can't use IDialogConstants.OK_ID here, as this always
        // closes the dialog, regardless if the settings couldn't be applied.
        m_okButton = createButton(parent, IDialogConstants.NEXT_ID, IDialogConstants.OK_LABEL, false);
        m_applyButton = createButton(parent, IDialogConstants.FINISH_ID, "Apply", false);
        final Button btnCancel = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

        ((GridLayout)parent.getLayout()).numColumns++;
        final Button btnHelp = new Button(parent, SWT.PUSH | SWT.FLAT);
        final Image img = ImageRepository.getIconImage(SharedImages.Help);
        btnHelp.setImage(img);

        updateWriteProtectedState();

        m_swtKeyListener = new KeyListener() {
            /** {@inheritDoc} */
            @Override
            public void keyReleased(final KeyEvent ke) {
                if (ke.keyCode == SWT.MOD1) {
                    m_okButton.setText("OK");
                }
            }

            /** {@inheritDoc} */
            @Override
            public void keyPressed(final KeyEvent ke) {
                if ((ke.keyCode == SWT.ESC) && m_dialogPane.closeOnESC()) {
                    // close dialog on ESC
                    doCancel();
                }
                // this locks the WFM so avoid calling it each time.
                final Predicate<NodeContainerUI> canExecutePredicate = n -> n.getParent().canExecuteNode(n.getID());
                if (ke.keyCode == SWT.MOD1 && canExecutePredicate.test(m_nodeContainer)) {
                    // change OK button label, when CTRL/COMMAND is pressed
                    m_okButton.setText("OK - Execute");
                }
                if ((ke.keyCode == SWT.CR) && ((ke.stateMask & SWT.MOD1) != 0)) {
                    // Bug 3942: transfer focus to OK button to have all component to auto-commit their changes
                    m_okButton.forceFocus();
                    // force OK - Execute when CTRL/COMMAND and ENTER is pressed
                    // open first out-port view if SHIFT is pressed
                    ke.doit = doApply();
                    if (ke.doit) {
                        runOK(canExecutePredicate.test(m_nodeContainer),  (ke.stateMask & SWT.SHIFT) != 0);
                    }
                    // reset ok button state/label
                    if (!ke.doit) {
                        m_okButton.setText("OK");
                    }
                }
            }
        };
        m_awtKeyListener = new KeyAdapter() {
            /** {@inheritDoc} */
            @Override
            public void keyReleased(final java.awt.event.KeyEvent ke) {
                final int menuAccelerator = (Platform.OS_MACOSX.equals(Platform.getOS()))
                    ? java.awt.event.KeyEvent.VK_META : java.awt.event.KeyEvent.VK_CONTROL;

                if (ke.getKeyCode() == menuAccelerator) {
                    Display.getDefault().asyncExec(() -> {
                        if (!m_okButton.isDisposed()) {
                            m_okButton.setText("OK");
                        }
                    });
                }
            }

            /** {@inheritDoc} */
            @Override
            public void keyPressed(final java.awt.event.KeyEvent ke) {
                if ((ke.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) && m_dialogPane.closeOnESC()) {
                    // close dialog on ESC
                    Display.getDefault().asyncExec(() -> {
                        doCancel();
                    });
                }

                // this locks the WFM so avoid calling it each time.
                final Predicate<NodeContainerUI> canExecutePredicate = n -> n.getParent().canExecuteNode(n.getID());
                final int menuAccelerator = (Platform.OS_MACOSX.equals(Platform.getOS()))
                    ? java.awt.event.KeyEvent.VK_META : java.awt.event.KeyEvent.VK_CONTROL;
                if ((ke.getKeyCode() == menuAccelerator) && canExecutePredicate.test(m_nodeContainer)) {
                    // change OK button label, when CTRL/COMMAND is pressed
                    Display.getDefault().asyncExec(() -> {
                        if (!m_okButton.isDisposed()) {
                            m_okButton.setText("OK - Execute");
                        }
                    });
                }
                final int modifierKey = (Platform.OS_MACOSX.equals(Platform.getOS())) ? InputEvent.META_DOWN_MASK
                    : InputEvent.CTRL_DOWN_MASK;
                if ((ke.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER)
                                    && ((ke.getModifiersEx() & modifierKey) != 0)) {
                    Display.getDefault().asyncExec(() -> {
                        if (m_okButton.isDisposed()) {
                            return;
                        }

                        // Bug 3942: transfer focus to OK button to have all component to auto-commit their changes
                        m_okButton.forceFocus();

                        // force OK - Execute when CTRL/COMMAND and ENTER is pressed
                        // open first out-port view if SHIFT is pressed
                        if (doApply()) {
                            runOK(canExecutePredicate.test(m_nodeContainer),
                                  ((ke.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0));
                        }
                        else {
                            // reset ok button state/label
                            m_okButton.setText("OK");
                        }
                    });
                }
            }
        };

        if (!Platform.OS_MACOSX.equals(Platform.getOS())) {
            m_okButton.addKeyListener(m_swtKeyListener);
            m_applyButton.addKeyListener(m_swtKeyListener);
            btnCancel.addKeyListener(m_swtKeyListener);
            m_wrapper.addKeyListener(m_swtKeyListener);
        }

        // Register listeners that notify the content object, which
        // in turn notify the dialog about the particular event.
        m_okButton.addSelectionListener(new SelectionAdapter() {
            /** {@inheritDoc} */
            @Override
            public void widgetSelected(final SelectionEvent se) {
                if ((se.stateMask & SWT.SHIFT) != 0 && (se.stateMask & SWT.MOD1) != 0) {
                    // OK plus execute and open first out-port view
                    doOK(se, true, true);
                } else if ((se.stateMask & SWT.MOD1) != 0) {
                    // OK plus execute only
                    doOK(se, true, false);
                } else {
                    // OK only
                    doOK(se, false, false);
                }
                if (!se.doit) {
                    // reset ok button state/label
                    if (!m_okButton.isDisposed()) {
                        m_okButton.setText("OK");
                    }
                }
            }
        });

        m_applyButton.addSelectionListener(new SelectionAdapter() {
            /** {@inheritDoc} */
            @Override
            public void widgetSelected(final SelectionEvent se) {
                se.doit = doApply();
                // reset ok button state/label
                if (!m_okButton.isDisposed()) {
                    m_okButton.setText("OK");
                }
            }
        });

        btnCancel.addSelectionListener(new SelectionAdapter() {
            /** {@inheritDoc} */
            @Override
            public void widgetSelected(final SelectionEvent se) {
                doCancel();
            }
        });

        btnHelp.addSelectionListener(new SelectionAdapter() {
            /** {@inheritDoc} */
            @Override
            public void widgetSelected(final SelectionEvent e) {
                doOpenNodeDescription();
            }
        });
    }

    private void doOpenNodeDescription() {
        HelpWindow.instance.open();

        final Rectangle bounds = getShell().getBounds();
        final int x = bounds.x + bounds.width + 10;
        final int y = bounds.y - (HelpWindow.instance.getShell().getBounds().height - bounds.height) / 2;
        HelpWindow.instance.getShell().setLocation(x, y);
        HelpWindow.instance.showDescriptionForNode(m_nodeContainer);
    }

    private void doCancel() {
        // delegate cancel&close event to underlying dialog pane
        pushNodeContext();
        try {
            m_dialogPane.callOnCancel();
            m_dialogPane.callOnClose();
        } finally {
            removeNodeContext();
        }
        buttonPressed(IDialogConstants.CANCEL_ID);
    }

    private void doOK(final SelectionEvent se, final boolean execute, final boolean openView) {
        // simulate #doApply
        se.doit = doApply();
        if (se.doit) {
            runOK(execute, openView);
        }
    }

    private void runOK(final boolean execute, final boolean openView) {
        // send close action to underlying dialog pane
        pushNodeContext();
        try {
            m_dialogPane.callOnClose();
            buttonPressed(IDialogConstants.OK_ID);
            if (execute) {
                m_nodeContainer.getParent().executeUpToHere(m_nodeContainer.getID());
            }
            if (openView) {
                final Rectangle knimeWindowBounds =
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getBounds();
                ViewUtils.invokeLaterInEDT(() -> {
                    // show out-port view for nodes with at least
                    // one out-port (whereby the first is used as flow
                    // variable port for SingleNodeContainer), otherwise
                    // handle meta node (without flow variable port)
                    final int pIndex;
                    if (m_nodeContainer instanceof SingleNodeContainer) {
                        pIndex = 1;
                    } else {
                        pIndex = 0;
                    }
                    if (m_nodeContainer.getNrOutPorts() > pIndex) {
                        NodeOutPortUI port = m_nodeContainer.getOutPort(pIndex);
                        if (wraps(port, NodeOutPort.class)) {
                            java.awt.Rectangle bounds = new java.awt.Rectangle(knimeWindowBounds.x,
                                knimeWindowBounds.y, knimeWindowBounds.width, knimeWindowBounds.height);
                            ((NodeOutPort)port).openPortView(port.getPortName(), bounds);
                        }
                    }
                });
            }
        } finally {
            removeNodeContext();
        }
    }

    private boolean doApply() {
        if (isWriteProtected()) {
            return false;
        }
        // event.doit = false cancels the SWT selection event, so that the
        // dialog is not closed on errors.
        try {
            // if the settings are equal and the node is executed
            // to the previous settings inform the user but do nothing
            // (no reset)
            if (m_nodeContainer.getNodeContainerState().isExecuted()) {
                if (m_nodeContainer.areDialogAndNodeSettingsEqual()) {
                    // settings not changed
                    informNothingChanged();
                    return true;
                } else {
                    // settings have changed
                    if (m_nodeContainer.areDialogSettingsValid()) {
                        // valid settings
                        if (confirmApply()) {
                            // apply settings
                            m_nodeContainer.applySettingsFromDialog();
                            return true;
                        } else {
                            // user canceled reset and apply
                            // let the dialog open
                            return false;
                        }
                    } else {
                        // invalid settings
                        // apply settings to get the exception
                        m_nodeContainer.applySettingsFromDialog();
                        // we should never go here
                        // (since we should have invalid settings)
                        throw new IllegalStateException("Settings are not valid but apply settings throws no exception");
                    }
                }
            } else {
                // not executed
                getShell().setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));
                m_nodeContainer.applySettingsFromDialog();
                return true;
            }
        } catch (InvalidSettingsException ise) {
            LOGGER.warn("failed to apply settings: " + ise.getMessage(), ise);
            showWarningMessage("Invalid settings:\n" + ise.getMessage());
            // SWT-AWT-Bridge doesn't properly repaint after dialog disappears
            m_dialogPane.getPanel().repaint();
        } catch (IllegalStateException ex) {
            LOGGER.warn("failed to apply settings: " + ex.getMessage(), ex);
            showWarningMessage("Invalid node state:\n" + ex.getMessage());
            // SWT-AWT-Bridge doesn't properly repaint after dialog disappears
            m_dialogPane.getPanel().repaint();
        } catch (Throwable t) {
            LOGGER.error("failed to apply settings: " + t.getMessage(), t);
            showErrorMessage(t.getClass().getSimpleName() + ": " + t.getMessage());
            // SWT-AWT-Bridge doesn't properly repaint after dialog disappears
            m_dialogPane.getPanel().repaint();
        } finally {
            getShell().setCursor(null);
        }
        return false;
    }

    /**
     * Show confirm dialog before applying settings.
     *
     * @return <code>true</code> if the settings should be applied
     */
    protected boolean confirmApply() {
        // no confirm dialog necessary, if the node was not executed before
        if (!m_nodeContainer.getNodeContainerState().isExecuted()) {
            return true;
        }

        // the following code has mainly been copied from
        // IDEWorkbenchWindowAdvisor#preWindowShellClose
        IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
        if (!store.contains(PreferenceConstants.P_CONFIRM_RESET)
            || store.getBoolean(PreferenceConstants.P_CONFIRM_RESET)) {
            final MessageDialogWithToggle dialog =
                    MessageDialogWithToggle
                            .openOkCancelConfirm(
                                    SWTUtilities.getActiveShell(),
                                    "Confirm reset...",
                                    "Warning, reset node(s)!\n"
                                            + "New settings will be applied after resetting this "
                                            + "and all connected nodes. Continue?",
                                    "Do not ask again", false, null, null);
            final boolean isOK = dialog.getReturnCode() == IDialogConstants.OK_ID;
            if (isOK && dialog.getToggleState()) {
                store.setValue(PreferenceConstants.P_CONFIRM_RESET, false);
                KNIMEUIPlugin.getDefault().savePluginPreferences();
            }
            return isOK;
        }
        return true;
    }

    // these are extra height and width value since the parent dialog
    // does not return the right sizes for the underlying dialog pane
    private static final int EXTRA_WIDTH;

    private static final int EXTRA_HEIGHT;

    static {
        // why scroll bar width/height? It fixes layout issues in all dialogs having preview
        // panes that when filled show a scroll bar and this additional scroll bar space
        // will make the 'outer' scroll bar to show
        final int scrollBarWidthOrHeight = ((Integer)UIManager.get("ScrollBar.width")).intValue();
        EXTRA_WIDTH = 25 + scrollBarWidthOrHeight;
        EXTRA_HEIGHT = 25 + scrollBarWidthOrHeight;
    }

    /**
     * This calculates the initial size of the dialog. As the wrapped AWT-Panel
     * ("NodeDialogPane") sometimes just won't return any useful preferred sizes
     * this is kinda tricky workaround :-(
     *
     * TODO: It seems like either this one is more correct, or the one in WrappedMultipleNodeDialog is more correct, and
     *  whichever one it is should get their implementation moved up into AbstractWrappedDialog.
     *
     * {@inheritDoc}
     */
    @Override
    protected Point getInitialSize() {
        final JPanel panel = m_dialogPane.getPanel();

        final AtomicReference<Dimension> preferredSize = new AtomicReference<Dimension>(new Dimension(0, 0));
        ViewUtils.invokeAndWaitInEDT(() -> {
            preferredSize.set(panel.getPreferredSize());
        });


        // button bar sizes
        final int widthButtonBar = buttonBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        final int heightButtonBar = buttonBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

        // init dialog sizes
        final int widthDialog = super.getInitialSize().x;
        final int heightDialog = super.getInitialSize().y;

        // we need to make sure that we have at least enough space for
        // the button bar (+ some extra space)
        final int width = Math.max(Math.max(widthButtonBar, widthDialog),
            preferredSize.get().width + widthDialog - widthButtonBar + EXTRA_WIDTH);
        final int height =
            Math.max(Math.max(heightButtonBar, heightDialog), preferredSize.get().height + heightDialog + EXTRA_HEIGHT);

        // set the size of the container composite
        final Point size = new Point(width, height);
        m_wrapper.setSize(size);
        return size;
    }

    private void pushNodeContext() {
        // so far node context is only supported for the non-UI NodeContainer
        unwrapNCOptional(m_nodeContainer).ifPresent(nc -> NodeContext.pushContext(nc));
    }

    private void removeNodeContext() {
        if (wraps(m_nodeContainer, NodeContainer.class)) {
            NodeContext.removeLastContext();
        }
    }

    private void updateWriteProtectedState() {
        boolean writeProtected = isWriteProtected();
        Display.getDefault().asyncExec(() -> {
            m_okButton.setEnabled(!writeProtected);
            m_applyButton.setEnabled(!writeProtected);
        });
    }

    private boolean isWriteProtected() {
        //first two if's check whether the underlying node  has a reset lock ({@link NodeContainer#getNodeLocks()}) and
        //is executed at the same time, or it has a configure lock ({@link NodeContainer#getNodeLocks()}).
        if (m_nodeContainer.getNodeLocks().hasResetLock() && (m_nodeContainer.getNodeContainerState().isExecuted()
            || m_nodeContainer.getNodeContainerState().isExecutionInProgress())) {
            return true;
        } else if (m_nodeContainer.getNodeLocks().hasConfigureLock()) {
            return true;
        } else {
            if (wraps(m_nodeContainer, NodeContainer.class)) {
                return m_dialogPane.isWriteProtected();
            } else {
                //get the write protected state directly from the underlying node/workflow manager, because it can change
                //e.g. when the workflow is disconnected
                return m_nodeContainer.getParent().isWriteProtected();
            }
        }
    }
}
