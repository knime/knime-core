/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   25.09.2006 (ohl): created
 */
package org.knime.core.node.defaultnodesettings;

/**
 * Used for components accepting numbers (doubles or ints). Requires toString
 * and fromString implementations. {@link SettingsModelInteger} and
 * {@link SettingsModelDouble} are derived from this.
 * 
 * @author ohl, University of Konstanz
 */
public abstract class SettingsModelNumber extends SettingsModel {

    /**
     * @return a string representation of the current value
     */
    abstract String getNumberValueStr();

    /**
     * Sets a new value in this object. Parses the passed string. Throws an
     * exception if the value is invalid.
     * 
     * @param newValueStr the string representation of the new value to set.
     */
    abstract void setNumberValueStr(final String newValueStr);

}
