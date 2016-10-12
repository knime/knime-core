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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 30, 2011 (wiswedel): created
 */
package org.knime.core.node.workflow.virtual.parchunk;

import org.knime.core.api.node.NodeType;
import org.knime.core.node.DelegateNodeDescription;
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.port.PortType;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class VirtualParallelizedChunkPortObjectOutNodeFactory extends DynamicNodeFactory<VirtualParallelizedChunkPortObjectOutNodeModel> {

	private PortType[] m_inTypes;

	/** Serialization constructor.
	 * @since 2.10 */
    public VirtualParallelizedChunkPortObjectOutNodeFactory() {
    }

	/**
	 */
	public VirtualParallelizedChunkPortObjectOutNodeFactory(final PortType[] inTypes) {
		if (inTypes == null) {
			throw new NullPointerException(
					"Port type array argument must not be null");
		}
		m_inTypes = inTypes;
		init();
	}

	/** {@inheritDoc} */
	@Override
	protected NodeDescription createNodeDescription() {
	    return new DelegateNodeDescription(super.parseNodeDescriptionFromFile()) {
	        @Override
	        public String getInportDescription(final int index) {
	            return "The data chunk arriving from the current execution branch. It's passed back to the loop end.";
	        }
	        @Override
	        public String getInportName(final int index) {
	            return String.format("Virtual Output %d", index + 1);
	        }
	    };
	}

	/** {@inheritDoc} */
	@Override
	public VirtualParallelizedChunkPortObjectOutNodeModel createNodeModel() {
		return new VirtualParallelizedChunkPortObjectOutNodeModel(m_inTypes);
	}

	/** {@inheritDoc} */
	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	public NodeView<VirtualParallelizedChunkPortObjectOutNodeModel> createNodeView(
			final int viewIndex,
			final VirtualParallelizedChunkPortObjectOutNodeModel nodeModel) {
		return null;
	}

	/** {@inheritDoc} */
	@Override
	protected boolean hasDialog() {
		return false;
	}

	/** {@inheritDoc} */
	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public void loadAdditionalFactorySettings(final ConfigRO config) throws InvalidSettingsException {
	    super.loadAdditionalFactorySettings(config);
	    m_inTypes = VirtualParallelizedChunkPortObjectInNodeFactory.loadPortTypeList(config);
	}

	/** {@inheritDoc} */
	@Override
	public void saveAdditionalFactorySettings(final ConfigWO config) {
	    super.saveAdditionalFactorySettings(config);
	    VirtualParallelizedChunkPortObjectInNodeFactory.savePortTypeList(m_inTypes, config);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public NodeType getType() {
	    return NodeType.VirtualOut;
	}

}
