/**
 *
 */
package org.compiere.apps.wf;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.compiere.util.Env;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFNodeNext;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.anchor.Anchor;
import org.netbeans.api.visual.anchor.AnchorShapeFactory;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.graph.GraphScene;
import org.netbeans.api.visual.layout.LayoutFactory.ConnectionWidgetLayoutAlignment;
import org.netbeans.api.visual.router.ConnectionWidgetCollisionsCollector;
import org.netbeans.api.visual.router.Router;
import org.netbeans.api.visual.router.RouterFactory;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Widget;

/**
 * Scene for workflow graph
 * @author hengsin
 */
public class WorkflowGraphScene extends GraphScene<Integer, MWFNodeNext> {
	private static final int SELF_LOOP_SIZE = 20;
	private static final int SELF_LOOP_INSET = 28;

	/** Minimum distance a connector endpoint keeps from a card corner */
	private static final int EDGE_INSET = 12;

	/** Allowed source and target side pairs, in deterministic tie-break order. */
	private enum AnchorSidePair {
		RIGHT_LEFT (Anchor.Direction.RIGHT, Anchor.Direction.LEFT),
		RIGHT_TOP (Anchor.Direction.RIGHT, Anchor.Direction.TOP),
		BOTTOM_LEFT (Anchor.Direction.BOTTOM, Anchor.Direction.LEFT),
		BOTTOM_TOP (Anchor.Direction.BOTTOM, Anchor.Direction.TOP);

		private final Anchor.Direction source;
		private final Anchor.Direction target;

		private AnchorSidePair (Anchor.Direction source, Anchor.Direction target) {
			this.source = source;
			this.target = target;
		}
	}
	private static final AnchorSidePair[] ANCHOR_SIDE_PAIRS = AnchorSidePair.values ();

	private static final BasicStroke EDGE_STROKE = new BasicStroke(1.5f);
	private static final Color EDGE_COLOR = Color.BLACK;
	private static final Color EDGE_LABEL_COLOR = Color.BLACK;

	private LayerWidget mainLayer;
    private LayerWidget connectionLayer;

    /** Self-reference routing index per connection widget (parallel self-references on the same node get separated loops). */
    private Map<ConnectionWidget, Integer> selfLoopIndex = new HashMap<> ();
    /** Self-reference counter per node widget. */
    private Map<Widget, Integer> selfLoopCount = new HashMap<> ();
    /** Connection widget per transition edge. */
    private Map<MWFNodeNext, ConnectionWidget> connectionWidgets = new HashMap<> ();

    private WidgetAction selectAction = createSelectAction();

    public WorkflowGraphScene() {
    	mainLayer = new LayerWidget (this);
    	mainLayer.setBackground(new Color(255,255,255,0));
        connectionLayer = new LayerWidget (this);
        connectionLayer.setBackground(new Color(255,255,255,0));
        addChild (mainLayer);
        addChild (connectionLayer);
        this.setBackground(new Color(255,255,255,0));
    }

    /**
     * Show or hide all transition connectors. This is used by the editor to
     * render a shadow-free second pass without connectors, so that the drag
     * ghost of a node card is a clean card without connector fragments.
     * @param visible whether the connectors should be visible
     */
    public void setConnectionsVisible (boolean visible) {
    	for (Widget widget : connectionLayer.getChildren ()) {
    		if (widget instanceof ConnectionWidget)
    			widget.setVisible (visible);
    	}
    }

    /**
     * Modern sans-serif font for node titles, descriptions and edge labels
     * instead of the scene default (Dialog).
     */
    @Override
    public java.awt.Font getDefaultFont() {
    	return new Font (Font.SANS_SERIF, Font.PLAIN, 12);
    }

	@Override
	protected void attachEdgeSourceAnchor(MWFNodeNext edge, Integer oldsource,
			Integer sourceNode) {
		((ConnectionWidget) findWidget (edge)).setSourceAnchor (createNodeAnchor (edge, sourceNode, true));
	}

