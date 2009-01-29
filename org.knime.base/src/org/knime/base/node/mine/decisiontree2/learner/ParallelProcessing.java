/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * ---------------------------------------------------------------------
 *
 * History
 *   26.03.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

/**
 * Holds information for the parallel processing and is also used for
 * synchronization stuff.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class ParallelProcessing {

    private int m_maxNumberThreads;

    private int m_currentThreadsInUse;

    /**
     * Constructor.
     *
     * @param numberThreads the number of threads to use at most in parallel
     */
    public ParallelProcessing(final int numberThreads) {
        m_maxNumberThreads = numberThreads;
        m_currentThreadsInUse = 1;
    }

    /**
     * Sets the number of parallel threads to use.
     *
     * @param numberThreads the number of threads to use in parallel
     */
    public void setNumberThreads(final int numberThreads) {
        m_maxNumberThreads = numberThreads;
    }

    /**
     * Returns the number of threads currently in use.
     *
     * @return the number of threads currently in use
     */
    public int getCurrentThreadsInUse() {
        return m_currentThreadsInUse;
    }

    /**
     * Increments the current number of threads in use by 1.
     *
     */
    public void incrementNumThreads() {
        m_currentThreadsInUse++;
    }

    /**
     * Decrements the current number of threads in use by 1.
     *
     */
    public void decrementNumThreads() {
        m_currentThreadsInUse--;
    }

    /**
     * Method invoked to manage available thread capacity. In case there are not
     * enough threads the invocation waits until a thread gets free.
     *
     */
    public synchronized void isThreadAvailableBlocking() {
        if (m_currentThreadsInUse < m_maxNumberThreads) {
            // do nothing
        } else {
            try {
                this.wait();
            } catch (Exception e) {
                // do nothing
            }
        }
        m_currentThreadsInUse++;
        return;
    }

    /**
     * Method invoked to manage available thread capacity. Returns true if there
     * was an available thread. Note: invoking this method and getting true
     * causes the thread counter to be incremented!! Non blocking!!
     *
     * @return whether a thread is available
     */
    public synchronized boolean isThreadAvailable() {
        if (m_currentThreadsInUse < m_maxNumberThreads) {
            m_currentThreadsInUse++;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Once a thread which synchronizes on this object has finished, it must
     * invoke this method to allow other threads to continue.
     */
    public synchronized void threadTaskFinished() {
        m_currentThreadsInUse--;
        notify();
    }

    /**
     * @return the maxNumberThreads
     */
    public int getMaxNumberThreads() {
        return m_maxNumberThreads;
    }

    /**
     * Sets the number of current threads to 1. Necessary if for example the
     * execution was canceled and the threads could not finish and decrement the
     * counter.
     *
     */
    public void reset() {
        m_currentThreadsInUse = 1;
    }
}
