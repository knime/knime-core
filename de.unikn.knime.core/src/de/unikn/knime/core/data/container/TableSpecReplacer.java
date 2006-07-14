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
 *   Jul 14, 2006 (wiswedel): created
 */
package de.unikn.knime.core.data.container;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.BufferedDataTable;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public final class TableSpecReplacer {
    
    private TableSpecReplacer() { }
    
    public static BufferedDataTable createReplaceSpecTable(
            final BufferedDataTable table, final DataTableSpec newSpec) {
        return new BufferedDataTable(new TableSpecReplacerTable(table, newSpec));
    }
}
