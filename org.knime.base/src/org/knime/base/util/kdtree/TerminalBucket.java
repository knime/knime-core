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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.util.kdtree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class represents a terminal bucket in a k-d tree. A bucket contains
 * several terminal nodes.
 * 
 * @param <T> the type of the data stored inside the tree
 * @author Thorsten Meinl, University of Konstanz
 */
class TerminalBucket<T> implements Node, Iterable<TerminalNode<T>> {
    private final ArrayList<TerminalNode<T>> m_nodes =
        new ArrayList<TerminalNode<T>>();
    
    /**
     * Creates a new terminal bucket.
     * 
     * @param nodes the nodes inside the bucket
     */
    public TerminalBucket(final List<TerminalNode<T>> nodes) {
        m_nodes.addAll(nodes);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<TerminalNode<T>> iterator() {
        return m_nodes.iterator();
    }
}
