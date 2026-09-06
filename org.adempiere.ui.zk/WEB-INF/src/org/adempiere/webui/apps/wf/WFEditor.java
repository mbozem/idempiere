/******************************************************************************
 * Copyright (C) 2008 Low Heng Sin                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.adempiere.webui.apps.wf;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.ToolBar;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.Icon;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.apps.wf.WFGraphLayout;
import org.compiere.apps.wf.WFNodeWidget;
import org.compiere.apps.wf.WorkflowGraphScene;
import org.compiere.model.MEntityType;
import org.compiere.model.MSysConfig;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFNodeNext;
import org.compiere.wf.MWorkflow;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.adempiere.webui.component.FlexHlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.North;
import org.zkoss.zul.Separator;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbarbutton;
import org.adempiere.webui.component.FlexVlayout;

/**
 * Workflow editor form
 * @author Low Heng Sin
 */
@org.idempiere.ui.zk.annotation.Form(name = "org.compiere.apps.wf.WFPanel")
public class WFEditor extends ADForm {
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 4293422396394778274L;

	/** Workflows dropdown list */
	private Listbox workflowList;
	private int m_workflowId = 0;
	private Toolbarbutton zoomButton;
	private Toolbarbutton refreshButton;
	private Toolbarbutton newButton;
	/** Content of {@link #center} */
	private Div graphPanel;
	/** Center of form */
	private Center center;
	private MWorkflow m_wf;
	private WFNodeContainer nodeContainer;
	/** Shadow-free rendering of the workflow scene, used as the source for
	 *  the node card drag images (same layout as the displayed background, so
	 *  the ghost is an exact shadow-free copy of the node) */
	private BufferedImage dragCardBg;

	/**
	 * Layout form
	 */
	@Override
	protected void initForm() {
		ZKUpdateUtil.setHeight(this, "100%");
		Borderlayout layout = new Borderlayout();
		layout.setStyle("width: 100%; height: 100%; position: relative;");
		appendChild(layout);
		KeyNamePair[] pp = MWorkflow.getWorkflowKeyNamePairs(true);

		workflowList = ListboxFactory.newDropdownListbox();
		for (KeyNamePair knp : pp) {
			workflowList.addItem(knp);
		}
		workflowList.addEventListener(Events.ON_SELECT, this);

		North north = new North();
		layout.appendChild(north);
		ToolBar toolbar = new ToolBar();
		north.appendChild(toolbar);
		toolbar.appendChild(workflowList);
		// Zoom
		zoomButton = new Toolbarbutton();
		if (ThemeManager.isUseFontIconForImage())
			zoomButton.setIconSclass(Icon.getIconSclass(Icon.ZOOM));
		else
			zoomButton.setImage(ThemeManager.getThemeResource("images/Zoom16.png"));
		toolbar.appendChild(zoomButton);
		zoomButton.addEventListener(Events.ON_CLICK, this);
		zoomButton.setTooltiptext(Util.cleanAmp(Msg.getMsg(Env.getCtx(), "Zoom")));
		// New Node
		newButton = new Toolbarbutton();
		if (ThemeManager.isUseFontIconForImage())
			newButton.setIconSclass(Icon.getIconSclass(Icon.NEW));
		else
			newButton.setImage(ThemeManager.getThemeResource("images/New16.png"));
		toolbar.appendChild(newButton);
		newButton.addEventListener(Events.ON_CLICK, this);
		newButton.setTooltiptext(Msg.getMsg(Env.getCtx(), "CreateNewNode"));
		// Refresh
		refreshButton = new Toolbarbutton();
		if (ThemeManager.isUseFontIconForImage())
			refreshButton.setIconSclass(Icon.getIconSclass(Icon.REFRESH));
		else
			refreshButton.setImage(ThemeManager.getThemeResource("images/Refresh16.png"));
		toolbar.appendChild(refreshButton);
		refreshButton.addEventListener(Events.ON_CLICK, this);
		refreshButton.setTooltiptext(Util.cleanAmp(Msg.getMsg(Env.getCtx(), "Refresh")));
		ZKUpdateUtil.setHeight(north, "30px");

		center = new Center();
		layout.appendChild(center);
		center.setAutoscroll(true);
		// position relative so the per-cell overlays can be placed absolutely
		graphPanel = new Div();
		graphPanel.setStyle("position:relative;");
		center.appendChild(graphPanel);

		ConfirmPanel confirmPanel = new ConfirmPanel(true);
		confirmPanel.addActionListener(this);
		South south = new South();
		layout.appendChild(south);
		south.appendChild(confirmPanel);
		ZKUpdateUtil.setHeight(south, "36px");
	}

