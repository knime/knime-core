/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   01.07.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

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

    private static final String ROWFILTER_MISSVAL = "MissingVal_RowFilter";

    private static final String ROWFILTER_RANGE = "RangeVal_RowFilter";

    private static final String ROWFILTER_STRINGCMP = "StringComp_RowFilter";

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
        } else if (filter instanceof MissingValueRowFilter) {
            cfg.addString(ROWFILTER_TYPEID, ROWFILTER_MISSVAL);
        } else if (filter instanceof RangeRowFilter) {
            cfg.addString(ROWFILTER_TYPEID, ROWFILTER_RANGE);
        } else if (filter instanceof StringCompareRowFilter) {
            cfg.addString(ROWFILTER_TYPEID, ROWFILTER_STRINGCMP);
        } else if (filter instanceof ColValFilterOldObsolete) {
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
        } else if (typeID.equals(ROWFILTER_MISSVAL)) {
            newFilter = new MissingValueRowFilter();
        } else if (typeID.equals(ROWFILTER_RANGE)) {
            newFilter = new RangeRowFilter();
        } else if (typeID.equals(ROWFILTER_STRINGCMP)) {
            newFilter = new StringCompareRowFilter();
        } else if (typeID.equals(ROWFILTER_TRUE)) {
            newFilter = new TrueRowFilter();
        } else if (typeID.equals(ROWFILTER_FALSE)) {
            newFilter = new FalseRowFilter();
        } else if (typeID.equals(ROWFILTER_INV)) {
            newFilter = new NegRowFilter();
        } else if (typeID.equals(ROWFILTER_COLVAL)) {
            newFilter = new ColValFilterOldObsolete();
        } else {
            throw new InvalidSettingsException("Invalid row filter type in"
                    + " config object");
        }

        newFilter.loadSettingsFrom(cfg);
        return newFilter;
    }
}
