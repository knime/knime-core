/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.workbench.editor2.figures;

import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.DelegatingLayout;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.RelativeLocator;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.SingleNodeContainer.LoopStatus;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.figures.ProgressFigure.ProgressMode;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Figure displaying a <code>NodeContainer</code> in a workflow. This serves as
 * a container for <code>NodeInPortFigure</code>s and
 * <code>NodeOutPortFigure</code>s
 *
 * This figure is composed of the following sub-figures:
 * <ul>
 * <li><code>ContentFigure</code> - hosts the child visuals (port figures) and
 * the center icon</li>
 * <li><code>StatusFigure</code> - contains description text and some color
 * codes</li>
 * <li><code>ProgressFigure</code> - displaying the execution progress</li>
 * </ul>
 *
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class NodeContainerFigure extends RectangleFigure {

    /** absolute width of this figure. */
    public static final int WIDTH = 80;

    /** absolute height of this figure. */
    public static final int HEIGHT = 48;

    /** Red traffic light. */
    public static final Image RED = ImageRepository
            .getImage("icons/ampel_red.png");

    /** Yellow traffic light. * */
    public static final Image YELLOW = ImageRepository
            .getImage("icons/ampel_yellow.png");

    /** Green traffic light. */
    public static final Image GREEN = ImageRepository
            .getImage("icons/ampel_green.png");

    /** Inactive traffic light. */
    public static final Image INACTIVE = ImageRepository
            .getImage("icons/ampel_inactive.png");

    /** Info sign. */
    public static final Image INFO_SIGN = ImageRepository
            .getImage("icons/roundInfo.jpg");

    /** Warning sign. */
    public static final Image WARNING_SIGN = ImageRepository
            .getImage("icons/warning.gif");

    /** Error sign. */
    public static final Image ERROR_SIGN = ImageRepository
            .getImage("icons/error.png");

    /** Delete sign. */
    public static final Image DELETE_SIGN = ImageRepository
            .getImage("icons/delete.png");

    /** Loop End Node extra icon: In Progress. */
    public static final Image LOOP_IN_PROGRESS_SIGN = ImageRepository
            .getImage("icons/loop_in_progress.png");

    /** Loop End Node extra icon: Done. */
    public static final Image LOOP_DONE_SIGN = ImageRepository
            .getImage("icons/loop_done.png");

    /** Loop End Node extra icon: No Status. */
    public static final Image LOOP_NO_STATUS = ImageRepository
            .getImage("icons/loop_nostatus.png");

    /** State: Node not configured. */
    public static final int STATE_NOT_CONFIGURED = 0;

    /** Default node font for the status figure. */
    private static final Font NODE_FONT = new Font(Display.getCurrent(),
            fontName(), 2, SWT.NORMAL);;

    /** Get font name either from the system, or default "Arial". */
    private static String fontName() {
        // I (Bernd) had problem using the hardcoded font "Arial" -
        // this static block derives the fonts from the system font.
        final Display current = Display.getCurrent();
        final Font systemFont = current.getSystemFont();
        final FontData[] systemFontData = systemFont.getFontData();
        String name = "Arial"; // fallback
        if (systemFontData.length >= 1) {
            name = systemFontData[0].getName();
        }
        return name;
    }

    /** Tooltip for displaying the full heading. */
    private final NewToolTipFigure m_headingTooltip;

    /** content pane, contains the port visuals and the icon. */
    private final SymbolFigure m_symbolFigure;

    /** contains the the "traffic light". * */
    private final StatusFigure m_statusFigure;

    /** contains the "progress bar". * */
    private ProgressFigure m_progressFigure;

    /** contains the image indicating the loop status (if available). */
    private Image m_loopStatusFigure = null;

    /** The background color to apply. */
    private final Color m_backgroundColor;

    /** contains the the warning/error sign. */
    private final InfoWarnErrorPanel m_infoWarnErrorPanel;

    /** The node name, e.g File Reader. */
    private final Label m_heading;

    /** The user specified node name, e.g. Molecule Data 4. */
    private final Label m_name;

    /**
     * Tooltip for displaying the custom description. This tooltip is displayed
     * with the custom name.
     */
    private final NewToolTipFigure m_nameTooltip;

    /**
     * An optional custom description.
     */
    private String m_description;

    private Image m_jobExec;

    private Image m_metaNodeLinkIcon;

    private boolean m_showFlowVarPorts;

    /**
     * Creates a new node figure.
     *
     * @param progressFigure the progress figure for this node
     */
    public NodeContainerFigure(final ProgressFigure progressFigure) {

        m_backgroundColor = ColorConstants.white;
        m_showFlowVarPorts = false;

        m_description = null;

        setOpaque(false);
        setFill(false);
        setOutline(false);

        // no border
        // setBorder(SimpleEtchedBorder.singleton);

        // add sub-figures
        setLayoutManager(new DelegatingLayout());

        final IPreferenceStore store =
                KNIMEUIPlugin.getDefault().getPreferenceStore();
        final int height = store.getInt(PreferenceConstants.P_NODE_LABEL_FONT_SIZE);
        final String fontName = fontName();
        final Display current = Display.getDefault();
        final Font normalFont = new Font(current, fontName, height, SWT.NORMAL);
        final Font boldFont = new Font(current, fontName, height, SWT.BOLD);

        // Heading (Label)
        m_heading = new Label();
        m_headingTooltip = new NewToolTipFigure("");
        m_heading.setToolTip(m_headingTooltip);
        m_heading.setFont(boldFont);

        // Name (Label)
        m_name = new Label();
        m_nameTooltip = new NewToolTipFigure("");
        m_name.setFont(normalFont);
        super.setFont(normalFont);

        // icon
        m_symbolFigure = new SymbolFigure();

        // Status: traffic light
        m_statusFigure = new StatusFigure();
        // progress bar
        if (progressFigure != null) {
            m_progressFigure = progressFigure;
        } else {
            m_progressFigure = new ProgressFigure();
        }
        m_progressFigure.setCurrentDisplay(Display.getCurrent());
        m_progressFigure.setOpaque(true);
        // Additional status (warning/error sign)
        m_infoWarnErrorPanel = new InfoWarnErrorPanel();

        // the locators depend on the order!
        add(m_heading);
        add(m_symbolFigure);
        add(m_infoWarnErrorPanel);
        add(m_statusFigure);
        add(m_name);

        // layout the components
        setConstraint(m_heading, new NodeContainerLocator(this));
        setConstraint(m_symbolFigure, new NodeContainerLocator(this));
        setConstraint(m_infoWarnErrorPanel, new NodeContainerLocator(this));
        setConstraint(m_statusFigure, new NodeContainerLocator(this));
        setConstraint(m_name, new NodeContainerLocator(this));
    }

    boolean getShowFlowVarPorts() {
        return m_showFlowVarPorts;
    }

    public void setShowFlowVarPorts(final boolean showPorts) {
        m_showFlowVarPorts = showPorts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final IFigure figure, final Object constraint,
            final int index) {
        // the only one who is allowed to add objects at a certain index
        // are we (with the method addAtIndex).
        // Ports are added by the framework with index - but we need them
        // at the end of the list (the layouter depends on it).
        // We ignore any index provided.
        super.add(figure, constraint, getChildren().size());
    }

    private void addAtIndex(final IFigure figure, final int index) {
        super.add(figure, null, index);
    }

    /**
     * Returns the progress figure of this node figure. The figure is also a
     * progress listener and can be registered with node progress monitors. It
     * renders the progress then.
     *
     * @return the progress figure associated with this node container figure.
     */
    public ProgressFigure getProgressFigure() {
        return m_progressFigure;
    }

    public StatusFigure getStatusFigure() {
        return m_statusFigure;
    }

    Label getHeader() {
        return m_heading;
    }

    /**
     * Sets the icon.
     *
     * @param icon the icon
     */
    public void setIcon(final Image icon) {
        m_symbolFigure.setIcon(icon);
    }

    /**
     * Sets the type.
     *
     * @param type the type
     */
    public void setType(final NodeType type) {
        m_symbolFigure.setType(type);

    }

    public void setJobExecutorIcon(final Image jobExecIcon) {
        m_jobExec = jobExecIcon;
        m_symbolFigure.refreshJobManagerIcon();
    }

    public void setMetaNodeLinkIcon(final Image icon) {
        if (!ConvenienceMethods.areEqual(m_metaNodeLinkIcon, icon)) {
            m_metaNodeLinkIcon = icon;
            m_symbolFigure.refreshMetaNodeLinkIcon();
        }
    }

    /**
     * Sets the text of the heading label.
     *
     * @param text The text to set.
     */
    public void setLabelText(final String text) {
        m_heading.setText(wrapText(text));
        m_headingTooltip.setText(text);
        repaint();
    }

    private String wrapText(final String text) {
        if (text == null || text.length() == 0) {
            return "";
        }
        // wrap the text with line breaks if too long
        // we split just one time (i.e. two lines at most)
        if (text.trim().length() < 20) {
            return text.trim();
        }
        if (text.trim().indexOf(" ") < 0) {
            return text.trim();
        }
        final int middle = text.length() / 2;
        // now go left and right to the next space
        // the closest space is used for a split
        int indexLeft = middle;
        int indexRight = middle + 1;
        for (; indexLeft >= 0 && indexRight < text.length(); indexLeft--, indexRight++) {
            if (text.charAt(indexLeft) == ' ') {
                final StringBuilder sb = new StringBuilder(text);
                return sb.replace(indexLeft, indexLeft + 1, "\n").toString();
            }
            if (text.charAt(indexRight) == ' ') {
                final StringBuilder sb = new StringBuilder(text);
                return sb.replace(indexRight, indexRight + 1, "\n").toString();
            }
        }

        return text;
    }

    /**
     * Sets the user name of the figure.
     *
     * @param name The name to set.
     */
    public void setCustomName(final String name) {
        if (name == null || name.trim().equals("")) {
            m_name.setText("");
        } else {
            // if the name is not already set
            m_name.setText(name.trim());

            // if the tooltip (description) contains content, set it
            String toolTipText = m_name.getText();
            if (m_description != null && !m_description.trim().equals("")) {
                toolTipText =
                        toolTipText + ":\n\n Description:\n" + m_description;
            }

            m_nameTooltip.setText(toolTipText);
            m_name.setToolTip(m_nameTooltip);
        }
    }

    /**
     * Sets the description for this node as the name's tooltip.
     *
     * @param description the description to set as tooltip
     */
    public void setCustomDescription(final String description) {

        if (description == null || description.trim().equals("")) {

            // if there is no description, reset the description
            // and invoke the set name method once more to
            // adjust the tooltip text
            m_description = null;
            setCustomName(m_name.getText());
            return;
        }

        m_description = description;

        String toolTipText = m_name.getText();

        toolTipText = toolTipText + ":\n\n Description:\n" + m_description;

        m_nameTooltip.setText(toolTipText);
        m_name.setToolTip(m_nameTooltip);

    }

    private boolean isChild(final Figure figure) {
        for (final Object contentFigure : getChildren()) {
            if (contentFigure == figure) {
                return true;
            }
        }

        return false;
    }

    /**
     * Replaces the status traffic light with the progress bar. This is done
     * once the node is paused, queued or executing.
     *
     * @param the mode @see ProgressMode.
     */
    private void setProgressBar(final ProgressMode mode) {

        // remove both intergangable onse
        if (isChild(m_statusFigure)) {
            remove(m_statusFigure);
        }

        boolean alreadySet = false;
        m_progressFigure.reset();
        // and set the progress bar
        if (!isChild(m_progressFigure)) {

            // reset the progress first
            addAtIndex(m_progressFigure, 3);
            setConstraint(m_progressFigure, new NodeContainerLocator(this));

        } else {
            // if already set, remember this
            alreadySet = true;
        }

        switch (mode) {
        case EXECUTING:
            // temporarily remember the execution state of the progress bar
            final ProgressMode oldMode = m_progressFigure.getProgressMode();
            m_progressFigure.setProgressMode(ProgressMode.EXECUTING);
            m_progressFigure.setStateMessage("Executing");

            // if the progress bar was not set already
            // init it with an unknown progress first
            if (!alreadySet || !ProgressMode.EXECUTING.equals(oldMode)) {
                m_progressFigure.activateUnknownProgress();
            }
            break;
        case QUEUED:
            m_progressFigure.setProgressMode(ProgressMode.QUEUED);
            m_progressFigure.setStateMessage("Queued");
            break;
        case PAUSED:
            m_progressFigure.setProgressMode(ProgressMode.PAUSED);
            m_progressFigure.setStateMessage("Paused");
            break;
        }
    }

    private void setStatusAmple() {

        // in every case reset the progress bar
        m_progressFigure.reset();

        // remove both intergangable onse
        if (isChild(m_progressFigure)) {
            remove(m_progressFigure);
            m_progressFigure.stopUnknownProgress();
            m_progressFigure.setProgressMode(ProgressMode.QUEUED);
        }

        // and set the progress bar
        if (!isChild(m_statusFigure)) {
            addAtIndex(m_statusFigure, 3);
            setConstraint(m_statusFigure, new NodeContainerLocator(this));
        }
    }

    /**
     *
     * @param state new state of underlying node
     * @param isInactive is true, the state is ignored and the inactive status
     *            figure set.
     */
    public void setState(final NodeContainer.State state,
            final LoopStatus loopStatus,
            final boolean isInactive) {
        if (!isInactive) {
            switch (state) {
            case IDLE:
                setStatusAmple();
                m_statusFigure.setIcon(RED);
                break;
            case CONFIGURED:
                setStatusAmple();
                m_statusFigure.setIcon(YELLOW);
                break;
            case EXECUTED:
                setStatusAmple();
                m_statusFigure.setIcon(GREEN);
                break;
            case PREEXECUTE:
            case EXECUTING:
            case EXECUTINGREMOTELY:
            case POSTEXECUTE:
                setProgressBar(ProgressMode.EXECUTING);
                break;
            case MARKEDFOREXEC:
                if (LoopStatus.PAUSED.equals(loopStatus)) {
                    setProgressBar(ProgressMode.PAUSED);
                    break;
                }
                // if not - just handle it like QUEUED and U_ME
            case UNCONFIGURED_MARKEDFOREXEC:
            case QUEUED:
                setProgressBar(ProgressMode.QUEUED);
                break;
            }
        } else {
            setStatusAmple();
            m_statusFigure.setIcon(INACTIVE);
        }
        setLoopStatus(loopStatus, state);
        repaint();
    }

    /**
     *
     * @param msg the node message
     */
    public void setMessage(final NodeMessage msg) {
        if (msg == null || msg.getMessageType() == null) {
            removeMessages();
            NodeLogger.getLogger(NodeContainerFigure.class).warn(
                    "Received NULL message!");
        } else {
            switch (msg.getMessageType()) {
            case RESET:
                removeMessages();
                break;
            case WARNING:
                m_infoWarnErrorPanel.setWarning(msg.getMessage());
                break;
            case ERROR:
                m_infoWarnErrorPanel.setError(msg.getMessage());
                break;
            }
        }
        m_statusFigure.repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getMinimumSize(final int whint, final int hhint) {
        return getPreferredSize(whint, hhint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize(final int wHint, final int hHint) {

        int prefWidth = Math.max(WIDTH, m_heading.getTextBounds().width);
        // add some offset, that the selection border is not directly at
        // the label
        prefWidth += 10;

        int prefHeight = 0;
        prefHeight += m_heading.getPreferredSize().height;
        prefHeight += m_symbolFigure.getPreferredSize().height;
        prefHeight += m_progressFigure.getPreferredSize().height;
        prefHeight += m_statusFigure.getPreferredSize().height;
        prefHeight += 20; // fixed size for the info error warn panel
        prefHeight += m_name.getPreferredSize().height;

        return new Dimension(prefWidth, prefHeight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getBackgroundColor() {
        return m_backgroundColor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getForegroundColor() {
        return ColorConstants.white;
    }

    /**
     * @return The content figure (= content pane for port visuals)
     */
    public IFigure getSymbolFigure() {
        return m_symbolFigure;
    }

    /**
     * Removes all currently set messages.
     */
    public void removeMessages() {
        m_infoWarnErrorPanel.removeAll();
    }

    /**
     * We need to set the color before invoking super. {@inheritDoc}
     */
    @Override
    protected void fillShape(final Graphics graphics) {
        graphics.setBackgroundColor(getBackgroundColor());
        super.fillShape(graphics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintFigure(final Graphics graphics) {
        graphics.setBackgroundColor(getBackgroundColor());
        super.paintFigure(graphics);
        if (m_loopStatusFigure != null) {
            final Rectangle r = getSymbolFigure().getBounds();
            graphics.drawImage(m_loopStatusFigure,
                    new Point(r.x + 32, r.y + 32));
        }

    }

    /**
     * Subfigure, hosts the icon and the job manager icon.
     */
    protected class SymbolFigure extends Figure {

        private static final int SYMBOL_FIG_HEIGHT = 48;

        private static final int SYMBOL_FIG_WIDTH = 32;

        private final Label m_iconFigure;

        private final Label m_deleteIcon;

        private static final String BACKGROUND_OTHER = "icons/node/"
                + "background_other.png";

        private static final String BACKGROUND_SOURCE = "icons/node/"
                + "background_source.png";

        private static final String BACKGROUND_SINK = "icons/node/"
                + "background_sink.png";

        private static final String BACKGROUND_LEARNER = "icons/node/"
                + "background_learner.png";

        private static final String BACKGROUND_PREDICTOR = "icons/node/"
                + "background_predictor.png";

        private static final String BACKGROUND_MANIPULATOR = "icons/node/"
                + "background_manipulator.png";

        private static final String BACKGROUND_META = "icons/node/"
                + "background_meta.png";

        private static final String BACKGROUND_VIEWER = "icons/node/"
                + "background_viewer.png";

        private static final String BACKGROUND_UNKNOWN = "icons/node/"
                + "background_unknown.png";

        private static final String BACKGROUND_LOOPER_START =
                "icons/node/background_looper_start.png";

        private static final String BACKGROUND_LOOPER_END =
                "icons/node/background_looper_end.png";

        private static final String BACKGROUND_QUICKFORM =
            "icons/node/background_quickform.png";

        private final Label m_backgroundIcon;

        /**
         * The base icon without overlays.
         *
         * Is used once the overlay has to be undone
         */
        private Image m_baseIcon;

        private Label m_jobExecutorLabel;

        private Label m_metaNodeLinkedLabel;

        /**
         * Creates a new figure containing the symbol. That is the background
         * icon (depending on the type of the node) and the node's icon. Also
         * the job manager indicator and the mark for deletion.
         */
        public SymbolFigure() {
            // delegating layout, children provide a Locator as constraint
            final DelegatingLayout layout = new DelegatingLayout();
            setLayoutManager(layout);
            setOpaque(false);
            setFill(false);

            // setLayoutManager(new BorderLayout());

            // the "frame", that indicates the node type
            m_backgroundIcon = new Label();
            // m_backgroundIcon.setIconAlignment(PositionConstants.CENTER);

            // create a label that shows the nodes' icon
            m_iconFigure = new Label();
            // m_iconFigure.setBorder(new RaisedBorder(2, 2, 2, 2));
            m_iconFigure.setOpaque(false);

            // create the delete icon
            m_deleteIcon = new Label();
            m_deleteIcon.setOpaque(false);
            m_deleteIcon.setIcon(DELETE_SIGN);

            // center the icon figure
            add(m_backgroundIcon);
            m_backgroundIcon.setLayoutManager(new DelegatingLayout());
            m_backgroundIcon.add(m_iconFigure);
            m_backgroundIcon.setConstraint(m_iconFigure, new RelativeLocator(
                    m_backgroundIcon, 0.5, 0.5));

            setConstraint(m_backgroundIcon, new RelativeLocator(this, 0.5, 0.5));
        }

        protected void refreshJobManagerIcon() {
            // do we have to remove it?
            if (m_jobExecutorLabel != null && m_jobExec == null) {
                m_backgroundIcon.remove(m_jobExecutorLabel);
                m_jobExecutorLabel = null;
            } else {
                if (m_jobExecutorLabel == null) {
                    m_jobExecutorLabel = new Label();
                    m_jobExecutorLabel.setOpaque(false);
                    m_backgroundIcon.add(m_jobExecutorLabel);
                    m_backgroundIcon.setConstraint(m_jobExecutorLabel,
                            new RelativeLocator(m_backgroundIcon, 0.73, 0.73));
                }
                m_jobExecutorLabel.setIcon(m_jobExec);
                repaint();
            }
        }

        protected void refreshMetaNodeLinkIcon() {
            // do we have to remove it?
            if (m_metaNodeLinkedLabel != null && m_metaNodeLinkIcon == null) {
                m_backgroundIcon.remove(m_metaNodeLinkedLabel);
                m_metaNodeLinkedLabel = null;
            } else {
                if (m_metaNodeLinkedLabel == null) {
                    m_metaNodeLinkedLabel = new Label();
                    m_metaNodeLinkedLabel.setOpaque(false);
                    m_backgroundIcon.add(m_metaNodeLinkedLabel);
                    m_backgroundIcon.setConstraint(m_metaNodeLinkedLabel,
                            new RelativeLocator(m_backgroundIcon, 0.21, .84));
                }
                m_metaNodeLinkedLabel.setIcon(m_metaNodeLinkIcon);
                repaint();
            }
        }

        /**
         * This determines the background image according to the "type" of the
         * node as stored in the repository model.
         *
         * @param type The Type
         * @return Image that should be uses as background for this node
         */
        private Image getBackgroundForType(final NodeType type) {

            String str = null;
            if (type == null) {
                str = BACKGROUND_UNKNOWN;
            } else if (type.equals(NodeType.Source)) {
                str = BACKGROUND_SOURCE;
            } else if (type.equals(NodeType.Sink)) {
                str = BACKGROUND_SINK;
            } else if (type.equals(NodeType.Manipulator)) {
                str = BACKGROUND_MANIPULATOR;
            } else if (type.equals(NodeType.Learner)) {
                str = BACKGROUND_LEARNER;
            } else if (type.equals(NodeType.Predictor)) {
                str = BACKGROUND_PREDICTOR;
            } else if (type.equals(NodeType.Meta)) {
                str = BACKGROUND_META;
            } else if (type.equals(NodeType.Other)) {
                str = BACKGROUND_OTHER;
            } else if (type.equals(NodeType.Visualizer)) {
                str = BACKGROUND_VIEWER;
            } else if (type.equals(NodeType.LoopStart)) {
                str = BACKGROUND_LOOPER_START;
            } else if (type.equals(NodeType.LoopEnd)) {
                str = BACKGROUND_LOOPER_END;
            } else if (type.equals(NodeType.QuickForm)) {
                str = BACKGROUND_QUICKFORM;
            } else {
                str = BACKGROUND_UNKNOWN;
            }
            final Image img = ImageRepository.getImage(str);

            return img == null ? ImageRepository.getImage(BACKGROUND_OTHER)
                    : img;
        }

        /**
         * Sets a type specific background image.
         *
         * @param type The node type, results in a different background
         * @see org.knime.workbench.repository.model.NodeTemplate
         */
        void setType(final NodeType type) {
            m_backgroundIcon.setIcon(getBackgroundForType(type));
        }

        /**
         * Sets the icon for the node (now provided from the factory class).
         *
         * @param icon Image to display as icon
         */
        void setIcon(final Image icon) {

            if (m_baseIcon == null) {
                m_baseIcon = icon;
            }

            m_iconFigure.setIcon(icon);
            // m_iconFigure.setIconAlignment(PositionConstants.CENTER);
            // m_iconFigure.setIconDimension(new Dimension(16, 16));

            m_iconFigure.revalidate();
        }

        void setBackgroundIcon(final Image icon) {
            m_backgroundIcon.setIcon(icon);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {
            return new Dimension(SYMBOL_FIG_WIDTH, SYMBOL_FIG_HEIGHT);
        }

    }

    /**
     * Subfigure containing the information/warning/error signs. The panel can
     * display any combination of the signs and also provides functionality to
     * set tool tips containing one or more messages for each category
     * (info/warning/error).
     */
    private class InfoWarnErrorPanel extends Figure {

        /**
         * The info figure.
         */
        private final InfoWarnErrorFigure m_infoFigure;

        /**
         * The warning figure.
         */
        private final InfoWarnErrorFigure m_warningFigure;

        /**
         * The error figure.
         */
        private final InfoWarnErrorFigure m_errorFigure;

        /**
         * Constructor for a new <code>SignPanel</code>.
         */
        public InfoWarnErrorPanel() {

            // a flow layout is used to arrange all signs in
            // a line

            final FlowLayout layout = new FlowLayout(true);
            layout.setMajorAlignment(FlowLayout.ALIGN_CENTER);
            layout.setMinorAlignment(FlowLayout.ALIGN_CENTER);
            layout.setMajorSpacing(3);
            setLayoutManager(layout);

            // create the info/warning/error figures
            m_infoFigure = new InfoWarnErrorFigure();
            m_infoFigure.setIcon(INFO_SIGN);

            m_warningFigure = new InfoWarnErrorFigure();
            m_warningFigure.setIcon(WARNING_SIGN);

            m_errorFigure = new InfoWarnErrorFigure();
            m_errorFigure.setIcon(ERROR_SIGN);

            setVisible(true);
            repaint();
        }

        /**
         * Sets a new warning message.
         *
         * @param message the message to set
         */
        public void setWarning(final String message) {

            // as the warning sign should always be before the error sign
            // but after the info sign, it must be checked in the case there
            // is only one sign so far at which position to insert the
            // warning sign
            final List<Figure> children = getChildren();

            // check if there is already a warning sign
            boolean alreadyInserted = false;
            for (final Figure child : children) {
                if (child == m_warningFigure) {
                    alreadyInserted = true;
                }
            }

            if (!alreadyInserted) {
                // in case of 0 children the sign can simply be inserted
                if (children.size() == 0) {
                    add(m_warningFigure);
                } else if (children.size() == 2) {
                    // in case of 2 children, the warning sign must be at idx 1
                    add(m_warningFigure, 1);
                } else {
                    // else there is exact 1 child
                    final Figure figure = children.get(0);
                    // in case of the error sign, the warning sign has to be
                    // inserted before the error sign
                    if (figure == m_errorFigure) {
                        add(m_warningFigure, 0);
                    } else {
                        // else append at the end (after the info sign)
                        add(m_warningFigure);
                    }
                }
            }

            if (message != null && !message.trim().equals("")) {
                m_warningFigure.setToolTip(message, WarnErrorToolTip.WARNING);
            } else {
                m_warningFigure.setToolTip("Warning occured: no details.",
                        WarnErrorToolTip.WARNING);
            }

            repaint();
        }

        /**
         * Sets a new error message.
         *
         * @param message the message to set
         */
        public void setError(final String message) {

            // the error sign is always the last sign.
            add(m_errorFigure);

            if (message != null && !message.trim().equals("")) {
                m_errorFigure.setToolTip(message, WarnErrorToolTip.ERROR);
            } else {
                m_errorFigure.setToolTip("Error occured: no details.",
                        WarnErrorToolTip.ERROR);
            }

            repaint();
        }
    }

    /**
     * Subfigure, contains the "traffic light".
     */
    private class StatusFigure extends Figure {

        private final Label m_label;

        /**
         * Creates a new bottom figure.
         */
        public StatusFigure() {
            // status figure must have exact same dimensions as progress bar
            setBounds(new Rectangle(0, 0, ProgressFigure.WIDTH,
                    ProgressFigure.HEIGHT));
            final ToolbarLayout layout = new ToolbarLayout(false);
            layout.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
            layout.setStretchMinorAxis(true);
            setLayoutManager(layout);
            m_label = new Label();

            // the font is just set due to a bug in the getPreferredSize
            // method of a label which accesses the font somewhere
            // if not set a NPE is thrown.
            // PO: Set a small font. The status image (as icon of the label) is
            // placed at the bottom of the label, which is too low, if the
            // font is bigger than the slot for the image.
            m_label.setFont(NODE_FONT);

            // m_label.setIconAlignment(PositionConstants.CENTER);
            add(m_label);
            setOpaque(false);
            setIcon(RED);
            // setIcon(null);

        }

        /**
         * Sets the icon to display.
         *
         * @param icon The icon (traffic light) to set
         */
        void setIcon(final Image icon) {
            m_label.setIcon(icon);
            revalidate();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {
            return new Dimension(ProgressFigure.WIDTH, ProgressFigure.HEIGHT);
        }

    }

    /**
     * Subfigure, contains the warning error signs.
     */
    private class InfoWarnErrorFigure extends Figure {

        private final Label m_label;

        /**
         * Creates a new bottom figure.
         */
        public InfoWarnErrorFigure() {

            final ToolbarLayout layout = new ToolbarLayout(false);
            layout.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
            layout.setStretchMinorAxis(true);
            setLayoutManager(layout);
            m_label = new Label();

            // m_label.setIconAlignment(PositionConstants.CENTER);
            add(m_label);
            setOpaque(false);
        }

        /**
         * Sets the icon to display.
         *
         * @param icon The icon (traffic light) to set
         */
        public void setIcon(final Image icon) {
            m_label.setIcon(icon);
            revalidate();
        }

        /**
         * Sets tool tip message.
         *
         * @param message The status message for the tool tip
         */
        private void setToolTip(final String message, final int type) {
            m_label.setToolTip(new WarnErrorToolTip(message, type));
            revalidate();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {
            return super.getPreferredSize(NodeContainerFigure.this
                    .getSymbolFigure().getPreferredSize().width, m_label
                    .getPreferredSize().height);
        }

    }

    /**
     * @return the rectangle of the name label.
     */
    public Rectangle getNameLabelRectangle() {
        return m_name.getClientArea().getCopy();
    }

    /**
     * @return the user node name of this figure
     */
    public String getCustomName() {
        return m_name.getText();
    }

    /**
     * Marks this node parts figure. Used to hilite it from the rest of the
     * parts.
     *
     * @see NodeContainerFigure#unmark()
     */
    public void mark() {
        m_symbolFigure.m_backgroundIcon.add(m_symbolFigure.m_deleteIcon);
        m_symbolFigure.m_backgroundIcon.setConstraint(
                m_symbolFigure.m_deleteIcon, new RelativeLocator(
                        m_symbolFigure.m_backgroundIcon, 0.5, 0.5));
    }

    /**
     * Resets the marked figure.
     *
     * @see NodeContainerFigure#mark()
     */
    public void unmark() {
        m_symbolFigure.m_backgroundIcon.remove(m_symbolFigure.m_deleteIcon);
    }

    /**
     * Set a new font size which is applied to the node name and label.
     *
     * @param fontSize the new font size to ba applied.
     */
    public void setFontSize(final int fontSize) {
        // apply new font for node name
        final Font font1 = m_heading.getFont();
        final FontData fontData1 = font1.getFontData()[0];
        fontData1.setHeight(fontSize);
        m_heading.setFont(new Font(Display.getDefault(), fontData1));
        font1.dispose();
        // apply new font for node label
        final Font font2 = m_name.getFont();
        final FontData fontData2 = font2.getFontData()[0];
        fontData2.setHeight(fontSize);
        final Font newFont2 = new Font(Display.getDefault(), fontData2);
        m_name.setFont(newFont2);
        // apply the standard node label font also to its parent figure to allow
        // editing the node label with the same font (size)
        super.setFont(newFont2);
        font2.dispose();
    }

    /**
     * Set the hide flag to hide/show the node name.
     *
     * @param hide true, if the node name is visible, otherwise false
     */
    public void hideNodeName(final boolean hide) {
        m_heading.setVisible(!hide);
    }

    /**
     * Set the image indicating the loop status.
     *
     * @param loopStatus loop status of the loop end node
     * @param state execution status of the node.
     */
    private void setLoopStatus(final LoopStatus loopStatus,
            final NodeContainer.State state) {
        if (loopStatus.equals(LoopStatus.NONE)) {
            m_loopStatusFigure = null;
        } else if (loopStatus.equals(LoopStatus.RUNNING)
                || loopStatus.equals(LoopStatus.PAUSED)) {
            m_loopStatusFigure = LOOP_IN_PROGRESS_SIGN;
        } else {
            assert loopStatus.equals(LoopStatus.FINISHED);
            if (state.equals(State.EXECUTED)) {
                m_loopStatusFigure = LOOP_DONE_SIGN;
            } else {
                m_loopStatusFigure = LOOP_NO_STATUS;
            }
        }
    }

    /**
     * New bounds describe the boundaries of figure with x/y at the top left
     * corner. Since 2.3.0 the UI info stores the boundaries with x/y relative
     * to the icon symbol. Width and height in the UI info is in both cases the
     * width and height of the figure. <br />
     * This method returns x/y offsets - basically the current distance of the
     * top left corner to the top left corner of the symbol (with the current
     * font size etc.).
     * @param uiInfo underlying node's ui information holding the bounds
     * @return the offset of the reference point in the figure to the figure's
     *         symbol (values could be negative, if bounds are very small).
     * @since KNIME 2.3.0
     */
    public Point getOffsetToRefPoint(final NodeUIInformation uiInfo) {
        final int yDiff = m_heading.getPreferredSize().height
                + NodeContainerLocator.GAP * 2;
        final Rectangle t = this.getBounds();
        int thiswidth = uiInfo.getBounds()[2];
        if (thiswidth <= 0) {
            thiswidth = t.width;
            if (thiswidth <= 0) {
                // figure with not set yet
                thiswidth = getPreferredSize().width;
            }
        }
        final int xDiff = (thiswidth - m_symbolFigure.getPreferredSize().width) / 2;

        final Point r = new Point(xDiff, yDiff);
        return r;
    }

}
