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
 */
package de.unikn.knime.core.node;

import static javax.swing.JFileChooser.APPROVE_OPTION;

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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;

/**
 * The standard node dialog used to display the node dialog pane.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class NodeDialog {

    /** The underlying dialog's pane. */
    private final NodeDialogPane m_dialogPane;

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
     * @param pane This dialog's underlying pane.
     */
    NodeDialog(final NodeDialogPane pane) {
        // keep node dialog pane and init this dialog
        m_dialogPane = pane;
        m_dialog = initDialog(m_dialogPane.getTitle());

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

        // create: Apply button adn actions
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

        // create: Canel button adn actions
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
                    if (jfc.showOpenDialog(m_dialog) == APPROVE_OPTION) {
                        File file = jfc.getSelectedFile();
                        InputStream is = new FileInputStream(file);
                        m_dialogPane.loadSettings(is);
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
                    if (jfc.showSaveDialog(m_dialog) == APPROVE_OPTION) {
                        File file = jfc.getSelectedFile();
                        OutputStream os = new FileOutputStream(file);
                        m_dialogPane.saveSettings(os, file.getName());
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
        
        JComponent root = dialog.getRootPane();

        // key stroke on ENTER
        KeyStroke entKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
        final Action entAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                onOK(e);
            }
        };
        root.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(entKey, "ENTER");
        root.getActionMap().put("ENTER", entAction);
        
        // key stroke on ESCAPE
        KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        final Action escAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                onCancel(e);
            }
        };
        root.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(escKey, "ESCAPE");
        root.getActionMap().put("ESCAPE", escAction);
        
        return dialog;
    }

    /**
     * Opens the dialog: packed, centered, and visible true.
     */
    void openDialog() {
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
            m_dialogPane.validateSettings();
            // if the node is executed
            if (m_dialogPane.isNodeExecuted()) {
                // show option pane with reset warning
                int r = JOptionPane.showConfirmDialog(m_dialog,
                        "Node is executed. Do you want to reset it\n"
                                + "and apply the current settings.", m_dialog
                                .getTitle()
                                + ": Warning", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                // if reset can be performed
                if (r == JOptionPane.OK_OPTION) {
                    m_dialogPane.doApply();
                    return true;
                } else {
                    return false;
                }
            }
            m_dialogPane.doApply();
            return true;
        } catch (InvalidSettingsException ise) {
            // show option pane with includes the message from the exception
            JOptionPane.showConfirmDialog(m_dialog, ise.getMessage(), m_dialog
                    .getTitle()
                    + ": Invalid Settings", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE);
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
    }

    /**
     * Invoked if the dialog is going to open. Method need to be overridden, if
     * this event should be evaluated.
     * 
     * @param event The event which invoked to open operation.
     */
    protected void onOpen(final AWTEvent event) {
        assert (event != null);
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
            // if no errors occured, i.e. apply() was succesful
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
