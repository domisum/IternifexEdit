package de.domisum.iternifexedit.navmesh.edit;

import com.darkblade12.particleeffect.ParticleEffect;
import de.domisum.lib.auxilium.data.container.direction.Direction2D;
import de.domisum.lib.auxilium.data.container.math.LineSegment3D;
import de.domisum.lib.auxilium.data.container.math.Vector3D;
import de.domisum.lib.auxilium.util.math.MathUtil;
import de.domisum.lib.auxiliumspigot.util.MessagingUtil;
import de.domisum.lib.iternifex.navmesh.NavMesh;
import de.domisum.lib.iternifex.navmesh.NavMeshRegistry;
import de.domisum.lib.iternifex.navmesh.components.NavMeshEdge;
import de.domisum.lib.iternifex.navmesh.components.NavMeshPoint;
import de.domisum.lib.iternifex.navmesh.components.NavMeshTriangle;
import de.domisum.lib.iternifex.navmesh.components.edges.NavMeshEdgeLadder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class NavMeshEditor
{

	// CONSTANTS
	private static final double VISIBILITY_RANGE = 32;
	private static final double LINE_PARTICLE_DISTANCE = 0.4;
	private static final double SELECTION_MAX_DISTANCE = 1.5;

	// DEPENDENCIES
	private final NavMeshEditCoordinator navMeshEditCoordinator;
	private final NavMeshRegistry navMeshRegistry;

	// INPUT
	@Getter
	private final Player player;

	// SETTINGS
	// editor
	@Setter
	private boolean snapPointsToBlockCorner = true;

	// display
	@Setter
	private boolean showTriangleConnections = false;

	// EDIT STATUS
	private final List<NavMeshPoint> selectedPoints = new ArrayList<>();
	private Vector3D ladderPosition;
	private NavMeshTriangle ladderTriangle;


	// GETTERS
	public boolean getSnapPointsToBlockCorner()
	{
		return snapPointsToBlockCorner;
	}

	public boolean getShowTriangleConnections()
	{
		return showTriangleConnections;
	}

	private NavMesh getNavMesh()
	{
		Vector3D location = convertLocationToVector(player.getLocation());
		return navMeshRegistry.getNavMeshAt(location);
	}

	private NavMeshPoint getNearestPoint()
	{
		NavMesh mesh = getNavMesh();
		if(mesh == null)
			return null;

		Vector3D playerLocation = convertLocationToVector(player.getLocation());

		double minDistanceSquared = Double.MAX_VALUE;
		NavMeshPoint closestPoint = null;

		for(NavMeshPoint point : mesh.getPoints())
		{
			double distanceSquared = point.distanceToSquared(playerLocation);
			if(distanceSquared < minDistanceSquared)
			{
				closestPoint = point;
				minDistanceSquared = distanceSquared;
			}
		}

		if(minDistanceSquared > (SELECTION_MAX_DISTANCE*SELECTION_MAX_DISTANCE))
			return null;

		return closestPoint;
	}

	private NavMeshTriangle getTriangle()
	{
		NavMesh mesh = getNavMesh();
		if(mesh == null)
			return null;

		Vector3D location = convertLocationToVector(player.getLocation());
		return mesh.getTriangleAt(location);
	}

	private NavMeshEdgeLadder getNearestNavMeshLadder(NavMeshTriangle triangle)
	{
		double minDistance = Double.MAX_VALUE;
		NavMeshEdgeLadder closestLadder = null;

		for(NavMeshEdge edge : triangle.getEdges())
			if(edge instanceof NavMeshEdgeLadder)
			{
				NavMeshEdgeLadder ladder = (NavMeshEdgeLadder) edge;
				Vector3D ladderPosition;

				if(ladder.getTriangleA() == triangle)
					ladderPosition = ladder.getBottomLadderLocation();
				else
					ladderPosition = ladder.getTopLadderLocation();

				Vector3D playerLocation = convertLocationToVector(player.getLocation());
				double distance = ladderPosition.distanceTo(playerLocation);
				if(distance < minDistance)
				{
					minDistance = distance;
					closestLadder = ladder;
				}
			}

		if(minDistance > SELECTION_MAX_DISTANCE)
			return null;

		return closestLadder;
	}


	// UPDATE
	void update()
	{
		NavMesh mesh = getNavMesh();

		if(mesh != null)
			if((navMeshEditCoordinator.getTickCount()%3) == 0)
				spawnParticles(mesh);

		sendNearbyNavMeshName(mesh);
	}

	private void sendNearbyNavMeshName(NavMesh mesh)
	{
		String meshName = ChatColor.RED+"No mesh in range";
		if(mesh != null)
			meshName = "Mesh: '"+mesh.getId()+"'";

		MessagingUtil.sendActionBarMessage(meshName, player);
	}


	// VISUALIZATION
	private void spawnParticles(NavMesh navMesh)
	{
		Vector3D playerLocation = convertLocationToVector(player.getLocation());

		for(NavMeshPoint point : navMesh.getPoints())
			if(point.distanceTo(playerLocation) < VISIBILITY_RANGE)
				spawnPointParticles(point);

		Set<LineSegment3D> triangleLines = new HashSet<>();
		Set<LineSegment3D> triangleConnectionLines = new HashSet<>();
		Set<LineSegment3D> ladderLines = new HashSet<>();

		for(NavMeshTriangle triangle : navMesh.getTriangles())
		{
			if(triangle.getCenter().distanceTo(playerLocation) > VISIBILITY_RANGE)
				continue;

			triangleLines.add(new LineSegment3D(triangle.getPointA(), triangle.getPointB()));
			triangleLines.add(new LineSegment3D(triangle.getPointB(), triangle.getPointC()));
			triangleLines.add(new LineSegment3D(triangle.getPointC(), triangle.getPointA()));

			addLinesOfTriangleEdges(triangleConnectionLines, ladderLines, triangle);
		}

		spawnParticles(triangleLines, triangleConnectionLines, ladderLines);
	}

	private void addLinesOfTriangleEdges(
			Set<LineSegment3D> triangleConnectionLines, Set<LineSegment3D> ladderLines, NavMeshTriangle triangle)
	{
		for(NavMeshEdge edge : triangle.getEdges())
		{
			NavMeshTriangle neighbor = edge.getOther(triangle);

			if(showTriangleConnections)
				triangleConnectionLines.add(new LineSegment3D(triangle.getCenter(), neighbor.getCenter()));

			if(edge instanceof NavMeshEdgeLadder)
			{
				NavMeshEdgeLadder ladder = (NavMeshEdgeLadder) edge;
				Vector3D cornerPoint = new Vector3D(
						ladder.getBottomLadderLocation().getX(),
						ladder.getTopLadderLocation().getY(),
						ladder.getBottomLadderLocation().getZ()
				);

				ladderLines.add(new LineSegment3D(ladder.getBottomLadderLocation(), cornerPoint));
				ladderLines.add(new LineSegment3D(cornerPoint, ladder.getTopLadderLocation()));
			}
		}
	}

	private void spawnParticles(
			Set<LineSegment3D> triangleLines, Set<LineSegment3D> triangleConnectionLines, Set<LineSegment3D> ladderLines)
	{
		for(LineSegment3D line : triangleLines)
			spawnLineParticles(line, ParticleEffect.FLAME, LINE_PARTICLE_DISTANCE);

		for(LineSegment3D line : triangleConnectionLines)
			spawnLineParticles(line, ParticleEffect.DRAGON_BREATH, LINE_PARTICLE_DISTANCE*1.3);

		for(LineSegment3D line : ladderLines)
			spawnLineParticles(line, ParticleEffect.FIREWORKS_SPARK, LINE_PARTICLE_DISTANCE);
	}

	private void spawnPointParticles(NavMeshPoint point)
	{
		Location location = convertVectorToLocation(point, player.getWorld());

		ParticleEffect particleEffect = ParticleEffect.DAMAGE_INDICATOR;
		if(selectedPoints.contains(point))
		{
			particleEffect = ParticleEffect.HEART;
			location = location.add(0, 0.3, 0);
		}

		particleEffect.display(0, 0, 0, 0, 1, location, player);
	}

	private void spawnLineParticles(LineSegment3D lineSegment, ParticleEffect effect, double distance)
	{
		Vector3D delta = lineSegment.getB().subtract(lineSegment.getA());
		for(double d = 0; d < delta.length(); d += distance)
		{
			Vector3D offset = delta.normalize().multiply(d);
			Vector3D vectorLocation = lineSegment.getA().add(offset);
			Location location = convertVectorToLocation(vectorLocation, player.getWorld()).add(0, 0.5, 0);

			effect.display(0, 0, 0, 0, 1, location, player);
		}

		// make sure end is displayed properly even with big distance between points
		Location location = convertVectorToLocation(lineSegment.getB(), player.getWorld()).add(0, 0.5, 0);
		effect.display(0, 0, 0, 0, 1, location, player);
	}


	// EDITING
	// create point
	public NavMeshPoint createPoint()
	{
		NavMesh navMesh = getNavMesh();
		if(navMesh == null)
		{
			player.sendMessage("Creating point failed. No NavMesh is covering this area.");
			return null;
		}

		NavMeshPoint point = createNavMeshPoint();
		addPointToNavMesh(navMesh, point);

		if(player.isSneaking())
			selectedPoints.add(point);

		return point;
	}

	private NavMeshPoint createNavMeshPoint()
	{
		Location location = player.getLocation();

		double pX = MathUtil.round(location.getX(), 5);
		double pY = MathUtil.round(location.getY(), 5);
		double pZ = MathUtil.round(location.getZ(), 5);
		if(snapPointsToBlockCorner)
		{
			pX = Math.round(pX);
			pY = Math.round(pY);
			pZ = Math.round(pZ);
		}

		return new NavMeshPoint(pX, pY, pZ);
	}

	private void addPointToNavMesh(NavMesh navMesh, NavMeshPoint point)
	{
		Set<NavMeshPoint> points = new HashSet<>(navMesh.getPoints());
		Set<NavMeshTriangle> triangles = new HashSet<>(navMesh.getTriangles());

		points.add(point);

		NavMesh newNavMesh = new NavMesh(navMesh.getId(), navMesh.getCenter(), navMesh.getRadius(), points, triangles);
		navMeshRegistry.register(newNavMesh);
	}


	// delete point
	public void deletePoint()
	{
		NavMeshPoint point = getNearestPoint();
		if(point == null)
		{
			player.sendMessage("Deleting point failed. No point is nearby.");
			return;
		}

		deletePointFromNavMesh(point);

		selectedPoints.remove(point);
	}

	private void deletePointFromNavMesh(NavMeshPoint point)
	{
		NavMesh navMesh = getNavMesh();

		Set<NavMeshPoint> points = new HashSet<>(navMesh.getPoints());
		Set<NavMeshTriangle> triangles = new HashSet<>(navMesh.getTriangles());

		points.remove(point);
		triangles.removeIf(t->t.getPointA().equals(point) || t.getPointB().equals(point) || t.getPointC().equals(point));

		NavMesh newNavMesh = new NavMesh(navMesh.getId(), navMesh.getCenter(), navMesh.getRadius(), points, triangles);
		navMeshRegistry.register(newNavMesh);
	}


	// select
	public void selectPoint()
	{
		NavMeshPoint point = getNearestPoint();
		if(point == null)
		{
			player.sendMessage("Selecting point failed. No point is nearby.");
			return;
		}

		if(selectedPoints.contains(point))
		{
			player.sendMessage("Selecting point failed. The point is already selected.");
			return;
		}

		selectedPoints.add(point);
	}


	// deselect
	public void deselectPoint()
	{
		if(player.isSneaking())
		{
			selectedPoints.clear();
			player.sendMessage("Deselected all points.");
			return;
		}

		NavMeshPoint point = getNearestPoint();
		if(point == null)
		{
			player.sendMessage("Deselecting point failed. No point is nearby.");
			return;
		}

		if(!selectedPoints.contains(point))
		{
			player.sendMessage("Deselecting point failed. The point is not selected.");
			return;
		}

		selectedPoints.remove(point);
	}


	// create triangle
	public void createTriangle()
	{
		if(selectedPoints.size() > 3)
		{
			player.sendMessage("Creating triangle failed. Too many points selected ("+selectedPoints.size()+").");
			return;
		}

		if(selectedPoints.size() < 2)
		{
			player.sendMessage("Creating triangle failed. Not enough points selected ("+selectedPoints.size()+").");
			return;
		}

		NavMeshPoint thirdPoint = getOrCreateThirdPoint();

		NavMesh mesh = getNavMesh();
		if(mesh == null)
		{
			player.sendMessage("Creating triangle failed. No NavMesh is covering this area.");
			return;
		}

		NavMeshTriangle triangle = new NavMeshTriangle(selectedPoints.get(0), selectedPoints.get(1), thirdPoint);
		addTriangleToNavMesh(triangle);

		selectedPoints.remove(0);
	}

	private NavMeshPoint getOrCreateThirdPoint()
	{
		NavMeshPoint thirdPoint;
		if(selectedPoints.size() == 2)
		{
			thirdPoint = createPoint();
			selectedPoints.add(thirdPoint);
		}
		else
			thirdPoint = selectedPoints.get(2);
		return thirdPoint;
	}

	private void addTriangleToNavMesh(NavMeshTriangle triangle)
	{
		NavMesh navMesh = getNavMesh();

		Set<NavMeshPoint> points = new HashSet<>(navMesh.getPoints());
		Set<NavMeshTriangle> triangles = new HashSet<>(navMesh.getTriangles());

		triangles.add(triangle);

		NavMesh newNavMesh = new NavMesh(navMesh.getId(), navMesh.getCenter(), navMesh.getRadius(), points, triangles);
		navMeshRegistry.register(newNavMesh);
	}


	// delete triangle
	public void deleteTriangle()
	{
		NavMeshTriangle triangle = getTriangle();
		if(triangle == null)
		{
			player.sendMessage("Deleting triangle failed. No triangle found at your position.");
			return;
		}

		removeTriangleFromNavMesh(triangle);

		if(ladderTriangle == triangle)
		{
			ladderTriangle = null;
			ladderPosition = null;
		}
	}

	private void removeTriangleFromNavMesh(NavMeshTriangle triangle)
	{
		NavMesh navMesh = getNavMesh();

		Set<NavMeshPoint> points = new HashSet<>(navMesh.getPoints());
		Set<NavMeshTriangle> triangles = new HashSet<>(navMesh.getTriangles());

		triangles.remove(triangle);
		for(NavMeshTriangle t : triangles)
		{
			NavMeshEdge edge = t.getEdgeTo(triangle);
			if(edge == null)
				continue;

			t.removeEdge(edge);
		}

		NavMesh newNavMesh = new NavMesh(navMesh.getId(), navMesh.getCenter(), navMesh.getRadius(), points, triangles);
		navMeshRegistry.register(newNavMesh);
	}


	// info
	public void info()
	{
		NavMesh mesh = getNavMesh();
		if(mesh == null)
		{
			player.sendMessage("Giving info failed. No mesh at current location.");
			return;
		}

		NavMeshTriangle triangle = getTriangle();
		if(triangle == null)
		{
			player.sendMessage("Giving info failed. No triangle at current location.");
			return;
		}

		player.sendMessage("Triangle '"+triangle.getId()+"' in mesh '"+mesh.getId()+"':");
	}


	// create ladder
	public void ladder()
	{
		if(player.isSneaking())
			removeLadder();
		else
			createLadder();
	}

	private void createLadder()
	{
		NavMeshTriangle triangle = getTriangle();
		if(triangle == null)
		{
			player.sendMessage("Creating ladder failed. The point has to be on a triangle.");
			return;
		}

		// first point
		if(ladderTriangle == null)
		{
			ladderTriangle = triangle;
			ladderPosition = convertLocationToVector(player.getLocation());
			return;
		}

		// second point
		Vector3D otherLadderPosition = convertLocationToVector(player.getLocation());
		Direction2D ladderDirection = Direction2D.getFromYaw(player.getLocation().getYaw());
		player.sendMessage("Ladder orientation: "+ladderDirection);

		NavMeshEdgeLadder ladder = createLadder(triangle, otherLadderPosition, ladderDirection);
		addLadderToNavMesh(triangle, ladder);

		ladderTriangle = null;
		ladderPosition = null;
	}

	private NavMeshEdgeLadder createLadder(NavMeshTriangle triangle, Vector3D otherLadderPosition, Direction2D direction)
	{
		NavMeshTriangle bottomTriangle = (triangle.getCenter().getY() < ladderTriangle.getCenter().getY()) ?
				triangle :
				ladderTriangle;
		NavMeshTriangle topTriangle = (triangle.getCenter().getY() < ladderTriangle.getCenter().getY()) ?
				ladderTriangle :
				triangle;
		Vector3D bottomLadderLocation = (otherLadderPosition.getY() < ladderPosition.getY()) ?
				otherLadderPosition :
				ladderPosition;
		Vector3D topLadderLocation = (otherLadderPosition.getY() < ladderPosition.getY()) ? ladderPosition : otherLadderPosition;

		return new NavMeshEdgeLadder(bottomTriangle, topTriangle, bottomLadderLocation, topLadderLocation, direction);
	}

	private void addLadderToNavMesh(NavMeshTriangle triangle, NavMeshEdgeLadder ladder)
	{
		NavMesh navMesh = getNavMesh();
		for(NavMeshTriangle t : navMesh.getTriangles())
			if(t.equals(triangle) || t.equals(ladderTriangle))
				t.addEdge(ladder);
	}

	private void removeLadder()
	{
		// canceling ladder creation
		if(ladderTriangle != null)
		{
			player.sendMessage("Cancelled ladder creation.");

			ladderTriangle = null;
			ladderPosition = null;
			return;
		}

		// deleting triangle
		NavMeshTriangle triangle = getTriangle();
		if(triangle == null)
		{
			player.sendMessage("Deleting ladder failed. No triangle nearby.");
			return;
		}

		NavMeshEdgeLadder ladder = getNearestNavMeshLadder(triangle);
		if(ladder == null)
		{
			player.sendMessage("Deleting ladder failed. No ladder on this triangle.");
			return;
		}

		for(NavMeshTriangle t : getNavMesh().getTriangles())
			if(t.getEdges().contains(ladder))
				t.removeEdge(ladder);
	}


	// UTIL
	private Vector3D convertLocationToVector(Location location)
	{
		return new Vector3D(location.getX(), location.getY(), location.getZ());
	}

	private Location convertVectorToLocation(Vector3D vector3D, World world)
	{
		return new Location(world, vector3D.getX(), vector3D.getY(), vector3D.getZ());
	}

}
