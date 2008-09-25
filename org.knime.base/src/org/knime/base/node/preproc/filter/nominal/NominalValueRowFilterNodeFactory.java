package org.knime.base.node.preproc.filter.nominal;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PossibleValueRowFilter" Node.
 * 
 *
 * @author KNIME GmbH
 */
public class NominalValueRowFilterNodeFactory extends NodeFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeModel createNodeModel() {
        return new NominalValueRowFilterNodeModel();
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
    public NodeView createNodeView(final int viewIndex,
            final NodeModel nodeModel) {
        throw new IllegalArgumentException("No view available!");
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
        return new NominalValueRowFilterNodeDialog();
    }

}

