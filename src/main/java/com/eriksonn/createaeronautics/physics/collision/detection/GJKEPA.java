package com.eriksonn.createaeronautics.physics.collision.detection;

import com.eriksonn.createaeronautics.physics.collision.shape.ICollisionShape;
import com.eriksonn.createaeronautics.utils.math.Quaternionf;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * GJK & Expanding Polytope algorithm.
 *
 * @author RyanHCode
 */
public class GJKEPA {

    /**
     * Default max simplex evolution iterations before canceling and returning.
     */
    public static int MAX_SIMPLEX_ITERATIONS = 20;

    public static final double EPSILON = 0.00001f;

    /**
     * Creates an ArrayList of Vector3d's
     *
     * @param points The points to add to the list
     * @return The list of points
     */
    public static ArrayList<Vector3d> Vector3dListOf(Vector3d... points) {
        return new ArrayList<Vector3d>(Arrays.asList(points));
    }

    /**
     * Checks if two vectors are in the same direction.
     *
     * @param a The first vector.
     * @param b The second vector.
     * @return True if the vectors are in the same direction.
     */
    public static boolean sameDirection(Vector3d a, Vector3d b) {
        return a.dot(b) > 0;
    }

    /**
     * Evolves the given simplex.
     * @param simplex The existing simplex.
     * @param direction The direction vector.
     */
    private static EvolutionResult evolve(Simplex simplex, Vector3d direction) {
        // Based on the length of the already given simplex, determine how to evolve it
        switch (simplex.size()) {

            // The simplex has a line.
            case 2: {
                return evolveLine(simplex, direction);
            }

            case 3: {
                return evolveTriangle(simplex, direction);
            }

            case 4: {
                return evolveTetrahedron(simplex, direction);
            }

        }

        throw new IllegalStateException("Invalid simplex size: " + simplex.size());
    }

    /**
     * Evolves the given line simplex.
     * @param simplex The existing simplex.
     * @param direction The direction vector.
     */
    private static EvolutionResult evolveLine(Simplex simplex, Vector3d direction) {
        // Get points on simplex
        Support a = simplex.get(0);
        Support b = simplex.get(1);

        // Vector from a -> b, or the line vector
        Vector3d ab = b.difference.subtract(a.difference);

        // Vector from a -> origin
        Vector3d ao = Vector3d.ZERO.subtract(a.difference);

        if(sameDirection(ab, ao)) {
            direction = ab.cross(ao).cross(ab);
        } else {
            simplex.clear();
            simplex.add(0, a);
            if(simplex.size() > 4) simplex.remove(simplex.size() - 1);
            direction = ao;
        }

        return new EvolutionResult(false, direction);
    }

    /**
     * Evolves the given triangle simplex.
     * @param simplex The existing simplex.
     * @param direction The direction vector.
     */
    private static EvolutionResult evolveTriangle(Simplex simplex, Vector3d direction) {
        // Get points on simplex
        Support a = simplex.get(0);
        Support b = simplex.get(1);
        Support c = simplex.get(2);

        // Vector from a -> b, an edge vector
        Vector3d ab = b.difference.subtract(a.difference);

        // Vector from a -> b, or the line vector
        Vector3d ac = c.difference.subtract(a.difference);

        // Vector from a -> origin
        Vector3d ao = Vector3d.ZERO.subtract(a.difference);

        // Cross of all vectors for abc
        Vector3d abc = ab.cross(ac);

        // If abc cross ac is in the same direction as a -> origin
        if (sameDirection(abc.cross(ac), ao)) {
            if (sameDirection(ac, ao)) {
                simplex.clear();
                simplex.addAll(new Simplex(a, c));
                direction = ac.cross(ao).cross(ac);
            } else {
                return evolveLine(new Simplex(a, b), direction);
            }
        } else {
            if (sameDirection(ab.cross(abc), ao)) {
                return evolveLine(new Simplex(a, b), direction);
            } else {
                if (sameDirection(abc, ao)) {
                    direction = abc;
                }

                else {
                    simplex.clear();
                    simplex.addAll(new Simplex(a, c, b));
                    direction = Vector3d.ZERO.subtract(abc);
                }
            }
        }

        return new EvolutionResult(false, direction);
    }

