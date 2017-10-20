package org.qcri.rheem.profiler.core.api;

import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.optimizer.DefaultOptimizationContext;
import org.qcri.rheem.core.optimizer.OptimizationContext;
import org.qcri.rheem.core.plan.executionplan.Channel;
import org.qcri.rheem.core.plan.executionplan.PlatformExecution;
import org.qcri.rheem.core.plan.rheemplan.ExecutionOperator;
import org.qcri.rheem.core.platform.ChannelDescriptor;
import org.qcri.rheem.core.platform.ChannelInstance;
import org.qcri.rheem.core.platform.Executor;
import org.qcri.rheem.core.platform.Platform;
import org.qcri.rheem.core.util.RheemArrays;
import org.qcri.rheem.core.util.RheemCollections;
import org.qcri.rheem.java.channels.CollectionChannel;
import org.qcri.rheem.java.channels.StreamChannel;
import org.qcri.rheem.java.execution.JavaExecutor;
import org.qcri.rheem.java.operators.JavaExecutionOperator;
import org.qcri.rheem.profiler.spark.SparkOperatorProfiler;
import org.qcri.rheem.profiler.util.ProfilingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Allows to instrument an {@link ExecutionOperator}.
 */
public abstract class OperatorProfiler {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public int cpuMhz;

    protected Supplier<? extends ExecutionOperator> operatorGenerator;

    public ExecutionOperator getOperator() {
        return operator;
    }

    public void setOperator(ExecutionOperator operator) {
        this.operator = operator;
    }

    protected ExecutionOperator operator;

    protected Executor executor;

    protected List<Supplier<?>> dataQuantumGenerators;

    protected Platform platform;

    public int getUDFcomplexity() {
        return UDFcomplexity;
    }

    public void setUDFcomplexity(int UDFcomplexity) {
        this.UDFcomplexity = UDFcomplexity;
    }

    public void setDataQuantumGenerators(Supplier<?> dataQuantumGenerators) {
        this.dataQuantumGenerators = Arrays.asList(dataQuantumGenerators);
    }
    private int UDFcomplexity;

    public OperatorProfiler(){
    }

    public OperatorProfiler(Supplier<? extends ExecutionOperator> operatorGenerator,
                            Supplier<?>... dataQuantumGenerators) {
        this.platform = operatorGenerator.get().getPlatform();
        this.operatorGenerator = operatorGenerator;
        this.dataQuantumGenerators = Arrays.asList(dataQuantumGenerators);
        // Assign the operator
        this.operator=this.operatorGenerator.get();
        //this.executor = ProfilingUtils.fakeJavaExecutor();
        this.cpuMhz = Integer.parseInt(System.getProperty("rheem.java.cpu.mhz", "2700"));
    }

    public void prepare( long dq,long... inputCardinalities) {
    }


    //public void prepare( long... inputCardinalities) {
    //}

    public OperatorProfiler.Result run() {
        List<Long> inputCard = null;
        return  new Result( inputCard,0,0,0,0,0,
                0,0);
    }

    public void cleanUp() {
    }

    public static OperatorProfiler.Result averageResult(List<OperatorProfiler.Result> allResults){
        long diskBytes=0;
        long networkBytes=0;
        long cpuCycles=0;
        long wallclockMillis=0;
        long outputCardinality=0;
        for(OperatorProfiler.Result result:allResults){
            diskBytes+=result.getDiskBytes();
            networkBytes+=result.getNetworkBytes();
            cpuCycles+=result.getCpuCycles();
            wallclockMillis+=result.getWallclockMillis();
            outputCardinality+=result.getOutputCardinality();
        }
        diskBytes=diskBytes/allResults.size();
        networkBytes=networkBytes/allResults.size();
        cpuCycles=cpuCycles/allResults.size();
        wallclockMillis=wallclockMillis/allResults.size();
        outputCardinality=outputCardinality/allResults.size();

        return new OperatorProfiler.Result(allResults.get(0).getInputCardinalities(),outputCardinality,wallclockMillis,diskBytes,networkBytes,cpuCycles,allResults.get(0).getNumMachines(),
                allResults.get(0).getNumCoresPerMachine());
    }

    /**
     * The result of a single profiling run.
     */
    public static class Result {

        private final List<Long> inputCardinalities;

        public int getNumMachines() {
            return numMachines;
        }

        public int getNumCoresPerMachine() {
            return numCoresPerMachine;
        }

        private final int numMachines, numCoresPerMachine;

        private final long outputCardinality;

        private final long diskBytes, networkBytes;

        private final long cpuCycles;

        private final long wallclockMillis;

        private long dataQuantaSize;

        public long getWallclockMillis() {
            return wallclockMillis;
        }

        private int udfComplexity;

        public Result(List<Long> inputCardinalities, long outputCardinality,
                      long wallclockMillis, long diskBytes, long networkBytes, long cpuCycles,
                      int numMachines, int numCoresPerMachine) {
            this.inputCardinalities = inputCardinalities;
            this.outputCardinality = outputCardinality;
            this.wallclockMillis = wallclockMillis;
            this.diskBytes = diskBytes;
            this.networkBytes = networkBytes;
            this.cpuCycles = cpuCycles;
            this.numMachines = numMachines;
            this.numCoresPerMachine = numCoresPerMachine;
        }

        public void setDataQuantaSize(long dataQuantaSize) {
            this.dataQuantaSize = dataQuantaSize;
        }

        public void setUdfComplexity(int udfComplexity) {
            this.udfComplexity = udfComplexity;
        }

        public List<Long> getInputCardinalities() {
            return this.inputCardinalities;
        }

        public long getOutputCardinality() {
            return this.outputCardinality;
        }

        public long getDiskBytes() {
            return this.diskBytes;
        }

        public long getNetworkBytes() {
            return this.networkBytes;
        }

        public long getCpuCycles() {
            return this.cpuCycles;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "inputCardinalities=" + inputCardinalities +
                    ", outputCardinality=" + outputCardinality +
                    ", dataQuantaSize=" + dataQuantaSize+
                    ", udfComplexity=" + udfComplexity +
                    ", numMachines=" + numMachines +
                    ", numCoresPerMachine=" + numCoresPerMachine +
                    ", wallclockMillis=" + wallclockMillis +
                    ", cpuCycles=" + cpuCycles +
                    ", diskBytes=" + diskBytes +
                    ", networkBytes=" + networkBytes +
                    '}';
        }

        public String getCsvHeader() {
            return String.join(",", RheemCollections.map(this.inputCardinalities, (index, card) -> "input_card_" + index)) + "," +
                    "output_card," +
                    "dataQuantaSize," +
                    "udfComplexity," +
                    "wallclock," +
                    "disk," +
                    "network," +
                    "cpu," +
                    "machines," +
                    "cores_per_machine";
        }

        public String toCsvString() {
            return String.join(",", RheemCollections.map(this.inputCardinalities, Object::toString)) + ","
                    + this.outputCardinality + ","
                    + this.dataQuantaSize + ","
                    + this.udfComplexity + ","
                    + this.wallclockMillis + ","
                    + this.diskBytes + ","
                    + this.networkBytes + ","
                    + this.cpuCycles + ","
                    + this.numMachines + ","
                    + this.numCoresPerMachine;
        }
    }

    protected abstract void prepareInput(int inputIndex, long dataQuantaSize ,long inputCardinality);


}
