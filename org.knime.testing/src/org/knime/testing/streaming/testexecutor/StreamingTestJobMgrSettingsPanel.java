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
 */
package org.knime.testing.streaming.testexecutor;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.node.workflow.NodeExecutionJobManagerPanel;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class StreamingTestJobMgrSettingsPanel extends NodeExecutionJobManagerPanel {

    static final String CFG_NUM_CHUNKS = "numChunks";

    static final int DEFAULT_NUM_CHUNKS = 3;

    private static final long serialVersionUID = 1;

    private JTextField m_numChunks;

    /**
     * @param nodeSplitType type of splitting permitted by the underlying node
     */
    public StreamingTestJobMgrSettingsPanel(final SplitType nodeSplitType) {
        m_numChunks =  new JTextField(DEFAULT_NUM_CHUNKS);
        setLayout(new FlowLayout());
        add(new JLabel("Number of (virtual) chunks"));
        add(m_numChunks);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettings(final NodeSettingsRO settings) {
        try {
            m_numChunks.setText("" + settings.getInt(CFG_NUM_CHUNKS));
        } catch (InvalidSettingsException e) {
            m_numChunks.setText("" + DEFAULT_NUM_CHUNKS);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInputSpecs(final PortObjectSpec[] inSpecs) {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettings(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        try {
            int numChunks = Integer.parseInt(m_numChunks.getText());
            if(numChunks < 1) {
                throw new InvalidSettingsException("Invalid number of chunks: " + numChunks);
            }
            settings.addInt(CFG_NUM_CHUNKS, numChunks);
        } catch (NumberFormatException e) {
            throw new InvalidSettingsException(e);
        }
    }
}
