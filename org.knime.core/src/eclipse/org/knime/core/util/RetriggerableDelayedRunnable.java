/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Apr 4, 2020 (loki): created
 */
package org.knime.core.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class provides the ability to specify a delay, a runnable, and triggers the specified runnable after a delay
 *  length of time has passed without a re-triggering.
 *
 * For example:
 *  0s: construct instance with 3s delay and start the runnable in a Thread instance
 *  1s: --
 *  2s: retrigger constructed instance
 *  3s: --  (instance DOES NOT execute the runnable here)
 *  4s: --
 *  5s: runnable executes the specified runnable
 *
 * @author loki der quaeler
 * @since 4.2
 */
public class RetriggerableDelayedRunnable implements Runnable {
    private static final long POLLING_SLEEP = 32;


    private final Runnable m_runnable;
    private final AtomicLong m_startTime;
    private final AtomicBoolean m_executionHasCommenced;
    private final long m_delayTime;

    /**
     * @param r the runnable to execute after the delay time has passed without a re-triggering
     * @param delay the length of time in milliseconds which must pass without re-triggering before the specified
     *            runnable get executed
     */
    public RetriggerableDelayedRunnable(final Runnable r, final long delay) {
        m_runnable = r;
        m_delayTime = delay;
        m_startTime = new AtomicLong(-1);
        m_executionHasCommenced= new AtomicBoolean(false);
    }

    /**
     * Attempts to retrigger the delay.
     *
     * @return true if we were able to retrigger the delay and false if we were too late and the execution of the
     *              specified runnable has already commenced
     */
    public boolean retrigger() {
        synchronized(m_executionHasCommenced) {
            if (!m_executionHasCommenced.get()) {
                m_startTime.set(System.currentTimeMillis());

                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        m_startTime.set(System.currentTimeMillis());
        while (true) {
            try {
                Thread.sleep(POLLING_SLEEP);
            } catch (final InterruptedException ie) { }

            final boolean trigger;
            synchronized(m_executionHasCommenced) {
                trigger = ((System.currentTimeMillis() - m_startTime.get()) >= m_delayTime);
                if (trigger) {
                    m_executionHasCommenced.set(true);
                }
            }

            if (trigger) {
                m_runnable.run();

                break;
            }
        }
    }
}
