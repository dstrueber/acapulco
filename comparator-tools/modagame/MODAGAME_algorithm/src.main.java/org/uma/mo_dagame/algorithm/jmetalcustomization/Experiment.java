/*
 * Copyright 2014 Gustavo García Pascual, Mónica Pinto and Lidia Fuentes
 * Copyright 2011 Antonio J. Nebro, Juan J. Durillo
 *
 * This file is part of MO-DAGAME
 * *
 * MO-DAGAME is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MO-DAGAME is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MO-DAGAME.  If not, see <http://www.gnu.org/licenses/>
 */

package org.uma.mo_dagame.algorithm.jmetalcustomization;

import com.carrotsearch.hppc.IntArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jmetal.core.Algorithm;
import jmetal.experiments.util.RBoxplot;
import jmetal.experiments.util.RWilcoxon;
import jmetal.experiments.util.Statistics;
import jmetal.qualityIndicator.Epsilon;
import jmetal.qualityIndicator.GenerationalDistance;
import jmetal.qualityIndicator.Hypervolume;
import jmetal.qualityIndicator.InvertedGenerationalDistance;
import jmetal.qualityIndicator.Spread;
import jmetal.qualityIndicator.util.MetricsUtil;
import jmetal.util.JMException;
import jmetal.util.NonDominatedSolutionList;

/**
 * Abstract class representing jMetal experiments
 */
public abstract class Experiment extends jmetal.experiments.Experiment  {

    public String outputTimeFile_; // Name of the file containing the execution time output
    public String outputFixTimeFile_; // File containing the exec. time spent in the fix operator
    public String outputFrontSizeFile_; // File containing the exec. time spent in the fix operator
    public HashMap<String, Object> map_ = new HashMap<String, Object>();
    /**
     * Constructor
     * <p/>
     * Contains default experiments.settings
     */
    public Experiment() {
        experimentName_ = "noName";

        problemsSettings_ = null;

        map_ = new HashMap<String, Object>();

        algorithmNameList_ = null;
        problemList_ = null;
        paretoFrontFile_ = null;
        indicatorList_ = null;

        // Inicio modificación ReferenceFronts
        frontPath_ = null;
        // Fin modificación ReferenceFronts

        experimentBaseDirectory_ = "";
        paretoFrontDirectory_ = "";
        latexDirectory_ = "latex";

        outputParetoFrontFile_ = "FUN";
        outputParetoSetFile_ = "VAR";
        outputTimeFile_ = "TIME";
        outputFixTimeFile_ = "FIXTIME";
        outputFrontSizeFile_ = "FRONTSIZE";

        algorithmSettings_ = null;
        //algorithm_ = null;

        independentRuns_ = 0;

        indicatorMinimize_ = new HashMap<String, Boolean>();
        indicatorMinimize_.put("HV", false);
        indicatorMinimize_.put("EPSILON", true);
        indicatorMinimize_.put("SPREAD", true);
        indicatorMinimize_.put("GD", true);
        indicatorMinimize_.put("IGD", true);
        indicatorMinimize_.put("TIME", true);
        indicatorMinimize_.put("FIXTIME", true);
        indicatorMinimize_.put("FRONTSIZE", false);

        // Inicio modificación planificación Threads
        problemIndex = 0;
        algorithmIndex = 0;
        irunIndex = 0;
        // Fin modificacion planificación Threads
    } // Constructor