	@Override
	protected void attachEdgeTargetAnchor(MWFNodeNext edge, Integer oldtarget,
			Integer targetNode) {
		((ConnectionWidget) findWidget (edge)).setTargetAnchor (createNodeAnchor (edge, targetNode, false));
	}

	/**
	 * Create the anchor for a transition endpoint. A transition may leave its
	 * source on the right or bottom and enter its target on the left or top. The
	 * pair of sides with the shortest distance between its anchor points is used.
	 * Multiple transitions on the same node side are fanned out across that side.
	 * Self-references are not part of the fan, they get their own loop route.
	 * @param edge transition
	 * @param nodeId source (exit) or target (entry) node of the transition
	 * @param exit true for the source/exit anchor, false for the target/entry anchor
	 * @return anchor
	 */
	private Anchor createNodeAnchor(MWFNodeNext edge, Integer nodeId, boolean exit) {
		return new NodeAnchor (findWidget (nodeId), exit, edge, nodeId);
	}

	/**
	 * Anchor that attaches a transition to the selected side of its node. The
	 * selected side is divided into as many equal sections as there are connectors
	 * on that side and each connector sits at the middle of its section.
	 */
	private class NodeAnchor extends Anchor {

		private final boolean exit;
		private final MWFNodeNext edge;
		private final Integer nodeId;

		private NodeAnchor (Widget widget, boolean exit, MWFNodeNext edge, Integer nodeId) {
			super (widget);
			this.exit = exit;
			this.edge = edge;
			this.nodeId = nodeId;
		}

		@Override
		public Result compute (Entry entry) {
			Anchor.Direction side = anchorSide (edge, exit);
			int[] fan = fanIndexAndCount (exit, edge, nodeId, side);
			int index = Math.max (fan[0], 0);
			int count = Math.max (fan[1], 1);

			Rectangle bounds = boundsOf (getRelatedWidget ());
			if (bounds == null)
				return new Result (new Point (0, 0), EnumSet.of (side));

			return new Result (pointOnSide (bounds, side, index, count), EnumSet.of (side));
		}
	}

	/**
	 * Select the source or target side chosen for a transition. Traditional
	 * right-to-left anchors win exact ties, keeping stable routes for horizontal
	 * transitions while allowing shorter vertical and mixed routes.
	 * @param edge transition
	 * @param exit true for its source side, false for its target side
	 * @return selected anchor side
	 */
	private Anchor.Direction anchorSide (MWFNodeNext edge, boolean exit) {
		Integer sourceId = getEdgeSource (edge);
		Integer targetId = getEdgeTarget (edge);
		if (sourceId != null && sourceId.equals (targetId))
			return exit ? Anchor.Direction.RIGHT : Anchor.Direction.LEFT;

		Rectangle sourceBounds = boundsOf (sourceId != null ? findWidget (sourceId) : null);
		Rectangle targetBounds = boundsOf (targetId != null ? findWidget (targetId) : null);
		if (sourceBounds == null || targetBounds == null)
			return exit ? Anchor.Direction.RIGHT : Anchor.Direction.LEFT;
		AnchorSidePair sides = shortestAnchorSides (sourceBounds, targetBounds);
		return exit ? sides.source : sides.target;
	}

