/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ------------------------------------------------------------------- *
 */
package org.knime.base.node.io.filereader;

import java.lang.reflect.Constructor;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;

/**
 * This a a little helper class that enables the FileReader to create
 * SmilesCells if the chem-Plugin is available. Because it is not visible from
 * the base plugin, the necessary classes and fields are loaded via reflection.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
final class SmilesTypeHelper {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(SmilesTypeHelper.class);

    /** The one and only instance of this class. */
    public static final SmilesTypeHelper INSTANCE = new SmilesTypeHelper();

    private Constructor<? extends DataCell> m_cons;

    private DataType m_smilesType;

    @SuppressWarnings("unchecked")
    private SmilesTypeHelper() {
        try {
            Class<? extends DataCell> smilesClass =
                    (Class<? extends DataCell>)Class
                            .forName("org.knime.chem.types.SmilesCell");
            m_cons = smilesClass.getConstructor(String.class);
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
