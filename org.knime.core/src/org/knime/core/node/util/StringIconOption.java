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
 *    26.11.2007 (Tobias Koetter): created
 */

package org.knime.core.node.util;

import javax.swing.Icon;



/**
 * Interface of a list option with an icon and a text which is used in the
 * string selection default components.
 *
 * @see DefaultStringIconOption
 * @author Tobias Koetter, University of Konstanz
 */
public interface StringIconOption {


    /**
     * @return the label to render must not be <code>null</code>
     */
    public String getText();

    /**
     * @return the {@link Icon} to render or <code>null</code> if no icon
     * should be rendered
     */
    public Icon getIcon();
}
