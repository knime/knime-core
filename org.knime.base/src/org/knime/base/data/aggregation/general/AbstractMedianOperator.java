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
 *   Apr 26, 2017 (marcel): created
 */
package org.knime.base.data.aggregation.general;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * Abstract base class for aggregation operators that return the median of a group. This class covers
 * {@link DataType}-independent operations such as extracting the middle element of an uneven, sorted list as median. It
 * is meant to be extended to handle cases that depend on the list's specific {@link DataType}, such as returning the
 * mean of the two middle elements as median when the list is even.
 * <P>
 * Allows the user to choose an extraction method when dealing with even lists (mean of middle elements, upper median,
 * lower median).
 *
 * @since 3.4
 *
 * @author Marcel Wiedenmann, KNIME.com, Konstanz, Germany
 */
public abstract class AbstractMedianOperator extends SortedListCellOperator {

    /**
     * The default label and result column name.
     */
    protected static final String LABEL = "Median";

    /**
     * @param utility utility of the implementation's specific {@DataValue}
     * @return unique identifier used for registration
     */
    protected static String formatId(final ExtensibleUtilityFactory utility) {
        return LABEL + " " + utility.getName();
    }

    /**
     * @param meanMedianMethod the method for calculating an even list's median as mean of the two middle elements
     * @return the default set of methods for median extraction
     */
    protected static EvenListMedianMethodDescription[]
        createDefaultMedianMethodDescriptions(final EvenListMedianMethod meanMedianMethod) {
        return new EvenListMedianMethodDescription[]{EvenListMedianMethodDescription.LOWER_MEDIAN,
            new EvenListMedianMethodDescription(EvenListMedianMethodDescription.MEAN_MEDIAN_LABEL,
                "Use the mean of the group's two middle values as median.", meanMedianMethod),
            EvenListMedianMethodDescription.UPPER_MEDIAN};
    }

    private final MedianSettings m_settings;

    private final EvenListMedianMethodDescription[] m_methodDescs;

    private final Map<String, EvenListMedianMethod> m_methods;

    private MedianSettingsPanel m_settingsPanel;

    /**
     * Constructor for class AbstractMedianOperator.
     *
     * @param id the unique identifier of the specific implementation of this aggregation operator used for registration
     * @param keepColSpec <code>true</code> if the original column specification should be kept if possible
     * @param supportedClass the {@link DataValue} class supported by the specific implementation of this aggregation
     *            operator
     * @param meanMedianMethod the method for calculating an even list's median as mean of the two middle elements
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    protected AbstractMedianOperator(final String id, final boolean keepColSpec,
        final Class<? extends DataValue> supportedClass, final EvenListMedianMethod meanMedianMethod,
        final GlobalSettings globalSettings, final OperatorColumnSettings opColSettings) {
        this(createDefaultMedianMethodDescriptions(meanMedianMethod),
            new OperatorData(id, LABEL, LABEL, true, keepColSpec, supportedClass, false), globalSettings,
            AggregationOperator.setInclMissingFlag(opColSettings, false));
    }

    /**
     * Constructor for class AbstractMedianOperator.
     *
     * @param methodDescs the available median methods and their descriptions for the specific implementation of this
     *            aggregation operator
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    protected AbstractMedianOperator(final EvenListMedianMethodDescription[] methodDescs,
        final OperatorData operatorData, final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
        m_settings = new MedianSettings();
        m_methodDescs = methodDescs;
        m_methods = new HashMap<>(methodDescs.length);
        for (final EvenListMedianMethodDescription methodDesc : methodDescs) {
            m_methods.put(methodDesc.getActionCommand(), methodDesc.getMethod());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the median per group. Missing values are skipped. "
            + "You can choose between different calculation methods for handling even groups. "
            + "By default, the mean of the two middle elements of the group is chosen as median.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasOptionalSettings() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MedianSettingsPanel getSettingsPanel() {
        if (m_settingsPanel == null) {
            m_settingsPanel = new MedianSettingsPanel(m_settings, m_methodDescs);
        }
        return m_settingsPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
        throws NotConfigurableException {
        getSettingsPanel().loadSettingsFrom(settings, new PortObjectSpec[]{spec});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws InvalidSettingsException {
        m_settings.validate(m_methods);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final DataTableSpec spec) throws InvalidSettingsException {
        m_settings.configure(spec);
    }

    /**
     * @return the name of the selected method for extracting the median from an even list
     */
    protected String getMedianMethod() {
        return m_settings.getMedianMethodModel().getStringValue();
    }

