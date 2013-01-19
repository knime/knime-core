/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
@Deprecated
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
