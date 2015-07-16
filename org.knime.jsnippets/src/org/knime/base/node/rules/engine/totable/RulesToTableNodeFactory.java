package org.knime.base.node.rules.engine.totable;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Rules to Table" Node.
 * Converts PMML RuleSets (with <tt>firstHit</tt>) to table containing the rules
 *
 * @author Gabor Bakos
 */
public class RulesToTableNodeFactory
        extends NodeFactory<RulesToTableNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public RulesToTableNodeModel createNodeModel() {
        return new RulesToTableNodeModel();
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
    public NodeView<RulesToTableNodeModel> createNodeView(final int viewIndex,
            final RulesToTableNodeModel nodeModel) {
        throw new IndexOutOfBoundsException("No views: " + viewIndex);
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
        return new RulesToTableNodeDialog();
    }
}

