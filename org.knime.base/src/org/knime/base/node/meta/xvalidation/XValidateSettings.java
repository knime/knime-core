/* Created on Jun 9, 2006 12:17:56 PM by thor
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 */
package org.knime.base.node.meta.xvalidation;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Simple class for managing the cross validation settings.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class XValidateSettings {
    private short m_validations = 10;

    private boolean m_randomSampling = true;

    private boolean m_leaveOneOut = false;

    private boolean m_stratifiedSampling = false;

    private String m_classColumn;


    /**
     * Returns if leave-one-out cross validation should be performed.
     *
     * @return <code>true</code> if leave-one-out should be done,
     *         <code>false</code> otherwise
     */
    public boolean leaveOneOut() {
        return m_leaveOneOut;
    }

    /**
     * Sets if leave-one-out cross validation should be performed.
     *
     * @param b <code>true</code> if leave-one-out should be done,
     *         <code>false</code> otherwise
     */
    public void leaveOneOut(final boolean b) {
        m_leaveOneOut = b;
    }

    /**
     * Writes the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addShort("validations", m_validations);
        settings.addBoolean("randomSampling", m_randomSampling);
        settings.addBoolean("leaveOneOut", m_leaveOneOut);
        settings.addBoolean("stratifiedSampling", m_stratifiedSampling);
        settings.addString("classColumn", m_classColumn);
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_validations = settings.getShort("validations");
        m_randomSampling = settings.getBoolean("randomSampling");
        m_leaveOneOut = settings.getBoolean("leaveOneOut");
        // added in v2.1
        m_stratifiedSampling = settings.getBoolean("stratifiedSampling", false);
        m_classColumn = settings.getString("classColumn", null);
    }

    /**
     * Returns if the rows of the input table should be sampled randomly.
     *
     * @return <code>true</code> if the should be sampled randomly,
     *         <code>false</code> otherwise
     */
    public boolean randomSampling() {
        return m_randomSampling;
    }

    /**
     * Sets if the rows of the input table should be sampled randomly.
     *
     * @param randomSampling <code>true</code> if the should be sampled
     *            randomly, <code>false</code> otherwise
     */
    public void randomSampling(final boolean randomSampling) {
        m_randomSampling = randomSampling;
    }

    /**
     * Returns if the rows of the input table should be sampled stratified.
     *
     * @return <code>true</code> if the should be sampled stratified,
     *         <code>false</code> otherwise
     */
    public boolean stratifiedSampling() {
        return m_stratifiedSampling;
    }

    /**
     * Sets if the rows of the input table should be sampled stratified.
     *
     * @param stratifiedSampling <code>true</code> if the should be sampled
     *            stratified, <code>false</code> otherwise
     */
    public void stratifiedSampling(final boolean stratifiedSampling) {
        m_stratifiedSampling = stratifiedSampling;
    }

    /**
     * Returns the class column's name for stratified sampling.
     *
     * @return the class column's name
     */
    public String classColumn() {
        return m_classColumn;
    }

    /**
     * Sets the class column's name for stratified sampling.
     *
     * @param classColumn the class column's name
     */
    public void classColumn(final String classColumn) {
        m_classColumn = classColumn;
    }

    /**
     * Returns the number of validation runs that should be performed.
     *
     * @return the number of validation runs
     */
    public short validations() {
        return m_validations;
    }

    /**
     * Sets the number of validation runs that should be performed.
     *
     * @param validations the number of validation runs
     */
    public void validations(final short validations) {
        m_validations = validations;
    }
}
