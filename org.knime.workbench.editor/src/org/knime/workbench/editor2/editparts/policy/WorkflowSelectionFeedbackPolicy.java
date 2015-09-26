package org.knime.workbench.editor2.editparts.policy;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editpolicies.AbstractEditPolicy;
import org.knime.workbench.editor2.figures.WorkflowAnnotationFigure;

/**
 */
public class WorkflowSelectionFeedbackPolicy extends AbstractEditPolicy {

	@Override
    public void eraseTargetFeedback(final Request request) {
	        if (request.getType().equals(RequestConstants.REQ_SELECTION)) {
	            showHighlight(false);
	        }
	}

	@Override
    public EditPart getTargetEditPart(final Request request) {
	    if (request.getType().equals(RequestConstants.REQ_SELECTION)) {
	        return getHost();
	    }
		return null;
	}

	/**
	 * @param showIt
	 */
	protected void showHighlight(final boolean showIt) {
	    IFigure f = getHostFigure();
	    if (f instanceof WorkflowAnnotationFigure) {
	        ((WorkflowAnnotationFigure)f).showEditIcon(showIt);
	    }
	}

	@Override
    public void showTargetFeedback(final Request request) {
		if (request.getType().equals(RequestConstants.REQ_SELECTION)) {
            showHighlight(true);
        }
	}


    /**
     * Adds the specified <code>Figure</code> to the
     * {@link LayerConstants#FEEDBACK_LAYER}.
     *
     * @param figure
     *            the feedback to add
     */
    protected void addFeedback(final IFigure figure) {
        getFeedbackLayer().add(figure);
    }

    /**
     * Returns the layer used for displaying feedback.
     *
     * @return the feedback layer
     */
    protected IFigure getFeedbackLayer() {
        return getLayer(LayerConstants.FEEDBACK_LAYER);
    }

    /**
     * Convenience method to return the host's Figure.
     *
     * @return The host GraphicalEditPart's Figure
     */
    protected IFigure getHostFigure() {
        return ((GraphicalEditPart) getHost()).getFigure();
    }

    /**
     * Obtains the specified layer.
     *
     * @param layer
     *            the key identifying the layer
     * @return the requested layer
     */
    protected IFigure getLayer(final Object layer) {
        return LayerManager.Helper.find(getHost()).getLayer(layer);
    }

    /**
     * Removes the specified <code>Figure</code> from the
     * {@link LayerConstants#FEEDBACK_LAYER}.
     *
     * @param figure
     *            the feedback to remove
     */
    protected void removeFeedback(final IFigure figure) {
        getFeedbackLayer().remove(figure);
    }


}
