/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   01.07.2005 (ohl): created
 */
package de.unikn.knime.base.node.filter.row.rowfilter;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Used to create {@link RowFilter} objects from
 * {@link org.knime.core.node.NodeSettings}. Each row filter must be
 * registered here (i.e. code must be added). Scan the file for "CHANGE HERE".
 * RowFilter must provide the default constructor.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public final class RowFilterFactory {

    /*
     * CHANGE HERE: New row filters must add a new (unique) config key here. It
     * will be used as an identifier for the row filter type in the config
     * object.
     */
    private static final String ROWFILTER_AND = "AND_RowFilter";

    private static final String ROWFILTER_OR = "OR_RowFilter";

    private static final String ROWFILTER_ROWID = "RowID_RowFilter";

    private static final String ROWFILTER_ROWNO = "RowNumber_RowFilter";

    private static final String ROWFILTER_COLVAL = "ColVal_RowFilter";

    private static final String ROWFILTER_TRUE = "Tauto_RowFilter";

    private static final String ROWFILTER_FALSE = "Kontra_RowFilter";

    private static final String ROWFILTER_INV = "Inverter_RowFilter";

    private static final String ROWFILTER_TYPEID = "RowFilter_TypeID";

    private RowFilterFactory() {
        // only use the static functions of this class!!!
    }

    /**
     * @param filter the filter for which the config object will be used to
     *            store the settings
     * @param cfg will be modified to be able to recreate the corresponding
     *            filter type from it
     * @return the config object passed in, modified in a way the
     *         {@link #createRowFilter(NodeSettingsRO)} method will be able to
     *         recreate the row filter object (with the corresponding type) from
     *         it. The method adds a row filter type identifier to the config.
     *         Passing the returned object to the
     *         {@link #createRowFilter(NodeSettingsRO)} method will recreate the
     *         filter. If <code>null</code> is returned the row filter type
     *         was not properly added to this factory - which should not happen.
     */
    public static NodeSettingsWO prepareConfigFor(final NodeSettingsWO cfg,
            final RowFilter filter) {

        /*
         * CHANGE HERE: Add a new "else if" branch to test for your new Row
         * Filter type and add a String to the cfg object with key
         * ROWFILTER_TYPEID and the new ROWFILTER_ <yourType> you've added
         * above.
         */
        if (filter == null) {
            throw new NullPointerException("Cannot create config for null"
                    + " filter object");
        } else if (filter instanceof AndRowFilter) {
            cfg.addString(ROWFILTER_TYPEID, ROWFILTER_AND);
        } else if (filter instanceof OrRowFilter) {
            cfg.addString(ROWFILTER_TYPEID, ROWFILTER_OR);
        } else if (filter instanceof RowIDRowFilter) {
            cfg.addString(ROWFILTER_TYPEID, ROWFILTER_ROWID);
        } else if (filter instanceof RowNoRowFilter) {
            cfg.addString(ROWFILTER_TYPEID, ROWFILTER_ROWNO);
        } else if (filter instanceof TrueRowFilter) {
            cfg.addString(ROWFILTER_TYPEID, ROWFILTER_TRUE);
        } else if (filter instanceof FalseRowFilter) {
            cfg.addString(ROWFILTER_TYPEID, ROWFILTER_FALSE);
        } else if (filter instanceof NegRowFilter) {
            cfg.addString(ROWFILTER_TYPEID, ROWFILTER_INV);
        } else if (filter instanceof ColValRowFilter) {
            cfg.addString(ROWFILTER_TYPEID, ROWFILTER_COLVAL);
        } else {
            assert false : "The row filter type must be "
                    + "registered with the Factory";
            return null;
        }
        return cfg;

    }

    /**
     * @param cfg config of a filter (created with the above method) to create
     *            the corresponding filter for. The settings in the config will
     *            be loaded into the filter.
     * @return a (configured) row filter of the type that was used to create the
     *         passed config spec.
     * @throws InvalidSettingsException if the config object contains no type ID
     *             (then it was probably not prepared with the method above), or
     *             if it contains an unknown filter type - which either means
     *             the type is not registered at all or the type was added to
     *             the method above but not to this method, of if it contains
     *             invalid/inconsistent settings.
     */
    public static RowFilter createRowFilter(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        /*
         * CHANGE HERE: Add a new "else if" branch testing the type ID to be
         * equal to your newly defined ROWFILTER_ <yourType> string. And if so,
         * return a new instance of your new row filter.
         */
        if (cfg == null) {
            throw new NullPointerException(
                    "Can't create filter from null config");
        }
        String typeID;
        try {
            typeID = cfg.getString(ROWFILTER_TYPEID);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("No row filter type specified"
                    + "in config object");
        }

        RowFilter newFilter = null;

        if (typeID == null) {
            throw new InvalidSettingsException("Invalid row filter type in"
                    + " config object");
        } else if (typeID.equals(ROWFILTER_AND)) {
            newFilter = new AndRowFilter();
        } else if (typeID.equals(ROWFILTER_OR)) {
            newFilter = new OrRowFilter();
        } else if (typeID.equals(ROWFILTER_ROWID)) {
            newFilter = new RowIDRowFilter();
        } else if (typeID.equals(ROWFILTER_ROWNO)) {
            newFilter = new RowNoRowFilter();
        } else if (typeID.equals(ROWFILTER_TRUE)) {
            newFilter = new TrueRowFilter();
        } else if (typeID.equals(ROWFILTER_FALSE)) {
            newFilter = new FalseRowFilter();
        } else if (typeID.equals(ROWFILTER_INV)) {
            newFilter = new NegRowFilter();
        } else if (typeID.equals(ROWFILTER_COLVAL)) {
            newFilter = new ColValRowFilter();
        } else {
            throw new InvalidSettingsException("Invalid row filter type in"
                    + " config object");
        }

        assert newFilter != null;

        newFilter.loadSettingsFrom(cfg);
        return newFilter;
    }
}
