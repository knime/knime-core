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
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.property.hilite;

import java.util.Set;

import org.knime.core.data.RowKey;

/**
 * This mapper has to be implemented by all classes that are interested in 
 * mapping hilite events between {@link RowKey}s.
 * 
 * @see HiLiteTranslator
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public interface HiLiteMapper {
    
    /**
     * Returns a set of <code>RowKey</code> elements which are associated
     * by the specified <b>key</b> or <code>null</code> if no mapping
     * is available.
     * 
     * @param key the key to get the mapping for
     * @return a set of mapped <code>RowKey</code> elements
     */
    Set<RowKey> getKeys(RowKey key);
    
    /**
     * Returns an unmodifiable set of key (source) for hiliting. 
     * @return A set of keys to hilite.
     */
    Set<RowKey> keySet();
}