    /**
     * @param name the name of the method to select for extracting the median from an even list
     */
    protected void setMedianMethod(final String name) {
        m_settings.getMedianMethodModel().setStringValue(name);
    }

    /**
     * @return the description of all available methods for extracting the median from an even list
     */
    protected EvenListMedianMethodDescription[] getMedianMethodDescriptions() {
        return m_methodDescs.clone();
    }

    /**
     * This method is called when the input list is uneven and, hence, the median clearly defined. Allows transformation
     * of the cell to match {@link #getDataType(DataType)} if necessary.
     *
     * @param median the unique median
     * @return the argument, possibly transformed
     */
    protected DataCell getResultInternal(final DataCell median) {
        return median;
    }

    /**
     * This method is called when the input list is even and, hence, different median definitions may be applied.
     * Returns the even list's median given the two middle values.
     *
     * @param cells the list
     * @param lowerCandidateIdx the index of the lower middle value
     * @param upperCandidateIdx the index of the upper middle value
     * @return the median
     */
    protected DataCell getResultInternal(final List<DataCell> cells, final int lowerCandidateIdx,
        final int upperCandidateIdx) {
        // use user-selected method to extract median
        final EvenListMedianMethod method = m_methods.get(getMedianMethod());
        if (method == null) {
            throw new RuntimeException("Selected method not found.");
        }
        return method.extractMedian(cells, lowerCandidateIdx, upperCandidateIdx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected abstract DataType getDataType(DataType origType);

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        final List<DataCell> cells = getCells();
        final int size = cells.size();
        if (size == 0) {
            return DataType.getMissingCell();
        }
        if (size == 1) {
            return getResultInternal(cells.get(0));
        }
        sortCells(cells);
        final double middle = size / 2.0;
        if (middle > (int)middle) {
            // list is uneven
            return getResultInternal(cells.get((int)middle));
        }
        return getResultInternal(cells, (int)middle - 1, (int)middle);
    }

    /**
     * A method for extracting a median from an even list.
     */
    protected static interface EvenListMedianMethod {

        /**
         * Returns the even list's median given the two middle values.
         *
         * @param cells the list
         * @param lowerCandidateIdx the index of the lower middle value
         * @param upperCandidateIdx the index of the upper middle value
         * @return the median
         */
        public DataCell extractMedian(List<DataCell> cells, int lowerCandidateIdx, int upperCandidateIdx);
    }

    /**
     * Description of an extraction method. Used in the settings panel of this aggregation operator.
     */
    protected static class EvenListMedianMethodDescription implements ButtonGroupEnumInterface {

        /**
         * The default label for the method that extracts the lesser of the two middle values.
         */
        protected static final String LOWER_MEDIAN_LABEL = "Lower middle value";

        /**
         * The default label for the method that takes the mean of the two middle values as median.
         */
        protected static final String MEAN_MEDIAN_LABEL = "Mean of middle values";

        /**
         * The default label for the method that extracts the greater of the two middle values.
         */
        protected static final String UPPER_MEDIAN_LABEL = "Upper middle value";

        static final EvenListMedianMethodDescription LOWER_MEDIAN =
            new EvenListMedianMethodDescription(LOWER_MEDIAN_LABEL,
                "Use the lesser of the group's two middle values as median.", new EvenListMedianMethod() {

                    @Override
                    public DataCell extractMedian(final List<DataCell> cells, final int lowerCandidateIdx,
                        final int upperCandidateIdx) {
                        return cells.get(lowerCandidateIdx);
                    }
                });

        static final EvenListMedianMethodDescription UPPER_MEDIAN =
            new EvenListMedianMethodDescription(UPPER_MEDIAN_LABEL,
                "Use the greater of the group's two middle values as median.", new EvenListMedianMethod() {

                    @Override
                    public DataCell extractMedian(final List<DataCell> cells, final int lowerCandidateIdx,
                        final int upperCandidateIdx) {
                        return cells.get(upperCandidateIdx);
                    }
                });

        private final String m_label;

        private final String m_desc;

        private final EvenListMedianMethod m_method;

        private EvenListMedianMethodDescription(final String label, final String desc,
            final EvenListMedianMethod method) {
            m_label = label;
            m_desc = desc;
            m_method = method;
        }

        /**
         * @return the corresponding extraction method
         */
        public EvenListMedianMethod getMethod() {
            return m_method;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_label;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getActionCommand() {
            return m_label;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getToolTip() {
            return m_desc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDefault() {
            return m_label.equals(MedianSettings.DEFAULT_MEDIAN_METHOD);
        }
    }

    /**
     * {@link JPanel} that allows the user to select a method for extracting the median from an even list.
     */
    protected static class MedianSettingsPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private final DialogComponentButtonGroup m_methodSelection;

        /**
         * Creates a settings panel that contains a button group for selecting a median extraction method.
         *
         * @param settings the settings of the median aggregation operator
         * @param methods the available methods for median extraction
         */
        public MedianSettingsPanel(final MedianSettings settings, final EvenListMedianMethodDescription[] methods) {
            m_methodSelection =
                new DialogComponentButtonGroup(settings.getMedianMethodModel(), "Median methods", true, methods);
            add(m_methodSelection.getComponentPanel());
        }

        /**
         * @see DialogComponent#loadSettingsFrom(NodeSettingsRO, PortObjectSpec[])
         *
         * @param settings
         * @param specs
         * @throws NotConfigurableException
         */
        public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
            m_methodSelection.loadSettingsFrom(settings, specs);
        }
    }

    /**
     * Class that saves the user-selected behavior for this class, e.g. how to extract the median from an even list.
     *
     * @see AbstractMedianOperator
     */
    protected static class MedianSettings {

        /**
         * Default median method: median is mean of the list's two middle values.
         */
        public static final String DEFAULT_MEDIAN_METHOD = EvenListMedianMethodDescription.MEAN_MEDIAN_LABEL;

        private static final String CFG_MEDIAN_METHOD = "medianMethod";

        private final SettingsModelString m_medianMethodModel;

        /**
         * Creates a new {@code MedianSettings} object with {@link MedianSettings#DEFAULT_MEDIAN_METHOD} as the selected
         * method.
         */
        public MedianSettings() {
            this(DEFAULT_MEDIAN_METHOD);
        }

        /**
         * Creates a new {@code MedianSettings} object with the argument as the selected method.
         *
         * @param medianMethod the selected method
         */
        public MedianSettings(final String medianMethod) {
            m_medianMethodModel = new SettingsModelString(CFG_MEDIAN_METHOD, medianMethod);
        }

        /**
         * @return the selected method's settings model
         */
        public SettingsModelString getMedianMethodModel() {
            return m_medianMethodModel;
        }

        /**
         * @param settings the {@link NodeSettingsRO} to read the settings from
         * @throws InvalidSettingsException
         */
        public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
            m_medianMethodModel.loadSettingsFrom(settings);
        }

        /**
         * @param settings the {@link NodeSettingsWO} to write to
         */
        public void saveSettingsTo(final NodeSettingsWO settings) {
            m_medianMethodModel.saveSettingsTo(settings);
        }

        /**
         * @param settings the {@link NodeSettingsRO} to read the settings from
         * @throws InvalidSettingsException
         */
        public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String method =
                m_medianMethodModel.<SettingsModelString> createCloneWithValidatedValue(settings).getStringValue();
            if (method == null) {
                throw new InvalidSettingsException("No method selected.");
            }
        }

        /**
         * @param methods the collection of available methods
         * @throws InvalidSettingsException
         */
        public void validate(final Map<String, EvenListMedianMethod> methods) throws InvalidSettingsException {
            final String method = m_medianMethodModel.getStringValue();
            if (method == null) {
                throw new InvalidSettingsException("No method selected.");
            }
            if (methods.get(method) == null) {
                throw new InvalidSettingsException("Unknown method selected.");
            }
        }

        /**
         * @param spec the {@link DataTableSpec} of the input table
         * @throws InvalidSettingsException
         */
        public void configure(final DataTableSpec spec) throws InvalidSettingsException {
            // nothing to configure by default; override if operator requires configuration
        }
    }
}
