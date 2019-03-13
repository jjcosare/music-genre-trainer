package com.jjcosare.music.service;

import com.jjcosare.music.Main;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.*;
import weka.core.converters.*;
import weka.filters.unsupervised.attribute.Remove;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by jjcosare on 3/9/19.
 */
public class MachineLearningService {

    private static MachineLearningService instance;

    private MachineLearningService() {
    }

    public static MachineLearningService getInstance() {
        if (instance == null) {
            instance = new MachineLearningService();
        }
        return instance;
    }

    public static final String J_AUDIO_BASE_ATTRIBUTES_ARFF = "j-audio-base-attributes.arff";
    public static final String WEKA_MUSIC_GENRE_ARFF = "weka-music-genre.arff";
    public static final String WEKA_MUSIC_GENRE_MODEL = "weka-music-genre.model";
    public static final String WEKA_MUSIC_GENRE_PREDICTED = "weka-music-genre.predict.csv";

    public void prepareDataForTraining(String fromFolder) {
        try {
            /*
             * Load the base data set which only has data structure
             */
            Instances baseDataSet = getDataSetWithArffFormat(J_AUDIO_BASE_ATTRIBUTES_ARFF);
            addMissingGenreAttribute(baseDataSet, null);

            /*
             * Add missing genre attribute and add all genre data set to base data set
             */
            for (Main.MusicGenre musicGenre : Main.MusicGenre.values()) {
                String genre = musicGenre.name().toLowerCase();
                try (InputStream genreInputStream = new FileInputStream(new File(fromFolder + "/generated/" + genre + ".xml"))) {
                    // Load generated audio features and set proper genre for evaluation
                    Instances genreDataSet = getDataSetWithArffFormat(genreInputStream);
                    addMissingGenreAttribute(genreDataSet, genre);
                    // Add the completed genre data set to base data set
                    baseDataSet.addAll(genreDataSet);
                }
            }

            /*
             * Save aggregated base data set into an arff file
             */
            Saver saver = new ArffSaver();
            saver.setInstances(baseDataSet);
            saver.setFile(new File(fromFolder + "/" + WEKA_MUSIC_GENRE_ARFF));
            saver.writeBatch();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private int getAttributeIndexOfGenre(Instances dataSet){
        return dataSet.numAttributes() - 1;
    }

    private int getAttributeIndexOfTitle(Instances dataSet){
        return dataSet.numAttributes() - 2;
    }

    public void prepareData(String fromFolder) {
        try {
            /*
             * Load the base data set which only has data structure
             */
            Instances baseDataSet = getDataSetWithArffFormat(J_AUDIO_BASE_ATTRIBUTES_ARFF);
            addTitleAttribute(baseDataSet, null);
            addMissingGenreAttribute(baseDataSet, null);

            /*
             * Add missing genre attribute and add all genre data set to base data set
             */
            try (Stream<Path> generatedFeatureFolder = Files.walk(Paths.get(fromFolder + "/generated"))) {
                generatedFeatureFolder.forEach(generatedFeatureFile -> {
                    if(Files.isRegularFile(generatedFeatureFile)) {
                        File featureFile = generatedFeatureFile.toFile();
                        String fileNameWithoutExtension = featureFile.getName()
                                //.replaceFirst("[.][^.]+$", "") // audio extension (.wav .mp3 .flac)
                                .replaceFirst("[.][^.]+$", "") // converted extension (.wav)
                                .replaceFirst("[.][^.]+$", ""); // extracted feature extension (.xml)
                        try (InputStream genreInputStream = new FileInputStream(featureFile)) {
                            // Load generated audio features and add blank title and genre
                            Instances genreDataSet = getDataSetWithArffFormat(genreInputStream);
                            addTitleAttribute(genreDataSet, null);
                            addMissingGenreAttribute(genreDataSet, null);
                            baseDataSet.addAll(genreDataSet);

                            // Add audio file name on title attribute
                            int titleAttributeIndex = getAttributeIndexOfTitle(baseDataSet);
                            Attribute titleAttribute = baseDataSet.attribute(titleAttributeIndex);
                            titleAttribute.addStringValue(fileNameWithoutExtension);
                            baseDataSet.get(titleAttribute.indexOfValue(fileNameWithoutExtension)).setValue(titleAttributeIndex, titleAttribute.indexOfValue(fileNameWithoutExtension));
                        } catch (FileNotFoundException ex) {
                            ex.printStackTrace();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

            /*
             * Save aggregated base data set into an arff file
             */
            Saver saver = new ArffSaver();
            saver.setInstances(baseDataSet);
            saver.setFile(new File(fromFolder + "/" + WEKA_MUSIC_GENRE_ARFF));
            saver.writeBatch();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public Instances getDataSetWithArffFormat(String arff) {
        return getDataSetWithArffFormat(new File(arff));
    }

    public Instances getDataSetWithArffFormat(File arff) {
        Instances dataSet = null;
        try {
            dataSet = getDataSetWithArffFormat(new FileInputStream(arff));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        return dataSet;
    }

    public Instances getDataSetWithArffFormat(InputStream arff) {
        Instances dataSet = null;
        try {
            ArffLoader arffLoader = new ArffLoader();
            arffLoader.setSource(arff);
            arffLoader.setRetrieval(Loader.BATCH);
            dataSet = arffLoader.getDataSet();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return dataSet;
    }

    public void createModelFromTrainingData(String baseFolder, String genreArff) {
        createModelFromTrainingData(baseFolder, new File(genreArff));
    }

    public void createModelFromTrainingData(String baseFolder, File genreArff) {
        try {
            /*
             * Load the aggregated data set
             */
            Instances dataSet = getDataSetWithArffFormat(genreArff);

            /*
             * Select column to classify (genre - last column)
             */
            dataSet.setClassIndex(getAttributeIndexOfGenre(dataSet));

            /*
             * Create a MultilayerPerceptron classifier and configure it
             */
            MultilayerPerceptron classifier = new MultilayerPerceptron();
            classifier.setGUI(false);
            classifier.setAutoBuild(true);
            classifier.setDebug(false);
            classifier.setDecay(false);
            classifier.setHiddenLayers("a");
            classifier.setLearningRate(0.3);
            classifier.setMomentum(0.2);
            classifier.setNominalToBinaryFilter(true);
            classifier.setNormalizeAttributes(true);
            classifier.setNormalizeNumericClass(true);
            classifier.setReset(true);
            classifier.setSeed(0);
            classifier.setTrainingTime(500);
            classifier.setValidationSetSize(3);
            classifier.setValidationThreshold(20);

            /*
             * Train the classifier
             */
            classifier.buildClassifier(dataSet);

            /*
             * Serialize model to disk
             */
            SerializationHelper.write(baseFolder + "/" + WEKA_MUSIC_GENRE_MODEL, classifier);
            Logger.getGlobal().log(Level.INFO, "Saved trained model to " + baseFolder + "/" + WEKA_MUSIC_GENRE_MODEL);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addAttribute(Instances dataSet, String attributeName, List<String> attributeValueList, String attributeValue) {
        // Add missing genre attribute to data set
        Attribute attribute = new Attribute(attributeName, attributeValueList);
        dataSet.insertAttributeAt(attribute, dataSet.numAttributes());
        // Loop and set the genre attribute
        if(attributeValue != null) {
            for (Instance data : dataSet) {
                if(!attribute.isString()){
                    data.setValue(getAttributeIndexOfGenre(dataSet), attributeValue);
                }
            }
        }
    }

    private void addTitleAttribute(Instances dataSet, String attributeValue) {
        addAttribute(dataSet, "title", null, attributeValue);
    }

    private void addMissingGenreAttribute(Instances dataSet, String attributeValue) {
        addAttribute(dataSet, "genre", Arrays.asList(new String[]{"classical", "rock", "pop", "jazz"}), attributeValue);
    }

    public void evaluateModelWith10FoldCrossValidationFromTrainingData(String baseFolder, String genreArff) {
        try {
            /*
             * Load the aggregated data set with proper genre attribute already added
             */
            Instances dataSet = getDataSetWithArffFormat(genreArff);

            /*
             * Select column to classify (genre - last column)
             */
            dataSet.setClassIndex(getAttributeIndexOfGenre(dataSet));

            /*
             * Read in the serialized model from disk
             */
            Classifier classifier = (Classifier) SerializationHelper
                    .read(baseFolder + "/" + WEKA_MUSIC_GENRE_MODEL);

            /*
             * Evaluate model with 10-fold cross validation
             */
            Evaluation evaluation = new Evaluation(dataSet);
            evaluation.crossValidateModel(classifier, dataSet, 10, new Random(1));

            /*
             * Write out the summary information
             */
            Logger.getGlobal().log(Level.INFO, classifier.toString());
            Logger.getGlobal().log(Level.INFO, evaluation.toSummaryString());
            Logger.getGlobal().log(Level.INFO, evaluation.toCumulativeMarginDistributionString());
            Logger.getGlobal().log(Level.INFO, evaluation.toClassDetailsString());
            Logger.getGlobal().log(Level.INFO, evaluation.toMatrixString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Map<String, String> predictWithTrainedModel(String baseFolder, String audioArff) {
        Map<String, String> predictionMap = null;
        try {
            /*
             * Load the aggregated data set
             */
            Instances dataSet = getDataSetWithArffFormat(audioArff);

            /*
             * Select column to classify (genre - last column)
             */
            dataSet.setClassIndex(getAttributeIndexOfGenre(dataSet));

            /*
             * Read in the serialized model from disk
             */
            Classifier classifier = (Classifier) SerializationHelper
                    .read(baseFolder + "/" + WEKA_MUSIC_GENRE_MODEL);

            /*
             * Disregard audio fileName
             */
            //dataSet.deleteStringAttributes();
            Remove remove = new Remove();
            remove.setAttributeIndices(String.valueOf(getAttributeIndexOfTitle(dataSet)));
            remove.setInvertSelection(false);
            remove.setInputFormat(dataSet);
            //dataSet = Filter.useFilter(dataSet, remove);
            FilteredClassifier filteredClassifier = new FilteredClassifier();
            filteredClassifier.setFilter(remove);
            filteredClassifier.setClassifier(classifier);

            /*
             * Classify each instance
             */
            predictionMap = new HashMap();
            for(Instance data : dataSet) {
                try {
                    double classification = filteredClassifier.classifyInstance(data);
                    data.setClassValue(classification);

                    String musicFileName = data.stringValue(getAttributeIndexOfTitle(dataSet));
                    String musicGenre = data.stringValue(getAttributeIndexOfGenre(dataSet));
                    predictionMap.put(musicFileName, musicGenre);
                    Logger.getGlobal().log(Level.INFO,musicFileName +" : "+ musicGenre);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            /*
             * Write predictions on csv file
             */
            CSVSaver csvSaver = new CSVSaver();
            csvSaver.setFile(new File(baseFolder + "/" + WEKA_MUSIC_GENRE_PREDICTED));
            csvSaver.setInstances(dataSet);
            csvSaver.writeBatch();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return predictionMap;
    }
}
