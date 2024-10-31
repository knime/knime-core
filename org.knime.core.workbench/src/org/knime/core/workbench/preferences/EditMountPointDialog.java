/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * Created: Mar 22, 2011
 * Author: ohl
 */
package org.knime.core.workbench.preferences;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory.AdditionalInformationPanel;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory.ValidationRequiredListener;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Dialog for selecting a new resource/item to be displayed in the KNIME Explorer view.
 *
 * @author ohl, University of Konstanz
 * @since 6.0
 */

public class EditMountPointDialog extends ListDialog {


    private static final ImageDescriptor IMG_NEWITEM = AbstractUIPlugin.imageDescriptorFromPlugin(
        KNIMEUIPlugin.PLUGIN_ID, "icons/new_knime55.png");

    private static final String INVALID_MSG = "A valid Mount ID contains only characters a-z, A-Z, 0-9, '.' or '-'.\n"
        + "It must start with a character and not end with a dot nor hyphen.\n"
        + "Additionally, Mount IDs starting with 'knime.' are reserved\n"
        + "for internal use.";

    private static final String MOUNT_ID_HEADER_TEXT = "Enter the name of the Mount ID.";

    private ValidationRequiredListener m_validationListener;

    private String m_mountIDval;

    private Button m_ok;

    private Label m_errIcon;

    private StyledText m_errText;

    private Set<String> m_invalidIDs;

    /* --- the result (after ok) ---- */

    private Text m_mountID;

    private AdditionalInformationPanel m_additionalPanel;

    private AbstractContentProvider m_contentProvider;

    private boolean m_isNew;

    private AbstractContentProviderFactory m_factory;

    private String m_additionalContent;

    private CLabel m_mountIDHeader;

    private String m_mountIDHeaderText;

    private String m_defaultMountID;

    private Button m_resetMountID;

    private String m_oldMountID;

    private Thread m_backgroundWaitThread;

    /**
     * Creates a new mount point dialog for creating a new mount point.
     *
     * @param parentShell the parent shell
     * @param input list of selectable items
     * @param invalidIDs list of invalid ids - rejected in the mountID field.
     */
    public EditMountPointDialog(final Shell parentShell, final List<AbstractContentProviderFactory> input,
        final Collection<String> invalidIDs) {
        super(parentShell);
        init(input, invalidIDs, null);
    }

    /**
     * Creates a new mount point dialog for editing existing MountSettings.
     *
     * @param parentShell the parent shell
     * @param input list of selectable items
     * @param invalidIDs list of invalid ids - rejected in the mountID field.
     * @param settings existing MountSettings to edit
     */
    public EditMountPointDialog(final Shell parentShell, final List<AbstractContentProviderFactory> input,
        final Collection<String> invalidIDs, final MountSettings settings) {
        super(parentShell);
        init(input, invalidIDs, settings);
    }

    @Override
    public boolean close() {
        if (m_additionalPanel != null) {
            m_additionalPanel.cancelBackgroundWork();
        }
        if (m_backgroundWaitThread != null && m_backgroundWaitThread.isAlive()) {
            m_backgroundWaitThread.interrupt();
        }
        return super.close();
    }

    private void init(final List<AbstractContentProviderFactory> input,
            final Collection<String> invalidIDs, final MountSettings settings) {
        m_isNew = (settings == null);
        m_validationListener = createValidationListener();
        m_invalidIDs = new HashSet<String>(invalidIDs);
        m_mountIDHeaderText = MOUNT_ID_HEADER_TEXT;
        setAddCancelButton(true);
        setContentProvider(new ContentFactoryProvider(input));
        setLabelProvider(new ContentFactoryLabelProvider());
        setInput(input);
        setHeightInChars(input.size() + 1);
        if (settings == null) {
            setTitle("Select New Content");
            m_mountIDval = "";
            m_isNew = true;
        } else {
            setTitle("Edit Mount Point");
            m_mountIDval = settings.getMountID();
            m_additionalContent = settings.getContent();
            m_defaultMountID = settings.getDefaultMountID();
            m_oldMountID = m_mountIDval;
            m_isNew = false;
        }
    }

