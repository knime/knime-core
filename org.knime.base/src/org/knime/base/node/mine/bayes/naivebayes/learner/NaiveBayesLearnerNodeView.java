package org.knime.base.node.bayes.naivebayes.learner;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.base.node.bayes.naivebayes.datamodel.NaiveBayesModel;


/**
 * <code>NodeView</code> for the "BayesianClassifier" Node.
 * This is the description of the Bayesian classifier
 *
 * @author Tobias Koetter
 */
public class NaiveBayesLearnerNodeView extends NodeView {


    private NaiveBayesModel m_model;
    private final JEditorPane m_htmlPane;

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (
     * class: <code>NaiveBayesPredictorNodeModel</code>)
     * @param title The title
     */
    protected NaiveBayesLearnerNodeView(final NodeModel nodeModel, 
            final String title) {
        super(nodeModel);
        setViewTitle(title);
        assert (nodeModel instanceof NaiveBayesLearnerNodeModel);
        m_model = ((NaiveBayesLearnerNodeModel)nodeModel).getNaiveBayesModel();
        //The output as HTML
        m_htmlPane = new JEditorPane("text/html", "");
//        m_htmlPane.setText(m_model.getHTMLTable());
        m_htmlPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(m_htmlPane);
/*  
        //The output as a JTABLE
        final String[] captions = m_model.getDataTableCaptions();
        final String[][] dataTable = m_model.getDataTable();
        JTable table = new JTable(dataTable, captions);
        JScrollPane scrollPane = new JScrollPane(table);
        table.setPreferredScrollableViewportSize(new Dimension(540, 400));
        */
        setComponent(scrollPane);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        NodeModel nodeModel = getNodeModel();
        assert (nodeModel instanceof NaiveBayesLearnerNodeModel);
        m_model = ((NaiveBayesLearnerNodeModel)nodeModel).getNaiveBayesModel();
        if (m_model != null) {
            m_htmlPane.setText(m_model.getHTMLView());
        } else {
            m_htmlPane.setText("No model available");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
//        nothing to do
    }
}
