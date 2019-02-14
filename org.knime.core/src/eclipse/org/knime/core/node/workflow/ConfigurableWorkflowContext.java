/*
 * ------------------------------------------------------------------------
 *
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
 *   Feb 14, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.workflow;

import java.io.File;

import org.knime.core.data.container.DataContainerSettings;

/**
 * This class extends the functionality of the {@link WorkflowContext} by allowing the define specific
 * {@link DataContainerSettings}. This class is solely used for benchmarking purposes.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class ConfigurableWorkflowContext extends WorkflowContext {

    /**
     * Factory.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    public static final class Factory extends WorkflowContext.Factory {

        private final DataContainerSettings m_settings;

        /**
         * Creates a new factory for configurable workflow contexts.
         *
         * @param currentLocation the current workflow location in the filesystem
         * @param settings the data container settings
         * @noreference This constructor is not intended to be referenced by clients.
         */
        public Factory(final File currentLocation, final DataContainerSettings settings) {
            super(currentLocation);
            m_settings = settings;
        }

        /**
         * New instance based on the value of the passed reference.
         *
         * @param origContext To copy from - not null.
         */
        public Factory(final ConfigurableWorkflowContext origContext) {
            super(origContext);
            m_settings = origContext.m_settings;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ConfigurableWorkflowContext createContext() {
            return new ConfigurableWorkflowContext(this);
        }

    }

    /** The {@code DataContainerSettings} . */
    private final DataContainerSettings m_settings;

    /**
     * Constructor.
     *
     * @param factory the factory
     */
    public ConfigurableWorkflowContext(final Factory factory) {
        super(factory);
        m_settings = factory.m_settings;
    }

    @Override
    public Factory createCopy() {
        return new Factory(this);
    }

    /**
     * Returns the {@link DataContainerSettings} settings.
     *
     * @return the data container settings
     * @noreference This method is not intended to be referenced by clients.
     */
    public DataContainerSettings getContainerSettings() {
        return m_settings;
    }

}
