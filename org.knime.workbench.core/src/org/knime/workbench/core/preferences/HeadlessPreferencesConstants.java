/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
package org.knime.workbench.core.preferences;

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
    
    /* --- Master Key constants --- */
    
    /** Preference constant if the master key dialog was opened. */
    public static final String P_MASTER_KEY_DEFINED 
        = "knime.master_key.defined";
    /** Preference constant if a master key should be used. */
    public static final String P_MASTER_KEY_ENABLED 
        = "knime.master.key.enabled";
    /** Preference constant to store the master key flag during a session. */
    public static final String P_MASTER_KEY_SAVED = "knime.master_key.saved";
    /** Preference constant to store the master key during a session. */
    public static final String P_MASTER_KEY = "knime.master_key";
    
    /* --- Database settings constants --- */
    
    /** Preference constant to store loaded database driver files. */
    public static final String P_DATABASE_DRIVERS = "database_drivers";

}
