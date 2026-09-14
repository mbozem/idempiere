<%@ page contentType="text/css;charset=UTF-8" %>
<%@ taglib uri="http://www.zkoss.org/dsp/web/core" prefix="c" %>
<%@ taglib uri="http://www.idempiere.org/dsp/web/util" prefix="u" %>

html,body {
	margin: 0;
	padding: 0;
	height: 100%;
	width: 100%;
	background-color: #D4E3F4;
	color: #333;
	font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
	overflow: hidden;
}

.z-html p{
	margin:0px;
}

[class*="z-"]:not([class*="z-icon-"]):not([class*="z-group-icon-"]) {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
}
@media screen and (min-device-width: 2500px) {
	[class*="z-"]:not([class*="z-icon-"]):not([class*="z-group-icon-"]) {
		font-size: 15px;
	}
}
@media screen and (max-device-width: 2499px) {
	[class*="z-"]:not([class*="z-icon-"]):not([class*="z-group-icon-"]) {
		font-size: 14px;
	}
}
@media screen and (max-device-width: 1899px) {
	[class*="z-"]:not([class*="z-icon-"]):not([class*="z-group-icon-"]) {
		font-size: 13px;
	}
}
@media screen and (max-device-width: 1399px) {
	[class*="z-"]:not([class*="z-icon-"]):not([class*="z-group-icon-"]) {
		font-size: 12px;
	}
}

<%-- Mobile/Tablet --%>
.tablet-scrolling {
	-webkit-overflow-scrolling: touch;
}
<%-- default font size for mobile --%>
.mobile [class*="z-"]:not([class*="z-icon-"]):not([class*="z-group-icon-"]) {
    font-size: 16px;
}
<%-- the not=xyz is needed to get this selected over standard zk rule --%>
.mobile [class*="z-icon-"]:not([class*="xyz"]), .mobile [class*="z-group-icon-"] {
    font-size: 16px;
}

<%-- vbox fix for firefox and ie --%>
table.z-vbox > tbody > tr > td > table {
	width: 100%;	
}

<%-- workflow activity --%>
.workflow-activity-form {
}
.workflow-panel-table {
	border: 0px;
}
<%-- workflow activities outlook layout (flat 3-line rows like KI-Copilot, slim expandable control) --%>
.wf-outlook .wf-west-layout {
	height: 100%;
}
.wf-search-bar {
	margin-bottom: 4px;
}
.wf-count-bar {
	font-size: 12px;
	color: #666;
	margin: 2px 2px 6px 2px;
}
.wf-card-scroll {
	border: 0;
}
.wf-card-list {
	padding: 0;
}
.wf-activity-card {
	border: 0;
	border-bottom: 1px solid var(--input-border-color, #d9d9d9);
	border-radius: 0;
	background: transparent;
	padding: 10px 12px;
	margin: 0;
	cursor: pointer;
}
.wf-activity-card:hover {
	background: var(--button-hover-background-color, #f3f7fb);
}
.wf-activity-card.wf-card-selected {
	background: var(--selected-background-color, #dceeff);
}
.wf-activity-card.wf-unread .wf-card-title,
.wf-activity-card.wf-unread .wf-card-subject {
	font-weight: 700;
}
.wf-card-top {
	margin-bottom: 3px;
}
.wf-card-bottom {
	margin-top: 3px;
}
.wf-card-title {
	font-size: 13px;
}
.wf-card-subject {
	font-size: 13px;
	margin-top: 3px;
}
.wf-card-excerpt {
	margin-top: 0;
}
.wf-card-date {
	font-size: 11px;
	color: #667085;
	white-space: nowrap;
	margin-left: 6px;
}
.wf-card-pill {
	font-size: 10px;
	margin-left: 6px;
}
.wf-grouping {
	margin-bottom: 4px;
}
.wf-group-header {
	padding: 8px 12px 4px 12px;
	font-weight: bold;
	font-size: 12px;
	color: #344054;
	background: #f2f4f7;
	border-bottom: 1px solid var(--input-border-color, #d9d9d9);
}
.wf-doc-container {
	border: 0;
}
.wf-doc-placeholder {
	color: #666;
}
.wf-control-title {
	font-size: 13px;
}
.wf-control-header {
	margin-bottom: 2px;
}
.wf-details-toggle {
	white-space: nowrap;
}
.wf-action-row > .z-cell {
	padding-top: 2px !important;
	padding-bottom: 2px !important;
}
.wf-action-bar > * {
	margin-right: 4px;
}
.wf-action-bar > *:last-child {
	margin-right: 0;
}
@media screen and (max-width: 900px) {
	.wf-outlook .wf-west {
		width: 260px !important;
	}
}

<%-- payment form --%>
.payment-form-content {
}

<%-- decorate file drop area --%>
.attachment-drag-entered {
    border: 5px dashed #3fb900 !important;
}

<c:include page="fragment/login.css.dsp" />

<c:include page="fragment/desktop.css.dsp" />

<c:include page="fragment/application-menu.css.dsp" />

<c:include page="fragment/gadget.css.dsp" />

<c:include page="fragment/toolbar.css.dsp" />

<c:include page="fragment/button.css.dsp" />

<c:include page="fragment/adwindow.css.dsp" />
			
<c:include page="fragment/grid.css.dsp" />

<c:include page="fragment/input-element.css.dsp" />

<c:include page="fragment/tree.css.dsp" /> 

<c:include page="fragment/field-editor.css.dsp" />

<c:include page="fragment/group.css.dsp" />

<c:include page="fragment/tab.css.dsp" />

<c:include page="fragment/menu-tree.css.dsp" />

<c:include page="fragment/info-window.css.dsp" />

<c:include page="fragment/window.css.dsp" />

<c:include page="fragment/form.css.dsp" />

<c:include page="fragment/toolbar-popup.css.dsp" />	

<c:include page="fragment/setup-wizard.css.dsp" />

<c:include page="fragment/about.css.dsp" />

<c:include page="fragment/tab-editor.css.dsp" />

<c:include page="fragment/find-window.css.dsp" />

<c:include page="fragment/help-window.css.dsp" />

<c:include page="fragment/drill-window.css.dsp" />

<c:include page="fragment/borderlayout.css.dsp" />

<c:include page="fragment/parameter-process.css.dsp" />

<c:include page="fragment/window-size.css.dsp" />

<c:include page="fragment/font-icons.css.dsp" />

<c:if test="${u:isThemeHasCustomCSSFragment()}">
    <c:include page="fragment/custom.css.dsp" />
</c:if>