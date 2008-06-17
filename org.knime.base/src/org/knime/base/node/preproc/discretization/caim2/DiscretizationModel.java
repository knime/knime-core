/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 9, 2006 (sieb): created
 */
package org.knime.base.node.preproc.discretization.caim2;

import java.util.Enumeration;

import javax.swing.tree.TreeNode;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.config.Config;

/**
 * Contains the {@link DiscretizationScheme}s for a given columns.
 * 
 * @author Christoph, University of Konstanz
 */
public class DiscretizationModel {

    private static final String CONFIG_KEY_COLUMN_NANES = "IncludedColumns";

    private static final String CONFIG_KEY_SCHEMES = "DiscretizationSchemes";

    private static final String CONFIG_KEY_SCHEME_PREFIX = "Scheme_";

    /**
     * The discretization schemes for the included column.
     */
    private DiscretizationScheme[] m_schemes;

    /**
     * The names of the included columns.
     */
    private String[] m_includedColumnNames;

    /**
     * Creates a <code>DiscretizationModel</code> from the included column
     * names and the column {@link DiscretizationScheme}s. Note: the indices of
     * the parameter arrays must corespond to each other.
     * 
     * @param includedColumnNames the included column names
     * @param discretizationSchemes the {@link DiscretizationScheme}s
     */
    public DiscretizationModel(final String[] includedColumnNames,
            final DiscretizationScheme[] discretizationSchemes) {
        m_includedColumnNames = includedColumnNames;
        m_schemes = discretizationSchemes;
    }

    /**
     * Creates a <code>DiscretizationModel</code> from a
     * {@link ModelContentRO} object.
     * 
     * @param content the content object to restore the model from
     * @throws InvalidSettingsException if the model content is not valid
     */
    public DiscretizationModel(final ModelContentRO content)
            throws InvalidSettingsException {

        m_includedColumnNames = content.getStringArray(CONFIG_KEY_COLUMN_NANES);

        Config schemesConfig = content.getConfig(CONFIG_KEY_SCHEMES);

        m_schemes = new DiscretizationScheme[schemesConfig.getChildCount()];
        int i = 0;
        Enumeration<TreeNode> schemeConfigEnum = schemesConfig.children();
        while (schemeConfigEnum.hasMoreElements()) {

            Config schemeConfig = (Config)schemeConfigEnum.nextElement();
            m_schemes[i] = new DiscretizationScheme(schemeConfig);

            i++;
        }
    }

    /**
     * Returns names of the columns that are included in the
     * {@link DiscretizationModel}.
     * 
     * @return the names of the columns that are included in the
     *         {@link DiscretizationModel}.
     */
    public String[] getIncludedColumnNames() {
        return m_includedColumnNames;
    }

    /**
     * Returns {@link DiscretizationScheme}s of the columns that are included
     * in the {@link DiscretizationModel}.
     * 
     * @return the {@link DiscretizationScheme}s of the columns that are
     *         included in the {@link DiscretizationModel}.
     */
    public DiscretizationScheme[] getSchemes() {
        return m_schemes;
    }

    /**
     * Saves this model to a {@link ModelContentWO} object.
     * 
     * @param modelContent the {@link ModelContentWO} object to store the
     *            {@link DiscretizationModel} to
     */
    public void saveToModelContent(final ModelContentWO modelContent) {

        modelContent.addStringArray(CONFIG_KEY_COLUMN_NANES,
                m_includedColumnNames);

        Config schemesConfig = modelContent.addConfig(CONFIG_KEY_SCHEMES);

        int i = 0;
        for (DiscretizationScheme scheme : m_schemes) {

            Config schemeConfig =
                    schemesConfig.addConfig(CONFIG_KEY_SCHEME_PREFIX
                            + m_includedColumnNames[i]);
            scheme.saveToModelContent(schemeConfig);
            i++;
        }
    }
}
