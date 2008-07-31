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
 *   Apr 13, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.scorer.entrop;


/**
 * Old entropy node. It differs from the new entropy node (see super class)
 * in that it doesn't have an outport.
 *  @author Bernd Wiswedel, University of Konstanz
 * @deprecated Replaced by {@link NewEntropyNodeFactory}.
 */
@Deprecated
public class EntropyNodeFactory extends NewEntropyNodeFactory {
    
    /** Constructor, invokes {@link NewEntropyNodeFactory#
     * NewEntropyNodeFactory(boolean)} with argument false. */
    public EntropyNodeFactory() {
        super(false);
    }
}
