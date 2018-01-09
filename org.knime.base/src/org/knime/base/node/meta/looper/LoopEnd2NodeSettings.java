/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   15.05.2009 (meinl): created
 */
package org.knime.base.node.meta.looper;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * This class holds the settings for the generic loop end node (2 ports).
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.9
 */
public class LoopEnd2NodeSettings extends AbstractLoopEndNodeSettings {

    /** @since 2.9 */
    private boolean m_ignoreEmptyTables1 = true;

    /** @since 2.9 */
    private boolean m_ignoreEmptyTables2 = true;

    /** @since 2.11 */
    private boolean m_tolerateColumnTypes1 = false;

    /** @since 2.11 */
    private boolean m_tolerateColumnTypes2 = false;

    /** @since 3.1 */
    private boolean m_tolerateChangingSpecs1 = false;

    /** @since 3.1 */
    private boolean m_tolerateChangingSpecs2 = false;

    /**
     * Sets if iterations with empty tables are ignored in the first output port.
     *
     * @param ignore <code>true</code> empty tables will be ignored,
     *               <code>false</code> empty tables that have different specs will cause an exception
     * @since 2.9
     */
    public void ignoreEmptyTables1(final boolean ignore) {
        m_ignoreEmptyTables1 = ignore;
    }

    /**
     * Returns if iterations with empty tables are ignored in the first output port.
     *
     * @return <code>true</code> empty tables will be ignored,
     *         <code>false</code> empty tables that have different specs will cause an exception
     * @since 2.9
     */
    public boolean ignoreEmptyTables1() {
        return m_ignoreEmptyTables1;
    }

    /**
     * Sets if iterations with empty tables are ignored in the second output port.
     *
     * @param ignore <code>true</code> empty tables will be ignored,
     *               <code>false</code> empty tables that have different specs will cause an exception
     * @since 2.9
     */
    public void ignoreEmptyTables2(final boolean ignore) {
        m_ignoreEmptyTables2 = ignore;
    }

    /**
     * Returns if iterations with empty tables are ignored in the second output port.
     *
     * @return <code>true</code> empty tables will be ignored,
     *         <code>false</code> empty tables that have different specs will cause an exception
     * @since 2.9
     */
    public boolean ignoreEmptyTables2() {
        return m_ignoreEmptyTables2;
    }

    /**
     * Sets if column types in different tables at port 1 are merged.
     * @param tolerate <code>true</code> merge columns types,
     *                 <code>false</code> don't merge column types.
     * @since 2.11
     */
    public void tolerateColumnTypes1(final boolean tolerate) {
        m_tolerateColumnTypes1 = tolerate;
    }

    /**
     * Returns if column types in different tables at port 1 are merged.
     * @return tolerate <code>true</code> merge columns types,
     *                  <code>false</code> don't merge column types.
     * @since 2.11
     */
    public boolean tolerateColumnTypes1() {
        return m_tolerateColumnTypes1;
    }

    /**
     * Sets if column types in different tables at port 2 are merged.
     * @param tolerate <code>true</code> merge columns types,
     *                 <code>false</code> don't merge column types.
     * @since 2.11
     */
    public void tolerateColumnTypes2(final boolean tolerate) {
        m_tolerateColumnTypes2 = tolerate;
    }

    /**
     * Returns if column types in different tables at port 2 are merged.
     * @return tolerate <code>true</code> merge columns types,
     *                  <code>false</code> don't merge column types.
     * @since 2.11
     */
    public boolean tolerateColumnTypes2() {
        return m_tolerateColumnTypes2;
    }

    /**
     * Returns if changing tables specs are to be tolerated at port 1.
     *
     * @param tolerate <code>true</code> changes are tolerated and missing values are inserted for missing column in
     *            respective iterations <code>false</code> the node fails if table spec varies
     * @since 3.1
     */
    public void tolerateChangingTableSpecs1(final boolean tolerate) {
        m_tolerateChangingSpecs1 = tolerate;
    }

    /**
     * Returns if changing tables specs are to be tolerated at port 1.
     *
     * @return <code>true</code> changes are tolerated and missing values are inserted for missing column in respective
     *         iterations
     *         <code>false</code> the node fails if table spec varies
     * @since 3.1
     */
    public boolean tolerateChangingTableSpecs1() {
        return m_tolerateChangingSpecs1;
    }

    /**
     * Returns if changing tables specs are to be tolerated at port 2.
     *
     * @param tolerate <code>true</code> changes are tolerated and missing values are inserted for missing column in
     *            respective iterations <code>false</code> the node fails if table spec varies
     * @since 3.1
     */
    public void tolerateChangingTableSpecs2(final boolean tolerate) {
        m_tolerateChangingSpecs2 = tolerate;
    }

    /**
     * Returns if changing tables specs are to be tolerated at port 2.
     *
     * @return <code>true</code> changes are tolerated and missing values are inserted for missing column in respective
     *         iterations
     *         <code>false</code> the node fails if table spec varies
     * @since 3.1
     */
    public boolean tolerateChangingTableSpecs2() {
        return m_tolerateChangingSpecs2;
    }


    /**
     * Writes the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        super.saveSettings(settings);
        settings.addBoolean("ignoreEmptyTables1", m_ignoreEmptyTables1);
        settings.addBoolean("ignoreEmptyTables2", m_ignoreEmptyTables2);
        settings.addBoolean("tolerateColumnTypes1", m_tolerateColumnTypes1);
        settings.addBoolean("tolerateColumnTypes2", m_tolerateColumnTypes2);
        settings.addBoolean("tolerateChangingSpecs1", m_tolerateChangingSpecs1);
        settings.addBoolean("tolerateChangingSpecs2", m_tolerateChangingSpecs2);
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     */
    @Override
    public void loadSettings(final NodeSettingsRO settings) {
        super.loadSettings(settings);
        m_ignoreEmptyTables1 = settings.getBoolean("ignoreEmptyTables1", true);
        m_ignoreEmptyTables2 = settings.getBoolean("ignoreEmptyTables2", true);
        m_tolerateColumnTypes1 = settings.getBoolean("tolerateColumnTypes1", false);
        m_tolerateColumnTypes2 = settings.getBoolean("tolerateColumnTypes2", false);
        m_tolerateChangingSpecs1 = settings.getBoolean("tolerateChangingSpecs1", false);
        m_tolerateChangingSpecs2 = settings.getBoolean("tolerateChangingSpecs2", false);
    }
}
