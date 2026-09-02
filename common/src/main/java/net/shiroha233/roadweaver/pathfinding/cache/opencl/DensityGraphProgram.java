/* 文件职责：保存可上传到 OpenCL 的紧凑多根 density graph 程序。 */
package net.shiroha233.roadweaver.pathfinding.cache.opencl;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * OpenCL density graph 编译产物。
 */
public record DensityGraphProgram(
        int rootNode,
        Map<DensityGraphRoot, Integer> roots,
        List<Integer> interpolatedNodes,
        List<DensityGraphNode> nodes,
        List<Double> constants,
        OpenCLNoiseTables noiseTables,
        List<OpenCLSpline> splines,
        List<DensityFunction> external2dFields
) {
    public DensityGraphProgram {
        EnumMap<DensityGraphRoot, Integer> copiedRoots = new EnumMap<>(DensityGraphRoot.class);
        if (roots != null) {
            copiedRoots.putAll(roots);
        }
        roots = Map.copyOf(copiedRoots);
        interpolatedNodes = List.copyOf(interpolatedNodes == null ? List.of() : interpolatedNodes);
        nodes = List.copyOf(nodes == null ? List.of() : nodes);
        constants = List.copyOf(constants == null ? List.of() : constants);
        noiseTables = noiseTables == null ? new OpenCLNoiseTables(List.of(), List.of(), List.of()) : noiseTables;
        splines = List.copyOf(splines == null ? List.of() : splines);
        external2dFields = List.copyOf(external2dFields == null ? List.of() : external2dFields);
    }

    public DensityGraphProgram(int rootNode,
                               List<DensityGraphNode> nodes,
                               List<Double> constants,
                               OpenCLNoiseTables noiseTables,
                               List<OpenCLSpline> splines) {
        this(rootNode, Map.of(), List.of(), nodes, constants, noiseTables, splines, List.of());
    }

    public DensityGraphProgram(int rootNode,
                               List<DensityGraphNode> nodes,
                               List<Double> constants,
                               OpenCLNoiseTables noiseTables,
                               List<OpenCLSpline> splines,
                               List<DensityFunction> external2dFields) {
        this(rootNode, Map.of(), List.of(), nodes, constants, noiseTables, splines, external2dFields);
    }

    public DensityGraphProgram(int rootNode,
                               Map<DensityGraphRoot, Integer> roots,
                               List<Integer> interpolatedNodes,
                               List<DensityGraphNode> nodes,
                               List<Double> constants,
                               OpenCLNoiseTables noiseTables,
                               List<OpenCLSpline> splines) {
        this(rootNode, roots, interpolatedNodes, nodes, constants, noiseTables, splines, List.of());
    }

    public int root(DensityGraphRoot root) {
        Integer node = roots.get(root);
        if (node == null) {
            throw new IllegalArgumentException("Density root not compiled: " + root);
        }
        return node;
    }

    public boolean hasRoot(DensityGraphRoot root) {
        return roots.containsKey(root);
    }

    public boolean isEmpty() {
        return rootNode < 0 || nodes.isEmpty();
    }
}
