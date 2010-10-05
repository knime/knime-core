/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
