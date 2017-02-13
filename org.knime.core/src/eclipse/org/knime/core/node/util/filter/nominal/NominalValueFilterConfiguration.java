/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 8, 2017 (ferry): created
 */
package org.knime.core.node.util.filter.nominal;

import java.util.ArrayList;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.filter.NameFilterConfiguration;

/**
 * Configuration for a nominal value filter that can include and exclude names and takes care on additional/missing names
 * using the enforce inclusion/exclusion option. It also supports filtering based on name patterns.
 *
 * @author Ferry Abt, KNIME.com AG, Zurich, Switzerland
 * @since 3.3
 */
public class NominalValueFilterConfiguration extends NameFilterConfiguration {

    /**
     * Creates a new nominal value filter configuration with the given settings name.
     *
     * @param configRootName the config name to used to store the settings
     * @throws IllegalArgumentException If config name is null or empty
     */
    public NominalValueFilterConfiguration(final String configRootName) {
        super(configRootName);
    }

    /**
     *
     * Load config in dialog, init defaults if necessary.
     *
     * @param settings to load from.
     * @param domain of the column to be filtered.
     *
     */
    public void loadConfigurationInDialog(final NodeSettingsRO settings, final Set<DataCell> domain) {
        ArrayList<String> names = new ArrayList<String>();
        if (domain != null) {
            for (DataCell dc : domain) {
                names.add(dc.toString());
            }
        }
        super.loadConfigurationInDialog(settings, names.toArray(new String[names.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getIncludeList() {
        return super.getIncludeList();
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getExcludeList() {
        return super.getExcludeList();
    }

    /**
     * Create and return a new name filter that contains a list of include and exclude names based on the
     * current configuration and domain provided as an argument.
     * @see NameFilterConfiguration#applyTo(String[])
     *
     * @param domain the domain to apply the current configuration on
     * @return a new name filter
     */
    public FilterResult applyTo(final Set<DataCell> domain) {
        ArrayList<String> names = new ArrayList<String>();
        //get array of domain values
        if (domain != null) {
            for (DataCell dc : domain) {
                names.add(dc.toString());
            }
        }
        return super.applyTo(names.toArray(new String[names.size()]));
    }

}