	/**
	 * Find the allowed source/target side pair whose center anchor points have
	 * the shortest rectilinear distance.
	 */
	private static AnchorSidePair shortestAnchorSides (Rectangle sourceBounds, Rectangle targetBounds) {
		AnchorSidePair best = AnchorSidePair.RIGHT_LEFT;
		long bestDistance = Long.MAX_VALUE;
		for (AnchorSidePair candidate : ANCHOR_SIDE_PAIRS) {
			Point source = pointOnSide (sourceBounds, candidate.source, 0, 1);
			Point target = pointOnSide (targetBounds, candidate.target, 0, 1);
			long distance = Math.abs ((long) source.x - target.x)
					+ Math.abs ((long) source.y - target.y);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = candidate;
			}
		}
		return best;
	}

	/**
	 * Resolve a widget's bounds in scene coordinates, including before its first
	 * validation when only preferred location and size are available.
	 */
	private Rectangle boundsOf (Widget widget) {
		if (widget == null)
			return null;
		if (widget.isValidated () && widget.getBounds () != null)
			return widget.convertLocalToScene (widget.getBounds ());
		Point location = widget.getPreferredLocation ();
		Dimension preferredSize = widget.getPreferredSize ();
		return location != null && preferredSize != null
				? new Rectangle (location.x, location.y, preferredSize.width, preferredSize.height)
				: null;
	}

	/**
	 * Calculate a centered fan position on one side of a node.
	 */
	private static Point pointOnSide (Rectangle bounds, Anchor.Direction side, int index, int count) {
		if (side == Anchor.Direction.TOP || side == Anchor.Direction.BOTTOM) {
			int width = Math.max (bounds.width - 2 * EDGE_INSET, 1);
			int x = bounds.x + EDGE_INSET + Math.round (width * (index + 0.5f) / count);
			int y = side == Anchor.Direction.TOP ? bounds.y : bounds.y + bounds.height;
			return new Point (x, y);
		}
		int height = Math.max (bounds.height - 2 * EDGE_INSET, 1);
		int x = side == Anchor.Direction.LEFT ? bounds.x : bounds.x + bounds.width;
		int y = bounds.y + EDGE_INSET + Math.round (height * (index + 0.5f) / count);
		return new Point (x, y);
	}

	/**
	 * Determine the fan slot of a transition among all transitions of its node:
	 * transitions are sorted by the position of their opposite node, so the fan
	 * follows the visual order of the connected nodes.
	 * @param exit true for the source/exit fan, false for the target/entry fan
	 * @param edge transition
	 * @param nodeId own node of the transition
	 * @param side selected side of the own node
	 * @return {slot index, slot count}; slot 0 is topmost on a vertical side and
	 *         leftmost on a horizontal side
	 */
	private int[] fanIndexAndCount (boolean exit, MWFNodeNext edge, Integer nodeId,
			Anchor.Direction side) {
		List<MWFNodeNext> related = new ArrayList<> ();
		for (MWFNodeNext e : getEdges ()) {
			Integer own = exit ? getEdgeSource (e) : getEdgeTarget (e);
			Integer other = exit ? getEdgeTarget (e) : getEdgeSource (e);
			if (own != null && own.equals (nodeId) && other != null && !other.equals (nodeId)
					&& anchorSide (e, exit) == side)
				related.add (e);
		}
		boolean horizontalSide = side == Anchor.Direction.TOP || side == Anchor.Direction.BOTTOM;
		related.sort (Comparator.comparingLong (e -> sortKeyOf (
				exit ? getEdgeTarget (e) : getEdgeSource (e), horizontalSide)));
		int index = related.indexOf (edge);
		return new int[] {Math.max (0, index), related.size ()};
	}

	/**
	 * Sort key for the fan order. Vertical sides follow row/column order;
	 * horizontal sides follow column/row order, matching their visual axis.
	 * @param nodeId node id
	 * @param horizontalSide whether the fan runs from left to right
	 * @return sort key
	 */
	private long sortKeyOf (Integer nodeId, boolean horizontalSide) {
		Widget widget = nodeId != null ? findWidget (nodeId) : null;
		if (widget instanceof WFNodeWidget) {
			WFNodeWidget nodeWidget = (WFNodeWidget) widget;
			return horizontalSide
					? nodeWidget.getColumn () * 1000L + nodeWidget.getRow ()
					: nodeWidget.getRow () * 1000L + nodeWidget.getColumn ();
		}
		return Long.MAX_VALUE;
	}

	@Override
	protected Widget attachEdgeWidget(MWFNodeNext edge) {
		 ConnectionWidget connection = new ConnectionWidget (this);
		 connection.setTargetAnchorShape (AnchorShapeFactory.createTriangleAnchorShape (12, true, false, 11));
		 if (edge.getAD_WF_Node_ID () == edge.getAD_WF_Next_ID ()) {
			 // nodes are attached before edges, so the node widget is available here
			 Widget nodeWidget = findWidget (edge.getAD_WF_Node_ID ());
			 int index = nodeWidget == null
					 ? selfLoopIndex.size ()
					 : selfLoopCount.merge (nodeWidget, 1, Integer::sum) - 1;
			 selfLoopIndex.put (connection, index);
		 }
		 Router orthogonalRouter = RouterFactory.createOrthogonalSearchRouter (createCollisionsCollector ());
		 Router directRouter = RouterFactory.createDirectRouter ();
		 // The orthogonal router can return no path depending on the node positions.
		 // Always provide a drawable direct route in that case.
		 connection.setRouter (widget -> {
			 List<Point> controlPoints = routeSelfConnection (widget);
			 if (controlPoints == null)
				 controlPoints = orthogonalRouter.routeConnection (widget);
			 return controlPoints == null || controlPoints.size () < 2
					? directRouter.routeConnection (widget)
					: controlPoints;
		 });
		 connection.setRoutingPolicy (ConnectionWidget.RoutingPolicy.ALWAYS_ROUTE);
		 connection.setStroke (EDGE_STROKE);
		 connection.setLineColor (EDGE_COLOR);

		 String description = edge.getDescription();
		 if (description != null && description.length() > 0) {
			 description = String.valueOf(edge.getSeqNo()) + ": " + description;
			 LabelWidget label = new LabelWidget(this, description);
			 label.setOpaque (true);
			 label.setBackground (Color.WHITE);
			 label.setForeground (EDGE_LABEL_COLOR);
			 label.setBorder (BorderFactory.createEmptyBorder (1, 3, 1, 3));
			 connection.addChild(label);
			 connection.setConstraint (label, ConnectionWidgetLayoutAlignment.TOP_CENTER, 0.5f);
		 }

	     connectionWidgets.put (edge, connection);
	     connectionLayer.addChild (connection);
	     return connection;
	}

	/**
	 * All transition connectors currently shown in the scene.
	 * @return list of transition edges that own a connection widget
	 */
	public List<MWFNodeNext> getTransitions () {
		List<MWFNodeNext> transitions = new ArrayList<> ();
		for (Map.Entry<MWFNodeNext, ConnectionWidget> entry : connectionWidgets.entrySet ())
			transitions.add (entry.getKey ());
		return transitions;
	}

	/**
	 * Routed path of a transition connector in scene coordinates.
	 * @param edge transition edge
	 * @return control points of the routed connector, or null if not routed
	 */
	public List<Point> getTransitionPath (MWFNodeNext edge) {
		ConnectionWidget connection = connectionWidgets.get (edge);
		if (connection == null)
			return null;
		List<Point> controlPoints = connection.getControlPoints ();
		if (controlPoints == null)
			return null;
		List<Point> scenePoints = new ArrayList<> (controlPoints.size ());
		for (Point point : controlPoints)
			scenePoints.add (connection.convertLocalToScene (point));
		return scenePoints;
	}

	/**
	 * Route a self-reference as a compact loop around the bottom-right corner
	 * of its node. The workflow grid leaves enough space on these two sides to
	 * keep the loop inside the canvas, including for nodes in the last row or
	 * column. Parallel self-references on the same node are separated by
	 * alternating between the bottom-right and bottom-left corner and by
	 * shrinking successive loops, so no two loops share the same geometry.
	 * @param connection connection being routed
	 * @return loop control points, or null if this is not a self-reference
	 */
	private List<Point> routeSelfConnection (ConnectionWidget connection) {
		if (connection.getSourceAnchor () == null || connection.getTargetAnchor () == null)
			return null;

		Widget source = connection.getSourceAnchor ().getRelatedWidget ();
		if (source == null || source != connection.getTargetAnchor ().getRelatedWidget ())
			return null;

		Rectangle bounds = null;
		if (source.isValidated () && source.getBounds () != null) {
			bounds = source.convertLocalToScene (source.getBounds ());
		}
		else {
			// fall back to preferred location and size, so a compact loop is still
			// routed before the widget has been validated (avoids a degenerate
			// zero-length route with an arbitrarily oriented arrow head)
			Point location = source.getPreferredLocation ();
			Dimension preferredSize = source.getPreferredSize ();
			if (location == null || preferredSize == null)
				return null;
			bounds = new Rectangle (location.x, location.y, preferredSize.width, preferredSize.height);
		}
		int right = bounds.x + bounds.width;
		int bottom = bounds.y + bounds.height;
		int left = bounds.x;
		int index = selfLoopIndex.getOrDefault (connection, 0);
		int corner = index % 2;
		int size = Math.max (SELF_LOOP_SIZE - (index / 2) * 5, 10);
		if (corner == 0) {
			return List.of (
					new Point (right, bottom - SELF_LOOP_INSET),
					new Point (right + size, bottom - SELF_LOOP_INSET),
					new Point (right + size, bottom + size),
					new Point (right - SELF_LOOP_INSET, bottom + size),
					new Point (right - SELF_LOOP_INSET, bottom));
		}
		return List.of (
				new Point (left, bottom - SELF_LOOP_INSET),
				new Point (left - size, bottom - SELF_LOOP_INSET),
				new Point (left - size, bottom + size),
				new Point (left + SELF_LOOP_INSET, bottom + size),
				new Point (left + SELF_LOOP_INSET, bottom));
	}

	/**
	 * Create a collision collector which avoids nodes and already routed edges,
	 * but excludes the connection currently being routed.
	 * @return collision collector
	 */
	private ConnectionWidgetCollisionsCollector createCollisionsCollector () {
		return (connection, verticalCollisions, horizontalCollisions) -> {
			// The editor reserves one empty column (right) and one empty row
			// (below) the occupied cells for dropping new nodes. Transitions
			// must not be routed over that empty reserve area, so it is added
			// here as an obstacle wall along the right and bottom border of the
			// occupied cells. The orthogonal router then stays inside the cells
			// actually covered by nodes and turns on the seams between rows and
			// columns instead of wasting the reserve area.
			//
			// A gutter of one half (grid - node) on each axis is left open next
			// to the occupied cells and may be used for routing. Connectors
			// that must wrap around their column (same column, last column or
			// last row) use this outer lane instead of degrading to a diagonal
			// fallback. The resulting margin on the right and bottom is a
			// measured win: about 7-15% fewer routing fallbacks in dense
			// workflows for ~2-3% more segment crossings, and slightly fewer
			// crossings in regular layouts. Walls on the left and top would
			// only take space away from the router (they measurably increase
			// both fallbacks and crossings), so those sides stay free.
			int marginX = (WFGraphLayout.COLUMN_WIDTH - WFNodeWidget.NODE_WIDTH) / 2;
			int marginY = (WFGraphLayout.ROW_HEIGHT - WFNodeWidget.NODE_HEIGHT) / 2;
			int occupiedWidth = 1;
			int occupiedHeight = 1;
			for (Widget node : mainLayer.getChildren ()) {
				if (node instanceof WFNodeWidget) {
					WFNodeWidget nodeWidget = (WFNodeWidget) node;
					occupiedWidth = Math.max (occupiedWidth,
							nodeWidget.getColumn () * WFGraphLayout.COLUMN_WIDTH);
					occupiedHeight = Math.max (occupiedHeight,
							nodeWidget.getRow () * WFGraphLayout.ROW_HEIGHT);
				}
			}
			// The layout wraps the cell matrix in a MARGIN_X/MARGIN_Y gutter on
			// every side, so the walls must sit that far outside the grid lines
			// to keep the one-margin routing band next to the outer nodes.
			Rectangle rightReserve = new Rectangle (WFGraphLayout.MARGIN_X + occupiedWidth + marginX, 0,
					WFGraphLayout.COLUMN_WIDTH, occupiedHeight + WFGraphLayout.ROW_HEIGHT);
			Rectangle bottomReserve = new Rectangle (0, WFGraphLayout.MARGIN_Y + occupiedHeight + marginY,
					occupiedWidth + WFGraphLayout.COLUMN_WIDTH, WFGraphLayout.ROW_HEIGHT);
			verticalCollisions.add (rightReserve);
			horizontalCollisions.add (rightReserve);
			verticalCollisions.add (bottomReserve);
			horizontalCollisions.add (bottomReserve);

			for (Widget node : mainLayer.getChildren ()) {
				if (!node.isValidated () || node.getBounds () == null)
					continue;
				Rectangle bounds = node.convertLocalToScene (node.getBounds ());
				// horizontal growth keeps a visible lane at the outer canvas border,
				// vertical growth forces detours through the gaps between rows
				// instead of routing across the top of the grid
				bounds.grow (8, 16);
				verticalCollisions.add (bounds);
				horizontalCollisions.add (bounds);
			}

			for (Widget widget : connectionLayer.getChildren ()) {
				if (widget == connection || !(widget instanceof ConnectionWidget))
					continue;
				ConnectionWidget otherConnection = (ConnectionWidget) widget;
				// Opposite transitions share a route. Treating the first one as an
				// obstacle makes the second one take a needlessly winding detour.
				if (connectsOppositeNodes (connection, otherConnection))
					continue;
				if (!otherConnection.isRouted ())
					continue;
				List<Point> controlPoints = otherConnection.getControlPoints ();
				for (int i = 0; i < controlPoints.size () - 1; i++) {
					Point first = otherConnection.convertLocalToScene (controlPoints.get (i));
					Point second = otherConnection.convertLocalToScene (controlPoints.get (i + 1));
					if (first.x == second.x) {
						Rectangle segment = new Rectangle (first.x, Math.min (first.y, second.y), 0,
								Math.abs (second.y - first.y));
						segment.grow (8, 8);
						verticalCollisions.add (segment);
					}
					else if (first.y == second.y) {
						Rectangle segment = new Rectangle (Math.min (first.x, second.x), first.y,
								Math.abs (second.x - first.x), 0);
						segment.grow (8, 8);
						horizontalCollisions.add (segment);
					}
				}
			}
		};
	}

	/**
	 * Test whether two connections link the same nodes in opposite directions.
	 * @param connection connection being routed
	 * @param otherConnection other connection in the scene
	 * @return true if both connections form a bidirectional transition
	 */
	private boolean connectsOppositeNodes (ConnectionWidget connection, ConnectionWidget otherConnection) {
		if (connection.getSourceAnchor () == null || connection.getTargetAnchor () == null
				|| otherConnection.getSourceAnchor () == null || otherConnection.getTargetAnchor () == null)
			return false;

		Widget source = connection.getSourceAnchor ().getRelatedWidget ();
		Widget target = connection.getTargetAnchor ().getRelatedWidget ();
		return source != null && target != null
				&& source == otherConnection.getTargetAnchor ().getRelatedWidget ()
				&& target == otherConnection.getSourceAnchor ().getRelatedWidget ();
	}

	@Override
	protected Widget attachNodeWidget(Integer node) {
		WFNodeWidget widget = (WFNodeWidget) findWidget(node);
		if (widget == null) {
			widget = new WFNodeWidget(this, MWFNode.getCopy(Env.getCtx(), node, null));
			widget.getActions ().addAction (selectAction);
			mainLayer.addChild (widget);
		}
		return widget;
	}
}
