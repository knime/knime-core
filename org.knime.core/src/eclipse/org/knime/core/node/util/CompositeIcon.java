/*
 * http://archive.oreilly.com/pub/a/mac/2002/03/22/vertical_text.html
 * License:
 * http://archive.oreilly.com/cs/user/view/cs_msg/6739
 * Anybody can use the code for any purpose; I don't want any compensation, nor do I accept any liability.
 */
package org.knime.core.node.util;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.SwingConstants;

/**
 CompositeIcon is an Icon implementation which draws two icons with a specified relative position:
 LEFT, RIGHT, TOP, BOTTOM specify how icon1 is drawn relative to icon2
 CENTER: icon1 is drawn first, icon2 is drawn over it

 and with horizontal and vertical orientations within the alloted space

 It's useful with VTextIcon when you want an icon with your text:
	if icon1 is the graphic icon and icon2 is the VTextIcon, you get a similar effect
	to a JLabel with a graphic icon and text

	From: http://archive.oreilly.com/pub/a/mac/2002/03/22/vertical_text.html
 */
@Deprecated
final class CompositeIcon implements Icon, SwingConstants {
	Icon fIcon1, fIcon2;
	int fPosition, fHorizontalOrientation, fVerticalOrientation;

	/**
	 Create a CompositeIcon from the specified Icons,
	 using the default relative position (icon1 above icon2)
	 and orientations (centered horizontally and vertically)
	 * @param icon1 First icon.
	 * @param icon2 Second icon.
	 */
	public CompositeIcon(final Icon icon1, final Icon icon2) {
		this(icon1, icon2, TOP);
	}

 	/**
	 Create a CompositeIcon from the specified Icons,
	 using the specified relative position
	 and default orientations (centered horizontally and vertically)
 	 * @param icon1 First icon.
 	 * @param icon2 Second icon.
 	 * @param position Relative position.
	 */
	public CompositeIcon(final Icon icon1, final Icon icon2, final int position) {
		this(icon1, icon2, position, CENTER, CENTER);
	}

 	/**
	 Create a CompositeIcon from the specified Icons,
	 using the specified relative position
	 and orientations
 	 * @param icon1 First icon.
 	 * @param icon2 Second icon.
 	 * @param position Relative position.
 	 * @param horizontalOrientation Horizontal orientation.
 	 * @param verticalOrientation Vertical orientation.
	 */
 	public CompositeIcon(final Icon icon1, final Icon icon2, final int position, final int horizontalOrientation, final int verticalOrientation) {
		fIcon1 = icon1;
		fIcon2 = icon2;
		fPosition = position;
		fHorizontalOrientation = horizontalOrientation;
		fVerticalOrientation = verticalOrientation;
	}

   /**
     * Draw the icon at the specified location.  Icon implementations
     * may use the Component argument to get properties useful for
     * painting, e.g. the foreground or background color.
     */
    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
		int width = getIconWidth();
		int height = getIconHeight();
		if (fPosition == LEFT || fPosition == RIGHT) {
			Icon leftIcon, rightIcon;
			if (fPosition == LEFT) {
				leftIcon = fIcon1;
				rightIcon = fIcon2;
			}
			else {
				leftIcon = fIcon2;
				rightIcon = fIcon1;
			}
			// "Left" orientation, because we specify the x position
			paintIcon(c, g, leftIcon, x, y, width, height, LEFT, fVerticalOrientation);
			paintIcon(c, g, rightIcon, x + leftIcon.getIconWidth(), y, width, height, LEFT, fVerticalOrientation);
		}
		else if (fPosition == TOP || fPosition == BOTTOM) {
			Icon topIcon, bottomIcon;
			if (fPosition == TOP) {
				topIcon = fIcon1;
				bottomIcon = fIcon2;
			}
			else {
				topIcon = fIcon2;
				bottomIcon = fIcon1;
			}
			// "Top" orientation, because we specify the y position
			paintIcon(c, g, topIcon, x, y, width, height, fHorizontalOrientation, TOP);
			paintIcon(c, g, bottomIcon, x, y + topIcon.getIconHeight(), width, height, fHorizontalOrientation, TOP);
		}
		else {
			paintIcon(c, g, fIcon1, x, y, width, height, fHorizontalOrientation, fVerticalOrientation);
			paintIcon(c, g, fIcon2, x, y, width, height, fHorizontalOrientation, fVerticalOrientation);
		}
	}

	/* Paints one icon in the specified rectangle with the given orientations
	*/
	void paintIcon(final Component c, final Graphics g, final Icon icon, final int x, final int y, final int width, final int height,
			final int horizontalOrientation, final int verticalOrientation) {

		int xIcon, yIcon;
		switch (horizontalOrientation) {
			case LEFT:
				xIcon = x;
				break;
			case RIGHT:
				xIcon = x + width - icon.getIconWidth();
				break;
			default:
				xIcon = x + (width - icon.getIconWidth()) / 2;
				break;
		}
		switch (verticalOrientation) {
			case TOP:
				yIcon = y;
				break;
			case BOTTOM:
				yIcon = y + height - icon.getIconHeight();
				break;
			default:
				yIcon = y + (height - icon.getIconHeight()) / 2;
				break;
		}
		icon.paintIcon(c, g, xIcon, yIcon);
	}

    /**
     * Returns the icon's width.
     *
     * @return an int specifying the fixed width of the icon.
     */
    @Override
    public int getIconWidth() {
		if (fPosition == LEFT || fPosition == RIGHT) {
            return fIcon1.getIconWidth() + fIcon2.getIconWidth();
        }

		return Math.max(fIcon1.getIconWidth(), fIcon2.getIconWidth());
	}

    /**
     * Returns the icon's height.
     *
     * @return an int specifying the fixed height of the icon.
     */
    @Override
    public int getIconHeight() {
		if (fPosition == TOP || fPosition == BOTTOM) {
            return fIcon1.getIconHeight() + fIcon2.getIconHeight();
        }

		return Math.max(fIcon1.getIconHeight(), fIcon2.getIconHeight());
	}

}