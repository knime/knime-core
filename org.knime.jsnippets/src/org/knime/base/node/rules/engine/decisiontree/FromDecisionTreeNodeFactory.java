package org.knime.base.node.rules.engine.decisiontree;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Decision Tree to Rules" Node.
 * Converts a decision tree model to PMML <a href="http://www.dmg.org/v4-2-1/RuleSet.html">RuleSet</a> model.
 *
 * @author Gabor Bakos
 */
public class FromDecisionTreeNodeFactory
        extends NodeFactory<FromDecisionTreeNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FromDecisionTreeNodeModel createNodeModel() {
        return new FromDecisionTreeNodeModel();
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
    public NodeView<FromDecisionTreeNodeModel> createNodeView(final int viewIndex,
            final FromDecisionTreeNodeModel nodeModel) {
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
        return new FromDecisionTreeNodeDialog();
    }
}

