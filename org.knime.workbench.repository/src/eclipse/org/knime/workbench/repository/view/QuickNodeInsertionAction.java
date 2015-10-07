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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 7, 2015 (albrecht): created
 */
package org.knime.workbench.repository.view;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.keys.IBindingService;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;

/**
 *
 * @author Christian Albrecht
 */
public class QuickNodeInsertionAction extends Action {

    private final IBindingService m_bindingService;

    private final String m_description;

    /**
     *
     */
    public QuickNodeInsertionAction() {
        setImageDescriptor(ImageRepository.getIconDescriptor(SharedImages.Search));
        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        m_bindingService = PlatformUI.getWorkbench().getService(IBindingService.class);

        Command command = commandService.getCommand(QuickNodeInsertionHandler.COMMAND_ID);

        try {
            m_description = command.getDescription();
        } catch (NotDefinedException e1) {
            throw new RuntimeException("Command: " + QuickNodeInsertionHandler.COMMAND_ID + " not defined!", e1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
        try {
            handlerService.executeCommand(QuickNodeInsertionHandler.COMMAND_ID, null);
        } catch (Exception ex) {
            throw new RuntimeException("fast selection command not found");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        String keySequence = getBindingSequence(m_bindingService);
        return m_description + keySequence;
    }

    private String getBindingSequence(final IBindingService bindingService) {
        String systemBinding = "";
        for (Binding bind : bindingService.getBindings()) {
            ParameterizedCommand parameterizedCommand = bind.getParameterizedCommand();
            if (parameterizedCommand != null
                && QuickNodeInsertionHandler.COMMAND_ID.equals(parameterizedCommand.getId())) {
                switch (bind.getType()) {

                    case Binding.USER:
                        // a user binding wins directly
                        return " (" + bind.getTriggerSequence().format() + ")";
                    default:
                        systemBinding = " (" + bind.getTriggerSequence().format() + ")";
                        break;
                }
            }
        }
        return systemBinding;
    }

}
