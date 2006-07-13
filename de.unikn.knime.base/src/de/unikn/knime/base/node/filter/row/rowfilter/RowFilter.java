/* -------------------------------------------------------------------
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
 *   29.06.2005 (ohl): created
 */
package de.unikn.knime.base.node.filter.row.rowfilter;

import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;

/**
 * Used by the RowFilterIterator to determine whether a row should be filtered
 * or not. <br>
 * New row filter implementations MUST also modify the RowFilterFactory in order
 * to get load and save work.
 * 
 * @author ohl, University of Konstanz
 */
public abstract class RowFilter implements Cloneable {

    /**
     * return true if the specified row matches the criteria set in the filter.
     * Can throw a EndOfTableException if the filter can tell that no more
     * rows of the table will be able to fulfill the criteria (the iterator will
     * flag atEnd() after receiving this exception!)
     * 
     * @param row the row to test
     * @param rowIndex the row index of the passed row in the original table.
     * @return true if the row matches the criteria set in the filter, false if
     *         not.
     * @throws EndOfTableException if there is no chance that any of the rows
     *         coming (including the current <code>rowIndex</code>) will fulfill
     *         the criteria, thus no further row in the original table will be 
     *         a match to this filter. (In general this is hard to tell, but a 
     *         row number filter can certainly use it.) If the exception is 
     *         received the row filter table iterator will flag an end of table.
     * @throws IncludeFromNowOn if the current and all following rows from now
     *         on are to be included into the result table.
     */
    public abstract boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn;

    /**
     * Load your internal settings from the configuration object. Throw an
     * exception if the config is invalid/incorrect/inconsistent.
     * 
     * @param cfg the object holding the settings to load
     * @throws InvalidSettingsException if cfg contains
     *             invalid/incorrect/inconsistent settings
     */
    public abstract void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException;

    /**
     * Save your internal settings into the specified configuration object.
     * Passing the object then to the loadSettingsFrom method should flawlessly
     * work.
     * 
     * @param cfg the object to add the current internal settings to.
     */
    public final void saveSettingsTo(final NodeSettingsWO cfg) {
        RowFilterFactory.prepareConfigFor(cfg, this);
        saveSettings(cfg);
    }

    /**
     * Do not call this function - rather call saveSettingsTo. This is just a
     * helperfunction for saveSettingsTo. RowFilter implement this and do the
     * work usualy done in saveSettingsTo. The passed config is prepared in a
     * way that the factory will be able to recreate this object from.
     * 
     * @param cfg object to add the current internal settings to.
     */
    protected abstract void saveSettings(final NodeSettingsWO cfg);

    /**
     * called when a new <code>DataTableSpec</code> is available. The filters
     * can grab whatever they need from that new config (e.g. a comparator),
     * should do some error checking (e.g. col number against number of cols) -
     * throw a InvalidSettingsException if settings are invalid, and can return
     * a new table spec according to their settings - if they can. If a filter
     * can not tell how it would modify the spec, it should return null.
     * (Returned table specs are not used right now anyway.)
     * 
     * @param inSpec the new spec propagated into the row filter node. Could be
     *            null or empty!
     * @return a new table spec, if you can.
     * @throws InvalidSettingsException if the settings in the row filter are
     *             not compatible with the table spec coming in.
     */
    public abstract DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException;
    


    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }
 }
