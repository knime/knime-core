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
 * -------------------------------------------------------------------
 * 
 * History
 *   12.01.2005 (Florian Georg): created
 */
package org.knime.workbench.ui.preferences;

import org.knime.core.node.NodeLogger.LEVEL;

/**
 * Constant definitions for plug-in preferences. Values are stored under these
 * keys in the PreferenceStore of the UI plugin.
 * 
 * 
 * @author Florian Georg, University of Konstanz
 */
public class PreferenceConstants {
    /** Peference constant: select default view mode. */
    public static final String P_CHOICE_VIEWMODE = "choice.view.mode";

    /** Choice: open views in JFrame. */
    public static final String P_CHOICE_VIEWMODE_JFRAME = "jframe";

    /** Choice: open views in eclipse views. */
    public static final String P_CHOICE_VIEWMODE_VIEW = "eclipse.jframe";

    /** Preference constant: log level for console appender. */
    public static final String P_LOGLEVEL_CONSOLE = "logging.loglevel.console";

    /** Preference constant: log level for console appender. */
    public static final String P_LOGLEVEL_LOG_FILE = "logging.loglevel.logfile";

    /** Preference constant: maximum threads to use. */
    public static final String P_MAXIMUM_THREADS = "knime.maxThreads";

    /** Preference constant: directory for temporary files. */
    public static final String P_TEMP_DIR = "knime.tempDir";

    /** Preference constant: whether user needs to confirm reset actions. */
    public static final String P_CONFIRM_RESET = "knime.confirm.reset";
    
    /** Preference constant: whether user needs to confirm delete actions. */
    public static final String P_CONFIRM_DELETE = "knime.confirm.delete";
    
    /** Choice: log >= debug events. */
    public static final String P_LOGLEVEL_DEBUG = LEVEL.DEBUG.toString();

    /** Choice: log >= info events. */
    public static final String P_LOGLEVEL_INFO = LEVEL.INFO.toString();

    /** Choice: log >= warn events. */
    public static final String P_LOGLEVEL_WARN = LEVEL.WARN.toString();

    /** Choice: log >= error events. */
    public static final String P_LOGLEVEL_ERROR = LEVEL.ERROR.toString();
    
    public static final String P_FAV_FREQUENCY_HISTORY_SIZE 
        = "knime.favorites.frequency";
    
    public static final String P_FAV_LAST_USED_SIZE = "knime.favorites.lastused";
    
    public static final String P_MASTER_KEY_DEFINED = "knime.master_key.defined";
    public static final String P_MASTER_KEY = "knime.master_key";
}
