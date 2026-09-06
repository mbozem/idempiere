/**
 *
 */
package org.compiere.apps.wf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.compiere.model.MImage;
import org.compiere.model.MTreeNode;
import org.compiere.util.Env;
import org.compiere.wf.MWFNode;
import org.netbeans.api.visual.border.Border;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.ImageWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.SeparatorWidget;
import org.netbeans.api.visual.widget.Widget;

/**
 * Widget for workflow node
 * @author hengsin
 */
public class WFNodeWidget extends Widget {

	public final static int NODE_WIDTH = 160;
	public final static int NODE_HEIGHT = 112;

	/**
	 * Inset of the rounded card border. The border is painted around the
	 * widget's client area, so the visible card extends that far in each
	 * direction from the widget location. The layout adds this offset again
	 * so the painted card is centered inside its grid cell.
	 */
	public static final int CARD_BORDER_INSET = 8;

	/** Gap between the node icon and the title text inside the title row;
	 *  equals the reduced padding of the title area */
	private static final int TITLE_GAP = 4;

	/** Top and bottom padding of the title area (kept small so three title
	 *  lines still fit next to the separator and description) */
	private static final int TITLE_PADDING = 4;
	private static final int TITLE_BOTTOM_PADDING = 1;

	/** Height reserved for the wrapped title text: three lines including descenders */
	private static final int TITLE_TEXT_HEIGHT = 43;

	/** Horizontal squeeze applied to the bold title font when no condensed
	 *  font family is installed: the title becomes narrower so more characters
	 *  fit into the title row and long node titles need fewer wrapped lines */
	private static final float TITLE_NARROW_SCALE = 0.85f;

	/** Candidate condensed/narrow font families for the title; the first one
	 *  installed on the server is used, otherwise the standard font is
	 *  compressed horizontally by {@link #TITLE_NARROW_SCALE} */
	private static final String[] TITLE_NARROW_FAMILIES = {
		"Arial Narrow",
		"DejaVu Sans Condensed",
		"Liberation Sans Narrow",
		"Noto Sans Condensed",
		"Roboto Condensed",
		"Ubuntu Condensed"
	};

	private static boolean narrowTitleResolved = false;
	private static String narrowTitleFamily;

	/** Padding between the card border and the text content on the left and right */
	private static final int CONTENT_PADDING = 6;

	/** Drop shadow of the card: several faint translucent layers stacked with
	 *  increasing offset. The faint outer layers render first so the shadow
	 *  edge is blurred and the overlay darkens towards the card; the shadow
	 *  reaches about 5-6px below/right of the card instead of a single hard
	 *  offset copy. */
	private static final int[] SHADOW_DX = { 3, 4, 5, 6 };
	private static final int[] SHADOW_DY = { 4, 5, 6, 7 };
	private static final int[] SHADOW_ALPHA = { 22, 16, 12, 8 };
	/** Corner radius of the blurred shadow shape */
	private static final int SHADOW_ARC = 18;

	/** true while the drop shadow is painted; the editor disables it when it
	 *  renders the node card standalone for the drag image, so the drag
	 *  cursor shows only the clean card without a shadow */
	private boolean shadowEnabled = true;

	private static final Color NODE_FILL_COLOR = Color.WHITE;
	private static final Color NODE_BORDER_COLOR = Color.BLACK;
	private static final Color TITLE_COLOR = Color.BLACK;
	private static final Color DESCRIPTION_COLOR = new Color(0x55677D);

	/** Colors of a card that is not editable (the node belongs to another
	 *  AD_Client): the whole card is rendered in gray. */
	private static final Color NON_EDITABLE_FILL_COLOR = new Color(0xE1E3E8);
	private static final Color NON_EDITABLE_BORDER_COLOR = new Color(0x9AA3AD);
	private static final Color NON_EDITABLE_TITLE_COLOR = new Color(0x47505B);
	private static final Color NON_EDITABLE_DESCRIPTION_COLOR = new Color(0x7A8490);

	private int row = 0;
	private int column = 0;

	private MWFNode model;

