/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *   Dec 17, 2005 (wiswedel): created
 */
package org.knime.core.node;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import org.knime.core.node.workflow.NodeProgress;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;

/**
 * A dialog that contains a progress bar, a label with a message, and a cancel
 * button. It serves as view and controller of a
 * <code>DefaultNodeProgressMonitor</code>. If you have some long-lasting
 * task to do in your view (for instance, write a big file in current view
 * content) and your task is cancelable, you can write code like this:
 *
 * <pre>
 * DefaultNodeProgressMonitor progMon = new DefaultNodeProgressMonitor();
 * ExecutionMonitor execMon = new ExecutionMonitor(progMon);
 * DefaultNodeProgressMonitorView progView = new DefaultNodeProgressMonitorView(
 *         frame, progMon);
 * Thread t = WorkerThread(param, execMon);
 * t.start();
 * progView.pack();
 * progView.setVisible(true);
 * </pre>
 *
 * This view extends JDialog in order to make it modal (view interaction is not
 * permitted by default). Of course, you can change that by calling the
 * <code>#setModal(boolean)</code> method.
 * <p>
 * Please also not: It's your job to dispose this view once the task is done.
 * Only if cancel has been pressed, the view will dispose automatically.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class NodeProgressMonitorView extends JDialog implements
        NodeProgressListener {

    private final JPanel m_containerPanel;

    private final NodeProgressMonitor m_monitor;

    private final JProgressBar m_progressBar;

    private final JLabel m_label;

    private final JButton m_cancelButton;

    /**
     * Constructor that takes the underlying model <code>mon</code> and a
     * frame, which is the parent (or the frame to be modal to).
     *
     * @param parent The frame from which the dialog is displayed.
     * @param mon The model.
     * @throws NullPointerException If <code>mon</code> is <code>null</code>.
     */
    public NodeProgressMonitorView(final Frame parent,
            final NodeProgressMonitor mon) {
        super(parent, false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        m_monitor = mon;
        m_monitor.addProgressListener(this);
        m_label = new JLabel();
        m_label.setHorizontalAlignment(SwingConstants.CENTER);
        m_progressBar = new JProgressBar(0, 100);
        m_progressBar.setStringPainted(true);
        m_cancelButton = new JButton("Cancel");
        m_cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onPressCancel();
            }
        });
        m_containerPanel = new JPanel(new GridLayout(0, 1));
        JPanel flowPanel1 = new JPanel(new FlowLayout());
        flowPanel1.add(m_progressBar);
        JPanel flowPanel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        flowPanel2.add(m_cancelButton);
        m_containerPanel.add(flowPanel1);
        m_containerPanel.add(m_label);
        m_containerPanel.add(flowPanel2);
        getContentPane().add(m_containerPanel);
        Dimension dimension = new Dimension(300, 130);
        setMinimumSize(dimension);
        setPreferredSize(dimension);
        setSize(dimension);
    }

    /**
     * Interface method that's called by the underlying model.
     * @param pe node progress object
     * @see NodeProgressListener#progressChanged(NodeProgressEvent)
     */
    public void progressChanged(final NodeProgress pe) {
        if (pe.hasProgress()) {
            double progress = pe.getProgress().doubleValue();
            int val = Math.max(0, Math.min(100,
                    (int)Math.round(100 * progress)));
            m_progressBar.setValue(val);
        }
        if (pe.hasMessage()) {
            m_label.setText(pe.getMessage());
        } else {
            m_label.setText("");
        }
    }

    /**
     * Interface method that's called by the underlying model.
     *
     * {@inheritDoc}
     */
    public void progressChanged(final NodeProgressEvent pe) {
        if (pe.getNodeProgress().hasProgress()) {
            double progress = pe.getNodeProgress().getProgress().doubleValue();
            int val = Math.max(0, Math.min(100,
                    (int)Math.round(100 * progress)));
            m_progressBar.setValue(val);
        }
        if (pe.getNodeProgress().hasMessage()) {
            m_label.setText(pe.getNodeProgress().getMessage());
        } else {
            m_label.setText("");
        }
    }


    /**
     * If you know that your task doesn't report progess, you can hide the
     * progress bar by calling this method.
     *
     * @param showIt If the progress should be shown.
     */
    public void setShowProgress(final boolean showIt) {
        m_progressBar.setVisible(showIt);
        m_containerPanel.validate();
    }

    /**
     * If you know that your task doesn't use progress message, you can hide
     * the label by calling this method.
     *
     * @param showIt If the label should be shown.
     */
    public void setShowLabel(final boolean showIt) {
        m_label.setVisible(showIt);
        m_containerPanel.validate();
    }

    /**
     * Called when cancel button is pressed. It will set the cancel flag in the
     * underlying <code>DefaultNodeProgressMonitor</code> and call
     * <code>dispose</code>. Intended for overriding.
     *
     * @see DefaultNodeProgressMonitor#setExecuteCanceled()
     */
    protected void onPressCancel() {
        m_monitor.setExecuteCanceled();
        dispose();
    }

    /**
     * Reference to the underlying progress monitor.
     *
     * @return The progress monitor passed in the constructor.
     */
    public NodeProgressMonitor getMonitor() {
        return m_monitor;
    }

}
