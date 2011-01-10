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
 *   09.02.2005 (georg): created
 */
package org.knime.workbench.ui.wrapper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * JFace implementation of a dialog containing the wrapped panel from the
 * original node dialog.
 *
 * @author Thomas Gabriel, University of Konstanz, Germany
 */
public class WrappedNodeDialog extends Dialog {

    private Composite m_container;

    private final NodeContainer m_nodeContainer;

    private Panel2CompositeWrapper m_wrapper;

    private final NodeDialogPane m_dialogPane;

    private Menu m_menuBar;

    private final NodeLogger m_logger;

    /**
     * Creates the (application modal) dialog for a given node.
     *
     * We'll set SHELL_TRIM - style to keep this dialog resizable. This is
     * needed because of the odd "preferredSize" behavior (@see
     * WrappedNodeDialog#getInitialSize())
     *
     * @param parentShell The parent shell
     * @param nodeContainer The node.
     * @throws NotConfigurableException if the dialog cannot be opened because
     * of real invalid settings or if any pre-conditions are not fulfilled, e.g.
     * no predecessor node, no nominal column in input table, etc.
     */
    public WrappedNodeDialog(final Shell parentShell,
            final NodeContainer nodeContainer) throws NotConfigurableException {
        super(parentShell);
        this.setShellStyle(SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
        m_nodeContainer = nodeContainer;
        m_dialogPane = m_nodeContainer.getDialogPaneWithSettings();
        m_logger = NodeLogger.getLogger(m_nodeContainer.getNameWithID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleShellCloseEvent() {
        // send cancel&close action to underlying dialog pane
        m_dialogPane.onCancel();
        m_dialogPane.onClose();
        super.handleShellCloseEvent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int open() {
        m_dialogPane.onOpen();
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
        m_menuBar = new Menu(newShell, SWT.BAR);
        newShell.setMenuBar(m_menuBar);
        Menu menu = new Menu(newShell, SWT.DROP_DOWN);
        MenuItem rootItem = new MenuItem(m_menuBar, SWT.CASCADE);
        rootItem.setText("File");
        rootItem.setAccelerator(SWT.CTRL | 'F');
        rootItem.setMenu(menu);

        final FileDialog openDialog = new FileDialog(newShell, SWT.OPEN);
        final FileDialog saveDialog = new FileDialog(newShell, SWT.SAVE);

        MenuItem itemLoad = new MenuItem(menu, SWT.PUSH);
        itemLoad.setText("Load Settings");
        itemLoad.setAccelerator(SWT.CTRL | 'L');
        itemLoad.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                String file = openDialog.open();
                if (file != null) {
                    try {
                        m_dialogPane.loadSettingsFrom(
                                new FileInputStream(file));
                    } catch (IOException ioe) {
                        showErrorMessage(ioe.getMessage());
                    } catch (NotConfigurableException ex) {
                        showErrorMessage(ex.getMessage());
                    }
                }
            }
        });
        MenuItem itemSave = new MenuItem(menu, SWT.PUSH);
        itemSave.setText("Save Settings");
        itemSave.setAccelerator(SWT.CTRL | 'S');
        itemSave.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                String file = saveDialog.open();
                if (file != null) {
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
        Composite area = (Composite)super.createDialogArea(parent);
        Color backgroundColor = Display.getCurrent().getSystemColor(
                SWT.COLOR_WIDGET_BACKGROUND);
        area.setBackground(backgroundColor);
        m_container = new Composite(area, SWT.NONE);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        m_container.setLayout(gridLayout);
        m_container.setLayoutData(new GridData(GridData.FILL_BOTH));

        // create the dialogs' panel and pass it to the SWT wrapper composite
        getShell().setText("Dialog - " + m_nodeContainer.getDisplayLabel());

        JPanel p = m_dialogPane.getPanel();
        m_wrapper = new Panel2CompositeWrapper(m_container, p, SWT.EMBEDDED);
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
        final Button btnOK = createButton(parent,
              IDialogConstants.NEXT_ID, IDialogConstants.OK_LABEL, false);
        final Button btnApply = createButton(parent,
                IDialogConstants.FINISH_ID, "Apply", false);
        final Button btnCancel = createButton(parent,
                IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);

        final KeyListener keyListener = new KeyListener() {
            /** {@inheritDoc} */
            @Override
            public void keyReleased(final KeyEvent ke) {
                if (ke.keyCode == SWT.CTRL) {
                    btnOK.setText("OK");
                }
                if (ke.keyCode == SWT.ESC) {
                    if (m_dialogPane.closeOnESC()) {
                        // close dialog on ESC
                        doCancel();
                    }
                }
            }
            /** {@inheritDoc} */
            @Override
            public void keyPressed(final KeyEvent ke) {
                if (ke.keyCode == SWT.CTRL) {
                    // change OK button label, when CTRL is pressed
                    btnOK.setText("OK - Execute");
                }
                if (ke.keyCode == SWT.CR) {
                    if (ke.stateMask == SWT.CTRL
                            || ke.stateMask == SWT.SHIFT + SWT.CTRL) {
                        // force OK - Execute when CTRL and ENTER is pressed
                        // open first out-port view if SHIFT is pressed
                        doOK(ke, true, ke.stateMask == SWT.SHIFT + SWT.CTRL);
                        // reset ok button state/label
                        if (ke.doit == false) {
                            btnOK.setText("OK");
                        }
                    }
                }
            }
        };

        btnOK.addKeyListener(keyListener);
        btnApply.addKeyListener(keyListener);
        btnCancel.addKeyListener(keyListener);
        m_wrapper.addKeyListener(keyListener);

        // Register listeners that notify the content object, which
        // in turn notify the dialog about the particular event.
        btnOK.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent se) {
                if (se.stateMask == SWT.SHIFT + SWT.CTRL) {
                    // OK plus execute and open first out-port view
                    doOK(se, true, true);
                } else if (se.stateMask == SWT.CTRL) {
                    // OK plus execute only
                    doOK(se, true, false);
                } else {
                    // OK only
                    doOK(se, false, false);
                }
                if (se.doit == false) {
                    // reset ok button state/label
                    btnOK.setText("OK");
                }
            }
        });

