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
 *   Dec 29, 2015 (wiswedel): created
 */
package org.knime.core.node;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

import java.util.function.Function;

import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.Assert;
import org.junit.Test;
import org.knime.core.data.RowKey;
import org.knime.core.node.DefaultNodeProgressMonitor.SubNodeProgressMonitor;
import org.knime.core.node.workflow.NodeProgress;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;
import org.knime.core.util.Pointer;

/**
 * Tests new API introduced via bug 6689:
 * New API: ExecutionMonitor setProgress(Supplier<String> msg) -- possible performance boost in streaming execution
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class DefaultNodeProgressMonitorTest {

    /** math in subnodeprogressmonitor is "complex" -- allow a lot of margin here. */
    private static final double PROG_EPSILON = 1E-3;

    /** Just a lot of incremental numeric progress updates. */
    @Test(timeout=1000L)
    public void testManySmallIncrementsDirect() throws Exception {
        DefaultNodeProgressMonitor m = new DefaultNodeProgressMonitor();
        internalTestManySmallIncrements(m, m);
    }

    /** Just a lot of incremental numeric progress updates on sub progress monitor. */
    @Test(timeout=10000L)
    public void testManySmallIncrementsSubProgress() throws Exception {
        // this has a ridiculous large timeout because the math we do in the implementation
        // of SubnodeProgresMonitor is insane. Not going to change it as we spend days getting it "right" years ago
        DefaultNodeProgressMonitor m = new DefaultNodeProgressMonitor();
        internalTestManySmallIncrements(m, new SubNodeProgressMonitor(m, 1.0));
    }

    /** Just a lot of incremental numeric progress updates. */
    private void internalTestManySmallIncrements(final NodeProgressMonitor toMonitor,
        final NodeProgressMonitor toControl) throws Exception {
        final Pointer<NodeProgress> progressPointer = new Pointer<>();
        final Function<NodeProgress, Boolean> isLastEventFunction = p -> p.getProgress() >= 1.0 - PROG_EPSILON;
        NodeProgressListener l = createListener(progressPointer, isLastEventFunction);
        toMonitor.addProgressListener(l);
        try {
            int parts = 10000000;
            for (int i = 0; i < parts; i++) {
                toControl.setProgress((i + 1) / (double)parts);
            }
            synchronized (isLastEventFunction) {
                isLastEventFunction.wait(1000);
            }
            assertThat(progressPointer.get().getProgress(), is(closeTo(1.0, PROG_EPSILON)));
        } finally {
            toMonitor.removeProgressListener(l);
        }
    }

    /** Calls internal test message for {@link DefaultNodeProgressMonitor}. */
    @Test(timeout=1000L)
    public void testManyMessageEventsDirect() throws Exception {
        final DefaultNodeProgressMonitor progMon = new DefaultNodeProgressMonitor();
        internalTestManyMessageEvents(progMon, progMon);
    }

    /** Calls internal test message for {@link SubNodeProgressMonitor}. */
    @Test(timeout=1000L)
    public void testManyMessageEventsSubProgress() throws Exception {
        final DefaultNodeProgressMonitor progMon = new DefaultNodeProgressMonitor();
        internalTestManyMessageEvents(progMon, new SubNodeProgressMonitor(progMon, 1.0));
    }

    /** A lot of incremental numeric progress updates + many message events
     * Previously, this took significantly longer due to expensive string construction. */
    private void internalTestManyMessageEvents(final NodeProgressMonitor toMonitor,
        final NodeProgressMonitor toControl) throws Exception {
        final int parts = 1000000;
        final MutableLong stringComposeCounter = new MutableLong();
        Function<Integer, String> msgFct = (index) -> {
            stringComposeCounter.increment();
            return "Row " + index + " (Row \"" + RowKey.createRowKey((long)index) + "\")";
        };

        final Pointer<NodeProgress> progressPointer = new Pointer<>();
        String lastExpectedMsg = msgFct.apply(parts);
        final Function<NodeProgress, Boolean> isLastEventFunction = p -> p.getMessage().equals(lastExpectedMsg);
        NodeProgressListener l = createListener(progressPointer, isLastEventFunction);
        toMonitor.addProgressListener(l);
        try {
            for (int i = 1; i < parts + 1; i++) {
                final int index = i;
                // if this line is replaced by a direct string composition this takes an order of magnitude longer
                toControl.setProgress(i / (double)parts, () -> msgFct.apply(index));
            }
            synchronized (isLastEventFunction) {
                isLastEventFunction.wait(500);
            }
            assertThat(progressPointer.get().getProgress(), is(closeTo(1.0, PROG_EPSILON)));
            assertThat(progressPointer.get().getMessage(), is(equalTo(lastExpectedMsg)));
            // the lazy string creation should only be called 4 times a second at most,
            // it must be at least two - one for the reference string creation and one during an event
            Assert.assertThat(stringComposeCounter.getValue(), is(allOf(greaterThanOrEqualTo(2L), lessThanOrEqualTo(5L))));
        } finally {
            toMonitor.removeProgressListener(l);
        }
    }

    private static NodeProgressListener createListener(final Pointer<NodeProgress> progressPointer,
        final Function<NodeProgress, Boolean> notificationFunction) {
        return new NodeProgressListener() {
            @Override
            public void progressChanged(final NodeProgressEvent pe) {
                NodeProgress prog = pe.getNodeProgress();
                progressPointer.set(prog);
                if (notificationFunction.apply(prog)) {
                    synchronized (notificationFunction) {
                        notificationFunction.notifyAll();
                    }
                }
            }
        };
    }

}
