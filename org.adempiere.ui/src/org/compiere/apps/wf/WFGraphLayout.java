/**
 *
 */
package org.compiere.apps.wf;

import java.awt.Point;
import java.util.Collection;

import org.compiere.wf.MWFNodeNext;
import org.netbeans.api.visual.graph.layout.GraphLayout;
import org.netbeans.api.visual.graph.layout.UniversalGraph;

/**
 * Layout for workflow graph
 * @author hengsin
 */
public class WFGraphLayout extends GraphLayout<Integer, MWFNodeNext> {

	public final static int COLUMN_WIDTH = 220;
	public final static int ROW_HEIGHT = 172;

	/**
	 * Routing gutter ("filler tile") of one half grid cell minus one half
	 * node card on every side of the occupied cell matrix. The orthogonal
	 * router may use the gutter and every rendering allocates it, so
	 * connectors that wrap around the first or the last column/row fold
	 * inside the rendered image instead of being clipped at the border.
	 */
	public final static int MARGIN_X = (COLUMN_WIDTH - WFNodeWidget.NODE_WIDTH) / 2;
	public final static int MARGIN_Y = (ROW_HEIGHT - WFNodeWidget.NODE_HEIGHT) / 2;

	@Override
	protected void performGraphLayout(UniversalGraph<Integer, MWFNodeNext> graph) {
		Collection<Integer> nodes = graph.getNodes();
		performNodesLayout(graph, nodes);
	}

	@Override
	protected void performNodesLayout(UniversalGraph<Integer, MWFNodeNext> graph,
			Collection<Integer> nodes) {

		for(Integer node : nodes) {
			WFNodeWidget widget = (WFNodeWidget) graph.getScene().findWidget(node);
			// Place the widget so that the painted card (which extends
			// CARD_BORDER_INSET beyond the client area on every side) is
			// centered inside its grid cell. The whole matrix is wrapped in a
			// MARGIN_X/MARGIN_Y gutter on all four sides.
			int inset = WFNodeWidget.CARD_BORDER_INSET;
			int x = MARGIN_X + (widget.getColumn() - 1) * COLUMN_WIDTH
					+ (COLUMN_WIDTH - WFNodeWidget.NODE_WIDTH) / 2 + inset;
			int y = MARGIN_Y + (widget.getRow() - 1) * ROW_HEIGHT
					+ (ROW_HEIGHT - WFNodeWidget.NODE_HEIGHT) / 2 + inset;
			Point point = new Point(x, y);
			setResolvedNodeLocation(graph, node, point);
			widget.setPreferredLocation(point);
		}
	}

}

