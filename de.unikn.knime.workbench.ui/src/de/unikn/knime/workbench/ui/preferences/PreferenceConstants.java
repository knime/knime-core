/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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

/**
 * Constant definitions for plug-in preferences. Values are stored under these
 * keys in the PreferenceStore of the UI plugin.
 * 
 * 
 * @author Florian Georg, University of Konstanz
 */
public class PreferenceConstants {

    /** Peference constant for the global default path to datafiles. */
    public static final String P_DATAFILES_PATH = "path.datafiles";

    /** Peference constant: enable experimental stuff. */
    public static final String P_FLAG_ENABLE_EXPERIMENTAL = "flag.enable."
            + "experimental";

    /** Peference constant: select default view mode. */
    public static final String P_CHOICE_VIEWMODE = "choice.view.mode";

    /** Choice: open views in JFrame. */
    public static final String P_CHOICE_VIEWMODE_JFRAME = "jframe";

    /** Choice: open views in eclipse views. */
    public static final String P_CHOICE_VIEWMODE_VIEW = "eclipse.jframe";

    /** Preference constant: Log4j pattern layout. */
    public static final String P_PATTERN_LAYOUT = "logging.pattern.layout";

    /** Preference constant: minimal log level. */
    public static final String P_LOGLEVEL = "logging.loglevel";

    /** Choice: log >= debug events. */
    public static final String P_LOGLEVEL_DEBUG = "DEBUG";

    /** Choice: log >= info events. */
    public static final String P_LOGLEVEL_INFO = "INFO";

    /** Choice: log >= warn events. */
    public static final String P_LOGLEVEL_WARN = "WARN";

    /** Choice: log >= error events. */
    public static final String P_LOGLEVEL_ERROR = "ERROR";

    /** Flag: redirect output to logfile? */
    public static final String P_FLAG_FILE_LOGGER = "flag.use.logfile";

    /** Filename for logfile. */
    public static final String P_FILE_LOGGER_FILENAME = "logging.logfile";

}
