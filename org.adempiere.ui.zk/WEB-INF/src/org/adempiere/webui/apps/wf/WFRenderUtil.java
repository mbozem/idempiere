/******************************************************************************
 * This file is part of iDempiere ERP Open Source                              *
 * http://www.idempiere.org                                                    *
 *                                                                             *
 * Copyright (C) Contributors                                                  *
 *                                                                             *
 * This program is free software; you can redistribute it and/or modify it     *
 * under the terms version 2 of the GNU General Public License as published    *
 * by the Free Software Foundation. This program is distributed in the hope    *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied  *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.            *
 * See the GNU General Public License for more details.                        *
 * You should have received a copy of the GNU General Public License along     *
 * with this program; if not, write to the Free Software Foundation, Inc.,     *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                      *
 *****************************************************************************/
package org.adempiere.webui.apps.wf;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Utility for offscreen rendering of the workflow graph.
 * The graph is painted at {@link #RENDER_SCALE} times its logical size
 * (supersampling) and scaled back to logical size in the browser, so it
 * stays sharp on high resolution (HiDPI/Retina) displays.
 */
public final class WFRenderUtil {

	/**
	 * Scale factor for the offscreen rendering of the workflow graph
	 */
	public static final double RENDER_SCALE = 2.0;

	private WFRenderUtil() {
	}

	/**
	 * Set high quality rendering hints (anti-aliasing for shapes and text)
	 * @param graphics graphics to configure
	 */
	public static void applyRenderingHints(Graphics2D graphics) {
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	}
}	//	WFRenderUtil
