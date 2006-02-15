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
 *   10.08.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.filereader;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class RecentFilesDialog extends JDialog {

    private int m_result;

    private final JList m_list;

    /**
     * Creates a new dialog with a list of files to allowing the user to select
     * one or to cancel.
     * 
     * @param parent the owner of this dialog
     * @param recentFiles a non-null, not-emtpy array of recent file names.
     */
    public RecentFilesDialog(final Frame parent, final String[] recentFiles) {

        super(parent, "Data File History Selection", /* modal= */true);

        if ((recentFiles == null) || (recentFiles.length == 0)) {
            throw new IllegalArgumentException("Can't create history list from"
                    + "from null or no strings");
        }
        // instantiate the components of the dialog
        // group components nicely - without those buttons
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Data File History"));
        listPanel.add(new JLabel("Select an ASCII file location from"));
        listPanel.add(new JLabel("the list of recently accessed data files."));
        listPanel.add(Box.createVerticalStrut(7));
        m_list = new JList(createSizedStrings(recentFiles, 100));
        m_list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onOK();
                }
            }
        });
        listPanel.add(new JScrollPane(m_list));

        // the OK and Cancel button
        JPanel control = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        // add action listener
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onOK();
            }
        });
        JButton cancel = new JButton("Cancel");
        // add action listener
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                onCancel();
            }
        });
        control.add(ok);
        control.add(cancel);

        // add dialog and control panel to the content pane
        Container cont = getContentPane();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));
        cont.add(listPanel);
        cont.add(Box.createVerticalStrut(3));
        cont.add(control);

        setModal(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    }

    /*
     * builds strings with the following pattern: if size is smaller than 30,
     * return the last 30 chars in the string; if the size is larger than 30:
     * return the first 12 chars + ... + chars from the end. Size more than 55:
     * the first 28 + ... + rest from the end.
     *  
     */
    private String[] createSizedStrings(final String[] str, final int size) {
        assert str != null;
        assert str.length > 0;
        String[] result = new String[str.length];
        for (int s = 0; s < str.length; s++) {
            if (str[s].length() <= size) {
                // short enough - return it unchanged
                result[s] = str[s];
                continue;
            }
            if (size <= 30) {
                result[s] = "..." + str[s].substring(str[s].length() - size + 3,
                        str[s].length());
            } else if (size <= 55) {
                result[s] = str[s].substring(0, 12)
                + "..."
                + str[s].subSequence(str[s].length() - size + 15,
                        str[s].length());
            } else {
                result[s] = str[s].substring(0, 28)
                + "..."
                + str[s].subSequence(str[s].length() - size + 31,
                        str[s].length());
            }

        }
        return result;
    }

    /**
     * open the dialog with the file list passed to the constructor. The method
     * will not return until the dialog has been closed by the user.
     * 
     * @return the index of the file selected (which is the index of the array
     *         passed to the constructor), or -1 if the user canceled.
     * 
     *  
     */
    public int openDialog() {

        pack();
        centerDialog();
        m_list.setSelectedIndex(0);

        setVisible(true);
        /* ---- won't come back before dialog is disposed -------- */
        /* ---- on Ok we tranfer the settings into the m_result -- */
        return m_result;
    }

    /**
     * called when user presses the ok button.
     */
    void onOK() {
        m_result = m_list.getSelectedIndex();
        shutDown();
    }

    /**
     * called when user presses the cancel button or closes the window.
     */
    void onCancel() {
        m_result = -1;
        shutDown();
    }

    /* blows away the dialog */
    private void shutDown() {
        setVisible(false);
        dispose();
    }

    /**
     * Sets this dialog in the center of the screen observing the current screen
     * size.
     */
    private void centerDialog() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = getSize();
        setBounds(Math.max(0, (screenSize.width - size.width) / 2), Math.max(0,
                (screenSize.height - size.height) / 2), Math.min(
                screenSize.width, size.width), Math.min(screenSize.height,
                size.height));
    }

}
