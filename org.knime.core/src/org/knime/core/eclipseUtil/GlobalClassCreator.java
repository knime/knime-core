/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * -------------------------------------------------------------------
 * 
 * History
 *   04.07.2005 (mb): created
 */
package org.knime.core.eclipseUtil;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.NodeLogger;

/**
 * Eclipse workaround to create new Classes through one, global gateway. We can
 * now plugin an Eclipse hack that knows about all plugins and can create
 * classes using the appropriate class loader. If no creator is specified the
 * usual Java class loader will be used.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public final class GlobalClassCreator {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(GlobalClassCreator.class);

    private static ClassCreator classCreator = null;
    
    /** Contains mappings from obsolete classes to new classes, replacing the 
     * old implementation (e.g. if chemical type implementations were moved into
     * org.knime.chem.types). */
    private static final Map<String, String> CLASS_REPLACEMENT_MAP = 
        new HashMap<String, String>();
    
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
     * Adds a replacement string for obsolete class names. This is used 
     * primarily by data cell class implementations, which were moved from 
     * specific vendor plugins to KNIME core (mostly in a org.knime.chem.types).
     * The replaced class is supposed to use the same internals (e.g. a similar
     * persistor) as the replacing class.
     * 
     * <p>This method is not intended for public use; if you want to replace
     * an obsolete class by a KNIME core class, contact the KNIME team. 
     * @param oldClassName The old, to be replaced class
     * @param replacedClassName The new class replacing oldClassName
     * @throws NullPointerException If either argument is null
     * @throws IllegalStateException If there is already an entry for the 
     *         old class name.
     */
    public static void addClassReplacementPair(
            final String oldClassName, final String replacedClassName) {
        if (oldClassName == null || replacedClassName == null) {
            throw new NullPointerException("Argument must not be null");
        }
        if (CLASS_REPLACEMENT_MAP.containsKey(oldClassName)) {
            throw new IllegalStateException(
                    "Replacement for class \"" + oldClassName 
                    + "\" already set (by \"" 
                    + CLASS_REPLACEMENT_MAP.get(oldClassName) + "\"");
        }
        LOGGER.debug("Adding class replacement tuple from \""
                + oldClassName + "\" to \"" + replacedClassName + "\""); 
        CLASS_REPLACEMENT_MAP.put(oldClassName, replacedClassName);
    }

    /**
     * return Class specified name - using either normal classloader or special
     * one if it was given before.
     * 
     * @param className qualified class name
     * @return <code>Class</code> of specified name
     * @throws ClassNotFoundException Class not found.
     */
    public static Class<?> createClass(final String className)
            throws ClassNotFoundException {

        String translatedClassName = CLASS_REPLACEMENT_MAP.get(className);
        if (translatedClassName == null) {
            translatedClassName = className;
        }
        
        if (className.startsWith("de.unikn.knime.")) {
            translatedClassName = className.replace("de.unikn.knime.",
                    "org.knime.");
        }

        if (classCreator == null) {
            // no class creator given, create it directly
            try {
                return Class.forName(translatedClassName);
            } catch (ClassNotFoundException ex) {
                return Class.forName(className);
            }
        } else {
            // class creator was given, use it!
            Class<?> result = classCreator.createClass(translatedClassName);
            if (result == null) {
                result = classCreator.createClass(className);
            }

            if (result == null) {
                result = Class.forName(translatedClassName);
            }

            if (result == null) {
                result = Class.forName(className);
            }

            return result;
        }
    }

    // TODO if we adopt to the Eclipse Buddy concept, replace the above method
    // by the one below. Also remove the setClassCreator method and add
    // Eclipse-BuddyPolicy: registered to the MANIFEST.MFs of core and base
    // The rest should then work almost automatically.
//    public static Class createClass(final String className)
//            throws ClassNotFoundException {
//
//        String translatedClassName = className;
//        if (className.startsWith("de.unikn.knime.")) {
//            translatedClassName = className.replace("de.unikn.knime.",
//                    "org.knime.");
//        }
//
//        try {
//            return Class.forName(translatedClassName);
//        } catch (ClassNotFoundException ex) {
//            return Class.forName(className);
//        }
//    }

    // hide default constructor
    private GlobalClassCreator() {
    }
}
