/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ------------------------------------------------------------------- *
 */
package org.knime.core.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.FileAppender;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;
import org.knime.core.node.KNIMEConstants;

/**
 * This is a special appender for KNIME that writes into the
 * <code>knime.log</code> file, which is typically located in the current
 * workspace. If the log file gets bigger than a certain size the
 * file is gzipped and renamed and a new empty file is created.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LogfileAppender extends FileAppender {
    private final File m_logFile;

    /** Name of the environment variable, which allows one to change the default
     * log file size. Values must be integer, possibly succeeded by "m" or "k"
     * to denote that the given value is in mega or kilo byte. */
    public static final String MAX_SIZE_ENV_VARIABLE = "knime.logfile.maxsize";

    /** Maximum size of log file before it is split (in bytes). */
    public static final long MAX_LOG_SIZE_DEFAULT = 10 * 1024 * 1024; // 10MB
    private long m_maxLogSize;

    /**
     * Creates a new LogfileAppender.
     */
    public LogfileAppender() {
        String maxSizeString = System.getProperty(MAX_SIZE_ENV_VARIABLE);
        if (maxSizeString == null) {
            m_maxLogSize = MAX_LOG_SIZE_DEFAULT;
        } else {
            maxSizeString = maxSizeString.toLowerCase().trim();
            int multiplier;
            if (maxSizeString.endsWith("m")) {
                multiplier = 1024 * 1024;
                maxSizeString = maxSizeString.substring(
                            0, maxSizeString.length() - 1).trim();
            } else if (maxSizeString.endsWith("k")) {
                multiplier = 1024;
                maxSizeString = maxSizeString.substring(
                        0, maxSizeString.length() - 1).trim();
            } else {
                multiplier = 1;
            }
            try {
                m_maxLogSize = multiplier * Long.parseLong(maxSizeString);
            } catch (Throwable e) {
                System.err.println("Unable to parse maximum log size variable "
                        + MAX_SIZE_ENV_VARIABLE + " (\""
                        + System.getProperty(MAX_SIZE_ENV_VARIABLE) + "\"), "
                        + "using default size");
                m_maxLogSize = MAX_LOG_SIZE_DEFAULT;
            }
        }
        // get user home
        final String tmpDir = KNIMEConstants.getKNIMEHomeDir() + File.separator;
        // check if home/.knime exists
        File tempDir = new File(tmpDir);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        m_logFile = new File(tmpDir + "knime.log");
        setFile(m_logFile.getAbsolutePath());
        setImmediateFlush(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activateOptions() {
        if (m_maxLogSize > 0 && m_logFile.exists()
                && (m_logFile.length() > m_maxLogSize) && m_logFile.canRead()) {
            compressOldLog();
        }
        super.activateOptions();
    }

    /** This is not private only because that it is visible inside the thread
     * below.
     */
    private void compressOldLog() {
        synchronized (m_logFile) {
            LogLog.debug("Compressing log file '" + m_logFile + "'");
            final File tmpFile = new File(m_logFile.getAbsolutePath() + ".old");
            closeFile();
            m_logFile.renameTo(tmpFile);
            setFile(m_logFile.getAbsolutePath());

            Thread t = new Thread() {
                @Override
                public void run() {
                    synchronized (m_logFile) {
                        if (tmpFile.exists()) {
                            BufferedInputStream in;
                            try {
                                in = new BufferedInputStream(
                                        new FileInputStream(tmpFile));
                                GZIPOutputStream out = new GZIPOutputStream(
                                        new FileOutputStream(new File(tmpFile
                                                .getAbsolutePath()
                                                + ".gz")));

                                byte[] buf = new byte[4096];
                                int count;
                                while ((count = in.read(buf)) > 0) {
                                    out.write(buf, 0, count);
                                }

                                in.close();
                                out.close();
                                tmpFile.delete();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
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
    protected void subAppend(final LoggingEvent event) {
        super.subAppend(event);
        if (m_maxLogSize > 0 && m_logFile.length() > m_maxLogSize) {
            compressOldLog();

            try {
                // This will also close the file. This is OK since multiple
                // close operations are safe.
                setFile(m_logFile.getAbsolutePath(), false, bufferedIO,
                        bufferSize);
            } catch (IOException e) {
                LogLog.error("setFile(" + fileName + ", false) call failed.",
                                e);
            }
        }
    }
}
