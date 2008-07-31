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
 *   04.07.2005 (mb): created
 */
package org.knime.core.eclipseUtil;

/** Interface for alternative ways to create classes based on class names.
 * This is a work around to tolerate Eclipse's "feature" that not all
 * class loaders from all plugins are known everywhere. A central class
 * creator facility should implement this interface and then be registered
 * with static <code>GlobalClassCreator</code>.
 * 
 * <p>Have a look at the package description to find related links to these
 * eclipse features. 
 * 
 * @author Michael Berthold, University of Konstanz
 */
public interface ClassCreator {

    /** returns the Class for the specified className.
     * 
     * @param className specifies the name of the class to be found.
     * @return corresponding <code>Class</code>.
     */
    public Class<?> createClass(String className);
}
