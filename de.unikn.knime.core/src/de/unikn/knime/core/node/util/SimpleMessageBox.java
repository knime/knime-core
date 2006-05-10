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
 *   11.05.2005 (sieb): created
 */
package de.unikn.knime.core.node.util;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Used for displaying a quick message. Has Just an Ok Button
 * 
 * @author Christoph Sieb (University of Konstanz)
 */
public class SimpleMessageBox extends JDialog implements ActionListener,
        Runnable {
    
    private String m_message;

    private JButton m_pbClose;

    private JLabel m_taMessage;

    /**
     * Creates a simple message dialog with a close button.
     * 
     * @param message the message to show
     * @param parent the parent frame
     */
    public SimpleMessageBox(final String message, final Frame parent) {
        super(parent, true);
        
        int xLocation = (int)(parent.getX() + parent.getWidth() / 2); 
        int yLocation = (int)(parent.getY() + parent.getHeight() / 2);
        setLocation(xLocation, yLocation);
        m_message = message;
        initialize();
        setTitle("Information");
        m_taMessage.setText(m_message);
        pack();
    }

    /**
     * @see java.awt.event.ActionListener#
     * actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(final ActionEvent e) {
        pbCloseClicked();
    }

    private JPanel buttonPanel() {
        
        JPanel p = new JPanel();
        m_pbClose = new JButton("Cancel");
        m_pbClose.addActionListener(this);
        p.add(m_pbClose);
        return p;
    }

    private void close() {
        
        setVisible(false);
        dispose();
    }

    private void initialize() {
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        m_taMessage = new JLabel();
        c.add("Center", m_taMessage);
        c.add("South", buttonPanel());
    }

    private void pbCloseClicked() {
        
        close();
    }

    /**
     * This prevents the caller from blocking on ask(), which if this class is
     * used on an awt event thread would cause a deadlock.
     */
    public void run() {
        Toolkit.getDefaultToolkit().beep();
        setVisible(true);
    }
}
