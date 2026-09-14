/******************************************************************************
 * Copyright (C) 2008 Low Heng Sin                                            *
 * Copyright (C) 2008 Idalica Corporation                                     *
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

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.StatusBarPanel;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.Icon;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MQuery;
import org.compiere.model.MRefList;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.SystemIDs;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.compiere.util.ValueNamePair;
import org.compiere.wf.MWFActivity;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFProcess;
import org.compiere.wf.MWorkflow;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.adempiere.webui.component.FlexHlayout;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Html;
import org.zkoss.zul.South;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.West;

/**
 * Workflow activity form - Outlook style.
 * <p>
 * Left side shows the workflow nodes as card list (like Outlook mail list),
 * right side shows the window referenced by the node (embedded) on top and
 * the workflow control (approve/forward/history/...) below it.
 * @author hengsin
 *
 */
@org.idempiere.ui.zk.annotation.Form(name = "org.compiere.apps.wf.WFActivity")
public class WWFActivity extends ADForm implements EventListener<Event>
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -1658595186719510159L;
	/**	Open Activities				*/
	private MWFActivity[] 		m_activities = null;
	/**	Current Activity			*/
	private MWFActivity 		m_activity = null;
	/**	Current Activity			*/
	private int	 				m_index = -1;
	/**	Set Column					*/
	private	MColumn 			m_column = null;
	/**	Logger			*/
	private static final CLogger log = CLogger.getCLogger(WWFActivity.class);

	//
	private Label lNode = new Label(Msg.translate(Env.getCtx(), "AD_WF_Node_ID"));
	private Textbox fNode = new Textbox();
	private Label lDesctiption = new Label(Msg.translate(Env.getCtx(), "Description"));
	private Textbox fDescription = new Textbox();
	private Label lHelp = new Label(Msg.translate(Env.getCtx(), "Help"));
	private Textbox fHelp = new Textbox();
	private Label lHistory = new Label(Msg.translate(Env.getCtx(), "History"));
	private Html fHistory = new Html();
	private Label lAnswer = new Label(Msg.getMsg(Env.getCtx(), "Answer"));
	private Textbox fAnswerText = new Textbox();
	private org.adempiere.webui.component.Listbox fAnswerList = new org.adempiere.webui.component.Listbox();
	private Button fAnswerButton = new Button();
	private Button bZoom = new Button();
	private Label lTextMsg = new Label(Msg.getMsg(Env.getCtx(), "Messages"));
	private Textbox fTextMsg = new Textbox();
	private Button bOK = new Button();
	private WSearchEditor fForward = null;	//	dynInit
	private Label lForward = new Label(Msg.getMsg(Env.getCtx(), "Forward") + " (" + Msg.translate(Env.getCtx(), "Optional") + ")");
	private StatusBarPanel statusBar = new StatusBarPanel();
	private Button bRefresh = new Button();

	// Outlook layout: card list (west) + embedded document (center) + workflow control (south of inner layout)
	private Textbox fSearch = new Textbox();
	private Label lCount = new Label("0");
	private org.adempiere.webui.component.Listbox fGrouping = new org.adempiere.webui.component.Listbox();
	private Vlayout cardList = new Vlayout();
	private List<Div> cardDivs = new ArrayList<Div>();
	private Div docContainer = new Div();
	private ADWindow embeddedADWindow = null;
	private int embeddedWindowId = 0;
	private MQuery embeddedQuery = null;

	/** grouping/sorting modes for the card list */
	private static final String GROUP_SORT_PRIORITY = "P";
	private static final String GROUP_BY_WORKFLOW = "W";
	private static final String GROUP_BY_NODE = "N";
	private String groupingMode = GROUP_SORT_PRIORITY;
	private Map<Integer, String> workflowNameCache = new HashMap<Integer, String>();

	// Workflow control (bottom): slim by default, expandable for details
	private South controlSouth = null;
	private Row rowNodeInfo = null;
	private Row rowDescInfo = null;
	private Row rowHelpInfo = null;
	private Row rowHistoryInfo = null;
	private Button bDetails = new Button();
	private boolean detailsExpanded = false;

	private final static String HISTORY_DIV_START_TAG = "<div style='overflow-y:scroll;height: 100px; border: 1px solid #7F9DB9;'>";

	/**
	 * default constructor
	 */
	public WWFActivity()
	{
		super();
		LayoutUtils.addSclass("workflow-activity-form", this);
		LayoutUtils.addSclass("wf-outlook", this);
	}

	/**
	 * Load activities and layout form
	 */
    protected void initForm()
    {
    	this.addEventListener("onOK", this);
    	this.addEventListener("onRefreshCards", this);
    	this.addEventListener("onInitialDisplay", this);

        fAnswerList.setMold("select");
        fGrouping.setMold("select");
        fillGroupingOptions();

		if (ThemeManager.isUseFontIconForImage()) {
        	bZoom.setIconSclass(Icon.getIconSclass(Icon.ZOOM));
        	bOK.setIconSclass(Icon.getIconSclass(Icon.OK));
			bRefresh.setIconSclass(Icon.getIconSclass(Icon.REFRESH));
			fAnswerButton.setIconSclass(Icon.getIconSclass(Icon.WINDOW));
        } else {
        	bZoom.setImage(ThemeManager.getThemeResource("images/Zoom16.png"));
        	bOK.setImage(ThemeManager.getThemeResource("images/Ok16.png"));
			bRefresh.setImage(ThemeManager.getThemeResource("images/Refresh16.png"));
			fAnswerButton.setImage(ThemeManager.getThemeResource("images/mWindow.png"));
        }
		setTooltipText(bZoom, "Zoom");
		setTooltipText(bOK, "Ok");
		setTooltipText(bRefresh, "Refresh");

        MLookup lookup = MLookupFactory.get(Env.getCtx(), m_WindowNo,
                0, SystemIDs.COLUMN_AD_WF_ACTIVITY_AD_USER_ID, DisplayType.Search);
        fForward = new WSearchEditor(lookup, Msg.translate(
                Env.getCtx(), "AD_User_ID"), "", true, false, true);

        init();
        loadActivities();
        // embedding needs an attached page (openForm attaches after initForm),
        // so refresh the display once the echo event is delivered post-attach
        Events.echoEvent("onInitialDisplay", this, null);
    }

    /**
     * set tooltip text of btn
     * @param btn
     * @param key AD_Message key
     */
	private void setTooltipText(Button btn, String key) {
		String text = Util.cleanAmp(Msg.translate(Env.getCtx(), key));
		if (!Util.isEmpty(text, true))
			btn.setTooltiptext(text);
	}

	/**
	 * Layout form - Outlook style: card list left, document + workflow control right
	 */
	private void init()
	{
		// --- workflow control grid (right-bottom, slim + expandable) ---
		Grid grid = createControlGrid();
		applyDetailsVisibility();
		fAnswerButton.addEventListener(Events.ON_CLICK, this);
		bZoom.addEventListener(Events.ON_CLICK, this);
		bOK.addEventListener(Events.ON_CLICK, this);
		bRefresh.addEventListener(Events.ON_CLICK, this);
		bDetails.addEventListener(Events.ON_CLICK, this);

		// --- west: search + card list ---
		Vlayout westLayout = new Vlayout();
		ZKUpdateUtil.setHflex(westLayout, "1");
		ZKUpdateUtil.setVflex(westLayout, "1");
		westLayout.setSclass("wf-west-layout");
		westLayout.setStyle("padding: 6px;");

		Hlayout searchBar = new Hlayout();
		ZKUpdateUtil.setHflex(searchBar, "1");
		searchBar.setSclass("wf-search-bar");
		ZKUpdateUtil.setHflex(fSearch, "1");
		fSearch.setPlaceholder(getSearchPlaceholder());
		fSearch.addEventListener(Events.ON_CHANGING, this);
		fSearch.addEventListener(Events.ON_CHANGE, this);
		searchBar.appendChild(fSearch);
		searchBar.appendChild(bRefresh);
		westLayout.appendChild(searchBar);

		ZKUpdateUtil.setHflex(fGrouping, "1");
		fGrouping.setSclass("wf-grouping");
		fGrouping.addEventListener(Events.ON_SELECT, this);
		westLayout.appendChild(fGrouping);

		Hlayout countBar = new Hlayout();
		countBar.setSclass("wf-count-bar");
		countBar.appendChild(lCount);
		westLayout.appendChild(countBar);

		Div cardScroll = new Div();
		cardScroll.setSclass("wf-card-scroll");
		ZKUpdateUtil.setHflex(cardScroll, "1");
		ZKUpdateUtil.setVflex(cardScroll, "1");
		cardScroll.setStyle("overflow-y: auto; overflow-x: hidden;");
		ZKUpdateUtil.setHflex(cardList, "1");
		cardList.setSclass("wf-card-list");
		cardScroll.appendChild(cardList);
		westLayout.appendChild(cardScroll);

		West west = new West();
		west.setSplittable(true);
		west.setCollapsible(true);
		west.setOpen(true);
		ZKUpdateUtil.setWidth(west, "340px");
		west.setSclass("wf-west");
		west.setStyle("background-color: transparent");
		west.appendChild(westLayout);

		// --- center: embedded document, starts directly with its own toolbar ---
		docContainer.setSclass("wf-doc-container");
		ZKUpdateUtil.setHflex(docContainer, "1");
		ZKUpdateUtil.setVflex(docContainer, "1");
		docContainer.setStyle("overflow: auto; background: #fff;");

		Center docCenter = new Center();
		docCenter.setSclass("wf-doc-center");
		docCenter.setStyle("background-color: transparent; overflow: hidden;");
		docCenter.appendChild(docContainer);

		// --- center bottom: workflow control ---
		Vlayout controlLayout = new Vlayout();
		ZKUpdateUtil.setHflex(controlLayout, "1");
		ZKUpdateUtil.setVflex(controlLayout, "1");
		controlLayout.setSclass("wf-control-layout");
		controlLayout.setStyle("padding: 6px; overflow: auto;");
		Hlayout controlHeader = new Hlayout();
		controlHeader.setSclass("wf-control-header");
		ZKUpdateUtil.setHflex(controlHeader, "1");
		controlHeader.setValign("middle");
		Label lControlTitle = new Label(getWorkflowControlTitle());
		lControlTitle.setSclass("wf-control-title");
		lControlTitle.setStyle("font-weight: bold; margin-bottom: 4px;");
		ZKUpdateUtil.setHflex(lControlTitle, "1");
		bDetails.setSclass("wf-details-toggle");
		bDetails.setTooltiptext(getDetailsLabel());
		controlHeader.appendChild(lControlTitle);
		controlHeader.appendChild(bDetails);
		controlLayout.appendChild(controlHeader);
		controlLayout.appendChild(grid);
		ZKUpdateUtil.setVflex(grid, "1");
		ZKUpdateUtil.setHflex(grid, "1");

		controlSouth = new South();
		controlSouth.setSplittable(true);
		controlSouth.setCollapsible(true);
		controlSouth.setOpen(true);
		ZKUpdateUtil.setHeight(controlSouth, "110px");
		controlSouth.setSclass("wf-control-south");
		controlSouth.setStyle("background-color: transparent; overflow: auto;");
		controlSouth.appendChild(controlLayout);

		Borderlayout inner = new Borderlayout();
		ZKUpdateUtil.setHflex(inner, "1");
		ZKUpdateUtil.setVflex(inner, "1");
		inner.setStyle("background-color: transparent;");
		inner.appendChild(docCenter);
		inner.appendChild(controlSouth);

		Center center = new Center();
		center.setStyle("background-color: transparent; overflow: hidden;");
		center.appendChild(inner);

		Borderlayout layout = new Borderlayout();
		ZKUpdateUtil.setWidth(layout, "100%");
		ZKUpdateUtil.setHeight(layout, "100%");
		layout.setStyle("background-color: transparent; position: relative;");
		layout.appendChild(west);
		layout.appendChild(center);

		South south = new South();
		south.appendChild(statusBar);
		layout.appendChild(south);
		south.setStyle("background-color: transparent");

		this.appendChild(layout);
		this.setStyle("height: 100%; width: 100%; position: relative;");

		rebuildCards();
		updateCountLabel();
	}

	/**
	 * Create the workflow control grid (node info, answer, message, forward, ok).
	 * Same fields as before, now shown on the right side below the embedded window.
	 * @return grid
	 */
	private Grid createControlGrid() {
		Grid grid = new Grid();
		grid.setStyle("margin:0; padding:0;");
        grid.makeNoStrip();

		Columns columns = new Columns();
		grid.appendChild(columns);
		Column column = new Column();
		ZKUpdateUtil.setWidth(column, "30%");
		columns.appendChild(column);
		column = new Column();
		ZKUpdateUtil.setWidth(column, "70%");
		columns.appendChild(column);

		Rows rows = new Rows();
		grid.appendChild(rows);

		rowNodeInfo = new Row();
		Row row = rowNodeInfo;
		rows.appendChild(row);
		Div div = new Div();
		div.setStyle("text-align: right;");
		div.appendChild(lNode);
		row.appendChild(div);
		row.appendChild(fNode);
		ZKUpdateUtil.setHflex(fNode, "true");
		fNode.setReadonly(true);

		row = new Row();
		rowDescInfo = row;
		rows.appendChild(row);
		row.setValign("top");
		div = new Div();
		div.setStyle("text-align: right;");
		div.appendChild(lDesctiption);
		row.appendChild(div);
		row.appendChild(fDescription);
		fDescription.setMultiline(true);
		ZKUpdateUtil.setHflex(fDescription, "true");
		fDescription.setReadonly(true);

		row = new Row();
		rowHelpInfo = row;
		rows.appendChild(row);
		div = new Div();
		div.setStyle("text-align: right;");
		div.appendChild(lHelp);
		row.appendChild(div);
		row.appendChild(fHelp);
		fHelp.setMultiline(true);
		fHelp.setRows(3);
		ZKUpdateUtil.setHflex(fHelp, "true");
		fHelp.setReadonly(true);

		row = new Row();
		rowHistoryInfo = row;
		rows.appendChild(row);
		div = new Div();
		div.setStyle("text-align: right;");
		div.appendChild(lHistory);
		row.appendChild(div);
		row.appendChild(fHistory);
		ZKUpdateUtil.setHflex(fHistory, "true");

		// single action row: everything for the compact one-line mode
		// (answer, message, forward, OK); detail rows above are toggled via Details
		row = new Row();
		row.setSclass("wf-action-row");
		rows.appendChild(row);
		FlexHlayout hbox = new FlexHlayout();
		hbox.setSclass("wf-action-bar");
		ZKUpdateUtil.setHflex(hbox, "1");
		hbox.appendChild(fAnswerText);
		ZKUpdateUtil.setHflex(fAnswerText, "1");
		hbox.appendChild(fAnswerList);
		hbox.appendChild(fAnswerButton);
		hbox.appendChild(bZoom);
		hbox.appendChild(fTextMsg);
		ZKUpdateUtil.setHflex(fTextMsg, "2");
		hbox.appendChild(fForward.getComponent());
		hbox.appendChild(bOK);
		row.appendCellChild(hbox, 2);
		fTextMsg.setMultiline(false);
		fTextMsg.setPlaceholder(getMessagePlaceholder());

		return grid;
	}

	private String getMessagePlaceholder() {
		String s = Msg.getMsg(Env.getCtx(), "Messages", true);
		if (Util.isEmpty(s, true) || (s.startsWith("[") && s.endsWith("]")))
			s = Util.cleanAmp(Msg.getMsg(Env.getCtx(), "Messages"));
		if (Util.isEmpty(s, true) || (s.startsWith("[") && s.endsWith("]")))
			s = "Nachricht...";
		return s;
	}

	private String getSearchPlaceholder() {
		String s = Msg.getMsg(Env.getCtx(), "Search", true);
		if (Util.isEmpty(s, true) || (s.startsWith("[") && s.endsWith("]")))
			s = "Suchen...";
		return s;
	}

	/**
	 * Fill the grouping/sorting dropdown.
	 */
	private void fillGroupingOptions() {
		fGrouping.removeAllItems();
		fGrouping.appendItem("Nach Priorität sortiert", GROUP_SORT_PRIORITY);
		fGrouping.appendItem("Gruppiert nach Workflow und sortiert nach Priorität", GROUP_BY_WORKFLOW);
		fGrouping.appendItem("Gruppiert nach Knoten und sortiert nach Priorität", GROUP_BY_NODE);
		fGrouping.setSelectedIndex(0);
		groupingMode = GROUP_SORT_PRIORITY;
	}

	private String getGroupingMode() {
		try {
			ListItem item = fGrouping != null ? fGrouping.getSelectedItem() : null;
			if (item != null && item.getValue() != null)
				return item.getValue().toString();
		} catch (Exception e) {
			log.log(Level.FINE, e.getMessage(), e);
		}
		return groupingMode != null ? groupingMode : GROUP_SORT_PRIORITY;
	}

	private String getWorkflowControlTitle() {
		String s = Msg.getMsg(Env.getCtx(), "WorkflowControl", true);
		if (Util.isEmpty(s, true) || (s.startsWith("[") && s.endsWith("]")))
			s = Msg.getMsg(Env.getCtx(), "WFActivities");
		if (Util.isEmpty(s, true) || (s.startsWith("[") && s.endsWith("]")))
			s = "Workflow-Steuerung";
		return s;
	}

	private String getDetailsLabel() {
		String s = Msg.getMsg(Env.getCtx(), "Details", true);
		if (Util.isEmpty(s, true) || (s.startsWith("[") && s.endsWith("]")))
			s = Util.cleanAmp(Msg.translate(Env.getCtx(), "Details"));
		if (Util.isEmpty(s, true) || (s.startsWith("[") && s.endsWith("]")))
			s = "Details";
		return s;
	}

	/**
	 * Toggle the workflow control between slim (answer/forward only) and
	 * expanded (with node info, description, help, history).
	 */
	private void toggleDetails() {
		detailsExpanded = !detailsExpanded;
		applyDetailsVisibility();
	}

	private void applyDetailsVisibility() {
		boolean v = detailsExpanded;
		if (rowNodeInfo != null) rowNodeInfo.setVisible(v);
		if (rowDescInfo != null) rowDescInfo.setVisible(v);
		if (rowHelpInfo != null) rowHelpInfo.setVisible(v);
		if (rowHistoryInfo != null) rowHistoryInfo.setVisible(v);
		if (bDetails != null) bDetails.setLabel((v ? "\u25B4 " : "\u25BE ") + getDetailsLabel());
		if (controlSouth != null) ZKUpdateUtil.setHeight(controlSouth, v ? "50%" : "110px");
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		Component comp = event.getTarget();
        String eventName = event.getName();

        if(eventName.equals(Events.ON_CLICK))
        {
    		if (comp == bZoom)
    			cmd_zoom();
    		else if (comp == bRefresh)
    		{
    			Clients.showBusy(Msg.getMsg(Env.getCtx(), "Processing"));
    			refreshAsync();
    		}
    		else if (comp == bOK)
    		{
    			Clients.showBusy(Msg.getMsg(Env.getCtx(), "Processing"));
    			// directly handle here (and via onOK echo for compatibility)
    			onOK();
    		}
    		else if (comp == fAnswerButton)
    			cmd_button();
    		else if (comp == bDetails)
    			toggleDetails();
    		else if (comp.getAttribute("activityIndex") != null)
    		{
    			Object attr = comp.getAttribute("activityIndex");
    			if (attr instanceof Integer) {
    				int idx = (Integer) attr;
    				if (idx != m_index && idx >= 0 && m_activities != null && idx < m_activities.length) {
    					m_index = idx;
    					display(m_index);
    				}
    			}
    		}
        }
        else if (Events.ON_CHANGING.equals(eventName) && comp == fSearch)
        {
        	String txt = ((InputEvent) event).getValue();
        	filterCards(txt);
        }
        else if (Events.ON_CHANGE.equals(eventName) && comp == fSearch)
        {
        	filterCards(fSearch.getValue());
        }
        else if (Events.ON_SELECT.equals(eventName) && comp == fGrouping)
        {
        	groupingMode = getGroupingMode();
        	rebuildCards();
        	filterCards(fSearch != null ? fSearch.getValue() : null);
        }
        else if ("onOK".equals(eventName))
        {
        	onOK();
        }
        else if ("onRefreshCards".equals(eventName))
        {
        	loadActivities();
        	Clients.clearBusy();
        }
        else if ("onInitialDisplay".equals(eventName))
        {
        	// delivered after the form is attached -> embedded preview can be created now
        	if (m_activities != null && m_activities.length > 0) {
        		if (m_index < 0 || m_index >= m_activities.length)
        			m_index = 0;
        		display(m_index);
        	} else {
        		display(-1);
        	}
        }
        else
        {
    		super.onEvent(event);
        }
	}

	private void refreshAsync() {
		ExecutionsScheduleHelper.scheduleRefresh(this);
	}

	private static class ExecutionsScheduleHelper {
		static void scheduleRefresh(WWFActivity form) {
			org.zkoss.zk.ui.Desktop desktop = form.getDesktop();
			if (desktop != null) {
				org.zkoss.zk.ui.Executions.schedule(desktop, e -> {
					form.loadActivities();
					Clients.clearBusy();
				}, new Event("onRefreshCards"));
			} else {
				form.loadActivities();
				Clients.clearBusy();
			}
		}
	}

	/**
	 * Get active activities count
	 * @return pending activities count
	 */
	public int getActivitiesCount()
	{
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		int count = new Query(Env.getCtx(), MWFActivity.Table_Name, MWFActivity.getWhereUserPendingActivities(), null)
				.setApplyAccessFilter(true, false)
				.setParameters(AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, AD_Client_ID)
				.count();
		return count;
	}

	/**
	 * 	Load Activities
	 * 	@return number of activities loaded
	 */
	public int loadActivities()
	{
		long start = System.currentTimeMillis();

		int MAX_ACTIVITIES_IN_LIST = MSysConfig.getIntValue(MSysConfig.MAX_ACTIVITIES_IN_LIST, 200, Env.getAD_Client_ID(Env.getCtx()));

		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		Iterator<MWFActivity> it = new Query(Env.getCtx(), MWFActivity.Table_Name, MWFActivity.getWhereUserPendingActivities(), null)
				.setApplyAccessFilter(true, false)
				.setParameters(AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, AD_Client_ID)
				.setOrderBy("AD_WF_Activity.Priority DESC, AD_WF_Activity.Created")
				.iterate();

		List<MWFActivity> list = new ArrayList<MWFActivity>();
		while (it.hasNext()) {
			MWFActivity activity = it.next();
			list.add (activity);
			if (list.size() > MAX_ACTIVITIES_IN_LIST && MAX_ACTIVITIES_IN_LIST > 0)
			{
				log.warning("More than " + MAX_ACTIVITIES_IN_LIST + " Activities - ignored");
				break;
			}
		}
		m_activities = new MWFActivity[list.size ()];
		list.toArray (m_activities);
		//
		if (log.isLoggable(Level.FINE)) log.fine("#" + m_activities.length
			+ "(" + (System.currentTimeMillis()-start) + "ms)");

		String filter = fSearch != null ? fSearch.getValue() : null;
		rebuildCards();
		if (!Util.isEmpty(filter, true))
			filterCards(filter);
		updateCountLabel();

		if (m_activities.length == 0) {
			m_index = -1;
			display(-1);
		} else {
			if (m_index < 0 || m_index >= m_activities.length)
				m_index = 0;
			display(m_index);
		}

		return m_activities.length;
	}	//	loadActivities

	/**
	 * Rebuild the left card list from m_activities (Outlook style).
	 * Depending on the grouping dropdown the rows are plain sorted by
	 * priority or grouped by workflow/node with priority sorting inside
	 * each group.
	 */
	private void rebuildCards() {
		if (cardList == null)
			return;
		cardList.getChildren().clear();
		cardDivs.clear();
		if (m_activities == null)
			return;
		String mode = getGroupingMode();
		groupingMode = mode;
		if (GROUP_BY_WORKFLOW.equals(mode) || GROUP_BY_NODE.equals(mode)) {
			boolean byWorkflow = GROUP_BY_WORKFLOW.equals(mode);
			Map<Integer, List<Integer>> groups = new LinkedHashMap<Integer, List<Integer>>();
			Map<Integer, String> groupNames = new HashMap<Integer, String>();
			for (int i = 0; i < m_activities.length; i++) {
				int key;
				String name;
				if (byWorkflow) {
					key = getWorkflowId(m_activities[i]);
					name = getWorkflowName(key);
				} else {
					key = m_activities[i].getAD_WF_Node_ID();
					try {
						name = m_activities[i].getNodeName();
					} catch (Exception e) {
						name = null;
					}
					if (Util.isEmpty(name, true))
						name = "Knoten " + key;
				}
				groupNames.put(key, name);
				List<Integer> idx = groups.get(key);
				if (idx == null) {
					idx = new ArrayList<Integer>();
					groups.put(key, idx);
				}
				idx.add(i);
			}
			List<Integer> keys = new ArrayList<Integer>(groups.keySet());
			final Map<Integer, List<Integer>> fGroups = groups;
			final Map<Integer, String> fNames = groupNames;
			keys.sort((a, b) -> {
				int pa = maxPriority(fGroups.get(a));
				int pb = maxPriority(fGroups.get(b));
				int c = Integer.compare(pb, pa);
				if (c != 0)
					return c;
				String na = fNames.get(a);
				String nb = fNames.get(b);
				if (na == null) return nb == null ? 0 : 1;
				if (nb == null) return -1;
				return na.compareToIgnoreCase(nb);
			});
			for (int key : keys) {
				List<Integer> idx = groups.get(key);
				idx.sort(this::compareByPriority);
				cardList.appendChild(createGroupHeader(groupNames.get(key), idx.size()));
				for (int i : idx) {
					Div card = createCard(m_activities[i], i);
					cardList.appendChild(card);
					cardDivs.add(card);
				}
			}
		} else {
			List<Integer> order = new ArrayList<Integer>();
			for (int i = 0; i < m_activities.length; i++)
				order.add(i);
			order.sort(this::compareByPriority);
			for (int i : order) {
				Div card = createCard(m_activities[i], i);
				cardList.appendChild(card);
				cardDivs.add(card);
			}
		}
		updateCardSelection();
	}

	/**
	 * Compare two activity indices by priority (descending), then created (ascending).
	 */
	private int compareByPriority(int ia, int ib) {
		MWFActivity a = m_activities[ia];
		MWFActivity b = m_activities[ib];
		int c = Integer.compare(b.getPriority(), a.getPriority());
		if (c != 0)
			return c;
		Timestamp ca = null;
		Timestamp cb = null;
		try { ca = a.getCreated(); } catch (Exception e) { ca = null; }
		try { cb = b.getCreated(); } catch (Exception e) { cb = null; }
		if (ca == null) return cb == null ? Integer.compare(ia, ib) : 1;
		if (cb == null) return -1;
		c = ca.compareTo(cb);
		return c != 0 ? c : Integer.compare(ia, ib);
	}

	private int maxPriority(List<Integer> idx) {
		int max = Integer.MIN_VALUE;
		if (idx != null) {
			for (int i : idx) {
				try {
					max = Math.max(max, m_activities[i].getPriority());
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return max;
	}

	private int getWorkflowId(MWFActivity activity) {
		try {
			MWFProcess proc = new MWFProcess(Env.getCtx(), activity.getAD_WF_Process_ID(), null);
			return proc.getAD_Workflow_ID();
		} catch (Exception e) {
			log.log(Level.FINE, e.getMessage(), e);
			return 0;
		}
	}

	private String getWorkflowName(int workflowId) {
		if (workflowId <= 0)
			return "?";
		String name = workflowNameCache.get(workflowId);
		if (name == null) {
			try {
				MWorkflow wf = MWorkflow.get(Env.getCtx(), workflowId);
				name = wf != null ? wf.getName() : null;
			} catch (Exception e) {
				log.log(Level.FINE, e.getMessage(), e);
				name = null;
			}
			if (Util.isEmpty(name, true))
				name = "Workflow " + workflowId;
			workflowNameCache.put(workflowId, name);
		}
		return name;
	}

	private Div createGroupHeader(String title, int count) {
		Div header = new Div();
		header.setSclass("wf-group-header");
		header.setAttribute("groupHeader", Boolean.TRUE);
		header.appendChild(new Label((title != null ? title : "") + " (" + count + ")"));
		return header;
	}

	/**
	 * Create a single activity row - flat 3-line style like the KI-Copilot
	 * inbox (no frame/shadow, only a separator line):
	 * line 1 = node name + date, line 2 = summary, line 3 = excerpt + state.
	 * @param activity
	 * @param index index in m_activities
	 * @return card div
	 */
	private Div createCard(MWFActivity activity, int index) {
		Div card = new Div();
		card.setAttribute("activityIndex", index);
		card.addEventListener(Events.ON_CLICK, this);
		card.setTooltiptext(Msg.getMsg(Env.getCtx(), "Zoom"));

		String nodeName = "";
		String summary = "";
		String description = "";
		String textMsg = "";
		String stateText = "";
		Timestamp created = null;
		int priority = 0;
		try {
			nodeName = activity.getNodeName();
			summary = activity.getSummary();
			description = activity.getNodeDescription();
			textMsg = activity.getTextMsg();
			stateText = activity.getWFStateText();
			created = activity.getCreated();
			priority = activity.getPriority();
		} catch (Exception e) {
			log.log(Level.FINE, e.getMessage(), e);
		}
		if (nodeName == null) nodeName = "";
		if (summary == null) summary = "";
		if (description == null) description = "";
		if (stateText == null) stateText = "";

		// high priority rows render bold (like unread mails in the Copilot inbox)
		boolean important = priority >= 70;
		card.setAttribute("cardImportant", important);
		card.setSclass("wf-activity-card" + (important ? " wf-unread" : ""));

		// line 1: node name + date
		Hlayout top = new Hlayout();
		top.setSclass("wf-card-top");
		top.setHflex("1");
		top.setValign("middle");
		top.setAttribute("activityIndex", index);
		top.addEventListener(Events.ON_CLICK, this);

		Label title = new Label(nodeName);
		title.setSclass("wf-card-title");
		ZKUpdateUtil.setHflex(title, "1");
		title.setStyle(ellipsis());
		title.setAttribute("activityIndex", index);
		top.appendChild(title);

		Label date = new Label(formatTimestamp(created));
		date.setSclass("wf-card-date");
		date.setStyle("color:#667085;font-size:11px;white-space:nowrap");
		date.setAttribute("activityIndex", index);
		top.appendChild(date);
		card.appendChild(top);

		// line 2: summary
		Label subject = new Label(truncate(summary, 120));
		subject.setSclass("wf-card-subject");
		subject.setStyle(ellipsis());
		subject.setAttribute("activityIndex", index);
		card.appendChild(subject);

		// line 3: excerpt + state pill
		Hlayout bottom = new Hlayout();
		bottom.setSclass("wf-card-bottom");
		bottom.setHflex("1");
		bottom.setValign("middle");
		bottom.setAttribute("activityIndex", index);
		bottom.addEventListener(Events.ON_CLICK, this);

		Label excerptLabel = new Label(excerpt(description, textMsg));
		excerptLabel.setSclass("wf-card-excerpt");
		ZKUpdateUtil.setHflex(excerptLabel, "1");
		excerptLabel.setStyle(ellipsis() + ";color:#667085;font-size:12px");
		excerptLabel.setAttribute("activityIndex", index);
		bottom.appendChild(excerptLabel);

		Label pill = new Label(stateText);
		pill.setSclass("wf-card-pill");
		pill.setStyle("border-radius:10px;padding:1px 7px;background:#e8eef5;color:#344054;font-size:10px;white-space:nowrap");
		pill.setAttribute("activityIndex", index);
		bottom.appendChild(pill);
		card.appendChild(bottom);

		// make inner labels clickable (select row)
		for (Component c : new Component[] {title, date, subject, excerptLabel, pill}) {
			c.addEventListener(Events.ON_CLICK, this);
			c.setAttribute("activityIndex", index);
		}

		return card;
	}

	private static String ellipsis() {
		return "display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;min-width:0";
	}

	private String excerpt(String description, String textMsg) {
		String v = description != null ? description : "";
		if (Util.isEmpty(v, true) && textMsg != null)
			v = textMsg;
		v = v.replaceAll("\\s+", " ").trim();
		if (v.length() > 140)
			v = v.substring(0, 137) + "...";
		return v;
	}

	private String formatTimestamp(Timestamp ts) {
		if (ts == null) return "";
		try {
			return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(ts);
		} catch (Exception e) {
			return ts.toString();
		}
	}

	private String truncate(String s, int max) {
		if (s == null) return "";
		s = s.trim().replaceAll("\\s+", " ");
		if (s.length() <= max) return s;
		return s.substring(0, Math.max(0, max - 1)) + "…";
	}

	private void filterCards(String text) {
		if (m_activities == null || cardDivs.isEmpty())
			return;
		String f = text != null ? text.trim().toLowerCase() : "";
		for (Div card : cardDivs) {
			Object attr = card.getAttribute("activityIndex");
			if (!(attr instanceof Integer))
				continue;
			int idx = (Integer) attr;
			if (idx < 0 || idx >= m_activities.length)
				continue;
			if (Util.isEmpty(f, true)) {
				card.setVisible(true);
				continue;
			}
			MWFActivity activity = m_activities[idx];
			String haystack = "";
			try {
				haystack = (activity.getNodeName() + " " + activity.getSummary() + " "
						+ activity.getNodeDescription() + " " + activity.getTextMsg()).toLowerCase();
			} catch (Exception e) {
				haystack = "";
			}
			card.setVisible(haystack.contains(f));
		}
		updateGroupHeaderVisibility();
	}

	/**
	 * Hide group headers without any visible card (search filter active).
	 */
	private void updateGroupHeaderVisibility() {
		if (cardList == null)
			return;
		Div lastHeader = null;
		boolean headerHasVisibleCard = false;
		for (Component child : cardList.getChildren()) {
			if (Boolean.TRUE.equals(child.getAttribute("groupHeader"))) {
				if (lastHeader != null)
					lastHeader.setVisible(headerHasVisibleCard);
				lastHeader = (Div) child;
				headerHasVisibleCard = false;
			} else if (child.isVisible()) {
				headerHasVisibleCard = true;
			}
		}
		if (lastHeader != null)
			lastHeader.setVisible(headerHasVisibleCard);
	}

	private void updateCardSelection() {
		for (Div card : cardDivs) {
			Object attr = card.getAttribute("activityIndex");
			if (!(attr instanceof Integer))
				continue;
			int idx = (Integer) attr;
			String base = "wf-activity-card";
			if (Boolean.TRUE.equals(card.getAttribute("cardImportant")))
				base += " wf-unread";
			if (idx == m_index)
				base += " wf-card-selected";
			card.setSclass(base);
		}
	}

	private void updateCountLabel() {
		if (lCount == null)
			return;
		int n = m_activities != null ? m_activities.length : 0;
		String label = Msg.getMsg(Env.getCtx(), "WFActivities");
		if (Util.isEmpty(label, true) || (label.startsWith("[") && label.endsWith("]")))
			label = "Aktivitäten";
		lCount.setValue(label + ": " + n);
	}

	/**
	 * 	Reset form and return activity at selIndex
	 *	@param selIndex select index
	 *	@return selected activity
	 */
	private MWFActivity resetDisplay(int selIndex)
	{
		if (fAnswerText != null) fAnswerText.setVisible(false);
		if (fAnswerList != null) fAnswerList.setVisible(false);
		if (fAnswerButton != null) fAnswerButton.setVisible(false);
		if (fAnswerButton != null) {
			if (ThemeManager.isUseFontIconForImage())
				fAnswerButton.setIconSclass(Icon.getIconSclass(Icon.WINDOW));
			else
				fAnswerButton.setImage(ThemeManager.getThemeResource("images/mWindow.png"));
		}
		if (fTextMsg != null) {
			fTextMsg.setReadonly(!(selIndex >= 0));
			fTextMsg.setValue("");
		}
		if (bZoom != null) bZoom.setEnabled(selIndex >= 0);
		if (bOK != null) bOK.setEnabled(selIndex >= 0);
		if (fForward != null) {
			fForward.setValue(null);
			fForward.setReadWrite(selIndex >= 0);
		}
		//
		if (statusBar != null)
			statusBar.setStatusDB(String.valueOf(selIndex+1) + "/" + (m_activities != null ? m_activities.length : 0));
		m_activity = null;
		m_column = null;
		if (m_activities != null && m_activities.length > 0)
		{
			if (selIndex >= 0 && selIndex < m_activities.length)
				m_activity = m_activities[selIndex];
		}
		//	Nothing to show
		if (m_activity == null)
		{
			if (fNode != null) fNode.setText ("");
			if (fDescription != null) fDescription.setText ("");
			if (fHelp != null) fHelp.setText ("");
			if (fHistory != null) fHistory.setContent(HISTORY_DIV_START_TAG + "&nbsp;</div>");
			if (statusBar != null) {
				statusBar.setStatusDB("0/" + (m_activities != null ? m_activities.length : 0));
				statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "WFNoActivities"));
			}
		}
		return m_activity;
	}	//	resetDisplay

	/**
	 * Display activity at index
	 * @param index
	 */
	public void display (int index)
	{
		if (log.isLoggable(Level.FINE)) log.fine("Index=" + index);
		//
		m_index = index;
		m_activity = resetDisplay(index);
		updateCardSelection();
		//	Nothing to show
		if (m_activity == null)
		{
			showDocPlaceholder(Msg.getMsg(Env.getCtx(), "WFNoActivities"));
			return;
		}
		//	Display Activity
		if (fNode != null) fNode.setText (m_activity.getNodeName());
		if (fDescription != null) fDescription.setValue (m_activity.getNodeDescription());
		if (fHelp != null) fHelp.setValue (m_activity.getNodeHelp());
		//
		if (fHistory != null) fHistory.setContent (HISTORY_DIV_START_TAG+m_activity.getHistoryHTML()+"</div>");

		//	User Actions
		MWFNode node = m_activity.getNode();
		if (MWFNode.ACTION_UserChoice.equals(node.getAction()))
		{
			if (m_column == null)
				m_column = node.getColumn();
			if (m_column != null && m_column.get_ID() != 0 && fAnswerList != null)
			{
				fAnswerList.removeAllItems();
				int dt = m_column.getAD_Reference_ID();
				if (dt == DisplayType.YesNo)
				{
					ValueNamePair[] values = MRefList.getList(Env.getCtx(), 319, false);		//	_YesNo
					for(int i = 0; i < values.length; i++)
					{
						fAnswerList.appendItem(values[i].getName(), values[i].getValue());
					}
					fAnswerList.setVisible(true);
				}
				else if (DisplayType.isList(dt))
				{
					ValueNamePair[] values = MRefList.getList(Env.getCtx(), m_column.getAD_Reference_Value_ID(), false);
					for(int i = 0; i < values.length; i++)
					{
						fAnswerList.appendItem(values[i].getName(), values[i].getValue());
					}
					fAnswerList.setVisible(true);
				}
				else	//	other display types come here
				{
					if (fAnswerText != null) {
						fAnswerText.setText ("");
						fAnswerText.setVisible(true);
					}
				}
			}
		}
		//	--
		else if (MWFNode.ACTION_UserWindow.equals(node.getAction())
			|| MWFNode.ACTION_UserForm.equals(node.getAction())
			|| MWFNode.ACTION_UserInfo.equals(node.getAction()))
		{
			if (fAnswerButton != null) {
				fAnswerButton.setLabel(node.getName());
				fAnswerButton.setTooltiptext(node.getDescription());
				fAnswerButton.setVisible(true);
			}
		}
		else
			log.log(Level.SEVERE, "Unknown Node Action: " + node.getAction());

		if (statusBar != null && m_activities != null) {
			statusBar.setStatusDB((m_index+1) + "/" + m_activities.length);
			statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "WFActivities"));
		}

		refreshEmbeddedWindow();
	}	//	display

	/**
	 * Refresh the embedded window on the right side.
	 * Shows the window referenced by the node (UserWindow) or - as fallback -
	 * the document window of the underlying record.
	 */
	private void refreshEmbeddedWindow() {
		disposeEmbeddedWindow();
		if (m_activity == null) {
			showDocPlaceholder(Msg.getMsg(Env.getCtx(), "WFNoActivities"));
			return;
		}
		if (docContainer == null || docContainer.getPage() == null || getPage() == null) {
			// form not attached yet (initForm runs before setParent) -> defer embedding
			// to onInitialDisplay echo; show lightweight placeholder meanwhile
			embeddedWindowId = 0;
			embeddedQuery = null;
			showDeferredPlaceholder();
			return;
		}
		int windowId = 0;
		MQuery query = null;
		try {
			MWFNode node = m_activity.getNode();
			int tableId = m_activity.getAD_Table_ID();
			int recordId = m_activity.getRecord_ID();
			String tableName = null;
			if (tableId > 0) {
				try {
					tableName = MTable.getTableName(Env.getCtx(), tableId);
				} catch (Exception e) {
					tableName = null;
				}
			}
			if (MWFNode.ACTION_UserWindow.equals(node.getAction()) && node.getAD_Window_ID() > 0) {
				windowId = node.getAD_Window_ID();
				if (tableName != null && recordId > 0)
					query = MQuery.getEqualQuery(tableName + "_ID", recordId);
			} else if (tableId > 0 && recordId > 0 && tableName != null) {
				// preview of the underlying document (approval / choice nodes)
				query = MQuery.getEqualQuery(tableName + "_ID", recordId);
				try {
					windowId = Env.getZoomWindowID(query);
				} catch (Exception e) {
					windowId = 0;
				}
			}
			if (MWFNode.ACTION_UserForm.equals(node.getAction()) || MWFNode.ACTION_UserInfo.equals(node.getAction())) {
				// forms/info windows cannot be embedded -> placeholder with button
				windowId = 0;
				query = null;
			}
		} catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
			windowId = 0;
			query = null;
		}

		if (windowId <= 0 || query == null) {
			showDocPlaceholderForActivity();
			// keep query for external open via zoom fallback
			embeddedWindowId = 0;
			embeddedQuery = null;
			return;
		}

		try {
			ADWindow adWindow = new ADWindow(Env.getCtx(), windowId, query);
			Component c = adWindow.createPart(docContainer);
			if (c == null) {
				try { SessionManager.getAppDesktop().unregisterWindow(adWindow.getADWindowContent().getWindowNo()); } catch (Exception ignore) {}
				showDocPlaceholderForActivity();
				embeddedWindowId = windowId;
				embeddedQuery = query;
				return;
			}
			embeddedADWindow = adWindow;
			embeddedWindowId = windowId;
			embeddedQuery = query;
		} catch (Exception e) {
			log.log(Level.WARNING, "Cannot embed window " + windowId + ": " + e.getMessage(), e);
			disposeEmbeddedWindow();
			showDocPlaceholderForActivity();
			embeddedWindowId = windowId;
			embeddedQuery = query;
		}
	}

	private void showDocPlaceholderForActivity() {
		String msg;
		try {
			MWFNode node = m_activity != null ? m_activity.getNode() : null;
			if (node != null && MWFNode.ACTION_UserForm.equals(node.getAction()))
				msg = node.getName() + " (Form)";
			else if (node != null && MWFNode.ACTION_UserInfo.equals(node.getAction()))
				msg = node.getName() + " (Info)";
			else if (m_activity != null && m_activity.getSummary() != null)
				msg = m_activity.getSummary();
			else
				msg = Msg.getMsg(Env.getCtx(), "NoPreview");
		} catch (Exception e) {
			msg = Msg.getMsg(Env.getCtx(), "NoPreview");
		}
		if (Util.isEmpty(msg, true) || (msg.startsWith("[") && msg.endsWith("]")))
			msg = m_activity != null ? String.valueOf(m_activity.getSummary()) : "";
		showDocPlaceholder(msg);
	}

	private void showDocPlaceholder(String msg) {
		disposeEmbeddedWindow();
		if (docContainer == null)
			return;
		docContainer.getChildren().clear();
		Div ph = new Div();
		ph.setSclass("wf-doc-placeholder");
		ph.setStyle("padding: 24px; color: #666; text-align: center;");
		Label l = new Label(msg != null ? msg : "");
		ph.appendChild(l);
		// button to open externally
		if (m_activity != null) {
			Div btnWrap = new Div();
			btnWrap.setStyle("margin-top: 12px;");
			Button b = new Button(Msg.getMsg(Env.getCtx(), "Zoom"));
			b.setTooltiptext(Msg.getMsg(Env.getCtx(), "Zoom"));
			if (ThemeManager.isUseFontIconForImage())
				b.setIconSclass(Icon.getIconSclass(Icon.ZOOM));
			else
				b.setImage(ThemeManager.getThemeResource("images/Zoom16.png"));
			b.addEventListener(Events.ON_CLICK, e -> cmd_openExternal());
			btnWrap.appendChild(b);
			ph.appendChild(btnWrap);
		}
		docContainer.appendChild(ph);
	}

	/**
	 * Lightweight placeholder while the form is not attached to a page yet.
	 * The real embedded preview is created on onInitialDisplay.
	 */
	private void showDeferredPlaceholder() {
		if (docContainer == null)
			return;
		docContainer.getChildren().clear();
		String msg = null;
		try {
			msg = m_activity != null ? m_activity.getSummary() : null;
		} catch (Exception e) {
			msg = null;
		}
		if (Util.isEmpty(msg, true))
			msg = Msg.getMsg(Env.getCtx(), "Processing");
		Div ph = new Div();
		ph.setSclass("wf-doc-placeholder");
		ph.setStyle("padding: 24px; color: #666; text-align: center;");
		ph.appendChild(new Label(msg != null ? msg : ""));
		docContainer.appendChild(ph);
	}

	private void disposeEmbeddedWindow() {
		if (embeddedADWindow != null) {
			try {
				Component comp = embeddedADWindow.getComponent();
				if (comp != null)
					comp.detach();
			} catch (Exception e) {
				log.log(Level.FINE, e.getMessage(), e);
			}
			try {
				int windowNo = embeddedADWindow.getADWindowContent().getWindowNo();
				SessionManager.getAppDesktop().unregisterWindow(windowNo);
			} catch (Exception e) {
				log.log(Level.FINE, e.getMessage(), e);
			}
			embeddedADWindow = null;
		}
		if (docContainer != null)
			docContainer.getChildren().clear();
	}

	/**
	 * Open the currently previewed document/window in a new tab.
	 */
	private void cmd_openExternal()
	{
		if (embeddedWindowId > 0 && embeddedQuery != null) {
			AEnv.zoom(embeddedWindowId, embeddedQuery);
			return;
		}
		if (m_activity == null)
			return;
		// fallback: same behaviour as answer button / zoom
		MWFNode node = m_activity.getNode();
		if (MWFNode.ACTION_UserWindow.equals(node.getAction()))
			cmd_button();
		else
			cmd_zoom();
	}

	/**
	 * Zoom to workflow activity window
	 */
	private void cmd_zoom()
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Activity=" + m_activity);
		if (m_activity == null)
			return;
		AEnv.zoom(m_activity.getAD_Table_ID(), m_activity.getRecord_ID());
	}	//	cmd_zoom

	/**
	 * 	Action Button
	 */
	private void cmd_button()
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Activity=" + m_activity);
		if (m_activity == null)
			return;
		//
		MWFNode node = m_activity.getNode();
		if (MWFNode.ACTION_UserWindow.equals(node.getAction()))
		{
			int AD_Window_ID = node.getAD_Window_ID();		// Explicit Window
			String ColumnName = null;
			int Record_ID = m_activity.getRecord_ID();
			try {
				PO po = m_activity.getPO();
				if (po != null)
					ColumnName = po.get_TableName() + "_ID";
			} catch (Exception e) {
				log.log(Level.FINE, e.getMessage(), e);
			}
			if (ColumnName == null && m_activity.getAD_Table_ID() > 0) {
				try {
					ColumnName = MTable.getTableName(Env.getCtx(), m_activity.getAD_Table_ID()) + "_ID";
				} catch (Exception e) {
					log.log(Level.FINE, e.getMessage(), e);
				}
			}
			if (ColumnName == null)
				return;
			MQuery query = MQuery.getEqualQuery(ColumnName, Record_ID);
			boolean IsSOTrx = m_activity.isSOTrx();
			//
			if (log.isLoggable(Level.INFO))
				log.info("Zoom to AD_Window_ID=" + AD_Window_ID
					+ " - " + query + " (IsSOTrx=" + IsSOTrx + ")");

			AEnv.zoom(AD_Window_ID, query);
		}
		else if (MWFNode.ACTION_UserForm.equals(node.getAction()))
		{
			int AD_Form_ID = node.getAD_Form_ID();

			ADForm form = ADForm.openForm(AD_Form_ID);
			form.setAttribute(Window.MODE_KEY, form.getWindowMode());
			AEnv.showWindow(form);
		}else if (MWFNode.ACTION_UserInfo.equals(node.getAction())){
			SessionManager.getAppDesktop().openInfo(node.getAD_InfoWindow_ID());
		}
		else
			log.log(Level.SEVERE, "No User Action:" + node.getAction());
	}	//	cmd_button


	/**
	 * 	Save
	 */
	public void onOK()
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Activity=" + m_activity);
		if (m_activity == null)
		{
			Clients.clearBusy();
			return;
		}
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		String textMsg = fTextMsg.getValue();
		//
		MWFNode node = m_activity.getNode();

		Object forward = fForward.getValue();

		// ensure activity is ran within a transaction - [ 1953628 ]
		Trx trx = null;
		try {
			trx = Trx.get(Trx.createTrxName("FWFA"), true);
			trx.setDisplayName(getClass().getName()+"_onOK");
			m_activity.set_TrxName(trx.getTrxName());

			if (forward != null)
			{
				if (log.isLoggable(Level.CONFIG)) log.config("Forward to " + forward);
				int fw = ((Integer)forward).intValue();
				if (fw == AD_User_ID || fw == 0)
				{
					log.log(Level.SEVERE, "Forward User=" + fw);
					trx.rollback();
					trx.close();
					return;
				}
				if (!m_activity.forwardTo(fw, textMsg))
				{
					Dialog.error(m_WindowNo, "CannotForward");
					trx.rollback();
					trx.close();
					return;
				}
			}
			//	User Choice - Answer
			else if (MWFNode.ACTION_UserChoice.equals(node.getAction()))
			{
				if (m_column == null)
					m_column = node.getColumn();
				//	Do we have an answer?
				int dt = m_column.getAD_Reference_ID();
				String value = fAnswerText.getText();
				if (dt == DisplayType.YesNo || DisplayType.isList(dt))
				{
					ListItem li = fAnswerList.getSelectedItem();
					if(li != null) value = li.getValue().toString();
				}
				if (value == null || value.length() == 0)
				{
					Dialog.error(m_WindowNo, "FillMandatory", Msg.getMsg(Env.getCtx(), "Answer"));
					trx.rollback();
					trx.close();
					return;
				}
				//
				if (log.isLoggable(Level.CONFIG)) log.config("Answer=" + value + " - " + textMsg);
				try
				{
					m_activity.setUserChoice(AD_User_ID, value, dt, textMsg);
					MWFProcess wfpr = new MWFProcess(m_activity.getCtx(), m_activity.getAD_WF_Process_ID(), m_activity.get_TrxName());
					wfpr.checkCloseActivities(m_activity.get_TrxName());
					
					if (!Util.isEmpty(m_activity.getProcessMsg(), true))
						Dialog.error(m_WindowNo, m_activity.getProcessMsg());
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, node.getName(), e);
					Dialog.error(m_WindowNo, "Error", e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getMessage());
					trx.rollback();
					trx.close();
					return;
				}
			}
			//	User Action
			else
			{
				if (log.isLoggable(Level.CONFIG)) log.config("Action=" + node.getAction() + " - " + textMsg);
				try
				{
					// ensure activity is ran within a transaction
					m_activity.setUserConfirmation(AD_User_ID, textMsg);
					MWFProcess wfpr = new MWFProcess(m_activity.getCtx(), m_activity.getAD_WF_Process_ID(), m_activity.get_TrxName());
					wfpr.checkCloseActivities(m_activity.get_TrxName());
					
					if (!Util.isEmpty(m_activity.getProcessMsg(), true))
						Dialog.error(m_WindowNo, m_activity.getProcessMsg());
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, node.getName(), e);
					Dialog.error(m_WindowNo, "Error", e.toString());
					trx.rollback();
					trx.close();
					return;
				}

			}

			trx.commit();
		}
		finally
		{
			Clients.clearBusy();
			if (trx != null)
				trx.close();
		}

		//	Next
		fSearch.setValue("");
		loadActivities();
	}	//	onOK

	@Override
	public void onPageDetached(Page page) {
		super.onPageDetached(page);
		disposeEmbeddedWindow();
	}
}
