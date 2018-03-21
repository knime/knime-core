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
 * ------------------------------------------------------------------------
 */
package org.knime.core.util;

import static org.knime.core.node.KNIMEConstants.PROPERTY_MAX_LOGFILESIZE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.FileAppender;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.helpers.LogLog;
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
    private final File m_logFile;
    /** Maximum size of log file before it is split (in bytes). */
    public static final long MAX_LOG_SIZE_DEFAULT = 10 * 1024 * 1024; // 10MB
    private long m_maxLogSize;

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
        ensureLogFileDirectoryExists(logFileDir);
        m_logFile = new File(logFileDir, NodeLogger.LOG_FILE);
        setFile(m_logFile.getAbsolutePath());
        setAppend(true);
        setImmediateFlush(true);
        setEncoding("UTF-8");
    }

    private void initMaxLogFileSize() {
        String maxSizeString = System.getProperty(PROPERTY_MAX_LOGFILESIZE);
        if (maxSizeString == null) {
            m_maxLogSize = MAX_LOG_SIZE_DEFAULT;
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
                m_maxLogSize = multiplier * Long.parseLong(maxSizeString);
            } catch (Throwable e) {
                System.err.println("Unable to parse maximum log size property "
                        + PROPERTY_MAX_LOGFILESIZE + " (\""
                        + System.getProperty(PROPERTY_MAX_LOGFILESIZE) + "\"), "
                        + "using default size");
                m_maxLogSize = MAX_LOG_SIZE_DEFAULT;
            }
        }
        setMaximumFileSize(m_maxLogSize);
    }

    private void ensureLogFileDirectoryExists(final File logFileDir) {
        if (!logFileDir.exists()) {
            logFileDir.mkdirs();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollOver() {
        super.rollOver();
        compressRotatedLog();
        try {
            // This will also close the file. This is OK since multiple
            // close operations are safe.
            setFile(m_logFile.getAbsolutePath(), false, bufferedIO, bufferSize);
        } catch (IOException e) {
            LogLog.error("setFile(" + fileName + ", false) call failed.", e);
        }
    }

    /**
     * RollingFileAppenders rollOver() method creates an uncompressed rotated log file
     * named 'knime.log.1'. This method finds this file by appending a .1 suffix to
     * the absolute path of the original log file and then compresses it.
     *
     * This is not private only because that it is visible inside the thread
     * below.
     */
    private void compressRotatedLog() {
        synchronized (m_logFile) {
            String rotatedLogFilePath = m_logFile.getAbsolutePath() + ".1";
            LogLog.debug("Compressing rotated log file '" + rotatedLogFilePath + "'");
            final File rotatedLogFile = new File(rotatedLogFilePath);
            closeFile();
            setFile(m_logFile.getAbsolutePath());
            final Thread t = new Thread() {
                @Override
                public void run() {
                    synchronized (m_logFile) {
                        if (rotatedLogFile.exists()) {
                            try (final BufferedInputStream in = new BufferedInputStream(new FileInputStream(rotatedLogFile));
                                    final GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(
                                        new File(correctName(rotatedLogFile))));) {
                                byte[] buf = new byte[4096];
                                int count;
                                while ((count = in.read(buf)) > 0) {
                                    out.write(buf, 0, count);
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            if (!rotatedLogFile.delete()) {
                                LogLog.warn("Failed to delete temporary log file");
                            }
                        }
                    }
                }

                /**
                 * The rotated log file should be named knime.log.old.gz, but the rollOver method in
                 * RollingFileAppender names it knime.log.1. In order to stay consistent with the previous
                 * naming convention the trailing one is here replaced by old.gz.
                 *
                 * @param file the rotated old log file
                 * @return name of the rotated log file
                 */
                private String correctName(final File file) {
                    String path = file.getAbsolutePath();
                    String pathWithoutTrailingOne = path.substring(0, path.length() - 1);
                    return pathWithoutTrailingOne + "old.gz";
                }
            };
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        }
    }

    /**
     * {@inheritDoc}
     */
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
        if (name != null && (obj instanceof FileAppender)) {
            return name.equals(((FileAppender)obj).getName());
        }
        return super.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_logFile.toString();
    }
}