        btnApply.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent se) {
                se.doit = doApply();
                // reset ok button state/label
                btnOK.setText("OK");
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
        m_dialogPane.onCancel();
        m_dialogPane.onClose();
        buttonPressed(IDialogConstants.CANCEL_ID);
    }

    private void doOK(final KeyEvent ke, final boolean execute,
            final boolean openView) {
        // simulate doApply
        ke.doit = doApply();
        if (ke.doit) {
            runOK(execute, openView);
        }
    }

    private void doOK(final SelectionEvent se, final boolean execute,
            final boolean openView) {
        // simulate #doApply
        se.doit = doApply();
        if (se.doit) {
            runOK(execute, openView);
        }
    }

    private void runOK(final boolean execute, final boolean openView) {
        // send close action to underlying dialog pane
        m_dialogPane.onClose();
        buttonPressed(IDialogConstants.OK_ID);
        if (execute) {
            m_nodeContainer.getParent().executeUpToHere(
                    m_nodeContainer.getID());
            if (openView) {
                SwingUtilities.invokeLater(new Runnable() {
                    /** {inheritDoc} */
                    @Override
                    public void run() {
                        if (m_nodeContainer.getNrOutPorts() >= 1) {
                            NodeOutPort port = m_nodeContainer.getOutPort(1);
                            port.openPortView(port.getPortName());
                        }
                    }
                });
            }
        }
    }

    private boolean doApply() {
        // event.doit = false cancels the SWT selection event, so that the
        // dialog is not closed on errors.
        try {
            // if the settings are equal and the node is executed
            // to the previous settings inform the user but do nothing
            // (no reset)
            if (m_nodeContainer.getState().equals(
                    NodeContainer.State.EXECUTED)) {
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
                        throw new IllegalStateException(
                                "Settings are not valid but apply "
                                        + "settings throws no exception");
                    }
                }
            } else {
                // not executed
                m_nodeContainer.applySettingsFromDialog();
                return true;
            }
        } catch (InvalidSettingsException ise) {
            m_logger.warn("failed to apply settings: " + ise.getMessage(), ise);
            showWarningMessage("Invalid settings:\n" + ise.getMessage());
            // SWT-AWT-Bridge doesn't properly repaint after dialog disappears
            m_dialogPane.getPanel().repaint();
        } catch (IllegalStateException ex) {
            m_logger.warn("failed to apply settings: " + ex.getMessage(), ex);
            showWarningMessage("Invalid node state:\n" + ex.getMessage());
            // SWT-AWT-Bridge doesn't properly repaint after dialog disappears
            m_dialogPane.getPanel().repaint();
        } catch (Throwable t) {
            m_logger.error("failed to apply settings: " + t.getMessage(), t);
            showErrorMessage(t.getClass().getSimpleName() + ": "
                    + t.getMessage());
            // SWT-AWT-Bridge doesn't properly repaint after dialog disappears
            m_dialogPane.getPanel().repaint();
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
     * Show confirm dialog before applying settings.
     *
     * @return <code>true</code> if the settings should be applied
     */
    protected boolean confirmApply() {

        // no confirm dialog necessary, if the node was not executed before
        if (!m_nodeContainer.getState().equals(NodeContainer.State.EXECUTED)) {
            return true;
        }

        // the following code has mainly been copied from
        // IDEWorkbenchWindowAdvisor#preWindowShellClose
        IPreferenceStore store =
            KNIMEUIPlugin.getDefault().getPreferenceStore();
        if (!store.contains(PreferenceConstants.P_CONFIRM_RESET)
                || store.getBoolean(PreferenceConstants.P_CONFIRM_RESET)) {
            MessageDialogWithToggle dialog =
                MessageDialogWithToggle.openOkCancelConfirm(
                    Display.getDefault().getActiveShell(),
                    "Confirm reset...",
                    "Warning, reset node(s)!\n"
                    + "New settings will be applied after resetting this "
                    + "and all connected nodes. Continue?",
                    "Do not ask again", false, null, null);
            boolean isOK = dialog.getReturnCode() == IDialogConstants.OK_ID;
            if (isOK && dialog.getToggleState()) {
                store.setValue(PreferenceConstants.P_CONFIRM_RESET, false);
                KNIMEUIPlugin.getDefault().savePluginPreferences();
            }
            return isOK;
        }
        return true;
    }

    /**
     * Show an information dialog that the settings were not changed and
     * therefore the settings are not reset (node stays executed).
     */
    protected void informNothingChanged() {
        MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(),
                SWT.ICON_INFORMATION | SWT.OK);
        mb.setText("Settings were not changed.");
        mb.setMessage("The settings were not changed. "
                + "The node will not be reset.");
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
    private static final int EXTRA_WIDTH  = 25;
    private static final int EXTRA_HEIGHT = 20;

    /**
     * This calculates the initial size of the dialog. As the wrapped AWT-Panel
     * ("NodeDialogPane") sometimes just won't return any useful preferred sizes
     * this is kinda tricky workaround :-(
     *
     * {@inheritDoc}
     */
    @Override
    protected Point getInitialSize() {

        // get underlying panel and do layout it
        JPanel panel = m_dialogPane.getPanel();
        panel.doLayout();

        // underlying pane sizes
        int width = panel.getWidth();
        int height = panel.getHeight();

        // button bar sizes
        int widthButtonBar = buttonBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        int heightButtonBar = buttonBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

        // init dialog sizes
        int widthDialog = super.getInitialSize().x;
        int heightDialog = super.getInitialSize().y;

        // we need to make sure that we have at least enough space for
        // the button bar (+ some extra space)
        width = Math.max(Math.max(widthButtonBar, widthDialog),
                width + widthDialog - widthButtonBar + EXTRA_WIDTH);
        height = Math.max(Math.max(heightButtonBar, heightDialog),
                height + heightDialog - heightButtonBar + EXTRA_HEIGHT);

        // set the size of the container composite
        Point size = new Point(width, height);
        m_container.setSize(size);
        return size;
    }
}
