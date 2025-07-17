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
 *   10 Sep 2016 (Gabor Bakos): created
 */
package org.knime.core.data.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.OptionalLong;

import org.apache.commons.io.input.CountingInputStream;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.CheckUtils;

/**
 * A cancellable input stream that can report its progress on reading.
 *
 * @author Gabor Bakos
 * @since 3.4
 */
public class CancellableReportingInputStream extends CountingInputStream {

    private final ExecutionMonitor m_exec;
    private final OptionalLong m_streamLengthIfKnown;

    /**
     * @param proxy The {@link InputStream} to wrap.
     * @param exec An {@link ExecutionMonitor} to use, not null
     */
    public CancellableReportingInputStream(final InputStream proxy, final ExecutionMonitor exec) {
        this(proxy, exec, -1L);
    }

    /**
     * @param proxy The {@link InputStream} to wrap.
     * @param exec An {@link ExecutionMonitor} to use, not null
     * @param streamLengthIfKnown The length of the stream or a negative number to indicate that it's unknown
     */
    public CancellableReportingInputStream(final InputStream proxy, final ExecutionMonitor exec,
        final long streamLengthIfKnown) {
        super(proxy);
        m_exec = CheckUtils.checkArgumentNotNull(exec);
        m_streamLengthIfKnown = streamLengthIfKnown >= 0L ? OptionalLong.of(streamLengthIfKnown) : OptionalLong.empty();
    }

    @Override
    protected void beforeRead(final int n) throws IOException {
        super.beforeRead(n);
        try {
            m_exec.checkCanceled();
            Thread currentThread = Thread.currentThread();
            if (currentThread.isInterrupted()) {
                currentThread.interrupt();
                throw new EOFException("Reading interrupted.");
            }
        } catch (final CanceledExecutionException e) {
            throw new EOFException("Reading has been cancelled");
        }
    }

    @Override
    protected synchronized void afterRead(final int n) throws IOException {
        super.afterRead(n);
        m_streamLengthIfKnown.ifPresent(l -> m_exec.setProgress(getByteCount() / (double) l));
    }

}