	@Override
	public void onEvent(Event event) throws Exception {
		super.onEvent(event);

		if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
			this.detach();
		else if (event.getTarget().getId().equals(ConfirmPanel.A_OK))
			this.detach();
		else if (event.getTarget() == workflowList) {
			graphPanel.getChildren().clear();
			ListItem item = workflowList.getSelectedItem();
			KeyNamePair knp = item != null ? item.toKeyNamePair() : null;
			if (knp != null && knp.getKey() > 0) {
				load(knp.getKey(), true);
			}
		}
		else if (event.getTarget() == zoomButton) {
			if (workflowList.getSelectedIndex() > 0)
				zoom();
		}
		else if (event.getTarget() == refreshButton) {
			if (workflowList.getSelectedIndex() > 0)
				reload(m_workflowId, true);
		}
		else if (event.getTarget() == newButton) {
			if (workflowList.getSelectedIndex() > 0)
				createNewNode();
		}
		else if (event.getTarget() instanceof WFPopupItem) {
			WFPopupItem item = (WFPopupItem) event.getTarget();
			item.execute(this);
		}
		else if (event.getName().equals(Events.ON_DROP)) {
			DropEvent dropEvent = (DropEvent) event;
			Integer AD_WF_Node_ID = (Integer) dropEvent.getDragged().getAttribute("AD_WF_Node_ID");
			Integer xPosition = (Integer) event.getTarget().getAttribute("Node.XPosition");
			Integer yPosition = (Integer) event.getTarget().getAttribute("Node.YPosition");
			if (AD_WF_Node_ID != null && xPosition != null && yPosition != null) {
				moveNode(AD_WF_Node_ID, xPosition, yPosition);
			}
			else if (AD_WF_Node_ID != null && event.getTarget().getAttribute("AD_WF_Next_ID") != null) {
				// dropped on a connector hit area: the connector div carries no
				// cell attributes, so resolve the cell under the pointer from
				// the div's own position plus the drop offset (see
				// addConnectorHitDiv)
				Integer xOffset = (Integer) event.getTarget().getAttribute("Connector.XOffset");
				Integer yOffset = (Integer) event.getTarget().getAttribute("Connector.YOffset");
				if (xOffset != null && yOffset != null) {
					int col = (int) Math.floor((xOffset + dropEvent.getX() - WFGraphLayout.MARGIN_X) / WFGraphLayout.COLUMN_WIDTH) + 1;
					if (col < 1)
						col = 1;
					int row = (int) Math.floor((yOffset + dropEvent.getY() - WFGraphLayout.MARGIN_Y) / WFGraphLayout.ROW_HEIGHT) + 1;
					if (row < 1)
						row = 1;
					if (col <= nodeContainer.getColumnCount() && nodeContainer.findWidget(row, col) == null)
						moveNode(AD_WF_Node_ID, col, row);
				}
			}
		}
	}

	/**
	 * Move a workflow node to a new grid cell and rerender the editor.
	 * @param AD_WF_Node_ID node to move
	 * @param xPosition new column (1 based)
	 * @param yPosition new row (1 based)
	 */
	private void moveNode(Integer AD_WF_Node_ID, int xPosition, int yPosition) {
		WFNodeWidget widget = (WFNodeWidget) nodeContainer.getGraphScene().findWidget(AD_WF_Node_ID);
		if (widget != null) {
			MWFNode node = widget.getModel();
			if (node.getAD_Client_ID() == Env.getAD_Client_ID(Env.getCtx())) {
				node.setXPosition(xPosition);
				node.setYPosition(yPosition);
				node.saveEx();
				reload(m_workflowId, true);
			}
		}
	}