    /**
     * Evolves the given tetrahedron simplex.
     *
     * @param simplex The existing simplex.
     * @param direction The direction vector.
     */
    private static EvolutionResult evolveTetrahedron(Simplex simplex, Vector3d direction) {

        // First, get all points from the simplex that we are assured to have
        Support a = simplex.get(0),
             b = simplex.get(1),
             c = simplex.get(2),
             d = simplex.get(3);

        // Get their coordinates
        Vector3d axyz = a.difference,
                bxyz = b.difference,
                cxyz = c.difference,
                dxyz = d.difference;

        // Next, get the direction from A to the other points & origin
        Vector3d ab = bxyz.subtract(axyz),
             ac = cxyz.subtract(axyz),
             ad = dxyz.subtract(axyz),
             ao = Vector3d.ZERO.subtract(axyz);

        // Then get the crosses
        Vector3d abc = ab.cross(ac),
             adb = ad.cross(ab),
             acd = ac.cross(ad);

        // If abc is in the same direction as a -> origin, return the evolution of the triangle a, b, c
        if (sameDirection(abc, ao)) {
            return evolveTriangle(new Simplex(a, b, c), direction);
        }

        // If acd is in the same direction as a -> origin, return the evolution of the triangle a, c, d
        if (sameDirection(acd, ao)) {
            return evolveTriangle(new Simplex(a, c, d), direction);
        }

        // If adb is in the same direction as a -> origin, return the evolution of the triangle a, d, b
        if (sameDirection(adb, ao)) {
            return evolveTriangle(new Simplex(a, d, b), direction);
        }

        return new EvolutionResult(true, direction);
    }

