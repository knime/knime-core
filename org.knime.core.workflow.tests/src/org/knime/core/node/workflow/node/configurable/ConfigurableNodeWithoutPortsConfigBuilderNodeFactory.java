package org.knime.core.node.workflow.node.configurable;

import java.util.Optional;

import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.workflow.ReplaceNodeTest;
import org.knime.core.node.workflow.node.adapter.AdapterNodeModel;

/**
 * Dummy node factory for test purposes only. See {@link ReplaceNodeTest}
 * 
 * @author Kai Franze, KNIME GmbH
 *
 */
public class ConfigurableNodeWithoutPortsConfigBuilderNodeFactory extends ConfigurableNodeFactory<AdapterNodeModel> {
	
	@Override
	protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
		return Optional.empty();
	}

	@Override
	protected AdapterNodeModel createNodeModel(NodeCreationConfiguration creationConfig) {
		return new AdapterNodeModel(0, 1);
	}

	@Override
	protected NodeDialogPane createNodeDialogPane(NodeCreationConfiguration creationConfig) {
		return null;
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<AdapterNodeModel> createNodeView(int viewIndex, AdapterNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return false;
	}

}
