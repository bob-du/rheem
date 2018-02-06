package org.qcri.rheem.flink.operators;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.io.LocalCollectionOutputFormat;
import org.apache.flink.api.java.io.PrintingOutputFormat;
import org.apache.flink.core.fs.FileSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SequenceFile;
import org.qcri.rheem.basic.channels.FileChannel;
import org.qcri.rheem.basic.operators.LocalCallbackSink;
import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.function.ConsumerDescriptor;
import org.qcri.rheem.core.optimizer.OptimizationContext;
import org.qcri.rheem.core.plan.rheemplan.ExecutionOperator;
import org.qcri.rheem.core.platform.ChannelDescriptor;
import org.qcri.rheem.core.platform.ChannelInstance;
import org.qcri.rheem.core.platform.lineage.ExecutionLineageNode;
import org.qcri.rheem.core.types.DataSetType;
import org.qcri.rheem.core.util.Tuple;
import org.qcri.rheem.core.util.fs.FileSystems;
import org.qcri.rheem.flink.channels.DataSetChannel;
import org.qcri.rheem.flink.compiler.RheemFileOutputFormat;
import org.qcri.rheem.flink.execution.FlinkExecutor;
import org.qcri.rheem.java.channels.CollectionChannel;
import org.qcri.rheem.java.channels.StreamChannel;
import org.qcri.rheem.java.operators.JavaObjectFileSource;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Implementation of the {@link LocalCallbackSink} operator for the Flink platform.
 */
public class FlinkLocalCallbackSink <Type extends Serializable> extends LocalCallbackSink<Type> implements FlinkExecutionOperator {

    /**
     * Creates a new instance.
     *
     * @param callback callback that is executed locally for each incoming data unit
     * @param type     type of the incoming elements
     */
    public FlinkLocalCallbackSink(ConsumerDescriptor.SerializableConsumer<Type> callback, DataSetType type) {
        super(callback, type);
    }

    /**
     * Copies an instance (exclusive of broadcasts).
     *
     * @param that that should be copied
     */
    public FlinkLocalCallbackSink(LocalCallbackSink<Type> that) {
        super(that);
    }