    private ValidationRequiredListener createValidationListener() {
        return new ValidationRequiredListener() {

            /** Used to check if the restore mount id dialog is already open.
             * This is the case if we edit the dialog and it's already checking the connection
             * and the set mount id is not equal to the default mount id. In that case the dialog
             * would pop up twice (probably a race condition). */
            private boolean m_isDialogShowing;

            @Override
            public void validationRequired() {
                validate();
            }

            @Override
            public void defaultMountIDChanged(final String defaultMountID) {
                m_defaultMountID = defaultMountID;
                String id = defaultMountID == null ? "" : defaultMountID;
                if (m_mountID.getText().isEmpty()) {
                    m_mountID.setText(id);
                }
                if (defaultMountID != null && !defaultMountID.isEmpty()
                        && !m_mountID.getText().equals(defaultMountID) && !m_isDialogShowing) {
                    m_isDialogShowing = true;
                    String confirmTitle = "Overwrite Mount ID";
                    String confirmMsg = "The default Mount ID is " + defaultMountID
                            + ". Do you want to overwrite your current Mount ID?";
                    if (MessageDialog.openQuestion(m_mountID.getShell(), confirmTitle, confirmMsg)) {
                        m_mountID.setText(id);
                    }
                    // When the defaultMountID changes we know the real default mountID so it can be set as
                    // as the old Mount ID.
                    m_oldMountID = id;
                    m_isDialogShowing = false;
                }
                validate();
            }

            @Override
            public void setMountIDLabel(final String label) {
                String newLabel = label == null ? "" : label;
                m_mountIDHeaderText = newLabel;
                m_mountIDHeader.setText(newLabel);
            }

            @Override
            public String getCurrentMountID() {
                return m_mountID.getText();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Button
        createButton(final Composite parent, final int id, final String label, final boolean defaultButton) {
        Button b = super.createButton(parent, id, label, defaultButton);
        if (id == IDialogConstants.OK_ID) {
            m_ok = b;
            m_ok.setText("OK");
        }
        return b;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void okPressed() {
        okPressed(true);
    }

    /**
     * See {@link #okPressed() okPressed}.
     *
     * @param waitForBackgroundWork {@code true} ift the current thread should wait for the background work to finish,
     *            {@code false} otherwise.
     */
    private void okPressed(final boolean waitForBackgroundWork) {
        // this method gets called through a double click (if cancel button is
        // added)
        if (!validate(waitForBackgroundWork)) {
            return;
        }
        Object selection = ((IStructuredSelection)getTableViewer().getSelection()).toArray()[0];
        m_factory = (AbstractContentProviderFactory)selection;
        m_mountIDval = m_mountID.getText().trim();
        if (m_additionalPanel != null) {
            if (m_contentProvider != null) {
                // we should disconnect the server when we edit it, especially as we get a new content provider.
                m_contentProvider.disconnect();
            }
            m_contentProvider = m_additionalPanel.createContentProvider();
        } else {
            m_contentProvider = m_factory.createContentProvider(m_mountIDval);
        }
        super.okPressed();
    }

    /**
     * @return the ID entered by the user (only valid after dialog is OKed.)
     */
    public String getMountID() {
        return m_mountIDval;
    }

    /**
     * @return an {@link AbstractContentProvider} if it can be created from the panel, null if not.
     */
    public AbstractContentProvider getContentProvider() {
        return m_contentProvider;
    }

    /**
     * @return the selected factory (only valid after dialog is OKed)
     */
    public AbstractContentProviderFactory getFactory() {
        return m_factory;
    }

    /**
     * @return the defaultMountID
     */
    public String getDefaultMountID() {
        return m_defaultMountID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {

        createHeader(parent);

        final Composite additionalPanel = new Composite(parent, SWT.FILL);
        Composite mountHdr = new Composite(parent, SWT.FILL);
        Composite mountInput = new Composite(mountHdr, SWT.FILL | SWT.BORDER);
        m_mountID = new Text(mountInput, SWT.BORDER);
        m_resetMountID = new Button(mountInput, SWT.BORDER);
        m_resetMountID.setText("Reset Mount ID");
        m_resetMountID.setEnabled(false);
        m_resetMountID.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(final Event event) {
                if (getDefaultMountID() != null) {
                    // Mount ID has been retrieved, so default == old
                    m_mountID.setText(getDefaultMountID());
                } else {
                    m_mountID.setText(m_oldMountID);
                }
            }
        });
        // insert the selection list

        Control c = super.createDialogArea(parent);
        TableViewer tableViewer = getTableViewer();
        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                Object selection = ((IStructuredSelection)event.getSelection()).getFirstElement();
                if (selection != null && selection instanceof AbstractContentProviderFactory) {
                    AbstractContentProviderFactory factory = (AbstractContentProviderFactory)selection;
                    for (Control cont : additionalPanel.getChildren()) {
                        cont.dispose();
                    }
                    if (m_isNew && !m_mountID.getText().isEmpty()) {
                        m_defaultMountID = null;
                        if (!m_mountID.getText().isEmpty()) {
                            m_mountID.setText("");
                        }
                    }
                    m_mountIDHeaderText = MOUNT_ID_HEADER_TEXT;
                    m_mountIDHeader.setText(m_mountIDHeaderText);
                    if (factory.isAdditionalInformationNeeded()) {
                        m_additionalPanel =
                            factory.createAdditionalInformationPanel(additionalPanel, m_mountID);
                        if (m_additionalPanel != null) {
                            m_additionalPanel.addValidationRequiredListener(m_validationListener);
                            m_additionalPanel.createPanel(m_additionalContent);
                        }
                    } else {
                        m_additionalPanel = null;
                        String mountID = factory.getDefaultMountID() == null ? "" : factory.getDefaultMountID();
                        m_mountID.setText(mountID);
                    }

                    setMountIdStatic(factory.isMountIdStatic());
                }
                validate();
            }
        });

        additionalPanel.moveBelow(c);
        GridLayout gl = new GridLayout(1, true);
        gl.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        gl.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        gl.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        gl.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        additionalPanel.setLayout(gl);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, false, false);
        gridData.widthHint = 100;
        additionalPanel.setLayoutData(gridData);