	/**
	 * Create new workflow node
	 */
	private void createNewNode() {
		String nameLabel = Msg.getElement(Env.getCtx(), MWFNode.COLUMNNAME_Name);
		String title = Msg.getMsg(Env.getCtx(), "CreateNewNode");
		final Window w = new Window();
		w.setTitle(title);
		FlexVlayout vbox = new FlexVlayout();
		w.appendChild(vbox);
		vbox.appendChild(new Separator());
		FlexHlayout hbox = new FlexHlayout();
		hbox.appendChild(new Label(nameLabel));
		hbox.appendChild(new Space());
		final Textbox text = new Textbox();
		hbox.appendChild(text);
		vbox.appendChild(hbox);
		vbox.appendChild(new Separator());
		final ConfirmPanel panel = new ConfirmPanel(true, false, false, false, false, false, false);
		vbox.appendChild(panel);
		panel.addActionListener(Events.ON_CLICK, new EventListener<Event>() {

			public void onEvent(Event event) throws Exception {
				if (event.getTarget() == panel.getButton(ConfirmPanel.A_CANCEL)) {
					text.setText("");
				}
				w.onClose();
			}
		});
		
		w.setBorder("normal");
		w.setPage(this.getPage());
		w.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {
				String name = text.getText();
				if (name != null && name.length() > 0)
				{
					int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
					MWFNode node = new MWFNode(m_wf, name, name);
					node.setClientOrg(AD_Client_ID, 0);
					if (AD_Client_ID > 11)
						node.setEntityType(MSysConfig.getValue(MSysConfig.DEFAULT_ENTITYTYPE, MEntityType.ENTITYTYPE_UserMaintained));
					node.saveEx();
					reload(m_wf.getAD_Workflow_ID(), true);
				}
			}
		});
		w.doHighlighted();				
	}

	/**
	 * reload and re-render workflow nodes
	 * @param workflowId
	 * @param reread
	 */
	protected void reload(int workflowId, boolean reread) {
		graphPanel.getChildren().clear();
		load(workflowId, reread);
	}

	/**
	 * Load and render workflow nodes
	 * @param workflowId
	 * @param reread
	 */
	private void load(int workflowId, boolean reread) {
		//	Get Workflow
		m_wf = MWorkflow.getCopy(Env.getCtx(), workflowId, (String)null);
		m_workflowId = workflowId;
		nodeContainer = new WFNodeContainer();
		nodeContainer.setWorkflow(m_wf);
		
		if (reread) {
			m_wf.reloadNodes();
		}

		//	Add Nodes for Paint
		MWFNode[] nodes = m_wf.getNodes(true, Env.getAD_Client_ID(Env.getCtx()));
		nodeContainer.setColumnCount(nodes, true);
		List<Integer> added = new ArrayList<Integer>();
		for (int i = 0; i < nodes.length; i++)
		{
			if (!added.contains(nodes[i].getAD_WF_Node_ID()))
				nodeContainer.addNode(nodes[i]);
		}
		
		//  Add lines (routed longest span first, keeps tunnels and streams straight)
		Map<Integer, MWFNode> nodesById = new HashMap<Integer, MWFNode>();
		for (int i = 0; i < nodes.length; i++)
			nodesById.put(nodes[i].getAD_WF_Node_ID(), nodes[i]);
		List<MWFNodeNext> transitions = new ArrayList<MWFNodeNext>();
		for (int i = 0; i < nodes.length; i++)
			Collections.addAll(transitions, nodes[i].getTransitions(Env.getAD_Client_ID(Env.getCtx())));
		for (MWFNodeNext edge : WFNodeContainer.inRouteOrder(transitions, nodesById))
			nodeContainer.addEdge(edge);

		// render workflow graph as image (supersampled, see WFRenderUtil.RENDER_SCALE).
			// The surface keeps the reserved row/column that the editor shows for
			// dropping new nodes and the routing gutter (filler tile) wraps the
			// whole cell matrix on all four sides; the workflow scene keeps
			// transitions off the empty reserve cells
			// (see WorkflowGraphScene.createCollisionsCollector).
			int row = nodeContainer.getRowCount();
			int rowsToRender = row + (nodeContainer.canAddRow() ? 1 : 0);
			int columns = nodeContainer.getColumnCount();
			int imageWidth = WFGraphLayout.MARGIN_X + columns * WFGraphLayout.COLUMN_WIDTH + WFGraphLayout.MARGIN_X;
			int imageHeight = WFGraphLayout.MARGIN_Y + rowsToRender * WFGraphLayout.ROW_HEIGHT + WFGraphLayout.MARGIN_Y;
		BufferedImage bi = new BufferedImage ((int) Math.round(imageWidth * WFRenderUtil.RENDER_SCALE),
				(int) Math.round(imageHeight * WFRenderUtil.RENDER_SCALE), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = bi.createGraphics();
		WFRenderUtil.applyRenderingHints(graphics);
		nodeContainer.validate(graphics);
		Graphics2D paintGraphics = (Graphics2D) graphics.create();
		paintGraphics.scale(WFRenderUtil.RENDER_SCALE, WFRenderUtil.RENDER_SCALE);
		// editable editor only: light gray 1px guide lines on the grid cell
		// borders, painted before the nodes so the workflow stays on top.
		// Every row gets a line below and the first row also above; every
		// column gets a line to the right and the first column also to the
		// left - i.e. a full grid frame around and between the cells.
		paintGraphics.setColor(new Color(0xD0, 0xD0, 0xD0));
		paintGraphics.setStroke(new BasicStroke(1.0f));
		for (int r = 0; r <= rowsToRender; r++)
			paintGraphics.drawLine(0, WFGraphLayout.MARGIN_Y + r * WFGraphLayout.ROW_HEIGHT,
					imageWidth, WFGraphLayout.MARGIN_Y + r * WFGraphLayout.ROW_HEIGHT);
		for (int c = 0; c <= columns; c++)
			paintGraphics.drawLine(WFGraphLayout.MARGIN_X + c * WFGraphLayout.COLUMN_WIDTH, 0,
					WFGraphLayout.MARGIN_X + c * WFGraphLayout.COLUMN_WIDTH, imageHeight);
		// paint the workflow (cards, connectors and the per-widget drop
		// shadow) on top; the widget shadow uses the scene coordinates of
		// the widgets themselves, exactly like the non-editable WFPanel
		nodeContainer.paint(paintGraphics);
		paintGraphics.dispose();
		graphics.dispose();

		// second pass without shadows: the drag images are cropped from this
		// shadow-free rendering, so the drag ghost shows a clean card and,
		// unlike a crop of the shadowed background, carries no shadow. The
		// connectors are hidden for this pass as well, so the ghost contains
		// no connector fragments.
		dragCardBg = new BufferedImage ((int) Math.round(imageWidth * WFRenderUtil.RENDER_SCALE),
				(int) Math.round(imageHeight * WFRenderUtil.RENDER_SCALE), BufferedImage.TYPE_INT_ARGB);
		Graphics2D cleanGraphics = dragCardBg.createGraphics();
		WFRenderUtil.applyRenderingHints(cleanGraphics);
		try {
			((WorkflowGraphScene) nodeContainer.getGraphScene()).setConnectionsVisible(false);
			for (int r = 0; r < row; r++)
				for (int cc = 0; cc < columns; cc++) {
					WFNodeWidget w = nodeContainer.findWidget(r+1, cc+1);
					if (w != null)
						w.setShadowEnabled(false);
				}
			Graphics2D cleanPaint = (Graphics2D) cleanGraphics.create();
			cleanPaint.scale(WFRenderUtil.RENDER_SCALE, WFRenderUtil.RENDER_SCALE);
			nodeContainer.paint(cleanPaint);
			cleanPaint.dispose();
		} finally {
			((WorkflowGraphScene) nodeContainer.getGraphScene()).setConnectionsVisible(true);
			for (int r = 0; r < row; r++)
				for (int cc = 0; cc < columns; cc++) {
					WFNodeWidget w = nodeContainer.findWidget(r+1, cc+1);
					if (w != null)
						w.setShadowEnabled(true);
				}
		}
		cleanGraphics.dispose();

		try {
			// The whole workflow is rendered into a single continuous image
			// (WFRenderUtil.RENDER_SCALE times larger than the logical size to
			// stay sharp on HiDPI). Tiling it into per-cell <img> parts would
			// leave visible seams and a grid line between the cells in the
			// browser, so the image is shown once and transparent overlay
			// divs provide the per-cell drag & drop, click and tooltip.
			graphPanel.getChildren().clear();
			org.zkoss.zul.Image image = new org.zkoss.zul.Image();
			image.setContent(bi);
			// display at logical size, the 2x source keeps it sharp on HiDPI displays
			ZKUpdateUtil.setWidth(image, imageWidth + "px");
			ZKUpdateUtil.setHeight(image, imageHeight + "px");
			image.setStyle("display:block;border:none;margin:0;padding:0;vertical-align:top;");
			graphPanel.appendChild(image);

			// visible card rectangles (scene == panel coordinates) used to keep
			// the connector hit areas outside the cards
			List<Rectangle> cardRects = new ArrayList<Rectangle>();

			for(int i = 0; i < rowsToRender; i++) {
				for(int c = 0; c < columns; c++) {
					Div overlay = new Div();
String style = "position:absolute;left:" + (WFGraphLayout.MARGIN_X + c * WFGraphLayout.COLUMN_WIDTH) + "px;"
						+ "top:" + (WFGraphLayout.MARGIN_Y + i * WFGraphLayout.ROW_HEIGHT) + "px;"
						+ "width:" + WFGraphLayout.COLUMN_WIDTH + "px;"
						+ "height:" + WFGraphLayout.ROW_HEIGHT + "px;";
					overlay.setStyle(style);

					WFNodeWidget widget = i < row ? nodeContainer.findWidget(i+1, c+1) : null;
					if (widget != null)
					{
						MWFNode node = widget.getModel();
						if (node.getHelp(true) != null) {
							overlay.setTooltiptext(node.getHelp(true));
						}
						overlay.setAttribute("AD_WF_Node_ID", node.getAD_WF_Node_ID());
						overlay.addEventListener(Events.ON_CLICK, new EventListener<Event>() {

							public void onEvent(Event event) throws Exception {
								showNodeMenu(event.getTarget());
							}
						});
						overlay.setStyle(style + ";cursor:pointer;");
						// a separate card image is dragged (not the whole tile),
						// so the drag ghost shows only the node. The card is
						// rendered standalone without its drop shadow into a
						// small padded image, so all four card edges are fully
						// visible and the ghost is a clean card.
						boolean nodeDragImage = false;
						try {
							Point pos = widget.getPreferredLocation();
							Rectangle cardRect = new Rectangle(
									pos.x - CARD_DRAG_MARGIN, pos.y - CARD_DRAG_MARGIN,
									WFNodeWidget.NODE_WIDTH + 2 * CARD_DRAG_MARGIN,
									WFNodeWidget.NODE_HEIGHT + 2 * CARD_DRAG_MARGIN);
							cardRects.add(cardRect);
							int sx = (int) Math.round(cardRect.x * WFRenderUtil.RENDER_SCALE);
							int sy = (int) Math.round(cardRect.y * WFRenderUtil.RENDER_SCALE);
							int sw = (int) Math.round(cardRect.width * WFRenderUtil.RENDER_SCALE);
							int sh = (int) Math.round(cardRect.height * WFRenderUtil.RENDER_SCALE);
							if (dragCardBg != null && sx >= 0 && sy >= 0
									&& sx + sw <= dragCardBg.getWidth() && sy + sh <= dragCardBg.getHeight()) {
								org.zkoss.zul.Image cardImg = new org.zkoss.zul.Image();
								cardImg.setContent(dragCardBg.getSubimage(sx, sy, sw, sh));
								cardImg.setStyle("position:absolute;left:"
										+ (cardRect.x - WFGraphLayout.MARGIN_X - c * WFGraphLayout.COLUMN_WIDTH) + "px;"
										+ "top:" + (cardRect.y - WFGraphLayout.MARGIN_Y - i * WFGraphLayout.ROW_HEIGHT) + "px;"
										+ "width:" + cardRect.width + "px;"
										+ "height:" + cardRect.height + "px;"
										+ "display:block;");
								cardImg.setAttribute("AD_WF_Node_ID", node.getAD_WF_Node_ID());
								cardImg.setDraggable("WFNode");
								cardImg.setSclass("wf-drag-center");
								overlay.appendChild(cardImg);
								nodeDragImage = true;
							}
						} catch (Exception e) {
							logger.log(Level.WARNING, "Unable to build node drag image", e);
						}
						if (!nodeDragImage)
							overlay.setDraggable("WFNode");
					}
					else
					{
						overlay.setDroppable("WFNode");
						overlay.addEventListener(Events.ON_DROP, this);
						overlay.setAttribute("Node.XPosition", c+1);
						overlay.setAttribute("Node.YPosition", i+1);
					}
					graphPanel.appendChild(overlay);
				}
			}
			// make the transition connectors clickable (see addConnectorHitAreas)
			addConnectorHitAreas(cardRects);

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}

		// The card drag ghost is placed by ZK with its top-left corner at the
		// cursor (plus a small offset). Patch the client-side draggable so the
		// workflow card ghost is instead centered on the pointer while dragging.
		// The ghost must not intercept pointer events either: ZK's default keeps
		// the cursor outside the ghost box via the +7/+5 offset, so the ghost
		// only covers the cursor once it is centered. Without pointer-events:
		// none the mouseup would hit the ghost clone instead of the drop cell.
		String js = "(function(){"
				+ "var st=document.getElementById('wf-drag-style');"
				+ "if(!st){st=document.createElement('style');st.id='wf-drag-style';"
				+ "st.textContent='#zk_ddghost{pointer-events:none;}';"
				+ "document.head.appendChild(st);}"
				+ "var D=zk.Draggable;"
				+ "if(!D||D.prototype._wfCenterPatched)return;"
				+ "var orig=D.prototype._draw;"
				+ "if(typeof orig!=='function'){D.prototype._wfCenterPatched=true;return;}"
				+ "D.prototype._draw=function(point,evt){"
				+ "var n=this.node;"
				+ "if(n&&evt&&(n.className||'').indexOf('wf-drag-center')>=0){"
				+ "var st=n.style;"
				+ "st.left=jq.px(evt.pageX+7-n.offsetWidth/2);"
				+ "st.top=jq.px(evt.pageY+5-n.offsetHeight/2);"
				+ "if(st.visibility=='hidden')st.visibility='';"
				+ "return;}"
				+ "return orig.call(this,point,evt);};"
				+ "D.prototype._wfCenterPatched=true;})();";
		Clients.evalJavaScript(js);

	}

	/**
	 * Show popup menu for workflow node
	 * @param target
	 */
	protected void showNodeMenu(Component target) {
		Integer AD_WF_Node_ID = (Integer) target.getAttribute("AD_WF_Node_ID");
		if (AD_WF_Node_ID != null) {
			WFNodeWidget widget = (WFNodeWidget) nodeContainer.getGraphScene().findWidget(AD_WF_Node_ID);
			if (widget != null) {
				MWFNode node = widget.getModel();
				Menupopup popupMenu = new Menupopup();
				// Zoom
				addMenuItem(popupMenu, Util.cleanAmp(Msg.getMsg(Env.getCtx(), "Zoom")), node, WFPopupItem.WFPOPUPITEM_ZOOM);
				if (node.getAD_Client_ID() == Env.getAD_Client_ID(Env.getCtx()))
				{
					// Properties
					addMenuItem(popupMenu, Msg.getMsg(Env.getCtx(), "Properties"), node, WFPopupItem.WFPOPUPITEM_PROPERTIES);
					// Delete node
					String title = Msg.getMsg(Env.getCtx(), "DeleteNode") +
						": " + node.getName(true);
					addMenuItem(popupMenu, title, node, WFPopupItem.WFPOPUPITEM_DELETENODE);
				}
				MWFNode[] nodes = m_wf.getNodes(true, Env.getAD_Client_ID(Env.getCtx()));
				MWFNodeNext[] lines = node.getTransitions(Env.getAD_Client_ID(Env.getCtx()));
				//	Add New Line
				for (MWFNode nn : nodes)
				{
					if (nn.getAD_WF_Node_ID() == node.getAD_WF_Node_ID())
						continue;	//	same
					if (nn.getAD_WF_Node_ID() == node.getAD_Workflow().getAD_WF_Node_ID())
						continue;	//	don't add line to starting node
					boolean found = false;
					for (MWFNodeNext line : lines)
					{
						if (nn.getAD_WF_Node_ID() == line.getAD_WF_Next_ID())
						{
							found = true; // line already exists
							break;
						}
					}
					if (!found) {
						// Check that inverse line doesn't exist
						for (MWFNodeNext revline : nn.getTransitions(Env.getAD_Client_ID(Env.getCtx()))) {
							if (node.getAD_WF_Node_ID() == revline.getAD_WF_Next_ID())
							{
								found = true; // inverse line already exists
								break;
							}
						}
					}
					if (!found)
					{
						String title = Msg.getMsg(Env.getCtx(), "AddLine")
							+ ": " + node.getName(true) + " -> " + nn.getName(true);
						addMenuItem(popupMenu, title, node, nn.getAD_WF_Node_ID());
					}
				}
				//	Delete Lines
				for (MWFNodeNext line : lines)
				{
					if (line.getAD_Client_ID() != Env.getAD_Client_ID(Env.getCtx()))
						continue;
					MWFNode next = MWFNode.get(Env.getCtx(), line.getAD_WF_Next_ID());
					String title = Msg.getMsg(Env.getCtx(), "DeleteLine")
						+ ": " + node.getName(true) + " -> " + next.getName(true);
					addMenuItem(popupMenu, title, line);
				}
				popupMenu.setPage(target.getPage());
				popupMenu.open(target);
			}

		}
	}

	/**
	 * Show popup menu for a workflow transition connector.
	 * @param target connector hit area (has AD_WF_Next_ID attribute)
	 */
	protected void showConnectorMenu(Component target) {
		Integer AD_WF_Next_ID = (Integer) target.getAttribute("AD_WF_Next_ID");
		if (AD_WF_Next_ID != null) {
			MWFNodeNext line = new MWFNodeNext(Env.getCtx(), AD_WF_Next_ID, null);
			if (line.getAD_WF_NodeNext_ID() == AD_WF_Next_ID) {
				Menupopup popupMenu = new Menupopup();
				// Zoom
				addMenuItem(popupMenu, Util.cleanAmp(Msg.getMsg(Env.getCtx(), "Zoom")), line, WFPopupItem.WFPOPUPITEM_ZOOMLINE);
				if (line.getAD_Client_ID() == Env.getAD_Client_ID(Env.getCtx()))
				{
					// Delete transition
					MWFNode node = MWFNode.get(Env.getCtx(), line.getAD_WF_Node_ID());
					MWFNode next = MWFNode.get(Env.getCtx(), line.getAD_WF_Next_ID());
					String title = Msg.getMsg(Env.getCtx(), "DeleteLine") +
						": " + node.getName(true) + " -> " + next.getName(true);
					addMenuItem(popupMenu, title, line);
				}
				popupMenu.setPage(target.getPage());
				popupMenu.open(target);
			}
		}
	}

	/**
	 * 	Zoom to WorkFlow window
	 */
	private void zoom()
	{
		if (m_workflowId > 0) {
			AEnv.zoom(MWorkflow.Table_ID, m_workflowId);
		}
	}	//	zoom

	/**
	 * Menu item to add line to next node or to apply actions (delete, properties or zoom) to source workflow node.
	 * @param menu popup  menu
	 * @param title title
	 * @param node source workflow node
	 * @param AD_WF_NodeTo_ID if > 0, next workflow node id. if < 0, actions to apply to node
	 */
	private void addMenuItem (Menupopup menu, String title, MWFNode node, int AD_WF_NodeTo_ID)
	{
		WFPopupItem item = new WFPopupItem (title, node, AD_WF_NodeTo_ID);
		menu.appendChild(item);
		item.addEventListener(Events.ON_CLICK, this);
	}	//	addMenuItem

	/**
	 * Add Menu Item to - delete line
	 * @param menu popup menu
	 * @param title title
	 * @param line
	 */
	private void addMenuItem (Menupopup menu, String title, MWFNodeNext line)
	{
		WFPopupItem item = new WFPopupItem (title, line);
		menu.appendChild(item);
		item.addEventListener(Events.ON_CLICK, this);
	}	//	addMenuItem

	/**
	 * Menu item to apply an action (zoom) to a transition.
	 * @param menu popup menu
	 * @param title title
	 * @param line transition
	 * @param action action to apply to the transition (WFPopupItem.WFPOPUPITEM_*
	 */
	private void addMenuItem (Menupopup menu, String title, MWFNodeNext line, int action)
	{
		WFPopupItem item = new WFPopupItem (title, line, action);
		menu.appendChild(item);
		item.addEventListener(Events.ON_CLICK, this);
	}	//	addMenuItem

	/**
	 * Make the transition connectors clickable by overlaying transparent hit
	 * areas along their routed paths. Every hit area carries the transition id
	 * and a pointer cursor; when two connectors share a route the topmost div
	 * wins. The areas are clipped to stay outside the node cards, so the cards
	 * keep their own click and drag surface (see
	 * {@link WFConnectorHitArea#compute}). The divs are droppable as well - the
	 * ON_DROP handler resolves the cell under the pointer by position, because
	 * a connector div carries no cell attributes.
	 * @param cardRects visible card rectangles in scene coordinates
	 */
	private void addConnectorHitAreas(List<Rectangle> cardRects) {
		WorkflowGraphScene scene = (WorkflowGraphScene) nodeContainer.getGraphScene();
		List<MWFNodeNext> transitions = scene.getTransitions();
		if (transitions.isEmpty())
			return;
		for (MWFNodeNext line : transitions) {
			List<Point> path = scene.getTransitionPath(line);
			if (path == null || path.size() < 2)
				continue;
			for (Rectangle area : WFConnectorHitArea.compute(path, cardRects))
				addConnectorHitDiv(line, area);
		}
	}

	/**
	 * Append a transparent click/drop area for a transition connector.
	 * @param line transition
	 * @param area hit rectangle (panel == scene coordinates)
	 */
	private void addConnectorHitDiv(MWFNodeNext line, Rectangle area) {
		Div hit = new Div();
		hit.setStyle("position:absolute;left:" + area.x + "px;top:" + area.y + "px;"
				+ "width:" + area.width + "px;height:" + area.height + "px;cursor:pointer;");
		hit.setAttribute("AD_WF_Next_ID", line.getAD_WF_NodeNext_ID());
		hit.setAttribute("Connector.XOffset", area.x);
		hit.setAttribute("Connector.YOffset", area.y);
		hit.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event event) throws Exception {
				showConnectorMenu(event.getTarget());
			}
		});
		hit.setDroppable("WFNode");
		hit.addEventListener(Events.ON_DROP, this);
		graphPanel.appendChild(hit);
	}

	/**
	 * Margin (in scene pixels) added around the card when it is rendered
	 * standalone for the drag image, so that all four card edges appear
	 * completely inside the drag ghost. The visible card is drawn with a
	 * rounded border that extends {@link WFNodeWidget#CARD_BORDER_INSET}
	 * pixels beyond the widget bounds on every side, so the crop must reach
	 * at least that far out to show the full border.
	 */
	private static final int CARD_DRAG_MARGIN = WFNodeWidget.CARD_BORDER_INSET;
}