    /**
     * Test collision between two shapes with GJK & EPA.
     *
     * @param a The first shape.
     * @param b The second shape.
     * @return A nullable manifold collision result.
     */
    public static Manifold collisionTest(ICollisionShape a, ICollisionShape b, int maxIterations) {
        Simplex simplex = new Simplex();

        // Starting direction for support points.
        Vector3d direction = a.getCenter().subtract(b.getCenter()).normalize();

        // Find initial support point, the first point of the simplex.
        Support initialSupport = Support.generate(a, b, direction);

        // Add the initial support point to the simplex.
        simplex.add(0, initialSupport);

        // Next direction is the direction to the origin from the initial support point.
        direction = Vector3d.ZERO.subtract(initialSupport.difference).normalize();

        // Iterate until the maximum iterations are reached or success/failure is determined.
        for(int i = 0; i < maxIterations; i++) {
            Support support = Support.generate(a, b, direction);

            if(support.difference.dot(direction) < 0) {
                // This means that the point did NOT pass the origin
                // Therefore is it impossible for the minkowski difference to contain the origin.
                // The shapes do not collide.
                return null;
            }

            // Add this vertex to the simplex.
            simplex.add(0, support);
            if(simplex.size() > 4) simplex.remove(simplex.size() - 1);

            // Evolve the simplex.
            EvolutionResult result = evolve(simplex, direction);
            direction = result.direction.normalize();

            if(result.complete) {
                try {
                    return EPA(simplex, a, b, EPSILON);
                } catch(Exception e) {
                    // print output
                    System.out.println("EPA failed");
                    System.out.println("Simplex: " + simplex);
                    System.out.println("Direction: " + direction);
                    System.out.println("Support: " + support);
                    System.out.println("Result: " + result);
                    e.printStackTrace();
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Normal calculation result for EPA
     */
    private static class EPANormalResult {
        public final List<NormalEntry> normals;
        public int minimumDistanceTriangle;
        public final double minimumDistance;

        public EPANormalResult(List<NormalEntry> normals, int minimumDistanceTriangle, double minimumDistance) {
            this.normals = normals;
            this.minimumDistanceTriangle = minimumDistanceTriangle;
            this.minimumDistance = minimumDistance;
        }

        /**
         * A normal result entry, consiting of a normal and the distance from the origin of this face.
         */
        public static class NormalEntry {
            public final Vector3d normal;
            public final double distance;
            public final int a, b, c;

            public NormalEntry(int a, int b, int c, Vector3d normal, double distance) {
                this.a = a;
                this.b = b;
                this.c = c;
                this.normal = normal;
                this.distance = distance;
            }
        }
    }

    /**
     * An edge with a start and end index
     */
    private static class Edge {
        public final int start, end;

        public Edge(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Edge edge = (Edge) other;
            return (start == edge.start && end == edge.end) || (start == edge.end && end == edge.start);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }

    /**
     * Expanding polytope algorithm for obtaining collision normal vector.
     *
     * @param simplex The given tetrahedron simplex from GJK evaluation.
     * @param a The first shape.
     * @param b The second shape.
     */
    public static Manifold EPA(Simplex simplex, ICollisionShape a, ICollisionShape b, double epsilon) {

        // At this point in the algorithm, the simplex is guaranteed to:
        // * Have a size of 4, as a tetrahedron.
        // * Be completely made of support points in the minkowski difference.
        // * Contain the origin

        // As the simplex is now going to be expanded, it will be classified as a polytope.
        Simplex polytope = simplex;

        // Triangles represented as pairs of 3 indexes of vertices in the polytope.
        List<Integer> triangles = new ArrayList<Integer>(Arrays.asList(
                0, 1, 2,
                0, 3, 1,
                0, 2, 3,
                1, 3, 2
        ));

        // Get the normals of the triangles.
        EPANormalResult normalResult = getNormals(polytope, triangles);

        // Get the normal entries
        List<EPANormalResult.NormalEntry> normals = normalResult.normals;

        // Minimum distance from the last iteration.
        double minDistance = Double.MAX_VALUE;

        // Number of iterations
        int iterations = 0;

        while (minDistance == Double.MAX_VALUE) {
            // Get the closest face
            int closestFace = normalResult.minimumDistanceTriangle * 3;

            minDistance = normals.get(normalResult.minimumDistanceTriangle).distance;

            // Find a new support point in the direction of the normal
            Support support = Support.generate(a, b, normals.get(normalResult.minimumDistanceTriangle).normal);

            double minFaceDistance = normals.get(normalResult.minimumDistanceTriangle).normal.dot(support.difference);

            if (Math.abs(minFaceDistance - minDistance) > epsilon) {
                minDistance = Double.MAX_VALUE;

                // Add the support point to the polytope
                polytope.add(support);

                // Index of the new vertex
                int supportIndex = polytope.size() - 1;

                // List of unique edges(for forming new triangles while expanding.
                List<Edge> uniqueEdges = new ArrayList<Edge>();

                // List of duplicate edges
                List<Edge> duplicateEdges = new ArrayList<Edge>();

                // List of triangles to remove
                List<Integer> trianglesToRemove = new ArrayList<Integer>();

                uniqueEdges.add(new Edge(triangles.get(closestFace), triangles.get(closestFace + 1)));
                uniqueEdges.add(new Edge(triangles.get(closestFace), triangles.get(closestFace + 2)));
                uniqueEdges.add(new Edge(triangles.get(closestFace + 1), triangles.get(closestFace + 2)));

                // Remove the closest face
                // Removing same index due to shifting
                triangles.remove(closestFace);
                triangles.remove(closestFace);
                triangles.remove(closestFace);
                normals.remove(normalResult.minimumDistanceTriangle);

                // Initial size of the triangles list
                int trianglesSize = triangles.size();

                normalResult = getNormals(polytope, triangles);
                normals = normalResult.normals;

                // Remove all faces that can see this support point
                for (int i = 0; i < trianglesSize; i += 3) {
                    // Get the normal entry at this triangle
                    EPANormalResult.NormalEntry entry = normals.get(i / 3);

                    // Check if the support point is in the positive halfspace of this normal
                    boolean seenBySupport = entry.normal.dot(support.difference.subtract(polytope.get(entry.a).difference)) > 0;

                    // Remove this triangle
                    if (seenBySupport) {
                        // Add an edge to the unique edges if it doesn't exist
                        Edge ab = new Edge(entry.a, entry.b);
                        Edge bc = new Edge(entry.b, entry.c);
                        Edge ca = new Edge(entry.c, entry.a);

                        if (uniqueEdges.contains(ab)) {
                            duplicateEdges.add(ab);
                            uniqueEdges.remove(ab);
                        } else if (!duplicateEdges.contains(ab)) {
                            uniqueEdges.add(ab);
                        }

                        if (uniqueEdges.contains(bc)) {
                            duplicateEdges.add(bc);
                            uniqueEdges.remove(bc);
                        } else if (!duplicateEdges.contains(bc)) {
                            uniqueEdges.add(bc);
                        }

                        if (uniqueEdges.contains(ca)) {
                            duplicateEdges.add(ca);
                            uniqueEdges.remove(ca);
                        } else if (!duplicateEdges.contains(ca)) {
                            uniqueEdges.add(ca);
                        }

                        triangles.remove(i);
                        triangles.remove(i);
                        triangles.remove(i);

                        normals.remove(i / 3);
                        i -= 3;
                        trianglesSize -= 3;
                    }
                }

                // Iterate over the unique edges to form new triangles
                // (this is the expanding part)
                for (Edge edge : uniqueEdges) {
                    // Get the vertices of the edge
                    int pointA = edge.start;
                    int pointB = edge.end;
                    int pointC = supportIndex;

                    // Add the triangle!
                    triangles.add(pointA);
                    triangles.add(pointB);
                    triangles.add(pointC);
                }

                // Get the normals of the triangles again.
                normalResult = getNormals(polytope, triangles);

                // Get the normal entries
                normals = normalResult.normals;

                if (iterations++ > GJKEPA.MAX_SIMPLEX_ITERATIONS) {
                    break;
                }
            }
        }

        if(minDistance == Double.POSITIVE_INFINITY) {
            return null;
        }

        Vector3d collisionNormal = normals.get(normalResult.minimumDistanceTriangle).normal.normalize();
        double collisionDepth = minDistance + epsilon;

        // The collision depth is now known
        // We can now calculate the contact points for colliders A and B
        // First, find the support points that formed the last triangle
        Support supportA = polytope.get(normals.get(normalResult.minimumDistanceTriangle).a),
                supportB = polytope.get(normals.get(normalResult.minimumDistanceTriangle).b),
                supportC = polytope.get(normals.get(normalResult.minimumDistanceTriangle).c);

        // Project the origin onto the plane of the triangle in minkowski space
        Vector3d originProjection = collisionNormal.scale(-collisionDepth);

        // Get the barycentric coordinates of the origin projection on both triangles the difference was formed from
        Vector3d barycentric = getBarycentricCoordinates(originProjection, supportA.difference, supportB.difference, supportC.difference, collisionNormal);

        // Use the barycentric coordinates to find the contact points on both colliders
        Vector3d contactPointA = supportA.pointA.scale(barycentric.x).add(supportB.pointA.scale(barycentric.y)).add(supportC.pointA.scale(barycentric.z));
        Vector3d contactPointB = supportA.pointB.scale(barycentric.x).add(supportB.pointB.scale(barycentric.y)).add(supportC.pointB.scale(barycentric.z));

        // If the barycentric coordinates are not valid, then the origin projection is not on the triangle
        if(barycentric.x < -epsilon || barycentric.y < -epsilon || barycentric.z < -epsilon || barycentric.x + barycentric.y + barycentric.z > 1 + (epsilon * 3)) {
            return null;
        }

        if(collisionNormal.equals(Vector3d.ZERO) || Double.isNaN(contactPointA.x)) {
            return null;
        }

        return new Manifold(collisionNormal, collisionDepth, contactPointA, contactPointB);

    }

    /**
     * Calculates the barycentric coordinates of a point on a given triangle.
     *
     * @param point The point to calculate the barycentric coordinates of. Assumed to be on the triangle
     * @param a The first vertex of the triangle
     * @param b The second vertex of the triangle
     * @param c The third vertex of the triangle
     * @param normal The normal of the triangle
     * @return The barycentric coordinates of the point on the triangle
     */
    public static Vector3d getBarycentricCoordinates(Vector3d point, Vector3d a, Vector3d b, Vector3d c, Vector3d normal) {
        double areaABC = normal.dot((b.subtract(a).cross((c.subtract(a)))));
        double areaPBC = normal.dot((b.subtract(point).cross((c.subtract(point)))));
        double areaPCA = normal.dot((c.subtract(point).cross((a.subtract(point)))));

        Vector3d barycentric = new Vector3d(areaPBC / areaABC, areaPCA / areaABC, 1.0 - (areaPBC / areaABC)  - (areaPCA / areaABC));

        return barycentric;
    }

    /**
     * Calculate the normals of the given triangles on a polytope.
     * @param polytope The given polytope.
     * @param triangles The triangles to calculate the normals of.
     * @return The normals of the given triangles.
     */
    private static EPANormalResult getNormals(Simplex polytope, List<Integer> triangles) {
        List<EPANormalResult.NormalEntry> normals = new ArrayList<>();

        double minimumDistance = Double.MAX_VALUE;
        int minimumDistanceTriangle = 0;

        for(int i = 0; i < triangles.size(); i += 3) {
            // Get all 3 vertices of the triangle.
            int a = triangles.get(i),
                b = triangles.get(i + 1),
                c = triangles.get(i + 2);

            // Get the normal of this triangle.
            Vector3d normal = polytope.get(b).difference
                    .subtract(polytope.get(a).difference)
                    .cross(
                            polytope.get(c).difference
                                    .subtract(polytope.get(a).difference)
                    )
                    .normalize();

            // Get the distance from the origin to the plane of this triangle.
            double distance = polytope.get(a).difference.dot(normal);

            // If this distance is less than 0, reverse the normal to face outwards relative to the origin.
            // Distance will need to be negated aswell
            if(distance < 0) {
                normal = normal.scale(-1);
                distance = -distance;
            }

            // If this distance is less than the minimum distance, set this as the minimum distance.
            if(distance < minimumDistance) {
                minimumDistance = distance;
                minimumDistanceTriangle = i / 3;
            }

            // Add this normal to the list of normals.
            normals.add(new EPANormalResult.NormalEntry(a, b, c, normal, distance));
        }

        return new EPANormalResult(normals, minimumDistanceTriangle, minimumDistance);
    }

    public static Vector3d rotateQuaternion (Vector3d vector, Quaternionf rotation)
    {
        Quaternionf vectorAsQuaternion = new Quaternionf((float)vector.x, (float)vector.y, (float)vector.z, 0.0f);
        Quaternionf mut = rotation.copy();
        vectorAsQuaternion.mul(mut) ;
        mut.conj();
        mut.mul(vectorAsQuaternion);
        return new Vector3d(mut.i(), mut.j(), mut.k());
    }

    public static Vector3d rotateQuaternionReverse(Vector3d vector, Quaternionf Q) {
        Quaternionf vectorAsQuaternion = new Quaternionf((float)vector.x, (float)vector.y, (float)vector.z, 0.0f) ;
        Quaternionf mut = Q.copy();
        mut.conj();
        vectorAsQuaternion.mul(mut);
        mut.conj();
        mut.mul(vectorAsQuaternion);
        return new Vector3d(mut.i(), mut.j(), mut.k());
    }
}
