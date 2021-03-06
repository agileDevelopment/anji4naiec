/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jgap;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgap.event.GeneticEvent;
import org.jgap.impl.FitnessReproductionOperator;
import org.jgap.impl.FitnessSelector;
import org.jgap.impl.NoveltyReproductionOperator;

/**
 *
 * @author Brian Woolley <brian.woolley at ieee.org>
 */
class Population {

    /**
     *
     */
    private List<Chromosome> m_chromsomes = new LinkedList<Chromosome>();
    /**
     * Species that makeup this Genotype's population.
     */
    private List<Specie> m_species = new LinkedList<Specie>();
    /**
     * The current active Configuration instance.
     */
    private Configuration m_activeConfiguration;

    Population(Configuration an_activeConfiguration) {
        assert (an_activeConfiguration != null);
        m_activeConfiguration = an_activeConfiguration;
    }

    /**
     *
     * @return
     */
    public int getPopulationSize() {
        return m_chromsomes.size();
    }

    /**
     *
     * @return
     */
    public List<Chromosome> getChromosomes() {
        return m_chromsomes;
    }

    public void setChromosomes(List<Chromosome> aNewChromosomeSet) {
        m_chromsomes.clear();
        updateSpecies();
        addChromosomes(aNewChromosomeSet);
        updateSpecies();
    }

    public void appendChromosomes(List<Chromosome> newChromosomes) {
        updateSpecies();
        for (Chromosome newChrom : newChromosomes) {
            if (!m_chromsomes.contains(newChrom)) {
                addChromosome(newChrom);
            }
        }
        updateSpecies();
    }

    /**
     * @param chromosomeMaterial
     * <code>Collection</code> contains ChromosomeMaterial objects
     * @see Genotype#addChromosomeFromMaterial(ChromosomeMaterial)
     */
    public void addChromosomesFromMaterial(Collection<ChromosomeMaterial> chromosomeMaterial) {
        for (ChromosomeMaterial cMat : chromosomeMaterial) {
            addChromosomeFromMaterial(cMat);
        }
    }

    /**
     * @param cMat chromosome material from which to construct new chromosome
     * object
     * @see Genotype#addChromosome(Chromosome)
     */
    public void addChromosomeFromMaterial(ChromosomeMaterial cMat) {
        Chromosome chrom = new Chromosome(cMat, m_activeConfiguration.nextChromosomeId());
        addChromosome(chrom);
    }

    /**
     * @param chromosomes
     * <code>Collection</code> contains Chromosome objects
     * @see Genotype#addChromosome(Chromosome)
     */
    public void addChromosomes(Collection<Chromosome> chromosomes) {
        for (Chromosome chrom : chromosomes) {
            addChromosome(chrom);
        }
    }

    /**
     * add chromosome to population and to appropriate specie
     *
     * @param chrom
     */
    public void addChromosome(Chromosome chrom) {
        m_chromsomes.add(chrom);
//        if (!chromosomeIdArchive.contains(chrom.getId())) {
//            chromosomeIdArchive.add(chrom.getId());
//        }

        // specie collection
        boolean added = false;
        for (Specie specie : m_species) {
            if (specie.match(chrom)) {
                specie.add(chrom);
                added = true;
                break;
            }
        }
        if (!added) {
            m_species.add(new Specie(m_activeConfiguration.getSpeciationParms(), chrom));
        }
    }

    /**
     * @return List contains Specie objects
     */
    public List<Specie> getSpecies() {
        return m_species;
    }

    /*
     * ------------------------------ EVOLUTIONARY OPERATIONS
     * ------------------------------
     */
    public void updateSpecies() {
        List<Specie> emptySpecies = new LinkedList<Specie>();
        // Culls old chromosomes from each species and removes empty species.
        // -------------------------------------
        for (Specie s : m_species) {
            s.cull(m_chromsomes);
            if (s.isEmpty()) {
                emptySpecies.add(s);
            }
        }
        m_species.removeAll(emptySpecies);
    }

    public Chromosome getFittestChromosome() {
        Chromosome champ = null;
        for (Chromosome next : m_chromsomes) {
            if (champ == null) {
                champ = next;
            }

            if (next.getFitnessValue() > champ.getFitnessValue()) {
                champ = next;
            }
        }
        return champ;
    }

    public Chromosome getMostNovelChromosome() {
        Chromosome champ = null;
        for (Chromosome next : m_chromsomes) {
            if (champ == null) {
                champ = next;
            }

            if (next.getNoveltyValue() > champ.getNoveltyValue()) {
                champ = next;
            }
        }
        return champ;
    }

    public void evaluateForNovelty() {
        // Evaluate the new population of of chromosomes.
        // -------------------------------------
        evaluateForFitness();
        m_activeConfiguration.getBulkFitnessFunction().evaluateNovelty(m_chromsomes);
    }

    private void removeLeastNovelIndividual() {
        Collections.sort(m_chromsomes, new ChromosomeNoveltyComparator(true, true));
        for (int i = 0; i < m_chromsomes.size(); i++) {
            if (m_chromsomes.get(i).isProtected()) {
                // Do nothing
            } else {
                m_chromsomes.remove(i);
                break;
            }
        }
        updateSpecies();
    }

