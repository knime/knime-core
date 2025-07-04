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
 *   Dec 13, 2024 (hornm): created
 */
package org.knime.node;

import java.util.Optional;
import java.util.function.BiConsumer;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.FluentNodeAPI;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;

/**
 * Fluent API to create a node model - not to be created directly but via the {@link DefaultNode}.
 *
 * The model of a node defines the logic of a node's configuration and execution. It requires the configuration logic,
 * the execution logic, and the model settings.<br>
 * The configuration step generates the expected output specs based on the given input specs and model settings. The
 * execution step transforms the input data.<br>
 * Model settings alter the output spec or data, i.e. whenever a model setting changes, the node is reexecuted.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public sealed abstract class DefaultModel implements FluentNodeAPI {

    private final Class<? extends DefaultNodeSettings> m_settingsClass;

    private DefaultModel(final Class<? extends DefaultNodeSettings> settingsClass) {
        m_settingsClass = settingsClass;
    }

    static RequireModelSettings create() {
        return settingsClass -> new RequireConfigureOrRearrangeColumns() {

            @Override
            public DefaultModel rearrangeColumns(
                final BiConsumerWithInvalidSettingsException<RearrangeColumnsInput, RearrangeColumnsOutput> rearrangeColumns) {
                return new RearrangeColumnsDefaultModel(settingsClass, rearrangeColumns);
            }

            @Override
            public RequireExecute
                configure(final BiConsumerWithInvalidSettingsException<ConfigureInput, ConfigureOutput> configure) {
                return execute -> new StandardDefaultModel(settingsClass, configure, execute);
            }
        };

    }

    record ConfigureAndExecute(BiConsumer<ConfigureInput, ConfigureOutput> configure,
        BiConsumer<ExecuteInput, ExecuteOutput> execute) {
    }

    static final class StandardDefaultModel extends DefaultModel {
        final ExecuteConsumer m_execute;

        final BiConsumerWithInvalidSettingsException<ConfigureInput, ConfigureOutput> m_configure;

        private StandardDefaultModel(final Class<? extends DefaultNodeSettings> settingsClass,
            final BiConsumerWithInvalidSettingsException<ConfigureInput, ConfigureOutput> configure,
            final ExecuteConsumer execute) {
            super(settingsClass);
            m_configure = configure;
            m_execute = execute;
        }

    }

    static final class RearrangeColumnsDefaultModel extends DefaultModel {
        final BiConsumerWithInvalidSettingsException<RearrangeColumnsInput, RearrangeColumnsOutput> m_rearrangeColumns;

        private RearrangeColumnsDefaultModel(final Class<? extends DefaultNodeSettings> settingsClass,
            final BiConsumerWithInvalidSettingsException<RearrangeColumnsInput, RearrangeColumnsOutput> rearrangeColumns) {
            super(settingsClass);
            m_rearrangeColumns = rearrangeColumns;
        }
    }

    Optional<Class<? extends DefaultNodeSettings>> getSettingsClass() {
        return Optional.ofNullable(m_settingsClass);
    }

    /**
     * The build stage that requires the model settings.
     */
    public interface RequireModelSettings {

        /**
         * @param settingsClass the model settings of the node
         * @return the subsequent build stage
         */
        RequireConfigureOrRearrangeColumns settingsClass(Class<? extends DefaultNodeSettings> settingsClass);

        /**
         * Indicates that the model does not have model settings.
         *
         * @return the next build stage
         */
        default RequireConfigureOrRearrangeColumns withoutSettings() {
            return settingsClass(null);
        }
    }

    /**
     * The build stage for defining how the node modifies or rearranges input data. This can either be a general
     * configuration method or a column-rearranging operation.
     */
    public interface RequireConfigureOrRearrangeColumns {

        /**
         * Used when the node has a different port configuration than one input and one output table port, and performs
         * operations beyond simple column-based modifications. It should use the provided {@link ConfigureInput} to
         * inspect the input and populate the corresponding {@link ConfigureOutput} accordingly.
         *
         * @param configure a function receiving the {@link ConfigureInput} and {@link ConfigureOutput}
         * @return the subsequent build stage
         */
        RequireExecute
            configure(final BiConsumerWithInvalidSettingsException<ConfigureInput, ConfigureOutput> configure);

        /**
         * Used when the node has one input and one output table port, and performs column-based operations—such as
         * deleting, replacing, or adding columns—to compute the output table. It should use the provided
         * {@link RearrangeColumnsInput} to inspect the input and populate the corresponding
         * {@link RearrangeColumnsInput} accordingly.
         *
         * @param rearrangeColumns a function receiving the {@link RearrangeColumnsInput} and
         *            {@link RearrangeColumnsOutput}
         * @return the {@link DefaultModel}
         */
        DefaultModel rearrangeColumns(
            final BiConsumerWithInvalidSettingsException<RearrangeColumnsInput, RearrangeColumnsOutput> rearrangeColumns);

    }

    /**
     * A {@link BiConsumer} that can throw an {@link InvalidSettingsException}.
     */
    @FunctionalInterface
    public interface BiConsumerWithInvalidSettingsException<I, O> {

        void accept(I input, O output) throws InvalidSettingsException;

    }

    /**
     * This interface is used within the
     * {@link RequireConfigureOrRearrangeColumns#configure(BiConsumerWithInvalidSettingsException)} phase of a node
     * model and provides access to the input specs.
     */
    public interface ConfigureInput {

        <S extends DefaultNodeSettings> S getSettings();

        /**
         * @param index the index of the input port
         * @param <S> the type of the input spec
         * @return the input specification at the specified index
         */
        <S extends PortObjectSpec> S getInSpec(int index);

        /**
         * @param <S> the type of the input specs
         * @return an array containing all input specs
         */
        <S extends PortObjectSpec> S[] getInSpecs();

    }

    /**
     * This interface is used within the
     * {@link RequireConfigureOrRearrangeColumns#configure(BiConsumerWithInvalidSettingsException)} phase of a node
     * model and provides methods to define output specs and access settings.
     */
    public interface ConfigureOutput {

        /**
         * @param index the index at which to set the input spec
         * @param spec the output spec to set
         * @param <S> the type of the output spec
         */
        <S extends PortObjectSpec> void setOutSpec(int index, S spec);

        /**
         * @param <S> the type of the output specs
         * @param specs an array containing all output specs
         */
        <S extends PortObjectSpec> void setOutSpec(S... specs);

    }

    /**
     * This interface is used within the {@link RequireExecute#execute(BiConsumer)} phase of a node model and provides
     * access to the settings, input data, and execution context.
     */
    public interface ExecuteInput {

        /**
         * @param <S> the type of settings
         * @return the current settings
         */
        <S extends DefaultNodeSettings> S getSettings();

        /**
         * @param index the index of the input port
         * @param <D> the type of the input data
         * @return the input data at the specified index
         */
        <D extends PortObject> D getInData(int index);

        /**
         * @param <D> the type of the input data
         * @return the input data of each port
         */
        <D extends PortObject> D[] getInData();

        /**
         * @return the current execution context
         */
        ExecutionContext getExecutionContext();

    }

    /**
     * This interface is used within the {@link RequireExecute#execute(BiConsumer)} phase of a node model and provides
     * methods to define output data and to specify a warning message for issues that occurred during the execution.
     */
    public interface ExecuteOutput {

        /**
         * @param index the index of the output port
         * @param data the output data to set
         */
        void setOutData(int index, PortObject data);

        /**
         * @param data the output data to set
         */
        void setOutData(PortObject... data);

        /**
         * @param data the data to keep in the node for the use in a view (see
         *            {@link DefaultView.ViewInput#getInternalTables()})
         */
        void setInternalPortObjects(PortObject... data);

        /**
         * @param message a warning message informing the user about an issue that occurred during execution
         */
        void setWarningMessage(String message);

    }

    /**
     * This interface is used within the
     * {@link RequireConfigureOrRearrangeColumns#rearrangeColumns(BiConsumerWithInvalidSettingsException)} phase of a
     * node model and provides access to the current settings, the input data table spec, and an
     * {@link ColumnRearranger}.
     */
    public interface RearrangeColumnsInput {

        /**
         * @param <S> the type of settings
         * @return the current settings
         */
        <S extends DefaultNodeSettings> S getSettings();

        /**
         * @return the data table spec of the input port
         */
        DataTableSpec getDataTableSpec();

        /**
         * @return a {@link ColumnRearranger}
         */
        ColumnRearranger getColumnRearranger();
    }

    /**
     * This interface is used within the
     * {@link RequireConfigureOrRearrangeColumns#rearrangeColumns(BiConsumerWithInvalidSettingsException)} phase of a
     * node model and provides methods to specify the column rearranger.
     */
    public interface RearrangeColumnsOutput {

        /**
         * @param rearranger set the populated {@link ColumnRearranger}
         */
        void setColumnRearranger(ColumnRearranger rearranger);
    }

    /**
     * The build stage requiring the execution logic of the node, i.e. it operates on the input data.
     */
    public interface RequireExecute {

        /**
         * Specifies the execution logic of the node. It should use the provided {@link ExecuteInput} to receive the
         * input data and populate the corresponding {@link ExecuteOutput} accordingly.
         *
         * @param execute a function receiving the {@link ExecuteInput} and {@link ExecuteOutput}.
         * @return the {@link DefaultModel}
         */
        DefaultModel execute(ExecuteConsumer execute);

    }

    public interface ExecuteConsumer {
        void accept(ExecuteInput input, ExecuteOutput output) throws Exception;
    }

}