    @Override
    public Tuple<Collection<ExecutionLineageNode>, Collection<ChannelInstance>> evaluate(
            ChannelInstance[] inputs,
            ChannelInstance[] outputs,
            FlinkExecutor flinkExecutor,
            OptimizationContext.OperatorContext operatorContext) {
        assert inputs.length == this.getNumInputs();
        assert outputs.length == this.getNumOutputs();

        final DataSetChannel.Instance input = (DataSetChannel.Instance) inputs[0];
        final DataSet<Type> inputDataSet = input.provideDataSet();



        try {
            if (this.collector != null) {
                //this.collector.add();
                this.collector.addAll(inputDataSet.filter(a -> true).setParallelism(1).collect());
                //final String path = "/root/anis/data/lala";
                //final String path = "/home/migiwara/tmp";

                //inputDataSet.write(new RheemFileOutputFormat<Type>(path), path, FileSystem.WriteMode.OVERWRITE);


                //inputDataSet.filter(a -> true).setParallelism(1).output(new LocalCollectionOutputFormat<Type>(this.collector));
            } else {
                inputDataSet.output(new PrintingOutputFormat<Type>()).setParallelism(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ExecutionOperator.modelEagerExecution(inputs, outputs, operatorContext);
    }

    public void convert(){
        //String path = "/home/migiwara/tmp";
        final String path = "/root/anis/data/lala";

        FlinkLocalCallbackSink.SequenceFileIterator sequenceFileIterator;

        try {
            final String actualInputPath = FileSystems.findActualSingleInputPath(path);
            sequenceFileIterator = new FlinkLocalCallbackSink.SequenceFileIterator<>(actualInputPath);
            Stream<Type> sequenceFileStream =
                    StreamSupport.stream(Spliterators.spliteratorUnknownSize(sequenceFileIterator, 0), false);
            sequenceFileStream.forEach(this.collector::add);
            //((CollectionChannel.Instance) outputs[0]).accept(this.collector);
        } catch (IOException e) {
            throw new RheemException(String.format("%s failed to read from %s.", this, path), e);
        }
    }

    @Override
    protected ExecutionOperator createCopy() {
        return new FlinkLocalCallbackSink<Type>(this);
    }

    @Override
    public String getLoadProfileEstimatorConfigurationKey() {
        return "rheem.flink.localcallbacksink.load";
    }

    @Override
    public List<ChannelDescriptor> getSupportedInputChannels(int index) {
        assert index <= this.getNumInputs() || (index == 0 && this.getNumInputs() == 0);
        return Arrays.asList(DataSetChannel.DESCRIPTOR, DataSetChannel.DESCRIPTOR_MANY);
    }

    @Override
    public List<ChannelDescriptor> getSupportedOutputChannels(int index) {
        throw new UnsupportedOperationException(String.format("%s does not have output channels.", this));
    }

    @Override
    public boolean containsAction() {
        return true;
    }



    ///test

    private static class SequenceFileIterator<T> implements Iterator<T>, AutoCloseable, Closeable {

        private SequenceFile.Reader sequenceFileReader;

        private final NullWritable nullWritable = NullWritable.get();

        private final BytesWritable bytesWritable = new BytesWritable();

        private Object[] nextElements;

        private ArrayList nextElements_cole;

        private int nextIndex;

        SequenceFileIterator(String path) throws IOException {
            final SequenceFile.Reader.Option fileOption = SequenceFile.Reader.file(new Path(path));
            this.sequenceFileReader = new SequenceFile.Reader(new Configuration(true), fileOption);
            Validate.isTrue(this.sequenceFileReader.getKeyClass().equals(NullWritable.class));
            Validate.isTrue(this.sequenceFileReader.getValueClass().equals(BytesWritable.class));
            this.tryAdvance();
        }

        private void tryAdvance() {
            if (this.nextElements != null && ++this.nextIndex < this.nextElements.length) return;
            if (this.nextElements_cole != null && ++this.nextIndex < this.nextElements_cole.size()) return;
            try {
                if (!this.sequenceFileReader.next(this.nullWritable, this.bytesWritable)) {
                    this.nextElements = null;
                    return;
                }
                Object tmp = new ObjectInputStream(new ByteArrayInputStream(this.bytesWritable.getBytes())).readObject();
                if(tmp instanceof Collection) {
                    this.nextElements = null;
                    this.nextElements_cole = (ArrayList) tmp;
                }else if(tmp instanceof Object[]){
                    this.nextElements = (Object[]) tmp;
                    this.nextElements_cole = null;
                }else {
                    this.nextElements = new Object[1];
                    this.nextElements[0] = tmp;

                }
                this.nextIndex = 0;
            } catch (IOException | ClassNotFoundException e) {
                this.nextElements = null;
                IOUtils.closeQuietly(this);
                throw new RheemException("Reading failed.", e);
            }
        }

        @Override
        public boolean hasNext() {
            return this.nextElements != null || this.nextElements_cole != null;
        }

        @Override
        public T next() {
            Validate.isTrue(this.hasNext());
            @SuppressWarnings("unchecked")
            final T result;
            if(this.nextElements_cole != null){
                result = (T) this.nextElements_cole.get(this.nextIndex);
            }else if (this.nextElements != null) {
                result = (T) this.nextElements[this.nextIndex];
            }else{
                result = null;
            }

            this.tryAdvance();
            return result;
        }

        @Override
        public void close() {
            if (this.sequenceFileReader != null) {
                try {
                    this.sequenceFileReader.close();
                } catch (Throwable t) {
                    LoggerFactory.getLogger(this.getClass()).error("Closing failed.", t);
                }
                this.sequenceFileReader = null;
            }
        }
    }
}
