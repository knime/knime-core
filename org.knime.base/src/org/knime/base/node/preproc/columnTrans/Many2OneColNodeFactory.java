/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 *
 */
package org.knime.base.node.preproc.columnTrans;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "BayesianPredictor" Node.
 * This is the description of the Bayesian Predictor
 
 * @author Tobias Koetter
 */
public class Many2OneColNodeFactory extends NodeFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public NodeModel createNodeModel() {
        return new Many2OneColNodeModel();
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
        return null;
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
        return new Many2OneColNodeDialog();
    }


}
