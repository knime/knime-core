/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   Oct 20, 2008 (wiswedel): created
 */
package org.knime.core.node;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public interface NodeContentPersistor {
    
    boolean needsResetAfterLoad();
    /** Indicate an error and that this node should better be reset after load.
     */
    void setNeedsResetAfterLoad();
    boolean mustWarnOnDataLoadError();
    
    ReferencedFile getNodeInternDirectory();
    PortObjectSpec getPortObjectSpec(final int outportIndex);
    PortObject getPortObject(final int outportIndex);
    String getPortObjectSummary(final int outportIndex);
    BufferedDataTable[] getInternalHeldTables();

}
