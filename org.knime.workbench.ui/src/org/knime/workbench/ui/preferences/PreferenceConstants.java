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


/**
 * Constant definitions for plug-in preferences. Values are stored under these
 * keys in the PreferenceStore of the UI plugin.
 * 
 * 
 * @author Florian Georg, University of Konstanz
 */
public interface PreferenceConstants {

    /** Preference constant: log level for console appender. */
    public static final String P_LOGLEVEL_CONSOLE = "logging.loglevel.console";

    /** Preference constant: whether user needs to confirm reset actions. */
    public static final String P_CONFIRM_RESET = "knime.confirm.reset";
    
    /** Preference constant: whether user needs to confirm delete actions. */
    public static final String P_CONFIRM_DELETE = "knime.confirm.delete";
    
    /** Preference constant to confirm reconnecting a node. */
    public static final String P_CONFIRM_RECONNECT = "knime.confirm.reconnect";
    
    /** Preference constant for the size of the favorite nodes frequency 
     * history size.
     */
    public static final String P_FAV_FREQUENCY_HISTORY_SIZE 
        = "knime.favorites.frequency";
    /** Preference constant for the size of the favorite nodes last used
     * history size.
     */    
    public static final String P_FAV_LAST_USED_SIZE 
        = "knime.favorites.lastused";
    
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
   
}
