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
 */
package org.knime.core.node;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.knime.core.node.workflow.NodeContainer;

/**
 * The standard node dialog used to display the node dialog pane.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class NodeDialog {

    /** The underlying dialog's pane. */
    private final NodeDialogPane m_dialogPane;

    /** The underlying node container. */
    private final NodeContainer m_node;

    /** The hidden dialog. */
    private final JDialog m_dialog;

    /** The ok button which has the focus by default. */
    private final JButton m_ok = new JButton("OK");

    /** Button width. */
    private static final int INIT_BTN_WIDTH = 75;

    /** Button height. */
    private static final int INIT_BTN_HEIGHT = 25;

    /**
     * Creates a new dialog which is used for the stand-alone application in
     * order to view the <code>NodeDialogPane</code>.
     *
     * @param pane this dialog's underlying pane
     * @param node the underlying node
     */
    public NodeDialog(final NodeDialogPane pane, final NodeContainer node) {
        m_node = node;
        // keep node dialog pane and init this dialog
        m_dialogPane = pane;
        
        m_dialog = initDialog("Dialog - " + node.getDisplayLabel());
        
        // init OK and Cancel button
        JPanel control = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // create: OK button and actions
        m_ok.setMnemonic(KeyEvent.VK_O);
        m_ok.setPreferredSize(new Dimension(INIT_BTN_WIDTH, INIT_BTN_HEIGHT));
        // add action listener
        m_ok.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onOK(e);
            }
        });
        control.add(m_ok);

        // create: Apply button and actions
        final JButton apply = new JButton("Apply");
        apply.setMnemonic(KeyEvent.VK_A);
        apply.setPreferredSize(new Dimension(INIT_BTN_WIDTH, INIT_BTN_HEIGHT));
        // add action listener
        apply.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                onApply(event);
            }
        });
        control.add(apply);

        // create: Cancel button and actions
        final JButton cancel = new JButton("Cancel");
        cancel.setMnemonic(KeyEvent.VK_C);
        cancel.setPreferredSize(new Dimension(INIT_BTN_WIDTH, INIT_BTN_HEIGHT));
        // add action listener
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                onCancel(event);
            }
        });
        control.add(cancel);

        // init settings menu
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menu.getPopupMenu().setLightWeightPopupEnabled(false);
        final JFileChooser jfc = new JFileChooser();
        JMenuItem btnLoad = new JMenuItem("Load Settings");
        btnLoad.setMnemonic(KeyEvent.VK_L);
        // add action listener
        btnLoad.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                try {
                    if (jfc.showOpenDialog(m_dialog)
                            == JFileChooser.APPROVE_OPTION) {
                        File file = jfc.getSelectedFile();
                        InputStream is = new FileInputStream(file);
                        m_dialogPane.loadSettingsFrom(is);
                    }
                } catch (FileNotFoundException fnfe) {
                    JOptionPane.showMessageDialog(m_dialog, fnfe.getMessage(),
                                    "Couldn't Load Settings",
                                    JOptionPane.ERROR_MESSAGE);
                } catch (IOException ioe) {
                    JOptionPane.showMessageDialog(m_dialog, ioe.getMessage(),
                                    "Couldn't Load Settings",
                                    JOptionPane.ERROR_MESSAGE);
                } catch (NotConfigurableException ex) {
                    /* should not happen, since the dialog is already open
                     * (and loadSettings successfully completed before)
                     */
                    JOptionPane.showMessageDialog(m_dialog, ex.getMessage(),
                            "Couldn't Load Settings",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        menu.add(btnLoad);

        JMenuItem btnSave = new JMenuItem("Save Settings");
        btnSave.setMnemonic(KeyEvent.VK_S);
        // add action listener
        btnSave.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                try {
                    if (jfc.showSaveDialog(m_dialog)
                            == JFileChooser.APPROVE_OPTION) {
                        File file = jfc.getSelectedFile();
                        OutputStream os = new FileOutputStream(file);
                        m_dialogPane.saveSettingsTo(os);
                    }
                } catch (InvalidSettingsException ise) {
                    JOptionPane.showMessageDialog(m_dialog, "Warning",
                            "Invalid Settings", JOptionPane.WARNING_MESSAGE);
                } catch (FileNotFoundException fnfe) {
                    JOptionPane
                            .showMessageDialog(m_dialog, fnfe.getMessage(),
                                    "Couldn't Save Settings",
                                    JOptionPane.ERROR_MESSAGE);
                } catch (IOException ioe) {
                    JOptionPane
                            .showMessageDialog(m_dialog, ioe.getMessage(),
                                    "Couldn't Save Settings",
                                    JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        menu.add(btnSave);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menu);
        m_dialog.setJMenuBar(menuBar);

        // add dialog and control panel to the content pane
        Container cont = m_dialog.getContentPane();
        cont.setLayout(new GridLayout());
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(m_dialogPane.getPanel(), BorderLayout.CENTER);
        p.add(control, BorderLayout.SOUTH);
        cont.add(p);
    }

    /*
     * Inits the underlying dialog with title and icon.
     */
    private JDialog initDialog(final String title) {
        JFrame dummy = new JFrame();
        if (KNIMEConstants.KNIME16X16 != null) {
            dummy.setIconImage(KNIMEConstants.KNIME16X16.getImage());
        }
        // init underlying dialog
        JDialog dialog = new JDialog(dummy);
        dialog.setTitle(title);
        dialog.setModal(true);
        // dialog.setResizable(false);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            /** Invoked when the window close icon X is pressed. */
            @Override
            public void windowClosed(final WindowEvent e) {
                onClose(e);
            }

            /** Invoked when the window is opened. */
            @Override
            public void windowOpened(final WindowEvent we) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        // invoke open operation
                        onOpen(we);
                    }
                });
            }
        });
        return dialog;
    }

    /**
     * Opens the dialog: packed, centered, and visible true.
     */
    public void openDialog() {
        // pack it
        m_dialog.pack();
        // center it
        centerDialog();
        // show it
        m_dialog.setVisible(true); // triggers WindowEvent 'Opened' which
        // invokes onOpen()
        // ----- ! -----
        // setVisible DOESNOT return until the (modal) dialog is disposed!
    }

    /**
     * Sets this dialog in the center of the screen observing the current screen
     * size.
     */
    private void centerDialog() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = m_dialog.getSize();
        m_dialog.setBounds(Math.max(0, (screenSize.width - size.width) / 2),
                Math.max(0, (screenSize.height - size.height) / 2), Math.min(
                        screenSize.width, size.width), Math.min(
                        screenSize.height, size.height));
    }

    /**
     * Triggered if this dialog's apply button is pressed.
     *
     * @param event The apply button event.
     * @return <code>true</code> if apply was successful, otherwise shows a
     *         warning message in a <code>JOptionPane</code>.
     */
    protected boolean onApply(final AWTEvent event) {
        assert (event != null);
        try {
            // validate settings first
            if (!m_node.areDialogAndNodeSettingsEqual()) {
                // if the node is executed
                if (m_node.getState().equals(NodeContainer.State.EXECUTED)) {
                    // show option pane with reset warning
                    int r = JOptionPane.showConfirmDialog(m_dialog,
                            "Node is executed. Do you want to reset it\n"
                          + "and apply the current settings.",
                              m_dialog.getTitle()
                          + ": Warning", JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    // if reset can be performed
                    if (r == JOptionPane.OK_OPTION) {
                        // try to load dialog settings to the model
                        m_node.applySettingsFromDialog();
                        return true;
                    } else {
                        return false;
                    }
                }
                // try to load dialog settings to the model
                m_node.applySettingsFromDialog();
                return true;
            } else {
                return true; // nothing done - everything ok!
            }
        } catch (InvalidSettingsException ise) {
            JOptionPane.showConfirmDialog(m_dialog, ise.getMessage(), m_dialog
                    .getTitle()
                    + ": Invalid Settings", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            return false;
        } catch (Throwable t) {
            JOptionPane.showConfirmDialog(m_dialog, t.getMessage(), m_dialog
                    .getTitle()
                    + ": Error Applying Settings", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Invoked when the cancel button is pressed.
     *
     * @param event The action event of the cancel button.
     */
    protected void onCancel(final AWTEvent event) {
        assert (event != null);
        // send cancel action to underlying dialog pane
        m_dialogPane.onCancel();
        closeDialog();
    }

    /**
     * Invoked if the dialog is going to be closed. Method need to be
     * overridden, if this event should be evaluated.
     *
     * @param event The event which invokes the close operation.
     */
    protected void onClose(final AWTEvent event) {
        assert (event != null);
        // send close action to underlying dialog pane
        m_dialogPane.onClose();
    }

    /**
     * Invoked if the dialog is going to open. Method need to be overridden, if
     * this event should be evaluated.
     *
     * @param event The event which invoked to open operation.
     */
    protected void onOpen(final AWTEvent event) {
        assert (event != null);
        // send cancel action to underlying dialog pane
        m_dialogPane.onOpen();
    }

    /**
     * Invoked when the ok button is pressed - automatically calls the
     * <code>#onApply(AWTEvent)</code> method which must be provided by the
     * derived class.
     *
     * @param event The action event of the m_ok button.
     */
    protected void onOK(final AWTEvent event) {
        if (onApply(event)) {
            // if no errors occurred, i.e. apply() was successful
            closeDialog();
        }
    }

    /**
     * Closes the dialog: visible false and dispose.
     */
    void closeDialog() {
        // emulate same handling as done by the underlying frame
        m_dialog.setVisible(false);
        m_dialog.dispose(); // triggers the WindowClosed action event
    }

} // NodeDialog
