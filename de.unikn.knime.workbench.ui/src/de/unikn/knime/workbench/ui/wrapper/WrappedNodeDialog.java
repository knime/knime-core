/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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

import java.awt.Dimension;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

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
import de.unikn.knime.core.node.workflow.NodeContainer;
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

    private NodeDialogPane m_dialogPane;

    private Menu m_menuBar;

    /**
     * Creates the (application modal) dialog for a given node.
     * 
     * We'll set SHELL_TRIM - style to keep this dialog resizable. This is
     * needed because of the odd "preferredSize" behavior (@see
     * WrappedNodeDialog#getInitialSize())
     * 
     * @param parentShell The parent shell
     * @param nodeContainer The node.
     */
    public WrappedNodeDialog(final Shell parentShell,
            final NodeContainer nodeContainer) {
        super(parentShell);
        this.setShellStyle(SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
        m_nodeContainer = nodeContainer;
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
        m_dialogPane = m_nodeContainer.getDialogPane();
        getShell().setText(m_dialogPane.getTitle());

        JPanel p = m_dialogPane.getPanel();
        m_wrapper = new Panel2CompositeWrapper(m_container, p, SWT.EMBEDDED);
        m_wrapper.setLayoutData(new GridData(GridData.FILL_BOTH));

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
     * @see org.eclipse.jface.dialogs.Dialog
     *      #createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
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
            public void widgetDefaultSelected(final SelectionEvent e) {
                try {
                    if (confirmApply()) {
                        m_dialogPane.doApply();
                        e.doit = true;
                        close();
                    } else {
                        e.doit = false;
                    }
                } catch (InvalidSettingsException ise) {
                    e.doit = false;
                    showErrorMessage("Invalid settings: " + ise.getMessage());
                } catch (Exception exc) {
                    e.doit = false;
                    showErrorMessage(exc.getClass().getSimpleName() 
                            + ": " + exc.getMessage());
                }
                // TODO TG: forward to your dialog
            }

            public void widgetSelected(final SelectionEvent e) {
                this.widgetDefaultSelected(e);
            }
        });
        btnApply.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(final SelectionEvent e) {
                try {
                    if (confirmApply()) {
                        m_dialogPane.doApply();
                        e.doit = true;
                    } else {
                        e.doit = false;
                    }
                } catch (InvalidSettingsException ise) {
                    e.doit = false;
                    showErrorMessage("Invalid Settings\n" + ise.getMessage());
                } catch (Exception exc) {
                    e.doit = false;
                    showErrorMessage(exc.getMessage());
                }
                // TODO TG: forward to your dialog

            }
        });
        btnCancel.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(final SelectionEvent e) {
                // TODO TG: forward to your dialog
                // m_dialogPane.doCancel();
            }
        });

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
     * This calculates the initial size of the dialog. As the wrapped AWT-Panel
     * ("NodeDialogPane") sometimes just won't return any useful preferred sizes
     * this is kinda tricky workaround :-(
     * 
     * @see org.eclipse.jface.window.Window#getInitialSize()
     */
    protected Point getInitialSize() {

        // First we ask the dialog pane for the preferred size
        JPanel panel = m_dialogPane.getPanel();
        panel.validate();
        int width = panel.getPreferredSize().width;
        int height = panel.getPreferredSize().height;

        // we need to make sure that we have at least enough space for
        // the button bar (+ some extra space)
        width = Math.max(this.getButtonBar().computeSize(SWT.DEFAULT,
                SWT.DEFAULT).x + 30, width);
        // TODO FIXME: Some panes are very, very nasty and simply won't
        // return useful preferred sizes - so we give this a minimum height of
        // 150 pixels :-(
        height = Math.max(height, 150);

        // set the size of the container composite
        m_container.setSize(width, height);

        // The wrapped dialog pane (AWT Panel) should grab all the available
        // space
        panel.setSize(new Dimension(width, height));

        // The dialog needs to add the height of the insets
        // (title area, button bar)
        return new Point(width, height
                + this.getButtonBar().computeSize(SWT.DEFAULT, SWT.DEFAULT).y
                /*
                 * + this.getTitleImageLabel().computeSize(SWT.DEFAULT,
                 * SWT.DEFAULT).y
                 */ + 100);
    }

}
