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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * Contains the {@link DiscretizationScheme}s for a given columns.
 *
 * @author Christoph, University of Konstanz
 */
public class DiscretizationModel extends AbstractSimplePortObject {

    /**
     * Convenience method to get the type of this port object.
     */
    public static final PortType TYPE = new PortType(DiscretizationModel.class);

    private static final String CONFIG_KEY_SCHEMES = "DiscretizationSchemes";

    private static final String CONFIG_KEY_SCHEME_PREFIX = "Scheme_";

    /**
     * The discretization schemes for the included column.
     */
    private DiscretizationScheme[] m_schemes;

    /**
     * The columns included in the model (used to build the model).
     */
    private DataTableSpec m_includedColumnNames;

    /**
     * Creates a <code>DiscretizationModel</code> from a
     * {@link ModelContentRO} object.
     *
     * @param content the content object to restore the model from
     * @param includedCols the columns included when building in the model
     * @throws InvalidSettingsException if the model content is not valid
     */
    public DiscretizationModel(final ModelContentRO content,
            final DataTableSpec includedCols) throws InvalidSettingsException {
        try {
            load(content, includedCols, null);
        } catch (CanceledExecutionException cee) {
            // won't be canceled.
        }
    }

    /**
     * Creates an empty and invalid model! Don't use the created instance until
     * loading content.
     *
     * @see #load(ModelContentRO, PortObjectSpec, ExecutionMonitor)
     */
    public DiscretizationModel() {
        m_schemes = new DiscretizationScheme[0];
        m_includedColumnNames = new DataTableSpec(new DataColumnSpec[0]);
    }

    /**
     * Creates a new model taking over the schemes (copying them) and storing
     * the included columns.
     *
     * @param schemes the schemes for this model
     * @param includedColumns the included columns, used to build the schemes
     */
    public DiscretizationModel(final DiscretizationScheme[] schemes,
            final DataTableSpec includedColumns) {
        m_schemes = new DiscretizationScheme[schemes.length];
        for (int s = 0; s < m_schemes.length; s++) {
            m_schemes[s] = new DiscretizationScheme(schemes[s]);
        }
        m_includedColumnNames = includedColumns;
    }

    /**
     * Returns names of the columns that are included in the
     * {@link DiscretizationModel}.
     *
     * @return the names of the columns that are included in the
     *         {@link DiscretizationModel}.
     */
    public String[] getIncludedColumnNames() {
        String[] result = new String[m_includedColumnNames.getNumColumns()];
        for (int c = 0; c < m_includedColumnNames.getNumColumns(); c++) {
            result[c] = m_includedColumnNames.getColumnSpec(c).getName();
        }
        return result;
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
        try {
            save(modelContent, null);
        } catch (CanceledExecutionException cee) {
            // won't be canceled
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getSpec() {
        return m_includedColumnNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        if (m_includedColumnNames.getNumColumns() == 0 || m_schemes.length == 0) {
            return "Empty Model";
        }
        return "Binning schemes for " + m_schemes.length + " columns";
    }

    /**
     * {@inheritDoc}
     * <p />
     * IMPORTANT NOTE: This method DOES NOT load the table spec of included
     * columns from the provided model. This table spec must be loaded before
     * and must be provided here. It is taken over and stored.
     */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws InvalidSettingsException,
            CanceledExecutionException {
        try {

            Config schemesConfig = model.getConfig(CONFIG_KEY_SCHEMES);

            m_schemes = new DiscretizationScheme[schemesConfig.getChildCount()];
            int i = 0;
            Enumeration<TreeNode> schemeConfigEnum = schemesConfig.children();
            while (schemeConfigEnum.hasMoreElements()) {
                if (exec != null) {
                    exec.checkCanceled();
                }
                Config schemeConfig = (Config)schemeConfigEnum.nextElement();
                m_schemes[i] = new DiscretizationScheme(schemeConfig);

                i++;
            }

        } catch (Exception e) {
            m_schemes = null;
            m_includedColumnNames = null;
        }
    }

    /**
     * {@inheritDoc}
     * <p />
     * IMPORTANT NOTE: This method DOES NOT save the table spec of included
     * columns. This table spec must be saved before (by calling getSpec and
     * saving the returned object).
     */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {

        Config schemesConfig = model.addConfig(CONFIG_KEY_SCHEMES);

        int i = 0;
        for (DiscretizationScheme scheme : m_schemes) {
            if (exec != null) {
                exec.checkCanceled();
            }
            Config schemeConfig =
                    schemesConfig.addConfig(CONFIG_KEY_SCHEME_PREFIX
                            + m_includedColumnNames.getColumnSpec(i).getName());
            scheme.saveToModelContent(schemeConfig);
            i++;
        }
    }

}
