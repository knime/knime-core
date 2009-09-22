/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   14.09.2009 (Fabian Dill): created
 */
package org.knime.core.data.date;

import javax.swing.Icon;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;

/**
 * The {@link UtilityFactory} for the {@link TimestampValue} providing access to
 * the icon, the renderer and the comparator.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class TimestampUtility extends UtilityFactory {
    
    /** Singleton icon to be used to display this cell type. */
    private static final Icon ICON = loadIcon(
            TimestampUtility.class, "icons/date_time.png");
    
    private static final DataValueComparator COMPARATOR 
        = new TimestampComparator();
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Icon getIcon() {
        return ICON;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected DataValueComparator getComparator() {
        return COMPARATOR;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected DataValueRendererFamily getRendererFamily(
            final DataColumnSpec spec) {
        return new DefaultDataValueRendererFamily(
                TimestampValueRenderer.EU, TimestampValueRenderer.US);
    }

}
