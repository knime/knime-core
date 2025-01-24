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
 *   Jul 23, 2024 (benjamin): created
 */
package org.knime.core.data.util.memory;

import java.util.function.LongConsumer;

import org.knime.core.monitor.ProcessWatchdog;

/**
 * Watchdog that tracks the memory usage of KNIME AP and its external processes. If the total memory usage surpasses a
 * threshold, the external process with the highest memory usage gets killed forcibly. The watchdog uses the
 * proportional set size (PSS) of the processes and their subprocesses to determine their memory usage.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 5.4
 * @deprecated use {@link ProcessWatchdog} instead
 */
@Deprecated
public final class ExternalProcessMemoryWatchdog {

    private static final ExternalProcessMemoryWatchdog INSTANCE = new ExternalProcessMemoryWatchdog();

    /**
     * @return the singleton instance of the watchdog
     * @deprecated use {@link ProcessWatchdog} instead
     */
    @Deprecated
    public static ExternalProcessMemoryWatchdog getInstance() {
        return INSTANCE;
    }

    /**
     * Start tracking the given external process. If the total memory usage of external processes surpasses a threshold,
     * the process that uses most memory gets killed forcibly ({@link ProcessHandle#destroyForcibly()}). The
     * <code>killCallback</code> is called for this process.
     * <P>
     * Note that the memory usage of the process and all subprocesses is tracked.
     *
     * @param process a handle for the process
     * @param killCallback a callback that gets called before a process is killed by the watchdog. The argument of the
     *            callback is the current memory usage of the process in kilo-bytes. The callback must not block. The
     *            killing of the process cannot be prevented by freeing up memory. The callback must only be used to
     *            record the reason why the process was killed.
     * @deprecated use {@link ProcessWatchdog#trackProcess(ProcessHandle, LongConsumer)} instead
     */
    @Deprecated
    @SuppressWarnings("static-method") // non-static for backward compatibility
    public void trackProcess(final ProcessHandle process, final LongConsumer killCallback) {
        ProcessWatchdog.getInstance().trackProcess(process, killCallback);
    }

    private ExternalProcessMemoryWatchdog() {
    }
}
