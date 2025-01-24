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
package org.knime.core.monitor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.SystemUtils;
import org.knime.core.node.NodeLogger;

import sun.misc.Unsafe;

/**
 * Utility class for accessing the process information from the Linux /proc filesystem.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @see <a href="https://docs.kernel.org/filesystems/proc.html">proc filesystem documentation</a>
 * @since 5.4
 */
final class ProcessStateUtil {

    private ProcessStateUtil() {
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ProcessStateUtil.class);

    private static final int PAGESIZE = getPagesize();

    /** Indicates if the system has smaps_rollup files (the file was added in 2017) */
    private static final boolean HAS_PROC_SMAPS_ROLLUP = hasProcFile("smaps_rollup");

    /**
     * Indicates if the system has smaps file (does not exist on kernels < 2.6.14 or if CONFIG_MMU kernel configuration
     * option is not enabled)
     */
    private static final boolean HAS_PROC_SMAPS = hasProcFile("smaps");

    /** Indicates if the system has statm file */
    private static final boolean HAS_PROC_STATM = hasProcFile("statm");

    /**
     * The buffer size used for reading files in the /proc filesystem. We use a larger buffer size than the default
     * because the file will change between each read.
     * <P>
     * See https://github.com/giampaolo/psutil/blob/c034e6692cf736b5e87d14418a8153bb03f6cf42/psutil/_common.py#L783-L795
     */
    private static final int PROC_FILE_BUFFER_SIZE = 32 * 1024;

    /** The index of the RSS column in the /proc/[pid]/statm file */
    private static final int RSS_STATM_COLUMN_INDEX = 1;

    /**
     * Check if the system supports PSS (Proportional Set Size) measurements. This is only the case on Linux systems
     * that have the /proc/[pid]/smaps_rollup or /proc/[pid]/smaps file.
     *
     * @return <code>true</code> if the system supports PSS measurements
     */
    public static boolean supportsPSS() {
        return SystemUtils.IS_OS_LINUX && (HAS_PROC_SMAPS_ROLLUP || HAS_PROC_SMAPS);
    }

    /**
     * Check if the system supports RSS measurements. This is only the case on Linux systems that have the
     * /proc/[pid]/statm file.
     *
     * @return <code>true</code> if the system supports RSS measurements
     */
    public static boolean supportsRSS() {
        return SystemUtils.IS_OS_LINUX && HAS_PROC_STATM && PAGESIZE > 0;
    }

    /**
     * Returns the PSS (Proportional Set Size) of the process with the given pid in KB. This is a slow but accurate
     * estimate of the current memory usage of the process.
     * <P>
     * Description from the proc filesystem documentation: <blockquote> The “proportional set size” (PSS) of a process
     * is the count of pages it has in memory, where each page is divided by the number of processes sharing it. So if a
     * process has 1000 pages all to itself, and 1000 shared with one other process, its PSS will be 1500. </blockquote>
     * <P>
     * Note that getting the PSS for a process is expensive if the process in question has a many memory pages.
     * Therefore, checking the PSS should be done sparingly.
     *
     * @param pid the process id
     * @return the PSS of the process with the given pid in KB
     * @throws IOException if the /proc/[pid]/smaps file could not be read
     * @throws UnsupportedOperationException if the PSS is not available on the current platform (call
     *             {@link #supportsPSS()} to check this beforehand)
     */
    public static long getPSS(final long pid) throws IOException {
        if (!SystemUtils.IS_OS_LINUX) {
            throw new UnsupportedOperationException("PSS is only available on Linux");
        }

        if (HAS_PROC_SMAPS_ROLLUP) {
            try {
                // Note that reading the smaps_rollup file is faster than reading the smaps file because the kernel
                // is faster at aggregating the values
                return readPSSFromSmapsRollup(pid);
            } catch (final IOException ex) {
                // NB: Only debug log because we will try the smaps file next
                LOGGER.debug("Failed to read PSS from smaps_rollup file", ex);
            }
        }
        if (HAS_PROC_SMAPS) {
            return readPSSFromSmaps(pid);
        }
        throw new UnsupportedOperationException(
            "PSS is not available on this platform. Because the /proc/[pid]/smaps file is not present.");
    }

