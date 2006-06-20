/* Created on Jun 20, 2006 1:28:07 PM by thor
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
package de.unikn.knime.workbench.editor2.actions.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.unikn.knime.core.node.NodeProgressListener;
import de.unikn.knime.core.node.NodeProgressMonitor;
import de.unikn.knime.core.node.NodeStateListener;
import de.unikn.knime.core.node.NodeStatus;
import de.unikn.knime.core.node.workflow.NodeContainer;

/**
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class DummyNodeJob extends Job implements NodeProgressListener,
    NodeStateListener {
    private final NodeProgressMonitor m_nodeMonitor;
    private final NodeContainer m_node;
    private IProgressMonitor m_eclipseMonitor;
    private volatile boolean m_finished;
    
    public DummyNodeJob(final String name, final NodeProgressMonitor monitor,
            final NodeContainer node) {
        super(name);
        m_nodeMonitor = monitor;
        m_node = node;
        m_node.addListener(this);
        
        setPriority(LONG);
        
    }

    /**
     * @see org.eclipse.core.runtime.jobs.Job
     *  #run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        m_eclipseMonitor = monitor;
        m_nodeMonitor.addProgressListener(this);        
        m_eclipseMonitor.beginTask("Executing", 100);
        
        try {
            while (!m_finished) {
                if (m_eclipseMonitor.isCanceled()) {
                    m_nodeMonitor.setExecuteCanceled();
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    // nothing to do here
                }
            }
        } finally {
            m_nodeMonitor.removeProgressListener(this);
            m_node.removeListener(this);
        }
        
        return Status.OK_STATUS;
    }

    /**
     * @see de.unikn.knime.core.node.NodeProgressListener
     *  #progressChanged(double, java.lang.String)
     */
    public void progressChanged(final double progress, final String message) {
        int worked = (int)Math.round(
                Math.max(0, Math.min(progress * 100, 100)));

        m_eclipseMonitor.worked(worked);
        m_eclipseMonitor.subTask(message == null ? "" : message);        
    }

    /**
     * @see de.unikn.knime.core.node.NodeStateListener
     *  #stateChanged(de.unikn.knime.core.node.NodeStatus, int)
     */
    public void stateChanged(final NodeStatus state, final int id) {
        if (state instanceof NodeStatus.EndExecute) {
            m_finished = true;
        }
    }
}
