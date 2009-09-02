/* Created on Jun 9, 2006 12:17:56 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
