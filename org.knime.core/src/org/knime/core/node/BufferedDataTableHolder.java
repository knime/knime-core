/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Aug 7, 2008 (mb): created
 */
package org.knime.core.node;

/** Interface which allows a NodeModel to hold (and keep) internal
 * BufferedDataTables. The framework will make sure to retrieve this
 * table from the model and set them again, for example after the
 * workflow has been loaded.
 * 
 * USE WITH CARE! In all likelihood if you are using this interface you
 * really should not use it. Storing BDTs inside a NodeModel is bad
 * practice.
 * 
 * @author M. Berthold, University of Konstanz
 */
public interface BufferedDataTableHolder {

    /** 
     * @return array of BDTs which are held and used internally.
     */
    BufferedDataTable[] getInternalTables();

    /** Allows the WorkflowManager to set information about new BDTs, for
     * instance after load.
     * 
     * @param tables the array of new tables
     */
    void setInternalTables(final BufferedDataTable[] tables);  
}
