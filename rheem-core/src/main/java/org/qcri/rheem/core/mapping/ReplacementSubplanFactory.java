package org.qcri.rheem.core.mapping;

import org.qcri.rheem.core.plan.rheemplan.Operator;
import org.qcri.rheem.core.plan.rheemplan.Subplan;

/**
 * This factory takes an {@link SubplanMatch} and derives a replacement {@link Subplan} from it.
 */
public abstract class ReplacementSubplanFactory {

    public Operator createReplacementSubplan(SubplanMatch subplanMatch, int epoch) {
        final Operator replacementSubplan = this.translate(subplanMatch, epoch);
        this.checkSanity(subplanMatch, replacementSubplan);
        return replacementSubplan;
    }

    protected void checkSanity(SubplanMatch subplanMatch, Operator replacementSubplan) {
        if (replacementSubplan.getNumInputs() != subplanMatch.getPattern().getNumInputs()) {
            throw new IllegalStateException("Incorrect number of inputs in the replacement subplan.");
        }
        if (replacementSubplan.getNumOutputs() != subplanMatch.getPattern().getNumOutputs()) {
            throw new IllegalStateException("Incorrect number of outputs in the replacement subplan.");
        }
    }

    protected abstract Operator translate(SubplanMatch subplanMatch, int epoch);

}
