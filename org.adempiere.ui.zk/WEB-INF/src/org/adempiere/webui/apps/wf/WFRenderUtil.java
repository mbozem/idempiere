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
 * The graph is painted at up to {@link #RENDER_SCALE} times its logical size
 * (supersampling) and scaled back to logical size in the browser. Large
 * layouts use a lower scale to keep their server-side raster bounded.
 */
public final class WFRenderUtil {
	/** Maximum memory used by one uncompressed workflow raster. */
	private static final long MAX_RASTER_BYTES = 64L * 1024L * 1024L;

	/** Bytes per pixel of {@link java.awt.image.BufferedImage#TYPE_INT_ARGB}. */
	private static final int ARGB_BYTES_PER_PIXEL = 4;

	/**
	 * Scale factor for the offscreen rendering of the workflow graph
	 */
	public static final double RENDER_SCALE = 2.0;

	private WFRenderUtil() {
	}

	/**
	 * Calculate a supersampling scale whose uncompressed ARGB raster stays
	 * within {@link #MAX_RASTER_BYTES}. Normal workflows retain the preferred
	 * 2x rendering; very large, sparse layouts are rendered at a lower scale
	 * instead of exhausting the server heap.
	 * @param logicalWidth displayed image width
	 * @param logicalHeight displayed image height
	 * @return positive render scale, at most {@link #RENDER_SCALE}
	 */
	public static double getRenderScale(int logicalWidth, int logicalHeight) {
		if (logicalWidth <= 0 || logicalHeight <= 0)
			throw new IllegalArgumentException("Workflow image dimensions must be positive");
		double logicalPixels = (double) logicalWidth * logicalHeight;
		double memoryScale = Math.sqrt(MAX_RASTER_BYTES / (logicalPixels * ARGB_BYTES_PER_PIXEL));
		return Math.min(RENDER_SCALE, memoryScale);
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
