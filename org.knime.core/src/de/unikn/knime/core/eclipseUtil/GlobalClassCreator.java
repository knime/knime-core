/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   04.07.2005 (mb): created
 */
package de.unikn.knime.core.eclipseUtil;

import de.unikn.knime.core.node.NodeLogger;

/**
 * Eclipse workaround to create new Classes through one, global gateway. We can
 * now plugin an Eclipse hack that knows about all plugins and can create
 * classes using the appropriate class loader. If no creator is specified the
 * usual Java class loader will be used.
 * 
 * @author mb, University of Konstanz
 */
public final class GlobalClassCreator {
    
    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(GlobalClassCreator.class);
    
    private static ClassCreator classCreator = null;

    /**
     * overwrite ClassCreator to be used by everybody.
     * 
     * @param cc new ClassCreator.
     */
    public static void setClassCreator(final ClassCreator cc) {
        classCreator = cc;
        LOGGER.debug("ClassCreater registered: " + cc);
    }

    /**
     * return Class specified name - using either normal classloader or special
     * one if it was given before.
     * 
     * @param className qualified class name
     * @return <code>Class</code> of specified name
     * @throws ClassNotFoundException Class not found.
     */
    public static Class createClass(final String className)
            throws ClassNotFoundException {
        if (classCreator == null) {
            // no class creator given, create it directly
            return Class.forName(className);
        } else {
            // class creator was given, use it!
            Class result = classCreator.createClass(className);
            if (result == null) {
                return Class.forName(className);
            }
            return result;
        }
    }

    // hide default constructor
    private GlobalClassCreator() {
    }

}
