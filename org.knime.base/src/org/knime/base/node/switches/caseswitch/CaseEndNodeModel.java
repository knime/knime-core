/*
 * ------------------------------------------------------------------------
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
 *   Apr 28, 2008 (wiswedel): created
 */
package org.knime.base.node.switches.caseswitch;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchConsumer;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 *
 * @author Tim-Oliver Buchholz, KNIME.com, Zurich, Switzerland
 */
final class CaseEndNodeModel extends NodeModel implements InactiveBranchConsumer {

    private final SettingsModelString m_multipleActiveHandlingSettingsModel;

    /** 3 ins, 1 out.
     * @param mandatoryPortType Type of first in and first (and only) out.
     * @param optionalPortType Type of 2nd and 3rd in. The optional counterpart to mandatoryPortType.
     *
     */
    CaseEndNodeModel(final PortType mandatoryPortType, final PortType optionalPortType) {
        super(new PortType[]{mandatoryPortType, optionalPortType, optionalPortType}, new PortType[]{mandatoryPortType});
        m_multipleActiveHandlingSettingsModel = createMultipleActiveHandlingSettingsModel();
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        PortObjectSpec result = InactiveBranchPortObjectSpec.INSTANCE;
        MultipleActiveHandling h = MultipleActiveHandling.valueOf(
            m_multipleActiveHandlingSettingsModel.getStringValue());
        for (int i = 0, l = inSpecs.length; i < l; i++) {
            if (inSpecs[i] != null && !(inSpecs[i] instanceof InactiveBranchPortObjectSpec)) {
                if (result instanceof InactiveBranchPortObjectSpec) {
                    result = inSpecs[i]; // only assign the first, never any subsequent
                } else if (h.equals(MultipleActiveHandling.Fail)) {
                    throw new InvalidSettingsException("Multiple inputs are active - causing node to fail. "
                            + "You can change this behavior in the node configuration dialog.");
                }
            }
        }
        return new PortObjectSpec[]{result};
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        PortObject result = InactiveBranchPortObject.INSTANCE;
        MultipleActiveHandling h = MultipleActiveHandling.valueOf(
            m_multipleActiveHandlingSettingsModel.getStringValue());
        for (int i = 0, l = inData.length; i < l; i++) {
            if (inData[i] != null && !(inData[i] instanceof InactiveBranchPortObject)) {
                if (result instanceof InactiveBranchPortObject) {
                    result = inData[i]; // only assign the first, never any subsequent
                } else if (h.equals(MultipleActiveHandling.Fail)) {
                    throw new InvalidSettingsException("Multiple inputs are active - causing node to fail. "
                            + "You can change this behavior in the node configuration dialog.");
                }
            }
        }
        return new PortObject[]{result};
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(m_multipleActiveHandlingSettingsModel.getKey())) {
            m_multipleActiveHandlingSettingsModel.loadSettingsFrom(settings);
        } else { // some nodes prior 2.12 were always failing on multiple active inputs (generic port object type)
            m_multipleActiveHandlingSettingsModel.setStringValue(MultipleActiveHandling.Fail.getActionCommand());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_multipleActiveHandlingSettingsModel.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(m_multipleActiveHandlingSettingsModel.getKey())) {
            SettingsModelString c = (SettingsModelString)m_multipleActiveHandlingSettingsModel
                    .createCloneWithValidatedValue(settings);
            String value = c.getStringValue();
            try {
                MultipleActiveHandling.valueOf(value);
            } catch (Exception e) {
                throw new InvalidSettingsException("invalid constant for multiple-active handling policy: " + value);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    /**
     * @return the multipleActiveHandlingSettingsModel
     */
    public static SettingsModelString createMultipleActiveHandlingSettingsModel() {
        return new SettingsModelString("multipleActiveHandling", MultipleActiveHandling.Fail.getActionCommand());
    }

    /** What to do when two or more inputs are active. */
    enum MultipleActiveHandling implements ButtonGroupEnumInterface {

        /** Fail the execution. */
        Fail("Fail", "Fails during node execution if multiple inputs are active", true),
        /** Pass on the first active input. */
        UseFirstActive("Use first non-inactive input", "Chooses the top-most active input object", false);

        private final String m_text;
        private final String m_tooltip;
        private final boolean m_isDefault;

        MultipleActiveHandling(final String text, final String tooltip, final boolean isDefault) {
            m_text = text;
            m_tooltip = tooltip;
            m_isDefault = isDefault;
        }

        /** {@inheritDoc} */
        @Override
        public String getText() {
            return m_text;
        }

        /** {@inheritDoc} */
        @Override
        public String getActionCommand() {
            return name();
        }

        /** {@inheritDoc} */
        @Override
        public String getToolTip() {
            return m_tooltip;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isDefault() {
            return m_isDefault;
        }

    }

}
