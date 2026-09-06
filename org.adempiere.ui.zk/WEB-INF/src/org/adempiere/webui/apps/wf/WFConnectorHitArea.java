/***********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * *********************************************************************/
package org.adempiere.webui.apps.wf;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry for the transparent click/drop hit areas that make the
 * workflow transition connectors clickable (see {@link WFEditor}).
 * <p>
 * A connector is drawn as a polyline through its routed control points. This
 * class turns that polyline into a set of thin hit rectangles that stay clear
 * of the node cards - the cards keep their own click and drag surface. Only
 * cards that really intersect a segment in both axes clip it, and clipping
 * happens along the direction of travel only, so the strip between two cards
 * stays fully clickable.
 * <p>
 * The class is deliberately free of any user interface dependency so the
 * geometry can be tested offline.
 */
public final class WFConnectorHitArea {

	/** Transparent hit radius around the connector path. */
	public static final int RADIUS = 4;

	/** Square side of the hit steps on diagonal segments. */
	private static final int DIAGONAL_STEP = 8;

	private WFConnectorHitArea() {
	}

	/**
	 * Hit rectangles for a routed connector polyline, clipped against the
	 * passed card rectangles. Consecutive, collinear segments are merged.
	 * @param path routed control points in scene coordinates
	 * @param cardRects visible card rectangles in scene coordinates
	 * @return list of hit rectangles, never null
	 */
	public static List<Rectangle> compute(List<Point> path, List<Rectangle> cardRects) {
		List<Rectangle> areas = new ArrayList<Rectangle>();
		if (path == null || path.size() < 2)
			return areas;
		for (int i = 0; i < path.size() - 1; i++) {
			Point from = path.get(i);
			Point to = path.get(i + 1);
			if (from.x == to.x)
				addSegment(areas, from.x - RADIUS, Math.min(from.y, to.y) - RADIUS,
						2 * RADIUS, Math.abs(to.y - from.y) + 2 * RADIUS, cardRects);
			else if (from.y == to.y)
				addSegment(areas, Math.min(from.x, to.x) - RADIUS, from.y - RADIUS,
						Math.abs(to.x - from.x) + 2 * RADIUS, 2 * RADIUS, cardRects);
			else
				addDiagonal(areas, from, to, cardRects);
		}
		return areas;
	}

	/**
	 * Add the hit rectangles for one axis-aligned segment: a thin strip
	 * around the path from which every overlapping card is subtracted, so the
	 * strip stays clickable everywhere it is not covered by a card. The strip
	 * is thin, so the subtraction yields at most two rectangles in practice.
	 */
	private static void addSegment(List<Rectangle> areas, int x0, int y0, int width, int height, List<Rectangle> cardRects) {
		List<Rectangle> current = new ArrayList<Rectangle>();
		current.add(new Rectangle(x0, y0, width, height));
		for (Rectangle r : cardRects) {
			List<Rectangle> next = new ArrayList<Rectangle>();
			for (Rectangle strip : current)
				subtract(next, strip, r);
			current = next;
			if (current.isEmpty())
				return;
		}
		areas.addAll(current);
	}

	/**
	 * Subtract a card rectangle from a strip rectangle. The strip is a thin
	 * hit band around the connector path; a card that overlaps it removes
	 * exactly the overlapped part, which splits the strip into up to four
	 * rectangles. If the card does not overlap the strip nothing is removed.
	 * @param out receives the result rectangles
	 * @param strip hit band to clip
	 * @param card visible card rectangle
	 */
	private static void subtract(List<Rectangle> out, Rectangle strip, Rectangle card) {
		int ix = Math.max(strip.x, card.x);
		int iy = Math.max(strip.y, card.y);
		int ix2 = Math.min(strip.x + strip.width, card.x + card.width);
		int iy2 = Math.min(strip.y + strip.height, card.y + card.height);
		if (ix2 - ix <= 0 || iy2 - iy <= 0) {
			// no overlap: the strip stays whole
			out.add(strip);
			return;
		}
		// above the card
		if (iy - strip.y > 0)
			out.add(new Rectangle(strip.x, strip.y, strip.width, iy - strip.y));
		// below the card
		if (strip.y + strip.height - iy2 > 0)
			out.add(new Rectangle(strip.x, iy2, strip.width, strip.y + strip.height - iy2));
		// left of the card
		if (ix - strip.x > 0)
			out.add(new Rectangle(strip.x, iy, ix - strip.x, iy2 - iy));
		// right of the card
		if (strip.x + strip.width - ix2 > 0)
			out.add(new Rectangle(ix2, iy, strip.x + strip.width - ix2, iy2 - iy));
	}

	/**
	 * Add stepped hit squares for a diagonal connector segment. Diagonal
	 * shortcuts occur only when the routers fall back to a direct route.
	 */
	private static void addDiagonal(List<Rectangle> areas, Point from, Point to, List<Rectangle> cardRects) {
		int dx = to.x - from.x;
		int dy = to.y - from.y;
		int steps = Math.max(Math.abs(dx), Math.abs(dy)) / DIAGONAL_STEP;
		if (steps < 1)
			steps = 1;
		for (int i = 0; i <= steps; i++) {
			int x = from.x + dx * i / steps;
			int y = from.y + dy * i / steps;
			Rectangle square = new Rectangle(x - RADIUS, y - RADIUS, 2 * RADIUS, 2 * RADIUS);
			List<Rectangle> current = new ArrayList<Rectangle>();
			current.add(square);
			for (Rectangle r : cardRects) {
				List<Rectangle> next = new ArrayList<Rectangle>();
				for (Rectangle strip : current)
					subtract(next, strip, r);
				current = next;
				if (current.isEmpty())
					break;
			}
			areas.addAll(current);
		}
	}
}