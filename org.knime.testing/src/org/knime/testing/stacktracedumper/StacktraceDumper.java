/*
 * ------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   21.01.2008 (ohl): created
 */
package org.knime.testing.stacktracedumper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.MutableInteger;

/**
 * Collects stack traces and writes them into a file. It launches two thread (in
 * its constructor) that periodically create snapshots of the stacks of all
 * threads and continuously write those snapshots into a file (specified as an
 * argument to the constructor). <br />
 * This goes together with the {@link StackDumpAnalyzer} class which uses the
 * file written out by this class and creates an HTML result file from it.<br />
 * The collecting and analyzing of stack traces of course only gives you a rough
 * idea of where time is spent in the code. But it is very easy to use and comes
 * with only a small performance hit. Sometimes it is good enough to find where
 * the bottleneck sits.<p>
 * How to use this:<br />
 * Instantiate this class, providing a file name (in which the stack traces are
 * written into) and an interval (in milli seconds). After that stack traces
 * are collected after each interval and written to file (in two independent
 * threads). If you wish to stop stack dumping, call stopDumping() on this
 * instance. Use the {@link StackDumpAnalyzer} to analyze the output file (or
 * its main method).
 *
 * @author ohl, University of Konstanz
 */
public final class StacktraceDumper {

    /**
     * A pattern for the analyzer.
     */
    static final String DUMPSTART = " --- Dump ";

    /**
     * A pattern for the analyzer.
     */
    static final String THREADSTART = " --- Thread ";

    /**
     * A pattern for the analyzer.
     */
    static final String THREADEND = " --- End Thread ";

    /**
     * A pattern for the analyzer.
     */
    static final String DUMPEND = " --- End Dump ";

    /**
     * A pattern for the analyzer.
     */
    static final String LINEENDTAG = " --- ";

    private static final String NL = System.getProperty("line.separator");

    private final BufferedWriter m_writer;

    private final Dumper m_dumper = new Dumper();

    private final Collector m_collector = new Collector();

    private final Object m_lock = new Object();

    private boolean m_cancel = false;

    private long m_interval;

    /*
     * these variables implement a ring buffer for the stacktraces still to be
     * dumped into the file.
     */
    private static final int BUFFER_SIZE = 10240;

    @SuppressWarnings("unchecked")
    private final Map<Thread, StackTraceElement[]>[] m_snapshot =
            new Map[BUFFER_SIZE];

    private final long[] m_timestamp = new long[BUFFER_SIZE];

    private final AtomicInteger m_first = new AtomicInteger(0);

    private final AtomicInteger m_last = new AtomicInteger(0);

    private final MutableInteger m_snapCnt = new MutableInteger(0);

    private final MutableInteger m_dumpCnt = new MutableInteger(0);

    /**
     *
     * @param dumpFile file to write stack traces to
     * @param interval the number of milliseconds between each stack m_snapshot
     */
    public StacktraceDumper(final File dumpFile, final int interval) {

        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(dumpFile));
        } catch (IOException ioe) {
            NodeLogger.getLogger(StacktraceDumper.class).error(
                    "Can't open dump file. Not writing stack traces.");
            m_writer = null;
            return;
        }

        m_writer = writer;

        m_interval = interval;

        new Thread(m_dumper, "Stacktrace File Writer").start();

        new Thread(m_collector, "Stack Snapshot Collector").start();

        NodeLogger.getLogger(StacktraceDumper.class).info(
                "Starting to write stack traces to file '" + dumpFile + "'");

    }

    /**
     * The writing of stacktraces stops after a call to this method.
     */
    public void stopDumping() {
        m_cancel = true;
        synchronized (m_lock) {
            m_lock.notifyAll();
        }
    }

    private final class Collector implements Runnable {
        /** {@inheritDoc} */
        @Override
        public void run() {
            while (!m_cancel) {

                m_snapshot[m_last.get()] = Thread.getAllStackTraces();
                m_timestamp[m_last.get()] = System.currentTimeMillis();
                m_snapCnt.inc();

                synchronized (m_lock) {
                    m_last.getAndIncrement();
                    // if pointer reached end of buffer, reset it.
                    m_last.compareAndSet(BUFFER_SIZE, 0);

                    if (m_first.get() == m_last.get()) {
                        NodeLogger.getLogger(StacktraceDumper.class).error(
                                "BUFFER OVERRUN: Buy a faster hard "
                                        + "disc, or create a larger buffer"
                                        + " (in class StacktraceDumper)."
                                        + "CANCELING STACKTRACE DUMPING");
                        m_cancel = true;
                        break;
                    }

                    // wake the dumper to write new snapshot to file
                    m_lock.notifyAll();

                    try {
                        m_lock.wait(m_interval);
                    } catch (InterruptedException iee) {
                        // then do the next round
                    }
                }
            }
        }

    }

    private final class Dumper implements Runnable {

        /** {@inheritDoc}  */
        @Override
        public void run() {
            int intFirst;
            int intLast;

            while (!m_cancel) {

                synchronized (m_lock) {
                    intFirst = m_first.get();
                    intLast = m_last.get();
                }

                try {
                    while (intFirst != intLast) {
                        // dump stacktraces to file until buffer is empty
                        dump(intFirst);

                        synchronized (m_lock) {
                            m_first.incrementAndGet();
                            // if pointer reached end of buffer, reset it.
                            m_first.compareAndSet(BUFFER_SIZE, 0);
                            intFirst = m_first.get();
                            intLast = m_last.get();
                        }
                    }

                    synchronized (m_lock) {
                        m_lock.wait();
                    }

                } catch (InterruptedException ie) {
                    // then continue
                } catch (IOException ioe) {
                    NodeLogger.getLogger(StacktraceDumper.class).error(
                            "IO Error while dumping to file. "
                                    + "CANCELING STACKTRACE DUMPING");
                    m_cancel = true;
                    break;

                }
            }

            try {
                m_writer.write("Stacksnapshots captured: " + m_snapCnt + NL);
                m_writer.write("Snapshots written to file: " + m_dumpCnt + NL);
                m_writer.close();
            } catch (IOException ioe) {
                // then don't close it
            }
        }

        private void dump(final int idx) throws IOException {

            m_writer.write(DUMPSTART + m_timestamp[idx] + LINEENDTAG + NL);

            Map<Thread, StackTraceElement[]> map = m_snapshot[idx];
            for (Map.Entry<Thread, StackTraceElement[]> e : map.entrySet()) {
                Thread t = e.getKey();
                StackTraceElement[] s = e.getValue();

                m_writer.write(THREADSTART + t.getName() + LINEENDTAG + NL);
                // start with the "highest" method (like main())
                for (int i = s.length - 1; i >= 0; i--) {
                    StackTraceElement elem = s[i];
                    m_writer.write(elem.getClassName() + "|"
                            + elem.getFileName() + "|" + elem.getLineNumber()
                            + "|" + elem.getMethodName() + NL);
                }

                m_writer.write(THREADEND + LINEENDTAG + NL);
            }

            m_writer.write(DUMPEND + m_timestamp[idx] + LINEENDTAG + NL);
            m_dumpCnt.inc();
        }

    }

}
