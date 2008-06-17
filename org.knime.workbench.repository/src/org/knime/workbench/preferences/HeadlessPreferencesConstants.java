/* This source code, its documentation and all appendant files
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
 */
package org.knime.workbench.preferences;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public interface HeadlessPreferencesConstants {
    
    /** Preference constant: log level for console appender. */
    public static final String P_LOGLEVEL_LOG_FILE = "logging.loglevel.logfile";
    
    /** Preference constant: maximum threads to use. */
    public static final String P_MAXIMUM_THREADS = "knime.maxThreads";

    /** Preference constant: directory for temporary files. */
    public static final String P_TEMP_DIR = "knime.tempDir";

}
