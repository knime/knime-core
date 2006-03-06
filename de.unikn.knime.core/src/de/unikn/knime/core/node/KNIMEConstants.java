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
 *   10.08.2005 (bernd): created
 */
package de.unikn.knime.core.node;

import java.io.File;
import java.util.Locale;

import javax.swing.ImageIcon;

/**
 * Class that hold static values about the knime platform. So far only the knime
 * directory, which is located in <code>$HOME$/.knime</code>, the welcome
 * message, and a icon are provided.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class KNIMEConstants {

    /**
     * The directory where knime will put log files and configuration files.
     * This is in <code>$USER_HOME$/.knime</code> where
     * <code>$USER_HOME$</code> is the user's home directory. This variable
     * does not have a trailing file separator character.
     */
    public static final String KNIME_HOME_DIR = System.getProperty("user.home")
            + File.separator + ".knime";

    /**
     * <i>Welcome to KNIME Konstanz Information Miner</i>.
     */
    public static final String WELCOME_MESSAGE = 
   "************************************************************************\n"
 + "***        Welcome to KNIME. The Konstanz Information Miner.         ***\n"
 + "*** Copyright 2003-2006 Konstanz University, Germany, Prof. Berthold ***\n"
 + "************************************************************************\n";

    /** Path to the <i>knime.png</i> icon. */
    private static final String KNIME_ICON_PATH = KNIMEConstants.class
            .getPackage().getName().replace('.', '/')
            + "/knime.png";

    /** Icon 16 times 16 pixel. */
    public static final ImageIcon KNIME16X16;

    /** Load icon. */
    static {
        ImageIcon icon;
        try {
            ClassLoader loader = KNIMEConstants.class.getClassLoader();
            icon = new ImageIcon(loader.getResource(KNIME_ICON_PATH));
        } catch (Exception e) {
            icon = null;
        }
        KNIME16X16 = icon;
        // we prefer to have all gui-related locales being set to us-standard,
        try {
            Locale.setDefault(Locale.US);
        } catch (Exception e) {
            // do nothing.
        }
    }

    /**
     * Hides public constructor.
     */
    private KNIMEConstants() {
    }

}
