package org.knime.base.node.rules.engine.twoports;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Rule Engine (Dictionary)" Node.
 * Applies the rules from the second input port to the first datatable.
 *
 * @author Gabor Bakos
 */
public class RuleEngine2PortsNodeFactory
        extends NodeFactory<RuleEngine2PortsNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public RuleEngine2PortsNodeModel createNodeModel() {
        return new RuleEngine2PortsNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<RuleEngine2PortsNodeModel> createNodeView(final int viewIndex,
            final RuleEngine2PortsNodeModel nodeModel) {
        throw new ArrayIndexOutOfBoundsException("No views! " + viewIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new RuleEngine2PortsNodeDialog();
    }
}

