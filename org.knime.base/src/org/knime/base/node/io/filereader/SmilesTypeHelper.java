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
package org.knime.base.node.io.filereader;

import java.lang.reflect.Constructor;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.NodeLogger;

/**
 * This a a little helper class that enables the FileReader to create
 * SmilesCells if the chem-Plugin is available. Because it is not visible from
 * the base plugin, the necessary classes and fields are loaded via reflection.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
final class SmilesTypeHelper {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SmilesTypeHelper.class);

    /** The one and only instance of this class. */
    public static final SmilesTypeHelper INSTANCE = new SmilesTypeHelper();

    private Constructor<? extends DataCell> m_cons;

    private DataType m_smilesType;

    @SuppressWarnings("unchecked")
    private SmilesTypeHelper() {
        try {
            Class<? extends DataCell> smilesClass =
                    (Class<? extends DataCell>)GlobalClassCreator
                            .createClass("org.knime.chem.types.SmilesCell");
            m_cons = (Constructor<? extends DataCell>)smilesClass
                            .getConstructor(String.class);
            if (m_cons == null) {
                throw new NullPointerException("Smiles cell doesn't provide "
                        + "the required String constructor.");
            }
            m_smilesType = DataType.getType(smilesClass);
            
        } catch (ClassNotFoundException ex) {
            LOGGER.info("Smiles type not available", ex);
        } catch (Exception ex) {
            LOGGER.error("Smiles type not available", ex);
        }
    }

    /**
     * Creates a new SmilesCell using the given String as Smiles.
     * 
     * @param smiles a Smiles string
     * @return a SmilesCell
     */
    public DataCell newInstance(final String smiles) {
        try {
            return m_cons.newInstance(smiles);
        } catch (Exception ex) {
            throw new IllegalStateException("Couldn't create SMILES cell.");
        } 
    }

    /**
     * Returns the Smiles type.
     * 
     * @return the Smiles type
     */
    public DataType getSmilesType() {
        return m_smilesType;
    }

    /**
     * Returns if SmilesCells are available.
     * 
     * @return <code>true</code> if SmilesCells are available,
     *         <code>false</code> otherwise
     */
    public boolean isSmilesAvailable() {
        return (m_smilesType != null);
    }
}
