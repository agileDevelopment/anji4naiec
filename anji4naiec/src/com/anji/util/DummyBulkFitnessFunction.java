/**
 * ----------------------------------------------------------------------------|
 * Created on Apr 12, 2003
 */
package com.anji.util;

import java.awt.geom.Point2D;
import java.util.*;
import org.jgap.BehaviorVector;

import org.jgap.EvaluationFunction;
import org.jgap.Chromosome;

/**
 * @author Philip Tucker
 */
public class DummyBulkFitnessFunction implements EvaluationFunction {

    private Random rand = null;

    /**
     * ctor
     *
     * @param newRand
     */
    public DummyBulkFitnessFunction(Random newRand) {
        rand = newRand;
    }

    /**
     * ctor
     */
    public DummyBulkFitnessFunction() {
        rand = new Random();
    }

    @Override
    public void evaluateFitness(Chromosome a_subject) {
        a_subject.setFitnessValue(rand.nextInt(100));
    }

    /**
     * @param aSubjects
     * @see org.jgap.EvaluationFunction#evaluateFitness(java.util.List)
     */
    @Override
    public void evaluateFitness(List<Chromosome> aSubjects) {
        for (Chromosome c : aSubjects) {
            evaluateFitness(c);
        }
    }

    /**
     * @see org.jgap.EvaluationFunction#getMaxFitnessValue()
     */
    @Override
    public int getMaxFitnessValue() {
        return 100;
    }

    @Override
    public void evaluateNovelty(List<Chromosome> subjects) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getNoveltyThreshold() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setNoveltyThreshold(double aNewNoveltyThreshold) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getNoveltyArchiveSize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void evaluate(List<Chromosome> subjects) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void evaluate(Chromosome subject) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<Chromosome, BehaviorVector> getAllPointsVisited() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