        mountHdr.moveBelow(additionalPanel);
        gl = new GridLayout(1, true);
        gl.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        gl.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        gl.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        gl.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        mountHdr.setLayout(gl);
        mountHdr.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        m_mountIDHeader = new CLabel(mountHdr, SWT.NONE);
        m_mountIDHeader.setText(m_mountIDHeaderText);

        mountInput.moveBelow(m_mountIDHeader);
        gl = new GridLayout(2, false);
        gl.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        gl.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        // gl.verticalSpacing =
        // convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        gl.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        mountInput.setLayout(gl);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = 100;
        mountInput.setLayoutData(gridData);

        Label l = new Label(mountInput, SWT.NONE);
        l.setText("Mount ID:");
        m_mountID.moveBelow(l);
        if (m_mountIDval != null) {
            m_mountID.setText(m_mountIDval);
        }
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        m_mountID.setLayoutData(gridData);

        GridData resetGridData = new GridData(GridData.BEGINNING);
        resetGridData.horizontalSpan = 2;
        m_resetMountID.moveBelow(m_mountID);
        m_resetMountID.setLayoutData(resetGridData);

        m_mountID.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                validate();
            }
        });

        Object firstElement = tableViewer.getElementAt(0);
        if (firstElement != null) {
            tableViewer.getTable().select(0);
            //force selectionChanged event
            tableViewer.setSelection(tableViewer.getSelection());
        }
        if (!m_isNew) {
            tableViewer.getTable().setEnabled(false);
        }
        return c;
    }

    private void setMountIdStatic(final boolean isStatic) {
        // If the mount ID is static, the components should be disabled or hidden. SRV-1284
        m_mountID.setEnabled(!isStatic);
        m_resetMountID.setVisible(!isStatic);
        if (isStatic) {
            m_mountIDHeaderText = "The Mount ID is static for this resource and cannot be edited.";
        } else {
            m_mountIDHeaderText = MOUNT_ID_HEADER_TEXT;
        }
    }

    /**
     * Sets the error icon/message.
     *
     * @return true, if the selection/input is okay.
     */
    protected boolean validate() {
       return validate(false);
    }

    /**
     * Sets the error icon/message. Disables all input field when waiting for background work to finish
     *
     * @param waitForBackgroundWork {@code true} ift the current thread should wait for the background work to finish,
     *            {@code false} otherwise.
     *
     * @return true, if the selection/input is okay.
     * @since 8.6
     */
    protected boolean validate(final boolean waitForBackgroundWork) {
        final Point offset = getShell() != null ? new Point(getShell().getSize().x - getShell().getMinimumSize().x,
            getShell().getSize().y - getShell().getMinimumSize().y) : new Point(0, 0);
        boolean valid = true;
        boolean loading = false;
        String errMsg = "";
        if (getTableViewer().getSelection().isEmpty()) {
            valid = false;
            errMsg = "Please select a resource to add.";
        }

        if (m_additionalPanel != null) {
            final String loadingMessage = m_additionalPanel.getLoadMessage();
            final String additionalError = m_additionalPanel.validate();

            if (!StringUtils.isEmpty(loadingMessage)) {
                loading = true;
                valid = false;
                errMsg = loadingMessage;
                if (waitForBackgroundWork) {
                    m_backgroundWaitThread = new Thread(this::waitForBackgroundWork);
                    m_backgroundWaitThread.start();
                }
            } else if (!StringUtils.isEmpty(additionalError)) {
                valid = false;
                errMsg = additionalError;
            }
        }

        String id = m_mountID.getText().trim();
        String mountIDHeaderText = m_mountIDHeaderText;
        Image mountIDHeaderImage = null;
        if (!StringUtils.isEmpty(m_defaultMountID) && !m_defaultMountID.equals(id)) {
            mountIDHeaderText += "\n\nChanging the default Mount ID is not recommended since it can cause\n"
                + "issues when trying to reference server resources\n"
                + "by the Mount ID (e.g. knime://knime-server/resource.txt).\n"
                + "Only change the default Mount ID if you are certain of what you are doing.\n\n"
                + "The default Mount ID is: " + m_defaultMountID;
            mountIDHeaderImage = ImageRepository.getIconImage(SharedImages.Warning);
            m_resetMountID.setEnabled(true);
        } else if (!StringUtils.isEmpty(m_oldMountID) && !m_oldMountID.equals(id)) {
            mountIDHeaderText += "\n\nChanging the default Mount ID is not recommended since it can cause\n"
                    + "issues when trying to reference server resources\n"
                    + "by the Mount ID (e.g. knime://knime-server/resource.txt).\n"
                    + "Only change the Mount ID if you are certain of what you are doing.\n\n"
                    + "The old Mount ID is: " + m_oldMountID;
                mountIDHeaderImage = ImageRepository.getIconImage(SharedImages.Warning);
                m_resetMountID.setEnabled(true);
        } else {
            m_resetMountID.setEnabled(false);
        }
        m_mountIDHeader.setText(mountIDHeaderText);
        m_mountIDHeader.setImage(mountIDHeaderImage);
        if (valid) {
            if (id == null || id.isEmpty()) {
                valid = false;
                errMsg = "Please enter a valid mount ID.";
            } else {
                if (m_invalidIDs.contains(id)) {
                    valid = false;
                    errMsg = "Mount ID already in use. Please enter a different ID.";
                }
            }
        }
        if (valid && !ExplorerMountTable.isValidMountID(id)) {
            valid = false;
            errMsg = INVALID_MSG;
        }

        m_errText.setText(errMsg);

        if (loading) {
            m_errIcon.setImage(ImageRepository.getIconImage(SharedImages.Busy));
        } else {
            m_errIcon.setImage(ImageRepository.getIconImage(SharedImages.Error));
        }
        m_errIcon.setVisible(!valid);

        if (m_ok != null) {
            layoutDialog(offset);
        }

        return valid;
    }

    private void waitForBackgroundWork() {
        Display.getDefault().syncExec(() -> {
            for (final Control c : getShell().getChildren()) {
                recursiveSetEnabled(c, false);
            }
        });
        if (m_additionalPanel.waitForBackgroundWork()) {
            Display.getDefault().syncExec(() -> okPressed(false));
        }
        Display.getDefault().syncExec(() -> {
            if (getShell() != null && !getShell().isDisposed()) {
                for (final Control c : getShell().getChildren()) {
                    recursiveSetEnabled(c, true);
                }
            }
        });
    }

    private void recursiveSetEnabled(final Control control, final boolean enabled) {
        if (control.isDisposed()) {
            // Do nothing since the control is already disposed
        } else if (control instanceof Composite) {
            final Composite comp = (Composite)control;
            for (final Control c : comp.getChildren()) {
                recursiveSetEnabled(c, enabled);
            }
        } else if ((control instanceof Text || control instanceof Button || control instanceof Combo)
            && !control.equals(getButton(CANCEL))) {
            control.setEnabled(enabled);
        }
    }

    /**
     * Adds the white header to the dialog.
     *
     * @param parent
     */
    protected void createHeader(final Composite parent) {
        Composite header = new Composite(parent, SWT.FILL);
        Color white = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
        header.setBackground(white);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        header.setLayoutData(gridData);
        header.setLayout(new GridLayout(3, false));
        // first row
        new Label(header, SWT.NONE);
        Label exec = new Label(header, SWT.NONE);
        exec.setBackground(white);
        if (m_isNew) {
            exec.setText("Mounting a new resource for display in the KNIME Explorer");
        } else {
            exec.setText("Edit a resource for display in the KNIME Explorer");
        }
        FontData[] fd = parent.getFont().getFontData();
        for (FontData f : fd) {
            f.setStyle(SWT.BOLD);
            f.setHeight(f.getHeight() + 2);
        }
        exec.setFont(new Font(parent.getDisplay(), fd));
        exec.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        Label execIcon = new Label(header, SWT.NONE);
        execIcon.setBackground(white);
        execIcon.setImage(IMG_NEWITEM.createImage());
        execIcon.setLayoutData(new GridData(SWT.END, SWT.BEGINNING, true, true));
        // second row
        if (m_isNew) {
            new Label(header, SWT.None);
            Label txt = new Label(header, SWT.NONE);
            txt.setBackground(white);
            txt.setText("Please select the type of resource that should " + "be mounted.");
            txt.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            new Label(header, SWT.None);
        }
        // third row
        m_errIcon = new Label(header, SWT.NONE);
        m_errIcon.setVisible(true);
        m_errIcon.setImage(ImageRepository.getIconImage(SharedImages.Error));
        m_errIcon.setLayoutData(new GridData(GridData.CENTER, GridData.BEGINNING, false , true));
        m_errIcon.setBackground(white);


        m_errText = new StyledText(header, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        final Listener scrollBarListener = new Listener() {
            @Override
            public void handleEvent(final Event event) {
                // Show vertical scrollbar only when needed
                final StyledText t = (StyledText)event.widget;
                final Rectangle r1 = t.getClientArea();
                final Rectangle r2 = t.computeTrim(r1.x, r1.y, r1.width, r1.height);
                final Point p = t.computeSize(r1.width, SWT.DEFAULT, true);
                t.getVerticalBar().setVisible(r2.height <= p.y);
                if (event.type == SWT.Modify) {
                    t.getParent().layout(true);
                    t.showSelection();
                }
            }
        };
        m_errText.addListener(SWT.Resize, scrollBarListener);
        m_errText.addListener(SWT.Modify, scrollBarListener);
        m_errText.setText("Please enter a mount id.");
        final GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.heightHint = 75;
        gd.widthHint = 50;
        gd.horizontalSpan = 2;
        m_errText.setLayoutData(gd);
        m_errText.setEditable(false);
        m_errText.setCaret(null);
        m_errText.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_ARROW));
        m_errText.setBackground(white);
        m_errText.addControlListener(new ControlListener() {

            @Override
            public void controlResized(final ControlEvent e) {
                m_errIcon.setVisible(!m_errText.getText().isEmpty());
            }

            @Override
            public void controlMoved(final ControlEvent e) {
               controlResized(e);

            }
        });
        parent.layout();
        new Label(header, SWT.None);
    }

    private static class ContentFactoryLabelProvider implements ILabelProvider {

        /**
         * {@inheritDoc}
         */
        @Override
        public void addListener(final ILabelProviderListener listener) {
            // not supported
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose() {
            // nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isLabelProperty(final Object element, final String property) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeListener(final ILabelProviderListener listener) {
            // not supported
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Image getImage(final Object element) {
            if (element instanceof AbstractContentProviderFactory) {
                return ((AbstractContentProviderFactory)element).getImage();
            } else {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText(final Object element) {
            if (element instanceof AbstractContentProviderFactory) {
                return ((AbstractContentProviderFactory)element).toString();
            } else {
                return null;
            }
        }

    }

    private static final class ContentFactoryProvider implements IStructuredContentProvider {
        private final List<AbstractContentProviderFactory> m_elements;

        private ContentFactoryProvider(final List<AbstractContentProviderFactory> elements) {
            m_elements = elements;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object[] getElements(final Object inputElement) {
            if (inputElement == m_elements) {
                return ((List<AbstractContentProviderFactory>)inputElement).toArray();
            }
            return new Object[0];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose() {
            // nothing here.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
            // empty.
        }
    }

    private void layoutDialog(final Point offset) {
        getShell().layout(true, true);
        final Point newSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        getShell().setMinimumSize(newSize);
        getShell().setSize(new Point(newSize.x + offset.x, newSize.y + offset.y));
    }

    @Override
    protected void initializeBounds() {
        super.initializeBounds();
        if (getShell() != null) {
            getShell().setMinimumSize(getInitialSize());
        }
    }
}
