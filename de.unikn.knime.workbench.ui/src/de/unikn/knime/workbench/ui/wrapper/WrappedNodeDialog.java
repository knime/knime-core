/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   09.02.2005 (georg): created
 */
package de.unikn.knime.workbench.ui.wrapper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JPanel;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
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

import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NotConfigurableException;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowInExecutionException;
import de.unikn.knime.workbench.ui.KNIMEUIPlugin;

/**
 * JFace implementation of a dialog containing the wrapped Panel from the
 * original node dialog.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class WrappedNodeDialog extends Dialog {

    private Composite m_container;

    private final NodeContainer m_nodeContainer; 

    private Panel2CompositeWrapper m_wrapper;

    private final NodeDialogPane m_dialogPane;

    private Menu m_menuBar;

//    private Listener m_listener;

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
     * of real invalid settings or if any predconditions are not fulfilled, e.g.
     * no predecessor node, no nominal column in input table, etc.
     */
    public WrappedNodeDialog(final Shell parentShell,
            final NodeContainer nodeContainer) throws NotConfigurableException {
        super(parentShell);
        this.setShellStyle(SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
        m_nodeContainer = nodeContainer;
        m_dialogPane = m_nodeContainer.getDialogPane();
    }

    /**
     * Configure shell, create top level menu.
     * 
     * @see org.eclipse.jface.window.Window
     *      #configureShell(org.eclipse.swt.widgets.Shell)
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
                        m_dialogPane.loadSettings(new FileInputStream(file));
                    } catch (FileNotFoundException fnfe) {
                        showErrorMessage(fnfe.getMessage());
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
                        m_dialogPane.saveSettings(new FileOutputStream(file),
                                file);
                    } catch (FileNotFoundException fnfe) {
                        showErrorMessage(fnfe.getMessage());
                    } catch (InvalidSettingsException ise) {
                        showErrorMessage("Invalid Settings\n"
                                + ise.getMessage());
                    } catch (IOException ioe) {
                        showErrorMessage(ioe.getMessage());
                    }
                }
            }
        });

    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#
     *      createDialogArea(org.eclipse.swt.widgets.Composite)
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
        // setMessage("Please change the settings below in order to configure"
        // + " the '" + m_nodeContainer.getNodeName() + "' node.");

        // create the dialogs' panel and pass it to the SWT wrapper composite
        getShell().setText(m_dialogPane.getTitle());

        JPanel p = m_dialogPane.getPanel();
        m_wrapper = new Panel2CompositeWrapper(m_container, p, SWT.EMBEDDED);
        m_wrapper.setLayoutData(new GridData(GridData.FILL_BOTH));

        // !!!!!!!!!!!!!!!!!! IMPORTANT... TODO. THINK ABOUT !!!!!!!!!!!!!!!!!!!
        // Here a key listener is added to the display for enter and esc
        // There are several problems with that.
        // 1. The key listener must be removed. Otherwise the dialog is indirect
        // ly registered in the display and can not be garbage collected
        // 2. The key listener was active (before it was removed with this work
        // around) and has affected on some strage way the Enter key once the
        // NewPictureChooser was executed????!!!!????
        // 3. Normally the key listener should be registered at the dialog
        // At the moment no posibility is seen to do that.
        // => The workaround is to de-register the listener once the ok button
        // is pressed. (only possible and usefull, as the dialog is modal and
        // therefore nothing else is effected by pressing enter
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // (tg) finally we decided to remove the key listener, since child
        // dialogs closed theirs parents on these events.
//        m_listener = new Listener() {
//            public void handleEvent(final Event event) {
//                if (event.keyCode == SWT.CR) { // enter
//                    doOK(new SelectionEvent(event));
//                } else if (event.keyCode == SWT.ESC) { // escape
//                    doCancel();
//                }
//            }
//        };
//
//        Display.getCurrent().addFilter(SWT.KeyDown, m_listener);

        return area;
    }

//    private void doCancel() {
//        buttonPressed(IDialogConstants.CANCEL_ID);
//    }

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
     * @see org.eclipse.jface.dialogs.Dialog
     *      #createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        // WORKAROUND !! We can't use IDialogConstants.OK_ID here, as this
        // always closes the dialog, regardless if the settings couldn't be
        // applied.
        Button btnOK = createButton(parent, IDialogConstants.NEXT_ID,
                IDialogConstants.OK_LABEL, true);
        Button btnApply = createButton(parent, IDialogConstants.FINISH_ID,
                "Apply", false);
        Button btnCancel = createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);

        // Register listeneres that notify the content object, whicch
        // in turn notify the dialog about the particular event.
        // event.doit = false cancels the SWT selection event, so that the
        // dialog is not closed on errors.
        btnOK.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                doOK(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                this.widgetDefaultSelected(e);
            }
        });

        btnApply.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                doApply(e);
            }
        });

        btnCancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {

            }
        });
    }

    private void doOK(final SelectionEvent e) {
        // simulate doApply
        if (doApply(e)) {
            buttonPressed(IDialogConstants.OK_ID);
        }
    }

    private boolean doApply(final SelectionEvent e) {
        boolean result = false;
        try {
            // if the settings are equal and the node is executed
            // to the previous settings inform the user but do nothing
            // (no reset)
            if (m_dialogPane.isModelAndDialogSettingsEqual()
                    && m_nodeContainer.isExecuted()) {
                informNothingChanged();
                result = true;
            } else if (confirmApply()) {
                m_nodeContainer.applyDialogSettings();
                e.doit = true;
                result = true;
            } else {
                e.doit = false;
            }
        } catch (InvalidSettingsException ise) {
            e.doit = false;
            showErrorMessage("Invalid Settings\n" + ise.getMessage());
        } catch (WorkflowInExecutionException ex) {
            e.doit = false;
            showErrorMessage("You cannot apply node settings if the workflow"
                    + " is executing. Please stop execution or wait until all"
                    + " nodes have been finished.");            
        } catch (Exception exc) {
            e.doit = false;
            showErrorMessage(exc.getClass().getSimpleName() + ": "
                    + exc.getMessage());
        }
        return result;
    }

    /**
     * Shows the latest error message of the dialog in a MessageBox.
     * 
     * @param message The error string.
     */
    protected void showErrorMessage(final String message) {
        MessageBox box = new MessageBox(getShell(), SWT.ICON_ERROR);
        box.setText("Error");
        box.setMessage(message != null ? message : "(no message)");
        box.open();
    }

    /**
     * Show confirm dialog before applying settings.
     * 
     * @return <code>true</code> if the settings should be applied
     */
    protected boolean confirmApply() {

        // no confirm dialog neccessary, if the node was not executed before
        if (!m_nodeContainer.isExecuted()) {
            return true;
        }
        // If the settings are invalid, we don't want to show our dialog here.
        try {
            m_dialogPane.validateSettings();
        } catch (InvalidSettingsException e) {
            return true;
        }

        MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(),
                SWT.ICON_WARNING | SWT.YES | SWT.NO | SWT.CANCEL);
        mb.setText("Confirm reset...");
        mb.setMessage("Warning, reset node(s)!\n"
                + "New settings will be applied to this\n"
                + "and all connected nodes. Continue?");
        return (mb.open() == SWT.YES);
    }

    /**
     * Show an information dialog that the settings were not changed and
     * therefore the settings are not reset (node stays executed). 
     */
    protected void informNothingChanged() {

        // no dialog neccessary, if the node was not executed before
        if (!m_nodeContainer.isExecuted()) {
            return;
        }
        // If the settings are invalid, we don't want to show our dialog here.
        try {
            m_dialogPane.validateSettings();
        } catch (InvalidSettingsException e) {
            return;
        }

        MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(),
                SWT.ICON_INFORMATION | SWT.OK);
        mb.setText("Settings were not changed.");
        mb.setMessage("The settings were not changed. "
                + "The node will not be reset.");
        mb.open();
    }

    /**
     * @see org.eclipse.jface.window.Window#close()
     */
    @Override
    public boolean close() {
//        Display.getCurrent().removeFilter(SWT.KeyDown, m_listener);
        return super.close();
    }

    // add border width to final dialog size
    private static final int EXTRA_WIDTH  = 10;
    // remove height for menu and title
    private static final int EXTRA_HEIGHT = -40;

    /**
     * This calculates the initial size of the dialog. As the wrapped AWT-Panel
     * ("NodeDialogPane") sometimes just won't return any useful preferred sizes
     * this is kinda tricky workaround :-(
     * 
     * @see org.eclipse.jface.window.Window#getInitialSize()
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
