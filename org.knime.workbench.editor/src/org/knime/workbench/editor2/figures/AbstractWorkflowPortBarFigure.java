/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * History
 *   20.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.DelegatingLayout;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.commands.ChangeWorkflowPortBarCommand;
import org.knime.workbench.editor2.editparts.WorkflowInPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortBarEditPart;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractWorkflowPortBarFigure extends RectangleFigure {
    
    /** Default width for the port bar. */
    protected static final int WIDTH = 30;
    /** Default offset from the workflow borders. */
    protected static final int OFFSET = 10;
    
    private boolean m_isInitialized = false;

    
    /**
     * 
     */
    public AbstractWorkflowPortBarFigure() {
        super();
        DelegatingLayout layout = new DelegatingLayout();
        setLayoutManager(layout);
        setBackgroundColor(Display.getCurrent().getSystemColor(
                SWT.COLOR_GRAY));
    }
    
    /**
     * 
     * @param initialized true if the ui info was set to the model
     * 
     * @see WorkflowOutPortBarFigure#paint(org.eclipse.draw2d.Graphics)
     * @see WorkflowInPortBarFigure#paint(org.eclipse.draw2d.Graphics)
     * @see WorkflowOutPortBarEditPart
     * @see WorkflowInPortBarEditPart
     */
    public void setInitialized(final boolean initialized) {
        m_isInitialized = initialized;
        if (initialized) {
            revalidate();
        }
    }
    
    /**
     * 
     * @return true if the ui info was set to the model (first time painted 
     *  or loaded from {@link WorkflowManager}) 
     */
    public boolean isInitialized() {
        return m_isInitialized;
    }
    
    /**
     * 
     * @see ChangeWorkflowPortBarCommand#canExecute()
     * 
     * {@inheritDoc}
     */
    @Override
    public Dimension getMinimumSize(final int hint, final int hint2) {
        return new Dimension(AbstractPortFigure.WF_PORT_SIZE + 10, 
                AbstractPortFigure.WF_PORT_SIZE + 10);
    }

}
