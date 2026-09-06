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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.desktop.IDesktop;
import org.adempiere.webui.panel.IHelpContext;
import org.adempiere.webui.part.WindowContainer;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.apps.wf.WFGraphLayout;
import org.compiere.apps.wf.WFNodeWidget;
import org.compiere.model.MSysConfig;
import org.compiere.model.X_AD_CtxHelp;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFNodeNext;
import org.compiere.wf.MWorkflow;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.KeyEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.South;

/**
 *	WorkFlow Panel
 *
 * 	@author Low Heng Sin
 */
public class WFPanel extends Borderlayout implements EventListener<Event>, IHelpContext
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 3748544216557474367L;

	/**
	 * SysConfig USE_ESC_FOR_TAB_CLOSING
	 */
	private boolean isUseEscForTabClosing = MSysConfig.getBooleanValue(MSysConfig.USE_ESC_FOR_TAB_CLOSING, false, Env.getAD_Client_ID(Env.getCtx()));
	
	/**
	 * 	Create Workflow Panel
	 */
	public WFPanel ()
	{
		try
		{
			jbInit();
			addEventListener(WindowContainer.ON_WINDOW_CONTAINER_SELECTION_CHANGED_EVENT, this);
		}
		catch(Exception e)
		{
			log.log(Level.SEVERE, "WFPanel", e);
		}
		m_WindowNo = SessionManager.getAppDesktop().registerWindow(this);
		setAttribute(IDesktop.WINDOWNO_ATTRIBUTE, m_WindowNo);	// for closing the window with shortcut
		SessionManager.getSessionApplication().getKeylistener().addEventListener(Events.ON_CTRL_KEY, this);
		addEventListener(IDesktop.ON_CLOSE_WINDOW_SHORTCUT_EVENT, this);
	}	//	WFPanel

	/**	Window No			*/
	private int         m_WindowNo = 0;


	/**	Workflow Model				*/
	private MWorkflow	m_wf = null;

	/**	Logger			*/
	private static final CLogger	log = CLogger.getCLogger(WFPanel.class);
	
	/** Workflow node container */
	private WFNodeContainer nodeContainer = new WFNodeContainer();
	
	private Html infoTextPane = new Html();
	private Div contentPanel = new Div();
	//
	/** container that holds the workflow graph image and the node overlays */
	private Div graphPanel;
		
	/**
	 * 	Static Init
	 *  <pre>
	 * 		centerScrollPane
	 * 			centerPanel
	 * 		south Panel
	 * 			infoScrollPane
	 * 			buttonPanel
	 * 	</pre>
	 * 	@throws Exception
	 */
	private void jbInit() throws Exception
	{
		this.setStyle("height: 100%; width: 100%; position: absolute");
		Center center = new Center();
		this.appendChild(center);
		graphPanel = new Div();
		// position relative so the per-node overlays can be placed absolutely
		graphPanel.setStyle("position:relative;");
		center.appendChild(graphPanel);
		contentPanel.setStyle("width: 100%; height: 100%;");
		center.setAutoscroll(true);
		
		South south = new South();
		this.appendChild(south);
		Div div = new Div();
		div.setStyle("width:100%; height: 100%");
		div.appendChild(infoTextPane);
		south.appendChild(div);
		ZKUpdateUtil.setHeight(south, "25%");
		south.setSplittable(true);
		south.setCollapsible(true);
		south.setAutoscroll(true);
		ZKUpdateUtil.setVflex(div, "1");
		ZKUpdateUtil.setHflex(div, "1");
	}	//	jbInit
		
	/**
	 * 	Dispose
	 */
	public void dispose()
	{
		SessionManager.getAppDesktop().closeActiveWindow();
	}	//	dispose
	
	/**
	 * 	Load Workflow and Nodes
	 * 	@param AD_Workflow_ID ID
	 */
	public void load (int AD_Workflow_ID)
	{
		if (log.isLoggable(Level.FINE)) log.fine("AD_Workflow_ID=" + AD_Workflow_ID);
		if (AD_Workflow_ID == 0)
			return;
		//	Get Workflow
		m_wf = new MWorkflow (Env.getCtx(), AD_Workflow_ID, null);
		nodeContainer.removeAll();
		nodeContainer.setWorkflow(m_wf);
		
		//	Add Nodes for Paint
		MWFNode[] nodes = m_wf.getNodes(true, Env.getAD_Client_ID(Env.getCtx()));
		nodeContainer.setColumnCount(nodes, false);
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
				
		// render workflow graph as image (supersampled, see WFRenderUtil.RENDER_SCALE)
		Dimension dimension = nodeContainer.getDimension();
		int imageWidth = (int) Math.round(dimension.width * WFRenderUtil.RENDER_SCALE);
		int imageHeight = (int) Math.round(dimension.height * WFRenderUtil.RENDER_SCALE);
		BufferedImage bi = new BufferedImage (imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = bi.createGraphics();
		WFRenderUtil.applyRenderingHints(graphics);
		nodeContainer.validate(graphics);
		Graphics2D paintGraphics = (Graphics2D) graphics.create();
		paintGraphics.scale(WFRenderUtil.RENDER_SCALE, WFRenderUtil.RENDER_SCALE);
		nodeContainer.paint(paintGraphics);
		paintGraphics.dispose();
		graphics.dispose();

		try {
			// The whole workflow is rendered into a single continuous image
			// (WFRenderUtil.RENDER_SCALE times larger than the logical size to
			// stay sharp on HiDPI). Tiling it into per-cell <img> parts would
			// leave visible seams between the tiles in the browser, so the image
			// is shown once and transparent overlay divs provide the per-node
			// tooltip and click handling.
			graphPanel.getChildren().clear();
			org.zkoss.zul.Image image = new org.zkoss.zul.Image();
			image.setContent(bi);
			// display at logical size, the 2x source keeps it sharp on HiDPI displays
			ZKUpdateUtil.setWidth(image, dimension.width + "px");
			ZKUpdateUtil.setHeight(image, dimension.height + "px");
			image.setStyle("display:block;border:none;margin:0;padding:0;");
			graphPanel.appendChild(image);

			int row = nodeContainer.getRowCount();
			int maxCol = nodeContainer.getMaxColumnWithNode();
			for(int i = 0; i < row; i++) {
				for(int c = 0; c < maxCol; c++) {
					WFNodeWidget widget = nodeContainer.findWidget(i+1, c+1);
					if (widget == null)
						continue;

					MWFNode node = widget.getModel();
					Div overlay = new Div();
					overlay.setStyle("position:absolute;left:" + (WFGraphLayout.MARGIN_X + c * WFGraphLayout.COLUMN_WIDTH) + "px;"
							+ "top:" + (WFGraphLayout.MARGIN_Y + i * WFGraphLayout.ROW_HEIGHT) + "px;"
							+ "width:" + WFGraphLayout.COLUMN_WIDTH + "px;"
							+ "height:" + WFGraphLayout.ROW_HEIGHT + "px;"
							+ "cursor:pointer;");
					if (node.getHelp(true) != null) {
						overlay.setTooltiptext(node.getHelp(true));
					}
					overlay.setAttribute("AD_WF_Node_ID", node.getAD_WF_Node_ID());
					overlay.addEventListener(Events.ON_CLICK, this);
					graphPanel.appendChild(overlay);
				}
			}

		} catch (Exception e) {
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		
		//	Info Text
		StringBuilder msg = new StringBuilder("");
		msg.append("<H2>").append(m_wf.getName(true)).append("</H2>");
		String s = m_wf.getDescription(true);
		if (s != null && s.length() > 0)
			msg.append("<B>").append(s).append("</B>");
		s = m_wf.getHelp(true);
		if (s != null && s.length() > 0)
			msg.append("<BR>").append(s);
		infoTextPane.setContent(msg.toString());

	}	//	load

	/**
	 * 	String Representation
	 * 	@return info
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("WorkflowPanel[");
		if (m_wf != null)
			sb.append(m_wf.getAD_Workflow_ID());
		sb.append("]");
		return sb.toString();
	}	//	toString
	
	/**
	 * Get workflow model
	 * @return workflow model
	 */
	public MWorkflow getWorkflow() 
	{
		return m_wf;
	}

	@Override
	public void onEvent(Event event) throws Exception {
		if (Events.ON_CLICK.equals(event.getName())) {
			Integer id = (Integer) event.getTarget().getAttribute("AD_WF_Node_ID");
			if (id != null) {
				MWFNode[] nodes = m_wf.getNodes(true, Env.getAD_Client_ID(Env.getCtx()));
				for(MWFNode node : nodes) {
					if (node.getAD_WF_Node_ID() == id) {
						start(node);
						break;
					}
				}
			}
		}
		else if (event.getName().equals(WindowContainer.ON_WINDOW_CONTAINER_SELECTION_CHANGED_EVENT)) {
    		SessionManager.getAppDesktop().updateHelpContext(X_AD_CtxHelp.CTXTYPE_Workflow, m_wf.getAD_Workflow_ID());
		}
		else if (event.getName().equals(Events.ON_CTRL_KEY)) {
        	KeyEvent keyEvent = (KeyEvent) event;
        	if (LayoutUtils.isReallyVisible(this))
	        	this.onCtrlKeyEvent(keyEvent);
		}
		else if(IDesktop.ON_CLOSE_WINDOW_SHORTCUT_EVENT.equals(event.getName())) {
        	IDesktop desktop = SessionManager.getAppDesktop();
        	if (m_WindowNo > 0 && desktop.isCloseTabWithShortcut())
        		desktop.closeWindow(m_WindowNo);
        	else
        		desktop.setCloseTabWithShortcut(true);
        }
	}

	/**
	 * Launch action for workflow node
	 * @param wfn
	 */
	private void start(MWFNode wfn) {
		String action = wfn.getAction();
		if (MWFNode.ACTION_UserWindow.equals(action) && wfn.getAD_Window_ID() > 0) {
			SessionManager.getAppDesktop().openWindow(wfn.getAD_Window_ID(), null);
		} else if (MWFNode.ACTION_UserForm.equals(action) && wfn.getAD_Form_ID() > 0) {
			SessionManager.getAppDesktop().openForm(wfn.getAD_Form_ID());
		} else if (MWFNode.ACTION_AppsProcess.equals(action) && wfn.getAD_Process_ID() > 0) {
			SessionManager.getAppDesktop().openProcessDialog(wfn.getAD_Process_ID(), false);
		} else if (MWFNode.ACTION_AppsTask.equals(action) && wfn.getAD_Task_ID() > 0) {
			SessionManager.getAppDesktop().openTask(wfn.getAD_Task_ID());
		} else if (MWFNode.ACTION_SubWorkflow.equals(action) && wfn.getWorkflow_ID() > 0) {
			SessionManager.getAppDesktop().openWorkflow(wfn.getWorkflow_ID());
		}else if (wfn.getAD_InfoWindow_ID() > 0) {
			SessionManager.getAppDesktop().openInfo(wfn.getAD_InfoWindow_ID());
		}
	}

	/**
	 * Handle shortcut key event
	 * @param keyEvent
	 */
	private void onCtrlKeyEvent(KeyEvent keyEvent) {
		if ((keyEvent.isAltKey() && keyEvent.getKeyCode() == 0x58)	// Alt-X
				|| (keyEvent.getKeyCode() == 0x1B && isUseEscForTabClosing)) {	// ESC
			keyEvent.stopPropagation();
			Events.echoEvent(new Event(IDesktop.ON_CLOSE_WINDOW_SHORTCUT_EVENT, this));
		}
	}
}	//	WFPanel