	/**
	 * Bold, narrow variant of the title font: a condensed/narrow font family
	 * is preferred when the server has one, otherwise the bold font is
	 * compressed horizontally so long node titles take less horizontal space.
	 * @param base the scene's default (plain sans-serif) font
	 * @return the font used for the node title
	 */
	private Font createTitleFont (Font base) {
		if (!narrowTitleResolved) {
			narrowTitleResolved = true;
			Set<String> available = new HashSet<> (Arrays.asList (
					GraphicsEnvironment.getLocalGraphicsEnvironment ()
							.getAvailableFontFamilyNames ()));
			for (String family : TITLE_NARROW_FAMILIES)
				if (available.contains (family)) {
					narrowTitleFamily = family;
					break;
				}
		}
		Font bold = base.deriveFont (Font.BOLD);
		if (narrowTitleFamily != null)
			return new Font (narrowTitleFamily, Font.PLAIN, base.getSize ()).deriveFont (Font.BOLD);
		return bold.deriveFont (AffineTransform.getScaleInstance (TITLE_NARROW_SCALE, 1.0));
	}

	/**
	 * @param scene
	 * @param node
	 */
	public WFNodeWidget(Scene scene, MWFNode node) {
		super(scene);

		setLayout (LayoutFactory.createVerticalFlowLayout ());
        setOpaque (true);
        // no clipping: the drop shadow is painted a few pixels outside the
        // card bounds, and overflowing label lines are cut at line boundaries
        setCheckClipping (false);
        // transparent background so the corners of the rounded card border are not filled
        setBackground(new Color(255, 255, 255, 0));

        setPreferredSize(new Dimension(NODE_WIDTH, NODE_HEIGHT));

        ImageWidget imageWidget = null;
        int imageId = node.getAD_Image_ID();
        if (imageId > 0) {
        	MImage mImage = MImage.get(Env.getCtx(), imageId);
        	Image image = null;
        	byte[] imageData = mImage.getBinaryData();
        	if (imageData != null && imageData.length > 0) {
        		try {
					image = ImageIO.read(new ByteArrayInputStream(imageData));
				} catch (IOException e) {
				}
        	} else {
        		String url = mImage.getImageURL();
        		if (url != null && url.trim().length() > 0) {
        			try {
    					image = ImageIO.read(new URL(url));
    				} catch (IOException e) {
    				}
        		}
        	}
        	if (image != null) {
        		imageWidget = new ImageWidget(scene, image);
        		imageWidget.setToolTipText(node.getName(true));
        		addChild(imageWidget);
        	}
        }
        else {
        	// Nodes of another AD_Client are not editable, their card is shown
        	// in gray (own-client nodes were always white, other-client nodes
        	// were gray in the original implementation).
        	Color fillColor = NODE_FILL_COLOR;
        	Color borderColor = NODE_BORDER_COLOR;
        	Color titleColor = TITLE_COLOR;
        	Color descriptionColor = DESCRIPTION_COLOR;
        	if (node.getAD_Client_ID() != Env.getAD_Client_ID(Env.getCtx())) {
        		fillColor = NON_EDITABLE_FILL_COLOR;
        		borderColor = NON_EDITABLE_BORDER_COLOR;
        		titleColor = NON_EDITABLE_TITLE_COLOR;
        		descriptionColor = NON_EDITABLE_DESCRIPTION_COLOR;
        	}
        	// rounded card border; the arc and uniform 8px inset keep the
        	// border margin equal on all sides so the card stays centered
        	setBorder (BorderFactory.createRoundedBorder (8, 6, 8, 8, fillColor, borderColor));
        	// title row: icon + wrapped title, vertically centered, gap equal to the
        	// reduced title padding
	        Widget titleWidget = new Widget (scene);
	        titleWidget.setLayout (LayoutFactory.createHorizontalFlowLayout (LayoutFactory.SerialAlignment.CENTER, TITLE_GAP));
	        titleWidget.setBorder (BorderFactory.createEmptyBorder (TITLE_PADDING, CONTENT_PADDING, TITLE_BOTTOM_PADDING, CONTENT_PADDING));

	        ImageWidget titleIcon = new ImageWidget (scene);
	        String action = node.getAction();
	        int index = MTreeNode.getImageIndex(action);
	        ImageIcon icon = (ImageIcon) MTreeNode.getIcon(index);  // TODO: font icon
	        if (icon != null)
	        {
	        	titleIcon.setImage (icon.getImage());
	        	titleIcon.setToolTipText(getActionType(node));
	        	titleWidget.addChild (titleIcon);
	        }

	        String titleText = node.getName(true);
	        MultilineLabelWidget titleTextWidget = new MultilineLabelWidget (scene, titleText);
	        titleTextWidget.setFont (createTitleFont (scene.getDefaultFont ()));
	        titleTextWidget.setForeground (titleColor);
	        titleTextWidget.setJustified (false);	// don't stretch short titles with large word gaps
	        // wrap at the client width minus icon and paddings, tall enough for
	        // three lines including descenders (a "g" below the baseline)
	        titleTextWidget.setPreferredSize (new Dimension (NODE_WIDTH - 48, TITLE_TEXT_HEIGHT));
	        titleTextWidget.setToolTipText(node.getName());
	        titleWidget.addChild (titleTextWidget);
	        addChild (titleWidget);

	        // thin separator, inset from the left/right padding, small gap above
	        SeparatorWidget separator = new SeparatorWidget (scene, SeparatorWidget.Orientation.HORIZONTAL);
	        separator.setBorder (BorderFactory.createEmptyBorder (1, CONTENT_PADDING, 0, CONTENT_PADDING));
	        addChild (separator);

	        String description = node.getDescription(true);
			if (description != null && description.length() > 0)
			{
				MultilineLabelWidget label = new MultilineLabelWidget(scene, description);
				label.setForeground(descriptionColor);
				// the description area is big enough for three full lines
				label.setBorder (BorderFactory.createEmptyBorder (1, CONTENT_PADDING, 1, CONTENT_PADDING));
				label.setPreferredSize(new Dimension(NODE_WIDTH - 12, NODE_HEIGHT - 66));
				addChild(label);
			}
        }

		model = node;
	}