    /**
     * Returns the Resident Set Size (RSS) of the process with the given pid in KB. This is the amount of memory that
     * the process has in RAM. This is a fast but less accurate estimate of the current memory usage of the process.
     *
     * @param pid the process id
     * @return the RSS of the process with the given pid in KB
     * @throws IOException if the /proc/[pid]/statm file could not be read
     * @throws UnsupportedOperationException if the PSS is not available on the current platform (call
     *             {@link #supportsRSS()} to check this beforehand)
     */
    public static long getRSS(final long pid) throws IOException {
        if (!SystemUtils.IS_OS_LINUX) {
            throw new UnsupportedOperationException("RSS is only available on Linux");
        }
        if (PAGESIZE <= 0) {
            throw new UnsupportedOperationException(
                "RSS is not available because the page size could not be determined");
        }
        try (var reader = readFile("/proc/" + pid + "/statm")) {
            // Note that the file only has one line
            var numPages = Long.parseLong(reader.readLine().split(" ")[RSS_STATM_COLUMN_INDEX]);
            return numPages * PAGESIZE / 1024;
        } catch (NumberFormatException e) {
            throw new IOException("Failed to parse RSS value", e);
        }
    }

    private static long readPSSFromSmapsRollup(final long pid) throws IOException {
        try (var reader = readFile("/proc/" + pid + "/smaps_rollup")) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Pss:")) {
                    // NB: Can return immediately because the PSS is only present once in the file
                    return parsePSSLine(line);
                }
            }
        }
        throw new IOException("PSS not found in smaps_rollup file");
    }

    private static long readPSSFromSmaps(final long pid) throws IOException {
        var totalPss = 0;
        try (var reader = readFile("/proc/" + pid + "/smaps")) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Pss:")) {
                    totalPss += parsePSSLine(line);
                }
            }
        }
        return totalPss;
    }

    private static BufferedReader readFile(final String path) throws IOException {
        // NB: The text files in /proc only contain ascii characters
        return new BufferedReader(new FileReader(path, StandardCharsets.US_ASCII), PROC_FILE_BUFFER_SIZE);
    }

    /** Parses the PSS memory from a line of the smaps or smaps_rollup file */
    private static long parsePSSLine(final String line) {
        // The line looks like "Pss: 1234 kB"
        var colonIndex = line.indexOf(':');
        var kbIndex = line.indexOf('k', colonIndex);
        if (colonIndex != -1 && kbIndex != -1) {
            var numberStr = line.substring(colonIndex + 1, kbIndex).trim();
            try {
                return Long.parseLong(numberStr);
            } catch (NumberFormatException e) {
                // Ignore parse errors
                // Might happen if the number is just at the border of the buffer and the file was changed between reads
                // We just ignore the line and continue reading
            }
        }
        return 0;
    }

    /** Utility to determine if the system generally has smaps_rollup files */
    private static boolean hasProcFile(final String fileName) {
        if (!SystemUtils.IS_OS_LINUX) {
            return false;
        }
        var selfPid = ProcessHandle.current().pid();
        var smapsPath = Path.of("/proc", "" + selfPid, fileName);
        return Files.exists(smapsPath);
    }

    /** @return the page size in bytes or -1 if it could not be determined */
    private static int getPagesize() {
        if (!SystemUtils.IS_OS_LINUX) {
            return -1;
        }

        try {
            var theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            var unsafe = (Unsafe)theUnsafe.get(null);
            return unsafe.pageSize();
        } catch (Throwable ex) { // NOSONAR
            // We catch everything to make sure that the class can always be loaded
            LOGGER.error("Failed to get the page size from Unsafe", ex);
            return -1;
        }
    }
}