    /**
     * Runs the experiment
     */
    public void runExperiment(int numberOfThreads, boolean saveFirstFront)
            throws JMException, IOException {

        //initExperiment();
        //SolutionSet[] resultFront = new SolutionSet[algorithmNameList_.length];

        // Inicio modificación planificación Threads
        System.out.println("Experiment: Name: " + experimentName_);
        System.out.println("Experiment: creating " + numberOfThreads + " threads");
        System.out.println("Experiment: Number of algorithms: " + algorithmNameList_.length);
        System.out.println("Experiment: Number of problems: " + problemList_.length);
        System.out.println("Experiment: runs: " + independentRuns_);
        System.out.println("Experiment: Experiment directory: " + experimentBaseDirectory_);

        // Fin modificación planificación Threads

        Thread[] p = new RunExperiment[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            //p[i] = new Experiment(map_, i, numberOfThreads, problemList_.length);
            p[i] = new RunExperiment(this, map_, i, numberOfThreads, problemList_.length,
                    saveFirstFront);
            p[i].start();
        }

        try {
            for (int i = 0; i < numberOfThreads; i++) {
                p[i].join();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(Experiment.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Inicio modificación ReferenceFronts
        //generateQualityIndicators();
        // Fin modificación ReferenceFronts
    }

    /**
     * Runs the experiment
     */
    public void runExperiment() throws JMException, IOException {
        runExperiment(1, false);
    } // runExperiment

    public void runExperiment(int numberOfThreads) throws IOException, JMException {
        runExperiment(numberOfThreads, true);
    }

    public void initExperiment() {
        // Step 1: check experiment base directory
        checkExperimentDirectory();

        map_.put("experimentName", experimentName_);
        map_.put("experimentDirectory", experimentBaseDirectory_);
        map_.put("algorithmNameList", algorithmNameList_);
        map_.put("problemList", problemList_);
        map_.put("indicatorList", indicatorList_);
        map_.put("paretoFrontDirectory", paretoFrontDirectory_);
        map_.put("paretoFrontFile", paretoFrontFile_);
        map_.put("independentRuns", independentRuns_);
        map_.put("outputParetoFrontFile", outputParetoFrontFile_);
        map_.put("outputTimeFile", outputTimeFile_);
        map_.put("outputFixTimeFile", outputFixTimeFile_);
        map_.put("outputFrontSizeFile", outputFrontSizeFile_);
        map_.put("outputParetoSetFile", outputParetoSetFile_);
        map_.put("problemsSettings", problemsSettings_);

        // Inicio modificación ReferenceFronts
        frontPath_ = new String[problemList_.length];
        map_.put("frontPath", frontPath_);
        // Fin modificación ReferenceFrons
    }

    public void runCompleteExperiment() throws JMException, IOException {
        runCompleteExperiment(1);
    }

    public void runCompleteExperiment(int numberOfThreads) throws JMException, IOException {
        this.initExperiment();
        this.runExperiment(numberOfThreads);
        this.generateQualityIndicators();
    }

    private void checkExperimentDirectory() {
        File experimentDirectory;

        experimentDirectory = new File(experimentBaseDirectory_);
        if (experimentDirectory.exists()) {
            System.out.println("Experiment directory exists");
            if (experimentDirectory.isDirectory()) {
                System.out.println("Experiment directory is a directory");
            } else {
                System.out.println(
                        "Experiment directory is not a directory. Deleting file and creating directory");
            }
            experimentDirectory.delete();
            new File(experimentBaseDirectory_).mkdirs();
        } // if
        else {
            System.out.println("Experiment directory does NOT exist. Creating");
            new File(experimentBaseDirectory_).mkdirs();
        } // else
    } // checkDirectories

    /**
     * Especifies the experiments.settings of each algorith. This method is checked in each
     * experiment run
     *
     * @param problemName Problem to solve
     * @param problemId   Index of the problem in problemList_
     * @param algorithm   Array containing the algorithms to execute
     * @throws ClassNotFoundException
     */
    public abstract void algorithmSettings(String problemName, int problemId, Algorithm[] algorithm)
            throws ClassNotFoundException;

    public static void main(String[] args) throws JMException, IOException {
    }

    //Inicio modificación ReferenceFronts

    /**
     * Checks if exist paretoFront files
     */
    private void checkParetoFronts(int nThreads) {
        if (paretoFrontDirectory_.equals("") || paretoFrontDirectory_ == null) {
            generateReferenceFrontsParallelizable(nThreads);
        } else {
            IntArrayList frontsToGenerate = new IntArrayList();
            for (int i = 0; i < problemList_.length; i++) {
                if (paretoFrontFile_[i] == null || paretoFrontFile_[i].equals("")) {
                    paretoFrontFile_[i] = "";
                    //generateReferenceFronts(i);
                    frontsToGenerate.add(i);
                } else {
                    String filePath = paretoFrontDirectory_ + "/" + paretoFrontFile_[i];
                    File f = new File(filePath);
                    if (!f.exists()) {
                        paretoFrontFile_[i] = "";
                        frontsToGenerate.add(i);
                        //generateReferenceFronts(i);
                    } else {
                        frontPath_[i] = paretoFrontDirectory_ + "/" + paretoFrontFile_[i];
                    } // if
                } // if
            } // for
            if (frontsToGenerate.size() > 0) {
                generateReferenceFrontsParallelizable(nThreads, frontsToGenerate.toArray());
            }
        } // if
    } // checkParetoFronts

    // Gustavo
    private class QualityIndicatorRunnable implements Runnable {

        private int mAlgorithmIndex;
        private int mProblemIndex;

        public QualityIndicatorRunnable(int algorithmIndex, int problemIndex) {
            mAlgorithmIndex = algorithmIndex;
            mProblemIndex = problemIndex;
        }

        @Override
        public void run() {
            System.out.println("Generating QI for " + algorithmNameList_[mAlgorithmIndex] + " - " +
                    problemList_[mProblemIndex]);
            String algorithmDirectory;
            algorithmDirectory =
                    experimentBaseDirectory_ + "/data/" + algorithmNameList_[mAlgorithmIndex] + "/";

            /* On some experiments, it is possible that TIME, FIXTIME and FRONTSIZE indicators
            are not defined */
            boolean timeIndicator = false;
            boolean fixTimeIndicator = false;
            boolean frontSizeIndicator = false;
            int i = 0;
            while ((!timeIndicator || !fixTimeIndicator || !frontSizeIndicator) &&
                    i < indicatorList_.length) {
                timeIndicator = timeIndicator || indicatorList_[i].equals("TIME");
                fixTimeIndicator = fixTimeIndicator || indicatorList_[i].equals("FIXTIME");
                frontSizeIndicator = frontSizeIndicator || indicatorList_[i].equals("FRONTSIZE");
                i++;
            }

            String problemDirectory = algorithmDirectory + problemList_[mProblemIndex];
            String paretoFrontPath = frontPath_[mProblemIndex];

            for (String anIndicatorList_ : indicatorList_) {
                System.out.println("Experiment - Quality indicator: [" + anIndicatorList_ + ", " +
                        algorithmNameList_[mAlgorithmIndex] + ", " + problemList_[mProblemIndex] +
                        "]");

                resetFile(problemDirectory + "/" + anIndicatorList_);

                for (int numRun = 0; numRun < independentRuns_; numRun++) {

                    String outputParetoFrontFilePath;
                    outputParetoFrontFilePath = problemDirectory + "/FUN." + numRun;
                    String solutionFrontFile = outputParetoFrontFilePath;
                    String qualityIndicatorFile = problemDirectory;
                    double value = 0;

                    if (anIndicatorList_.equals("HV")) {

                        Hypervolume indicators = new Hypervolume();
                        double[][] solutionFront = indicators.utils_.readFront(solutionFrontFile);
                        double[][] trueFront = indicators.utils_.readFront(paretoFrontPath);
                        value = indicators
                                .hypervolume(solutionFront, trueFront, trueFront[0].length);

                        qualityIndicatorFile = qualityIndicatorFile + "/HV";

                    }
                    if (anIndicatorList_.equals("SPREAD")) {
                        Spread indicators = new Spread();
                        double[][] solutionFront = indicators.utils_.readFront(solutionFrontFile);
                        double[][] trueFront = indicators.utils_.readFront(paretoFrontPath);
                        value = indicators.spread(solutionFront, trueFront, trueFront[0].length);

                        qualityIndicatorFile = qualityIndicatorFile + "/SPREAD";
                    }
                    if (anIndicatorList_.equals("GD")) { // Added manually (Gustavo G. Pascual)
                        GenerationalDistance indicators = new GenerationalDistance();
                        double[][] solutionFront = indicators.utils_.readFront(solutionFrontFile);
                        double[][] trueFront = indicators.utils_.readFront(paretoFrontPath);
                        value = indicators.generationalDistance(solutionFront, trueFront,
                                trueFront[0].length);

                        qualityIndicatorFile = qualityIndicatorFile + "/GD";
                    }
                    if (anIndicatorList_.equals("IGD")) {
                        InvertedGenerationalDistance indicators =
                                new InvertedGenerationalDistance();
                        double[][] solutionFront = indicators.utils_.readFront(solutionFrontFile);
                        double[][] trueFront = indicators.utils_.readFront(paretoFrontPath);
                        value = indicators.invertedGenerationalDistance(solutionFront, trueFront,
                                trueFront[0].length);

                        qualityIndicatorFile = qualityIndicatorFile + "/IGD";
                    }
                    if (anIndicatorList_.equals("EPSILON")) {
                        Epsilon indicators = new Epsilon();
                        double[][] solutionFront = indicators.utils_.readFront(solutionFrontFile);
                        double[][] trueFront = indicators.utils_.readFront(paretoFrontPath);
                        value = indicators.epsilon(solutionFront, trueFront, trueFront[0].length);

                        qualityIndicatorFile = qualityIndicatorFile + "/EPSILON";
                    }

                    if (!qualityIndicatorFile.equals(problemDirectory)) {
                        FileWriter os;
                        try {
                            os = new FileWriter(qualityIndicatorFile, true);
                            os.write("" + value + "\n");
                            os.close();
                        } catch (IOException ex) {
                            Logger.getLogger(Experiment.class.getName())
                                    .log(Level.SEVERE, null, ex);
                        }
                    } // if
                } // for
            } // for

            if (timeIndicator) {
                String file = problemDirectory + "/TIME";
                try {
                    File f = new File(file);
                    if (!f.exists()) {
                        f.createNewFile();
                    }
                    PrintWriter writer = new PrintWriter(new FileWriter(file));

                    for (i = 0; i < independentRuns_; i++) {
                        BufferedReader reader =
                                new BufferedReader(new FileReader(problemDirectory + "/TIME." + i));
                        double time = Double.parseDouble(reader.readLine());
                        reader.close();
                        writer.println(Double.toString(time));
                    }
                    writer.close();
                } catch (IOException ex) {
                    Logger.getLogger(Experiment.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (fixTimeIndicator) {
                String file = problemDirectory + "/FIXTIME";
                try {
                    File f = new File(file);
                    if (!f.exists()) {
                        f.createNewFile();
                    }
                    PrintWriter writer = new PrintWriter(new FileWriter(file));

                    for (i = 0; i < independentRuns_; i++) {
                        BufferedReader reader = new BufferedReader(
                                new FileReader(problemDirectory + "/FIXTIME." + i));
                        double time = Double.parseDouble(reader.readLine());
                        reader.close();
                        writer.println(Double.toString(time));
                    }
                    writer.close();
                } catch (IOException ex) {
                    Logger.getLogger(Experiment.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (frontSizeIndicator) {
                String file = problemDirectory + "/FRONTSIZE";
                try {
                    File f = new File(file);
                    if (!f.exists()) {
                        f.createNewFile();
                    }

                    double frontSize = 0;
                    for (i = 0; i < independentRuns_; i++) {
                        BufferedReader reader = new BufferedReader(
                                new FileReader(problemDirectory + "/FRONTSIZE." + i));
                        int success = Integer.parseInt(reader.readLine());
                        reader.close();
                        frontSize += success;
                    }
                    frontSize = frontSize / independentRuns_;

                    PrintWriter writer = new PrintWriter(new FileWriter(file));
                    writer.println(Double.toString(frontSize));
                    writer.close();
                } catch (IOException ex) {
                    Logger.getLogger(Experiment.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    ;

    /**
     * Generate the Quality Indicators
     */
    public void generateQualityIndicators(int nThreads) {

        checkParetoFronts(nThreads);

        if (indicatorList_.length > 0) {

            ExecutorService executorService = Executors.newFixedThreadPool(nThreads);

            for (int algorithmIndex = 0; algorithmIndex < algorithmNameList_.length;
                 algorithmIndex++) {
                for (int problemIndex = 0; problemIndex < problemList_.length; problemIndex++) {
                    executorService
                            .execute(new QualityIndicatorRunnable(algorithmIndex, problemIndex));
                }
            }

            executorService.shutdown();

            boolean finished = false;
            do {
                try {
                    finished = executorService.awaitTermination(5, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (!finished);
        }
    }

    /**
     * @param file
     */
    private void resetFile(String file) {
        File f = new File(file);
        if (f.exists()) {
            System.out.println("File " + file + " exist.");

            if (f.isDirectory()) {
                System.out.println("File " + file + " is a directory. Deleting directory.");
                if (f.delete()) {
                    System.out.println("Directory successfully deleted.");
                } else {
                    System.out.println("Error deleting directory.");
                }
            } else {
                System.out.println("File " + file + " is a file. Deleting file.");
                if (f.delete()) {
                    System.out.println("File successfully deleted.");
                } else {
                    System.out.println("Error deleting file.");
                }
            }
        } else {
            System.out.println("File " + file + " does NOT exist.");
        }
    } // resetFile

    /**
     * @param problemIndex
     */
    public void generateReferenceFronts(int problemIndex) {

        File rfDirectory;
        String referenceFrontDirectory = experimentBaseDirectory_ + "/referenceFronts";

        rfDirectory = new File(referenceFrontDirectory);

        if (!rfDirectory
                .exists()) {                                                    // Si no existe el directorio
            boolean result =
                    new File(referenceFrontDirectory).mkdirs();                // Lo creamos
            System.out.println("Creating " + referenceFrontDirectory);
        }

        frontPath_[problemIndex] =
                referenceFrontDirectory + "/" + problemList_[problemIndex] + ".rf";

        MetricsUtil metricsUtils = new MetricsUtil();
        NonDominatedSolutionList solutionSet = new NonDominatedSolutionList();
        for (String anAlgorithmNameList_ : algorithmNameList_) {

            String problemDirectory = experimentBaseDirectory_ + "/data/" + anAlgorithmNameList_ +
                    "/" + problemList_[problemIndex];

            for (int numRun = 0; numRun < independentRuns_; numRun++) {

                String outputParetoFrontFilePath;
                outputParetoFrontFilePath = problemDirectory + "/FUN." + numRun;
                String solutionFrontFile = outputParetoFrontFilePath;

                metricsUtils.readNonDominatedSolutionSet(solutionFrontFile, solutionSet);
            } // for
        } // for
        solutionSet.printObjectivesToFile(frontPath_[problemIndex]);
    } // generateReferenceFronts


    private class ReferenceFrontsGenerationsRunnable implements Runnable {

        private int mProblemIndex;

        public ReferenceFrontsGenerationsRunnable(int problemIndex) {
            mProblemIndex = problemIndex;
        }

        @Override
        public void run() {
            generateReferenceFronts(mProblemIndex);
        }
    }

    /**
     *
     */
    // Parallel implementation - Gustavo
    public void generateReferenceFrontsParallelizable(int nThreads) {


        int[] problems = new int[problemList_.length];
        for (int i = 0; i < problemList_.length; i++) {
            problems[i] = i;
        }
        generateReferenceFrontsParallelizable(nThreads, problems);
    } // generateReferenceFrontsParallelizable

    public void generateReferenceFrontsParallelizable(int nThreads, int[] problems) {
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        // Por cada problema, generamos el frente de referencia
        for (int problemIndex = 0; problemIndex < problemList_.length; problemIndex++) {
            executorService.execute(new ReferenceFrontsGenerationsRunnable(problemIndex));
        } // for

        executorService.shutdown();

        boolean finished = false;
        do {
            try {
                finished = executorService.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!finished);
    }
    // Fin modificación ReferenceFronts

    public void generateLatexTables() throws FileNotFoundException, IOException {
        latexDirectory_ = experimentBaseDirectory_ + "/" + latexDirectory_;
        System.out.println("latex directory: " + latexDirectory_);

        Vector[][][] data = new Vector[indicatorList_.length][][];
        for (int indicator = 0; indicator < indicatorList_.length; indicator++) {
            // A data vector per problem
            data[indicator] = new Vector[problemList_.length][];

            for (int problem = 0; problem < problemList_.length; problem++) {
                data[indicator][problem] = new Vector[algorithmNameList_.length];

                for (int algorithm = 0; algorithm < algorithmNameList_.length; algorithm++) {
                    data[indicator][problem][algorithm] = new Vector();

                    String directory = experimentBaseDirectory_;
                    directory += "/data/";
                    directory += "/" + algorithmNameList_[algorithm];
                    directory += "/" + problemList_[problem];
                    directory += "/" + indicatorList_[indicator];
                    // Read values from data files
                    FileInputStream fis = new FileInputStream(directory);
                    InputStreamReader isr = new InputStreamReader(fis);
                    BufferedReader br = new BufferedReader(isr);
                    //System.out.println(directory);
                    String aux = br.readLine();
                    while (aux != null) {
                        data[indicator][problem][algorithm].add(Double.parseDouble(aux));
                        //System.out.println(Double.parseDouble(aux));
                        aux = br.readLine();
                    } // while
                } // for
            } // for
        } // for

        double[][][] mean;
        double[][][] median;
        double[][][] stdDeviation;
        double[][][] iqr;
        double[][][] max;
        double[][][] min;
        int[][][] numberOfValues;

        Map<String, Double> statValues = new HashMap<String, Double>();

        statValues.put("mean", 0.0);
        statValues.put("median", 0.0);
        statValues.put("stdDeviation", 0.0);
        statValues.put("iqr", 0.0);
        statValues.put("max", 0.0);
        statValues.put("min", 0.0);

        mean = new double[indicatorList_.length][][];
        median = new double[indicatorList_.length][][];
        stdDeviation = new double[indicatorList_.length][][];
        iqr = new double[indicatorList_.length][][];
        min = new double[indicatorList_.length][][];
        max = new double[indicatorList_.length][][];
        numberOfValues = new int[indicatorList_.length][][];

        for (int indicator = 0; indicator < indicatorList_.length; indicator++) {
            // A data vector per problem
            mean[indicator] = new double[problemList_.length][];
            median[indicator] = new double[problemList_.length][];
            stdDeviation[indicator] = new double[problemList_.length][];
            iqr[indicator] = new double[problemList_.length][];
            min[indicator] = new double[problemList_.length][];
            max[indicator] = new double[problemList_.length][];
            numberOfValues[indicator] = new int[problemList_.length][];

            for (int problem = 0; problem < problemList_.length; problem++) {
                mean[indicator][problem] = new double[algorithmNameList_.length];
                median[indicator][problem] = new double[algorithmNameList_.length];
                stdDeviation[indicator][problem] = new double[algorithmNameList_.length];
                iqr[indicator][problem] = new double[algorithmNameList_.length];
                min[indicator][problem] = new double[algorithmNameList_.length];
                max[indicator][problem] = new double[algorithmNameList_.length];
                numberOfValues[indicator][problem] = new int[algorithmNameList_.length];

                for (int algorithm = 0; algorithm < algorithmNameList_.length; algorithm++) {
                    Collections.sort(data[indicator][problem][algorithm]);

                    String directory = experimentBaseDirectory_;
                    directory += "/" + algorithmNameList_[algorithm];
                    directory += "/" + problemList_[problem];
                    directory += "/" + indicatorList_[indicator];

                    //System.out.println("----" + directory + "-----");
                    //calculateStatistics(data[indicator][problem][algorithm], meanV, medianV, minV, maxV, stdDeviationV, iqrV) ;
                    calculateStatistics(data[indicator][problem][algorithm], statValues);
                    /*
          System.out.println("Mean: " + statValues.get("mean"));
          System.out.println("Median : " + statValues.get("median"));
          System.out.println("Std : " + statValues.get("stdDeviation"));
          System.out.println("IQR : " + statValues.get("iqr"));
          System.out.println("Min : " + statValues.get("min"));
          System.out.println("Max : " + statValues.get("max"));
          System.out.println("N_values: " + data[indicator][problem][algorithm].size()) ;
					 */
                    mean[indicator][problem][algorithm] = statValues.get("mean");
                    median[indicator][problem][algorithm] = statValues.get("median");
                    stdDeviation[indicator][problem][algorithm] = statValues.get("stdDeviation");
                    iqr[indicator][problem][algorithm] = statValues.get("iqr");
                    min[indicator][problem][algorithm] = statValues.get("min");
                    max[indicator][problem][algorithm] = statValues.get("max");
                    numberOfValues[indicator][problem][algorithm] =
                            data[indicator][problem][algorithm].size();
                }
            }
        }

        File latexOutput;
        latexOutput = new File(latexDirectory_);
        if (!latexOutput.exists()) {
            boolean result = new File(latexDirectory_).mkdirs();
            System.out.println("Creating " + latexDirectory_ + " directory");
        }
        //System.out.println("Experiment name: " + experimentName_);
        String latexFile = latexDirectory_ + "/" + experimentName_ + ".tex";
        printHeaderLatexCommands(latexFile);
        for (int i = 0; i < indicatorList_.length; i++) {
            printMeanStdDev(latexFile, i, mean, stdDeviation);
            printMedianIQR(latexFile, i, median, iqr);
        } // for
        printEndLatexCommands(latexFile);
    } // generateLatexTables

    /**
     * Calculates statistical values from a vector of Double objects
     *
     * @param vector
     * @param values
     */
    public void calculateStatistics(Vector vector, Map<String, Double> values) {

        if (vector.size() > 0) {
            double sum, minimum, maximum, sqsum, min, max, median, mean, iqr, stdDeviation;

            sqsum = 0.0;
            sum = 0.0;
            min = 1E300;
            max = -1E300;
            median = 0;

            for (int i = 0; i < vector.size(); i++) {
                double val = (Double) vector.elementAt(i);

                sqsum += val * val;
                sum += val;
                if (val < min) {
                    min = val;
                }
                if (val > max) {
                    max = val;
                } // if
            } // for

            // Mean
            mean = sum / vector.size();

            // Standard deviation
            if (sqsum / vector.size() - mean * mean < 0.0) {
                stdDeviation = 0.0;
            } else {
                stdDeviation = Math.sqrt(sqsum / vector.size() - mean * mean);
            } // if

            // Median
            if (vector.size() % 2 != 0) {
                median = (Double) vector.elementAt(vector.size() / 2);
            } else {
                median = ((Double) vector.elementAt(vector.size() / 2 - 1) +
                        (Double) vector.elementAt(vector.size() / 2)) / 2.0;
            } // if

            values.put("mean", mean);
            values.put("median", Statistics.calculateMedian(vector, 0, vector.size() - 1));
            values.put("iqr", Statistics.calculateIQR(vector));
            values.put("stdDeviation", stdDeviation);
            values.put("min", (Double) min);
            values.put("max", (Double) max);
        } // if
        else {
            values.put("mean", Double.NaN);
            values.put("median", Double.NaN);
            values.put("iqr", Double.NaN);
            values.put("stdDeviation", Double.NaN);
            values.put("min", Double.NaN);
            values.put("max", Double.NaN);
        } // else
    } // calculateStatistics


    public void printHeaderLatexCommands(String fileName) throws IOException {
        FileWriter os = new FileWriter(fileName, false);
        os.write("\\documentclass{article}" + "\n");
        os.write("\\title{" + experimentName_ + "}" + "\n");
        os.write("\\usepackage{colortbl}" + "\n");
        os.write("\\usepackage[table*]{xcolor}" + "\n");
        os.write("\\xdefinecolor{gray95}{gray}{0.65}" + "\n");
        os.write("\\xdefinecolor{gray25}{gray}{0.8}" + "\n");
        os.write("\\author{}" + "\n");
        os.write("\\begin{document}" + "\n");
        os.write("\\maketitle" + "\n");
        os.write("\\section{Tables}" + "\n");

        os.close();
    }

    public void printEndLatexCommands(String fileName) throws IOException {
        FileWriter os = new FileWriter(fileName, true);
        os.write("\\end{document}" + "\n");
        os.close();
    } // printEndLatexCommands

    public void printMeanStdDev(String fileName, int indicator, double[][][] mean,
                                double[][][] stdDev) throws IOException {
        FileWriter os = new FileWriter(fileName, true);
        os.write("\\" + "\n");
        os.write("\\begin{table}" + "\n");
        os.write(
                "\\caption{" + indicatorList_[indicator] + ". Mean and standard deviation}" + "\n");
        os.write("\\label{table:mean." + indicatorList_[indicator] + "}" + "\n");
        os.write("\\centering" + "\n");
        os.write("\\begin{scriptsize}" + "\n");
        os.write("\\begin{tabular}{l");

        // calculate the number of columns
        for (String anAlgorithmNameList_ : algorithmNameList_) {
            os.write("l");
        }
        os.write("}\n");

        os.write("\\hline");
        // write table head
        for (int i = -1; i < algorithmNameList_.length; i++) {
            if (i == -1) {
                os.write(" & ");
            } else if (i == (algorithmNameList_.length - 1)) {
                os.write(" " + algorithmNameList_[i] + "\\\\" + "\n");
            } else {
                os.write("" + algorithmNameList_[i] + " & ");
            }
        }
        os.write("\\hline" + "\n");

        String m, s;
        // write lines
        for (int i = 0; i < problemList_.length; i++) {
            // find the best value and second best value
            double bestValue;
            double bestValueIQR;
            double secondBestValue;
            double secondBestValueIQR;
            int bestIndex = -1;
            int secondBestIndex = -1;
            if ((Boolean) indicatorMinimize_.get(indicatorList_[indicator])) {// minimize by default
                bestValue = Double.MAX_VALUE;
                bestValueIQR = Double.MAX_VALUE;
                secondBestValue = Double.MAX_VALUE;
                secondBestValueIQR = Double.MAX_VALUE;
                for (int j = 0; j < (algorithmNameList_.length); j++) {
                    if ((mean[indicator][i][j] < bestValue) ||
                            ((mean[indicator][i][j] == bestValue) &&
                                    (stdDev[indicator][i][j] < bestValueIQR))) {
                        secondBestIndex = bestIndex;
                        secondBestValue = bestValue;
                        secondBestValueIQR = bestValueIQR;
                        bestValue = mean[indicator][i][j];
                        bestValueIQR = stdDev[indicator][i][j];
                        bestIndex = j;
                    } else if ((mean[indicator][i][j] < secondBestValue) ||
                            ((mean[indicator][i][j] == secondBestValue) &&
                                    (stdDev[indicator][i][j] < secondBestValueIQR))) {
                        secondBestIndex = j;
                        secondBestValue = mean[indicator][i][j];
                        secondBestValueIQR = stdDev[indicator][i][j];
                    } // else if
                }
            } // if
            else { // indicator to maximize e.g., the HV
                bestValue = Double.MIN_VALUE;
                bestValueIQR = Double.MIN_VALUE;
                secondBestValue = Double.MIN_VALUE;
                secondBestValueIQR = Double.MIN_VALUE;
                for (int j = 0; j < (algorithmNameList_.length); j++) {
                    if ((mean[indicator][i][j] > bestValue) ||
                            ((mean[indicator][i][j] == bestValue) &&
                                    (stdDev[indicator][i][j] < bestValueIQR))) {
                        secondBestIndex = bestIndex;
                        secondBestValue = bestValue;
                        secondBestValueIQR = bestValueIQR;
                        bestValue = mean[indicator][i][j];
                        bestValueIQR = stdDev[indicator][i][j];
                        bestIndex = j;
                    } else if ((mean[indicator][i][j] > secondBestValue) ||
                            ((mean[indicator][i][j] == secondBestValue) &&
                                    (stdDev[indicator][i][j] < secondBestValueIQR))) {
                        secondBestIndex = j;
                        secondBestValue = mean[indicator][i][j];
                        secondBestValueIQR = stdDev[indicator][i][j];
                    } // else if
                } // for
            } // else

            os.write(problemList_[i].replace("_", "\\_") + " & ");
            for (int j = 0; j < (algorithmNameList_.length - 1); j++) {
                if (j == bestIndex) {
                    os.write("\\cellcolor{gray95}");
                }
                if (j == secondBestIndex) {
                    os.write("\\cellcolor{gray25}");
                }

                m = String.format(Locale.ENGLISH, "%10.2e", mean[indicator][i][j]);
                s = String.format(Locale.ENGLISH, "%8.1e", stdDev[indicator][i][j]);
                os.write("$" + m + "_{" + s + "}$ & ");
            }
            if (bestIndex == (algorithmNameList_.length - 1)) {
                os.write("\\cellcolor{gray95}");
            }
            m = String.format(Locale.ENGLISH, "%10.2e",
                    mean[indicator][i][algorithmNameList_.length - 1]);
            s = String.format(Locale.ENGLISH, "%8.1e",
                    stdDev[indicator][i][algorithmNameList_.length - 1]);
            os.write("$" + m + "_{" + s + "}$ \\\\" + "\n");
        } // for
        //os.write("" + mean[0][problemList_.length-1][algorithmNameList_.length-1] + "\\\\"+ "\n" ) ;

        os.write("\\hline" + "\n");
        os.write("\\end{tabular}" + "\n");
        os.write("\\end{scriptsize}" + "\n");
        os.write("\\end{table}" + "\n");
        os.close();
    } // printMeanStdDev

    public void printMedianIQR(String fileName, int indicator, double[][][] median,
                               double[][][] IQR) throws IOException {
        FileWriter os = new FileWriter(fileName, true);
        os.write("\\" + "\n");
        os.write("\\begin{table}" + "\n");
        os.write("\\caption{" + indicatorList_[indicator] + ". Median and IQR}" + "\n");
        os.write("\\label{table:median." + indicatorList_[indicator] + "}" + "\n");
        os.write("\\begin{scriptsize}" + "\n");
        os.write("\\centering" + "\n");
        os.write("\\begin{tabular}{l");

        // calculate the number of columns
        for (String anAlgorithmNameList_ : algorithmNameList_) {
            os.write("l");
        }
        os.write("}\n");

        os.write("\\hline");
        // write table head
        for (int i = -1; i < algorithmNameList_.length; i++) {
            if (i == -1) {
                os.write(" & ");
            } else if (i == (algorithmNameList_.length - 1)) {
                os.write(" " + algorithmNameList_[i] + "\\\\" + "\n");
            } else {
                os.write("" + algorithmNameList_[i] + " & ");
            }
        }
        os.write("\\hline" + "\n");

        String m, s;
        // write lines
        for (int i = 0; i < problemList_.length; i++) {
            // find the best value and second best value
            double bestValue;
            double bestValueIQR;
            double secondBestValue;
            double secondBestValueIQR;
            int bestIndex = -1;
            int secondBestIndex = -1;
            if ((Boolean) indicatorMinimize_.get(indicatorList_[indicator])) {// minimize by default
                bestValue = Double.MAX_VALUE;
                bestValueIQR = Double.MAX_VALUE;
                secondBestValue = Double.MAX_VALUE;
                secondBestValueIQR = Double.MAX_VALUE;
                for (int j = 0; j < (algorithmNameList_.length); j++) {
                    if ((median[indicator][i][j] < bestValue) ||
                            ((median[indicator][i][j] == bestValue) &&
                                    (IQR[indicator][i][j] < bestValueIQR))) {
                        secondBestIndex = bestIndex;
                        secondBestValue = bestValue;
                        secondBestValueIQR = bestValueIQR;
                        bestValue = median[indicator][i][j];
                        bestValueIQR = IQR[indicator][i][j];
                        bestIndex = j;
                    } else if ((median[indicator][i][j] < secondBestValue) ||
                            ((median[indicator][i][j] == secondBestValue) &&
                                    (IQR[indicator][i][j] < secondBestValueIQR))) {
                        secondBestIndex = j;
                        secondBestValue = median[indicator][i][j];
                        secondBestValueIQR = IQR[indicator][i][j];
                    } // else if
                } // for
            } // if
            else { // indicator to maximize e.g., the HV
                bestValue = Double.MIN_VALUE;
                bestValueIQR = Double.MIN_VALUE;
                secondBestValue = Double.MIN_VALUE;
                secondBestValueIQR = Double.MIN_VALUE;
                for (int j = 0; j < (algorithmNameList_.length); j++) {
                    if ((median[indicator][i][j] > bestValue) ||
                            ((median[indicator][i][j] == bestValue) &&
                                    (IQR[indicator][i][j] < bestValueIQR))) {
                        secondBestIndex = bestIndex;
                        secondBestValue = bestValue;
                        secondBestValueIQR = bestValueIQR;
                        bestValue = median[indicator][i][j];
                        bestValueIQR = IQR[indicator][i][j];
                        bestIndex = j;
                    } else if ((median[indicator][i][j] > secondBestValue) ||
                            ((median[indicator][i][j] == secondBestValue) &&
                                    (IQR[indicator][i][j] < secondBestValueIQR))) {
                        secondBestIndex = j;
                        secondBestValue = median[indicator][i][j];
                        secondBestValueIQR = IQR[indicator][i][j];
                    } // else if
                } // for
            } // else

            os.write(problemList_[i].replace("_", "\\_") + " & ");
            for (int j = 0; j < (algorithmNameList_.length - 1); j++) {
                if (j == bestIndex) {
                    os.write("\\cellcolor{gray95}");
                }
                if (j == secondBestIndex) {
                    os.write("\\cellcolor{gray25}");
                }
                m = String.format(Locale.ENGLISH, "%10.2e", median[indicator][i][j]);
                s = String.format(Locale.ENGLISH, "%8.1e", IQR[indicator][i][j]);
                os.write("$" + m + "_{" + s + "}$ & ");
            }
            if (bestIndex == (algorithmNameList_.length - 1)) {
                os.write("\\cellcolor{gray95}");
            }
            m = String.format(Locale.ENGLISH, "%10.2e",
                    median[indicator][i][algorithmNameList_.length - 1]);
            s = String.format(Locale.ENGLISH, "%8.1e",
                    IQR[indicator][i][algorithmNameList_.length - 1]);
            os.write("$" + m + "_{" + s + "}$ \\\\" + "\n");
        } // for
        //os.write("" + mean[0][problemList_.length-1][algorithmNameList_.length-1] + "\\\\"+ "\n" ) ;

        os.write("\\hline" + "\n");
        os.write("\\end{tabular}" + "\n");
        os.write("\\end{scriptsize}" + "\n");
        os.write("\\end{table}" + "\n");
        os.close();
    } // printMedianIQR

    /**
     * Invoking the generateScripts method on the RBoxplot class
     *
     * @param rows
     * @param cols
     * @param problems
     * @param prefix
     * @param notch
     * @param experiment
     * @throws java.io.IOException
     * @throws java.io.FileNotFoundException
     */
    public void generateRBoxplotScripts(int rows, int cols, String[] problems, String prefix,
                                        boolean notch, Experiment experiment)
            throws FileNotFoundException, IOException {
        RBoxplot.generateScripts(rows, cols, problems, prefix, notch, this);
    } // generateRBoxplotScripts

    /**
     * Invoking the generateScripts method on the RWilcoxon class
     *
     * @param problems
     * @param prefix
     * @param experiment
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public void generateRWilcoxonScripts(String[] problems, String prefix, Experiment experiment)
            throws FileNotFoundException, IOException {
        RWilcoxon.generateScripts(problems, prefix, this);
    } // generateRWilcoxonScripts

    // Inicio modificación planificación Threads

    /**
     * @return
     */
    public synchronized int[] getNextProblem() {
        int[] res = new int[3];

        int alg = this.algorithmIndex;
        int runs = this.irunIndex;
        int prob = this.problemIndex;

        if (this.problemList_.length == 1) {
            prob = 0;
        }
        if (this.algorithmNameList_.length == 1) {
            alg = 0;
        }
        if (this.independentRuns_ == 1) {
            runs = 0;
        }

        if (this.problemIndex < this.problemList_.length - 1) {
            this.problemIndex++;
        } else {
            if (this.algorithmIndex < this.algorithmNameList_.length - 1) {
                this.algorithmIndex++;
                this.problemIndex = 0;
            } else {
                if (this.irunIndex < this.independentRuns_ - 1) {
                    this.irunIndex++;
                    this.problemIndex = 0;
                    this.algorithmIndex = 0;
                } else {
                    this.finished_ = true;
                }
            }
        }

        //System.out.println("Prob = " + prob + " Alg = " + alg + " Run = " + runs);

        res[0] = prob;
        res[1] = alg;
        res[2] = runs;

        return res;
    }// getNextProblem
    // Fin modificación planificación Threads
}  // Experiment

