/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.util;

import static org.knime.core.node.KNIMEConstants.PROPERTY_MAX_LOGFILESIZE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;

/**
 * This is a special appender for KNIME that writes into the
 * <code>knime.log</code> file, which is typically located in the current
 * workspace. If the log file gets bigger than a certain size the
 * file is gzipped and renamed and a new empty file is created.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LogfileAppender extends RollingFileAppender {

    /** Maximum size of log file before it is split (in bytes). */
    public static final long MAX_LOG_SIZE_DEFAULT = 10 * FileUtils.ONE_MB;

    private final File m_logFile;

    private boolean m_isActivated;

    /** The executors that compress and rotate log files.
     * (see AP-15868 -- needs to be static due per "per-workflow" logging.)
     */
    private static final ExecutorService LOG_COMPRESSOR_EXECUTOR_SERVICE =
        Executors.newCachedThreadPool(new ThreadFactory() {

            private final AtomicInteger m_threadCounter = new AtomicInteger(0);

            @Override
            public Thread newThread(final Runnable r) {
                final var t = new Thread(r, "KNIME-Logfile-Compressor-" + m_threadCounter.getAndIncrement());
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });

    /**
     * Creates a new LogfileAppender.
     */
    public LogfileAppender() {
        this(new File(KNIMEConstants.getKNIMEHomeDir() + File.separator));
    }

    /**
     * @param logFileDir the directory in which the log file should be created
     * @since 2.12
     */
    public LogfileAppender(final File logFileDir) {
        initMaxLogFileSize();
        m_logFile = new File(logFileDir, NodeLogger.LOG_FILE);
        setFile(m_logFile.getAbsolutePath());
        setAppend(true);
        setImmediateFlush(true);
        setEncoding("UTF-8");
    }

    private void initMaxLogFileSize() {
        String maxSizeString = System.getProperty(PROPERTY_MAX_LOGFILESIZE);
        long maxLogSize;
        if (maxSizeString == null) {
            maxLogSize = MAX_LOG_SIZE_DEFAULT;
        } else {
            maxSizeString = maxSizeString.toLowerCase().trim();
            int multiplier;
            if (maxSizeString.endsWith("m")) {
                multiplier = 1024 * 1024;
                maxSizeString = maxSizeString.substring(0, maxSizeString.length() - 1).trim();
            } else if (maxSizeString.endsWith("k")) {
                multiplier = 1024;
                maxSizeString = maxSizeString.substring(0, maxSizeString.length() - 1).trim();
            } else {
                multiplier = 1;
            }
            try {
                maxLogSize = multiplier * Long.parseLong(maxSizeString);
            } catch (Throwable e) {
                System.err.println("Unable to parse maximum log size property "
                        + PROPERTY_MAX_LOGFILESIZE + " (\""
                        + System.getProperty(PROPERTY_MAX_LOGFILESIZE) + "\"), "
                        + "using default size");
                maxLogSize = MAX_LOG_SIZE_DEFAULT;
            }
        }
        setMaximumFileSize(maxLogSize < 0 ? Long.MAX_VALUE : maxLogSize);
    }

    @Override
    public void append(final LoggingEvent event) {
        if (!m_isActivated) {
            // AP-24515: `FileAppender#activateOptions()` creates the log file (and enclosing directories if needed).
            // We defer this call until the first message is logged in order to support live changing of the
            // `logInWFDir` and to avoid file I/O for workflows that are created but never saved or run.
            activateOptions();
        }
        super.append(event);
    }

    @Override
    public synchronized void activateOptions() {
        super.activateOptions();
        // set the flag here so we can't miss calls from outside this class
        m_isActivated = true;
    }

    @Override
    public void rollOver() {
        super.rollOver();
        compressRotatedLog();
    }

    /*
     * RollingFileAppenders rollOver() method creates an uncompressed rotated log file named 'knime.log.1'. This method
     * finds this file by appending a .1 suffix to the absolute path of the original log file and then compresses it.
     *
     * Note that this method is indirectly synchronized through rollOver -> subAppend -> append -> doAppend.
     */
    private void compressRotatedLog() {
        try {
            Path tempLog = Files.createTempFile(m_logFile.toPath().getParent(), m_logFile.getName(), ".old");
            Path rotatedLogFilePath = Paths.get(m_logFile.getAbsolutePath() + ".1");
            if (Files.isRegularFile(rotatedLogFilePath)) {
                Files.move(rotatedLogFilePath, tempLog, StandardCopyOption.ATOMIC_MOVE);

                LOG_COMPRESSOR_EXECUTOR_SERVICE.submit(() -> {
                        LogLog.debug("Compressing rotated log file '" + tempLog + "'");
                        Path compressedFile = tempLog.getParent().resolve(NodeLogger.LOG_FILE + ".old.gz");
                        try (final InputStream in = Files.newInputStream(tempLog);
                             final GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(compressedFile))) {
                            IOUtils.copy(in, out, 8192);
                        } catch (IOException ex) {
                            LogLog.warn("Could not compress rotated log file: " + ex.getMessage(), ex);
                        }
                        try {
                            Files.delete(tempLog);
                        } catch (IOException ex) {
                            LogLog.warn("Could not delete rotated log file: " + ex.getMessage(), ex);
                        }
                    }
                );
            }
        } catch (IOException ex) {
            LogLog.warn("Could not compress rotated log file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public int hashCode() {
        if (name != null) {
            return name.hashCode();
        }
        return super.hashCode();
    }
    /**
     * {@inheritDoc}
     * We have to compare the name of the logger in the equals method to prevent duplicate log file registration
     * in the NodeLogger#addWorkflowDirAppender() method !!!
     */
    @Override
    public boolean equals(final Object obj) {
        //We have to compare the name of the logger in the equals method to prevent duplicate log file registration
        //in the NodeLogger#addWorkflowDirAppender() method !!!
        if (name != null && obj instanceof FileAppender appender) {
            return name.equals(appender.getName());
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return m_logFile.toString();
    }
}
