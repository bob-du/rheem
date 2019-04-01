package org.qcri.rheem.profiler.java;

import org.apache.commons.lang3.Validate;
import org.qcri.rheem.core.plan.rheemplan.Operator;
import org.qcri.rheem.java.channels.JavaChannelInstance;
import org.qcri.rheem.java.operators.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by migiwara on 11/06/17.
 */
public class JavaUnaryOperatorProfiler extends JavaOperatorProfiler {

    private JavaChannelInstance inputChannelInstance, outputChannelInstance;

    public JavaUnaryOperatorProfiler(Supplier<JavaExecutionOperator> operatorGenerator, Supplier<?> dataQuantumGenerator) {
        super(operatorGenerator, dataQuantumGenerator);
    }

    public void prepare(long dataQuata, long... inputCardinalities) {
        Validate.isTrue(inputCardinalities.length == 1);

        super.prepare(dataQuata, inputCardinalities);
        int inputCardinality = (int) inputCardinalities[0];

        // Create input data.
        Collection<Object> dataQuanta = new ArrayList<>(inputCardinality);
        final Supplier<?> supplier = this.dataQuantumGenerators.get(0);
        for (int i = 0; i < inputCardinality; i++) {
            dataQuanta.add(supplier.get());
        }


        // Channel creation separation between operators that requires Collection channels vs Stream channels for the evaluation.
        List operatorsWithCollectionInput = new ArrayList<Class<Operator>>();

        // List of operators requiring collection channels.
        operatorsWithCollectionInput.addAll(Arrays.asList(JavaReduceByOperator.class, JavaGlobalReduceOperator.class, JavaCountOperator.class, JavaMaterializedGroupByOperator.class));

        if (operatorsWithCollectionInput.contains(this.executionOperator.getClass())) {
            // Create Collection channels.
            // Allocate input.
            this.inputChannelInstance = createCollectionChannelInstance(dataQuanta);

            // Allocate output.
            this.outputChannelInstance = createCollectionChannelInstance();
        } else {
            // Create Stream channels.
            // Allocate input.
            this.inputChannelInstance = createChannelInstance(dataQuanta);

            // Allocate output.
            this.outputChannelInstance = createChannelInstance();
        }
    }

    @Override
    protected void prepareInput(int inputIndex, long dataQuantaSize, long inputCardinality) {

    }

    public void prepare(long dataQuantaSize ,long inputCardinalities) {
        //Validate.isTrue(inputCardinalities.length == 1);

        super.prepare(dataQuantaSize, inputCardinalities);
        int inputCardinality = (int) inputCardinalities;

        // Create input data.
        Collection<Object> dataQuanta = new ArrayList<>(inputCardinality);
        final Supplier<?> supplier = this.dataQuantumGenerators.get(0);
        for (int i = 0; i < inputCardinality; i++) {
            dataQuanta.add(supplier.get());
        }

        // Channel creation separation between operators that requires Collection channels vs Stream channels for the evaluation.
        List operatorsWithCollectionInput = new ArrayList<Class< Operator>>();

        // List of operators requiring collection channels.
        operatorsWithCollectionInput.addAll(Arrays.asList(JavaReduceByOperator.class, JavaGlobalReduceOperator.class, JavaCountOperator.class, JavaMaterializedGroupByOperator.class));

        if (operatorsWithCollectionInput.contains(this.executionOperator.getClass())) {
            // Create Collection channels.
            // Allocate input.
            this.inputChannelInstance = createCollectionChannelInstance(dataQuanta);

            // Allocate output.
            this.outputChannelInstance = createCollectionChannelInstance();
        } else {
            // Create Stream channels.
            // Allocate input.
            this.inputChannelInstance = createChannelInstance(dataQuanta);

            // Allocate output.
            this.outputChannelInstance = createChannelInstance();
        }
    }


    public long executeOperator() {
        this.evaluate(
                new JavaChannelInstance[]{this.inputChannelInstance},
                new JavaChannelInstance[]{this.outputChannelInstance}
        );
        return this.outputChannelInstance.provideStream().count();
    }

    @Override
    public JavaExecutionOperator getExecutionOperator() {
        return (JavaExecutionOperator)this.executionOperator;
    }

}
