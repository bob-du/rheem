package org.qcri.rheem.tests;

import org.junit.Test;
import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.optimizer.mloptimizer.api.OperatorProfiler;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Shape;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Topology;
import org.qcri.rheem.profiler.core.*;
import org.qcri.rheem.profiler.core.api.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests log generation of rheem profiler.
 */
public class ProfilingApp {

    private final String[] PROFILE_TYPES = {"single_operator_profiling", "exhaustive_profiling"};

    @Test
    public static void main(String[] args) {

        String profileTesting;
        String platform;
        int maxNodeNumber = 5;

        // Check which profile type
        if (args.length==1)
            profileTesting = args[0];
        else
            profileTesting = "exhaustive_profiling";
        
        List<Topology> topologies;
        ProfilingConfig profilingConfig;
        List< ? extends OperatorProfiler> operatorProfilers;
        List<Shape> shapes;

        if (args.length>=2)
            maxNodeNumber = Integer.valueOf(args[1]);

        if (args.length>=3)
            platform = String.valueOf(args[2]);
        else
            platform = "";

        switch (profileTesting){
            case "single_operator_profiling":
                topologies = TopologyGenerator.generateTopology(1);
                profilingConfig = ProfilingConfigurer.exhaustiveProfilingConfig();
                operatorProfilers = ProfilingPlanBuilder.PlanBuilder(topologies.get(0),profilingConfig);
                ProfilingRunner.SingleOperatorProfiling(operatorProfilers,profilingConfig);
                return;
            case "exhaustive_profiling":
                for(int nodeNumber=maxNodeNumber;nodeNumber<=maxNodeNumber;nodeNumber++){
                    profilingConfig = ProfilingConfigurer.exhaustiveProfilingConfig();
                    if (platform.length()!=0)
                        profilingConfig.setProfilingPlateform(Arrays.asList(new String[]{platform}));
                    // Initialize the generator
                    TopologyGenerator topologyGenerator =new TopologyGenerator(nodeNumber,profilingConfig);

                    // Generate the topologies
                    topologyGenerator.startGeneration();
                    topologies = topologyGenerator.getTopologyList();

                    // Instantiate the topologies
                    InstantiateTopology.instantiateTopology(topologies, nodeNumber);

                    // create shapes
                    shapes = topologies.stream().map(t -> new Shape(t, new Configuration())).collect(Collectors.toList());
                    shapes.stream().forEach(s -> s.populateShape(s.getSinkTopology()));

                    // populate shapes
                    ProfilingPlanBuilder.ProfilingPlanBuilder(shapes,profilingConfig);

                    // Execute sub-shapes [execution plans]
                    ProfilingRunner.exhaustiveProfiling(shapes,profilingConfig);
                }
                return;
        }
    }
}