	/**
	 * Draw a soft drop shadow a few pixels below and to the right of the card
	 * before the card itself is painted on top by the superclass border.
	 */
	@Override
	protected void paintBorder() {
		Graphics2D gr = getGraphics();
		Rectangle bounds = getBounds();
		if (gr != null && bounds != null && shadowEnabled) {
			paintShadow(gr, bounds);
		}
		super.paintBorder();
	}

	/**
	 * @param enabled true to paint the drop shadow, false to render only the
	 *  clean card (used for drag images)
	 */
	public void setShadowEnabled(boolean enabled) {
		shadowEnabled = enabled;
	}

	/**
	 * Paint the blurred drop shadow for a card at the given bounds. The
	 * widget paints its shadow before the opaque card, so the shadow stays
	 * behind the card and is only visible around the edge (and through the
	 * rounded card corners). The editor also re-uses this shadow when it
	 * crops node cards for drag images.
	 * @param graphics graphics of the same (scaled) coordinate space
	 * @param bounds card bounds
	 */
	public static void paintShadow(Graphics2D graphics, Rectangle bounds) {
		// faint far layers first, darker near layers last: the shadow
		// darkens towards the card (soft, blurred edge)
		for (int i = SHADOW_DX.length - 1; i >= 0; i--) {
			graphics.setColor(new Color(0, 0, 0, SHADOW_ALPHA[i]));
			graphics.fillRoundRect(bounds.x + SHADOW_DX[i], bounds.y + SHADOW_DY[i],
					bounds.width, bounds.height, SHADOW_ARC, SHADOW_ARC);
		}
	}

	/**
	 * 	Get Action Info
	 *	@return info
	 */
	public String getActionType(MWFNode node)
	{
		String action = node.getAction();
		if (MWFNode.ACTION_AppsProcess.equals(action))
			return "Process";
		else if (MWFNode.ACTION_DocumentAction.equals(action))
			return "Document Action: " + node.getDocAction();
		else if (MWFNode.ACTION_AppsReport.equals(action))
			return "Report";
		else if (MWFNode.ACTION_AppsTask.equals(action))
			return "Task";
		else if (MWFNode.ACTION_SetVariable.equals(action))
			return "Set Variable";
		else if (MWFNode.ACTION_SubWorkflow.equals(action))
			return "Workflow";
		else if (MWFNode.ACTION_UserChoice.equals(action))
			return "User Choice";
		else if (MWFNode.ACTION_UserForm.equals(action))
			return "Form";
		else if (MWFNode.ACTION_UserWindow.equals(action))
			return "Window";
		else if (MWFNode.ACTION_UserInfo.equals(action))
			return "Info";
		else if (MWFNode.ACTION_WaitSleep.equals(action))
			return "Sleep:WaitTime=" + node.getWaitTime();
		return "";
	}	//	getActionInfo

	public int getColumn() {
		return column;
	}

	public void setColumn(int column) {
		this.column = column;
	}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}

	public MWFNode getModel() {
		return model;
	}
}
