/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   12.01.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.ui.preferences;

import de.unikn.knime.core.node.NodeLogger.LEVEL;

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
    public static final String P_LOGLEVEL_CONSOLE = 
        "logging.loglevel.console";

    /** Preference constant: log level for console appender. */
    public static final String P_LOGLEVEL_LOG_FILE = 
        "logging.loglevel.logfile";

    /** Preference constant: maximum threads to use. */
    public static final String P_MAXIMUM_THREADS = "knime.maxThreads";
    
    /** Choice: log >= debug events. */
    public static final String P_LOGLEVEL_DEBUG = LEVEL.DEBUG.toString();

    /** Choice: log >= info events. */
    public static final String P_LOGLEVEL_INFO = LEVEL.INFO.toString();

    /** Choice: log >= warn events. */
    public static final String P_LOGLEVEL_WARN = LEVEL.WARN.toString();

    /** Choice: log >= error events. */
    public static final String P_LOGLEVEL_ERROR = LEVEL.ERROR.toString();

}
