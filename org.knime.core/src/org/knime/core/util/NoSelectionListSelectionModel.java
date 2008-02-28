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
 *   29.08.2006 (ohl): created
 */

package org.knime.core.util;

import javax.swing.DefaultListSelectionModel;

/**
 * A ListSelectionModel not allowing any selection. As the default model
 * supports only single or multiple selections, we override a couple of
 * methods and provide this wrapper class.
 *
 * @author ohl, University of Konstanz
 */
public class NoSelectionListSelectionModel extends DefaultListSelectionModel {
    /**
     * {@inheritDoc}
     */
    @Override
    public void insertIndexInterval(final int index, final int length,
            final boolean before) {
        // skip it.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveLeadSelectionIndex(final int leadIndex) {
        // skip it.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeIndexInterval(final int index0, final int index1) {
        // skip it.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAnchorSelectionIndex(final int anchorIndex) {
        // skip it.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLeadSelectionIndex(final int leadIndex) {
        // skip it.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSelectionInterval(final int index0, final int index1) {
        // skip it.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSelectionInterval(final int index0, final int index1) {
        // skip it.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeSelectionInterval(final int index0,
            final int index1) {
        // skip it.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSelectionMode(final int selectionMode) {
        throw new IllegalStateException("Can't change the selection mode"
                + " of the NO SELECTION model");
    }

}