    private void reproduceForNovelty(int offspringCount) {
        try {
            // Execute Reproduction Operators.
            // -------------------------------------
            List<ChromosomeMaterial> offspring = new ArrayList<ChromosomeMaterial>();
            for (NoveltyReproductionOperator operator : m_activeConfiguration.getNoveltyReproductionOperators()) {
                operator.reproduce(m_activeConfiguration, m_species, offspring);
            }

            adjustChromosomeList(offspring, offspringCount);
            mutateChromosomeMaterial(offspring);

            // add offspring
            // -------------------------------------
            addChromosomesFromMaterial(offspring);

        } catch (InvalidConfigurationException ex) {
            Logger.getLogger(Population.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void replaceLeastNovelIndividual() {
        if (m_chromsomes.size() < m_activeConfiguration.getNoveltySearchPopulationSize()) {
            // do nothing
        } else {
            removeLeastNovelIndividual();
        }
        reproduceForNovelty(1);
        evaluateForNovelty();
    }

    public void evaluateForFitness() {
        // Evaluate the new population of of chromosomes.
        // -------------------------------------
        m_activeConfiguration.getBulkFitnessFunction().evaluateFitness(m_chromsomes);
    }

    /**
     * Removes the chromosome with the lowest speciated fitness score from the
     * current population.
     */
    public void removeLeastFitIndividual() {
        Collections.sort(m_chromsomes, new ChromosomeFitnessComparator(true, true));
        for (int i = 0; i < m_chromsomes.size(); i++) {
            if (m_chromsomes.get(i).isProtected()) {
                // Do nothing
            } else {
                m_chromsomes.remove(i);
                break;
            }
        }
        updateSpecies();
    }

    private void reproduceForFitness(int offspringCount) {
        try {

            // Execute Reproduction Operators.
            // -------------------------------------
            List<ChromosomeMaterial> offspring = new ArrayList<ChromosomeMaterial>();
            for (FitnessReproductionOperator operator : m_activeConfiguration.getFitnessReproductionOperators()) {
                operator.reproduce(m_activeConfiguration, m_species, offspring);
            }

            adjustChromosomeList(offspring, offspringCount);
            mutateChromosomeMaterial(offspring);

            Collections.shuffle(offspring);
            while (offspring.size() > offspringCount) {
                offspring.remove(0);
            }

            // add offspring
            // ------------------------------
            for (ChromosomeMaterial chrom : offspring) {
                addChromosomeFromMaterial(chrom);
            }
        } catch (InvalidConfigurationException ex) {
            Logger.getLogger(Population.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void replaceLeastFitIndividual() {
        if (m_chromsomes.size() < m_activeConfiguration.getFitnessSearchPopulationSize()) {
            // Do Nothing
        } else {
            removeLeastFitIndividual();
        }
        reproduceForFitness(1);
        evaluateForFitness();
    }

    private void mutateChromosomeMaterial(List<ChromosomeMaterial> offspring) throws InvalidConfigurationException {
        // Execute Mutation Operators.
        // -------------------------------------
        for (MutationOperator operator : m_activeConfiguration.getMutationOperators()) {
            operator.mutate(m_activeConfiguration, offspring);
        }
    }

    /**
     * adjust chromosome list to fit population size; first, clone population
     * (starting at beginning of list) until we reach or exceed pop. size or
     * trim excess (from end of list)
     *
     * @param chroms
     * <code>List</code> contains
     * <code>Chromosome</code> objects
     * @param targetSize
     */
    private void adjustChromosomeList(List<ChromosomeMaterial> chroms, int targetSize) throws InvalidConfigurationException {
        List<ChromosomeMaterial> extras = new ArrayList<ChromosomeMaterial>();

        if (chroms.isEmpty()) {
            for (int i = 0; i < targetSize; i++) {
                chroms.add(ChromosomeMaterial.randomInitialChromosomeMaterial(m_activeConfiguration));
            }
        } else {
            for (ChromosomeMaterial cMat : chroms) {
                extras.add(cMat.clone(cMat.getPrimaryParentId()));
            }
        }

        while (chroms.size() < targetSize) {
            for (ChromosomeMaterial cMat : extras) {
                chroms.add(cMat.clone(cMat.getPrimaryParentId()));
            }
        }

        Collections.shuffle(chroms);
        while (chroms.size() > targetSize) {
            chroms.remove(0);
        }
    }

    /**
     * @return
     * <code>String</code> representation of this
     * <code>Genotype</code> instance.
     */
    @Override
    public synchronized String toString() {
        StringBuilder buffer = new StringBuilder();

        Iterator<Chromosome> iter = m_chromsomes.iterator();
        while (iter.hasNext()) {
            Chromosome chrom = iter.next();
            buffer.append(chrom.toString());
            buffer.append(" [");
            buffer.append(chrom.getFitnessValue());
            buffer.append(']');
            buffer.append('\n');
        }

        return buffer.toString();
    }
    private static final long serialVersionUID = 1212096533609834342L;
}
