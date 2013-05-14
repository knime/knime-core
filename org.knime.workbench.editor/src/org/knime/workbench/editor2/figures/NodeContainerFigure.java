/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
import org.eclipse.draw2d.OrderedLayout;
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
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.SingleNodeContainer;
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
    public static final int WIDTH = SymbolFigure.SYMBOL_FIG_WIDTH
            + (2 * AbstractPortFigure.NODE_PORT_SIZE);

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
            fontName(), 2, SWT.NORMAL);

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
    private final Figure m_headingContainer;

    private String m_label;

    /**
     * Tooltip for displaying the custom description. This tooltip is displayed
     * with the custom name.
     */
    private final NewToolTipFigure m_symbolTooltip;

    private Image m_jobExec;

    private Image m_metaNodeLinkIcon;

    private Image m_metaNodeLockIcon;

    private boolean m_showFlowVarPorts;

    /**
     * Creates a new node figure.
     *
     * @param progressFigure the progress figure for this node
     */
    public NodeContainerFigure(final ProgressFigure progressFigure) {

        m_backgroundColor = ColorConstants.white;
        m_showFlowVarPorts = false;

        setOpaque(false);
        setFill(false);
        setOutline(false);

        // add sub-figures
        setLayoutManager(new DelegatingLayout());


        final IPreferenceStore store =
            KNIMEUIPlugin.getDefault().getPreferenceStore();
        final int height = store.getInt(PreferenceConstants.P_NODE_LABEL_FONT_SIZE);
        final String fontName = fontName();
        Font normalFont = new Font(Display.getDefault(), fontName, height, SWT.NORMAL);
        super.setFont(normalFont);


        // Heading (Label)
        m_headingContainer = new Figure();

        // icon
        m_symbolFigure = new SymbolFigure();
        m_symbolTooltip = new NewToolTipFigure("");

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
        add(m_headingContainer);
        add(m_symbolFigure);
        add(m_infoWarnErrorPanel);
        add(m_statusFigure);

        // layout the components
        setConstraint(m_headingContainer, new NodeContainerLocator(this));
        setConstraint(m_symbolFigure, new NodeContainerLocator(this));
        setConstraint(m_infoWarnErrorPanel, new NodeContainerLocator(this));
        setConstraint(m_statusFigure, new NodeContainerLocator(this));
    }

    /**
     * @return true if implicit flow variable ports are currently shown
     */
    boolean getShowFlowVarPorts() {
        return m_showFlowVarPorts;
    }

    /**
     * @param showPorts true if implicit flow variable ports should be shown
     */
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

    /**
     * @return the figure showing the status (traffic light)
     */
    public StatusFigure getStatusFigure() {
        return m_statusFigure;
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

    public void setMetaNodeLockIcon(final Image icon) {
        if (!ConvenienceMethods.areEqual(m_metaNodeLockIcon, icon)) {
            m_metaNodeLockIcon = icon;
            m_symbolFigure.refreshMetaNodeLockIcon();
        }
    }

    /**
     * Sets the text of the heading label.
     *
     * @param text The text to set.
     */
    @SuppressWarnings("unchecked")
    public void setLabelText(final String text) {
        m_label = text;
        m_headingContainer.removeAll();
        // needed, otherwise labels disappear after font size has changed
        m_headingContainer.setBounds(new Rectangle(0, 0, 0, 0));

        final IPreferenceStore store =
                KNIMEUIPlugin.getDefault().getPreferenceStore();
        final int fontSize =
                store.getInt(PreferenceConstants.P_NODE_LABEL_FONT_SIZE);
        final String fontName = fontName();
        final Display current = Display.getDefault();
        Font boldFont = new Font(current, fontName, fontSize, SWT.BOLD);
        m_headingContainer.setFont(boldFont);

        int width = 0;
        for (String s : wrapText(text).split("\n")) {
            Label l = new Label(s);
            l.setForegroundColor(ColorConstants.black);
            l.setFont(boldFont);
            m_headingContainer.add(l);
            Dimension size = l.getPreferredSize();
            width = Math.max(width, size.width);
        }

        int height = 0;
        for (IFigure child : (List<IFigure>)m_headingContainer.getChildren()) {
            Dimension size = child.getPreferredSize();
            int offset = (width - size.width) / 2;

            child.setBounds(new Rectangle(offset, height, size.width,
                    size.height));
            height += size.height;
        }

        m_headingContainer.setBounds(new Rectangle(0, 0, width, height));
        repaint();
    }

    private static String wrapText(final String text) {
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
     * Sets the description for this node as the symbol's tooltip.
     *
     * @param description the description to set as tooltip
     */
    public void setCustomDescription(final String description) {
        if (description == null || description.trim().equals("")) {
            m_symbolTooltip.setText("");
            m_symbolFigure.setToolTip(null);
        } else {
            m_symbolTooltip.setText(description);
            m_symbolFigure.setToolTip(m_symbolTooltip);
        }
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
     * @param nc new state of underlying node
     */
    public void setStateFromNC(final NodeContainer nc) {
        boolean isInactive = false;
        LoopStatus loopStatus = LoopStatus.NONE;
        if (nc instanceof SingleNodeContainer) {
            SingleNodeContainer snc = (SingleNodeContainer)nc;
            isInactive = snc.isInactive();
            loopStatus = snc.getLoopStatus();
        }
        NodeContainerState state = nc.getNodeContainerState();
        if (!isInactive) {
            if (state.isIdle()) {
                setStatusAmple();
                m_statusFigure.setIcon(RED);
            } else if (state.isConfigured()) {
                setStatusAmple();
                m_statusFigure.setIcon(YELLOW);
            } else if (state.isExecuted()) {
                setStatusAmple();
                m_statusFigure.setIcon(GREEN);
            } else if (state.isWaitingToBeExecuted()) {
                if (LoopStatus.PAUSED.equals(loopStatus)) {
                    setProgressBar(ProgressMode.PAUSED);
                } else {
                    setProgressBar(ProgressMode.QUEUED);
                }
            } else if (state.isExecutionInProgress()) {
                setProgressBar(ProgressMode.EXECUTING);
            } else {
                setStatusAmple();
                m_statusFigure.setIcon(INACTIVE);
            }
        } else {
            setStatusAmple();
            m_statusFigure.setIcon(INACTIVE);
        }
        setLoopStatus(loopStatus, state.isExecuted());
        repaint();
    }

    /**
     *
     * @param msg the node message
     */
    public void setMessage(final NodeMessage msg) {
        removeMessages();
        if (msg == null || msg.getMessageType() == null) {
            NodeLogger.getLogger(NodeContainerFigure.class).warn("Received NULL message!");
        } else {
            switch (msg.getMessageType()) {
                case RESET:
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
    public boolean containsPoint(final int x, final int y) {
        if (!getBounds().contains(x, y)) {
            return false;
        }
        for (final Object contentFigure : getChildren()) {
            if (((IFigure)contentFigure).containsPoint(x, y)) {
                if (contentFigure == m_headingContainer) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
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

        int prefWidth = Math.max(WIDTH, m_headingContainer.getBounds().width);

        int prefHeight = 0;
        int compCount = 3;
        prefHeight += m_headingContainer.getPreferredSize().height;
        prefHeight += m_symbolFigure.getPreferredSize().height;
        // meta node don't have a status figure
        if (isChild(m_statusFigure) || isChild(m_progressFigure)) {
            prefHeight += m_statusFigure.getPreferredSize().height;
            compCount++;
        }
        prefHeight += m_infoWarnErrorPanel.getPreferredSize().height;
        prefHeight += (compCount * NodeContainerLocator.GAP);
        // make sure all ports fit in the figure (ports start below the label)
        int minPortsHeight = m_headingContainer.getPreferredSize().height + getMinimumPortsHeight();
        if (minPortsHeight > prefHeight) {
            prefHeight = minPortsHeight;
        }
        return new Dimension(prefWidth, prefHeight);
    }

    /**
     * @return the minimum height required to display all (input or output) ports
     */
    private int getMinimumPortsHeight() {
        int minH = 0;
        NodeInPortFigure inPort = null;
        NodeOutPortFigure outPort = null;
        for (Object o : getChildren()) {
            if ((inPort == null) && (o instanceof NodeInPortFigure)) {
                inPort = (NodeInPortFigure)o;
            }
            if ((outPort == null) && (o instanceof NodeOutPortFigure)) {
                outPort = (NodeOutPortFigure)o;
            }
        }
        if (inPort != null) {
            int minIn = ((NodePortLocator)inPort.getLocator()).getMinimumHeightForPorts();
            if (minH < minIn) {
                minH = minIn;
            }
        }
        if (outPort != null) {
            int minOut = ((NodePortLocator)outPort.getLocator()).getMinimumHeightForPorts();
            if (minH < minOut) {
                minH = minOut;
            }
        }
        return minH;
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
    public void paint(final Graphics graphics) {
        // paints the figure and its children
        super.paint(graphics);
        if (m_loopStatusFigure != null) {
            final Rectangle r = getSymbolFigure().getBounds();
            graphics.drawImage(m_loopStatusFigure,
                    new Point(r.x + 24, r.y + 32));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintFigure(final Graphics graphics) {
        graphics.setBackgroundColor(getBackgroundColor());
        super.paintFigure(graphics);
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

        private static final String BACKGROUND_MISSING = "icons/node/"
                + "background_missing.png";

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

        private static final String BACKGROUND_SCOPE_START =
                "icons/node/background_scope_start.png";

        private static final String BACKGROUND_SCOPE_END =
                "icons/node/background_scope_end.png";

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

        private Label m_metaNodeLockLabel;


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

        protected void refreshMetaNodeLockIcon() {
            // do we have to remove it?
            if (m_metaNodeLockLabel != null && m_metaNodeLockIcon == null) {
                m_backgroundIcon.remove(m_metaNodeLockLabel);
                m_metaNodeLockLabel = null;
            } else {
                if (m_metaNodeLockLabel == null) {
                    m_metaNodeLockLabel = new Label();
                    m_metaNodeLockLabel.setOpaque(false);
                    m_backgroundIcon.add(m_metaNodeLockLabel);
                    m_backgroundIcon.setConstraint(m_metaNodeLockLabel,
                            new RelativeLocator(m_backgroundIcon, 0.79, .24));
                }
                m_metaNodeLockLabel.setIcon(m_metaNodeLockIcon);
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
            } else if (type.equals(NodeType.Missing)) {
                str = BACKGROUND_MISSING;
            } else if (type.equals(NodeType.Visualizer)) {
                str = BACKGROUND_VIEWER;
            } else if (type.equals(NodeType.LoopStart)) {
                str = BACKGROUND_LOOPER_START;
            } else if (type.equals(NodeType.LoopEnd)) {
                str = BACKGROUND_LOOPER_END;
            } else if (type.equals(NodeType.ScopeStart)) {
                str = BACKGROUND_SCOPE_START;
            } else if (type.equals(NodeType.ScopeEnd)) {
                str = BACKGROUND_SCOPE_END;
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
            layout.setMajorAlignment(OrderedLayout.ALIGN_CENTER);
            layout.setMinorAlignment(OrderedLayout.ALIGN_CENTER);
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
                m_warningFigure.setToolTip("Warning occurred: no details.",
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
                m_errorFigure.setToolTip("Error occurred: no details.",
                        WarnErrorToolTip.ERROR);
            }

            repaint();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {
            if (getChildren().size() == 0) {
                return new Dimension(0, 0);
            }
            org.eclipse.swt.graphics.Rectangle err_bnds = ERROR_SIGN.getBounds();
            org.eclipse.swt.graphics.Rectangle wrn_bnds = WARNING_SIGN.getBounds();
            int h = Math.max(0, Math.max(err_bnds.height, wrn_bnds.height));
            int w = Math.max(0, Math.max(err_bnds.width, wrn_bnds.width));
            return new Dimension(w, h);
        }
    }

    /**
     * Subfigure, contains the "traffic light".
     */
    private static class StatusFigure extends Figure {

        private final Label m_label;

        /**
         * Creates a new bottom figure.
         */
        public StatusFigure() {
            // status figure must have exact same dimensions as progress bar
            setBounds(new Rectangle(0, 0, ProgressFigure.WIDTH,
                    ProgressFigure.HEIGHT));
            final ToolbarLayout layout = new ToolbarLayout(false);
            layout.setMinorAlignment(OrderedLayout.ALIGN_CENTER);
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
            layout.setMinorAlignment(OrderedLayout.ALIGN_CENTER);
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
        setLabelText(m_label);

        // apply new font for node label
        final Font font2 = super.getFont();
        final FontData fontData2 = font2.getFontData()[0];
        fontData2.setHeight(fontSize);
        final Font newFont2 = new Font(Display.getDefault(), fontData2);
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
        m_headingContainer.setVisible(!hide);
    }

    /**
     * Set the image indicating the loop status.
     *
     * @param loopStatus loop status of the loop end node
     * @param isExecuted is true when node is executed
     */
    private void setLoopStatus(final LoopStatus loopStatus, final boolean isExecuted) {
        if (loopStatus.equals(LoopStatus.NONE)) {
            m_loopStatusFigure = null;
        } else if (loopStatus.equals(LoopStatus.RUNNING) || loopStatus.equals(LoopStatus.PAUSED)) {
            m_loopStatusFigure = LOOP_IN_PROGRESS_SIGN;
        } else {
            assert loopStatus.equals(LoopStatus.FINISHED);
            if (isExecuted) {
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
        final int yDiff = m_headingContainer.getPreferredSize().height
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
