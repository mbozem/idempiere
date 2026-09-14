.form-label
{
	text-align: right;
}
.form-label-heading
{
	text-align: center;
}
td.form-label-above-input div.form-label {
    text-align: left;
}
td.form-label-above-input div.form-label > span.z-label {
    font-weight: 400;
    font-size: smaller !important;
}
td.form-label-above-input.z-cell {
    padding-top: 5px;
    padding-bottom: 5px;
}
td.form-label-above-input.login-label {
    text-align: left;
}
td.form-label-above-input.login-label .z-label,
.form-label-above-input .idempiere-label.z-label {
    font-weight: 400;
    font-size: smaller !important;
}
.form-label-above-input.z-window .login-box-body,
.form-label-above-input.adwindow-form,
.form-label-above-input.input-paramenter-layout {
    padding-left: 1em !important;
    padding-right: 1em !important;
}

.form-button {
	width: 99%;
}
.form-button img {
	width: 16px;
	height: 16px;
}

<%-- confirm panel --%>
.confirm-panel {
	width: auto;
	height: auto;
	position: relative;
	padding-left: 2px;
	padding-right: 2px;
}
.confirm-panel-right {
	float: right;
	padding: 1px;
}
.confirm-panel-left {
	float: left;
}
.confirm-panel-center{
	padding-left: 5px;
	float: left;
}

<%-- busy dialog --%>
.busy-dialog {
	cursor: wait;
	background-color: var(--zk-form-busy-dialog-background-color);
}
.busy-dialog-box {
	background-color: var(--zk-form-busy-dialog-box-background-color);
	padding: 9px; 
}
.busy-dialog-img {
	height: 16px; 
	width: 16px;
	background: var(--zk-form-busy-dialog-image-background-color) no-repeat center;
	background-image: url(../images/progress3.gif) !important;
	display: -moz-inline-box; 
	vertical-align: top; 
	display: inline-block;
}
.busy-dialog-label {
	color: var(--zk-form-busy-dialog-label-color);
}

<%-- status bar --%>
.status {
	width: 100%;
	height: 29px;
}
.status-db {
	white-space: nowrap;
	padding-top: 0;
	padding-bottom: 0;
	padding-left: 5px;
	padding-right: 5px;
	cursor: pointer;
	width: 100%;
	height: 100%;
	margin: 0;
	border-left: solid 1px var(--zk-form-status-border-color);
	display: inline-block;
}
.status-info {
	padding-right: 10px;
	border-left: solid 1px var(--zk-form-status-border-color);
}

.status-selected{
	padding-right: 5px;
	border-right: solid 1px var(--zk-form-status-border-color);
}

.status-border {
	border: solid 1px var(--zk-form-status-border-color);
}

<%-- report wizard --%>
.report-wizard-footer {
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
