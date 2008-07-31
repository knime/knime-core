/*
 * ------------------------------------------------------------------ *
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
 *   28.08.2007 (ohl): created
 */
package org.knime.base.node.util.exttool;

/**
 * Object sent to the node views to notify them about a new line that should be
 * displayed. Views should copy the line as it will be overridden next time
 * around. <br>
 * A flag indicates whether this is for the standard out or standard err view.
 * 
 * @author ohl, University of Konstanz
 */
public class ViewUpdateNotice {

    /**
     * the different types of view reacting to update notifications.
     */
    enum ViewType {
        /**
         * the views displaying output to standard out.
         */
        stdout,
        /**
         * the views displaying output to standard error.
         */
        stderr
    }

    /**
     * the type of view that should listen to this notification.
     */
    public final ViewType TYPE;

    private String m_newLine;

    /**
     * Creates a new notification object for the specified view type.
     * 
     * @param type the type of view that should listen to this notification.
     */
    ViewUpdateNotice(final ViewType type) {
        TYPE = type;
        m_newLine = null;
    }

    /**
     * @return the new line to add to the view
     */
    public String getNewLine() {
        return m_newLine;
    }

    /**
     * @param line the new line to transfer to the views
     */
    public void setNewLine(final String line) {
        if (line == null) {
            throw new NullPointerException(
                    "Can't notify views with a null line");
        }
        m_newLine = line;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return TYPE.name() + ": " + m_newLine;
    }
}
