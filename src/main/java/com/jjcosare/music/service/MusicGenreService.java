package com.jjcosare.music.service;

import javax.sound.sampled.AudioFileFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by jjcosare on 3/9/19.
 */
public class MusicGenreService {

    private static MusicGenreService instance;

    private MusicGenreService() {
    }

    public static MusicGenreService getInstance() {
        if (instance == null) {
            instance = new MusicGenreService();
        }
        return instance;
    }

    public void train(String audioFolder, String baseFolder, String convertedFolder, String featureFolder) {
        AudioConversionService.getInstance().convertFolderForTraining(audioFolder, convertedFolder);

        AudioFeatureService.getInstance().extractForTraining(convertedFolder, featureFolder);

        MachineLearningService.getInstance().prepareDataForTraining(featureFolder);

        MachineLearningService.getInstance().createModelFromTrainingData(baseFolder, featureFolder + "/" + MachineLearningService.WEKA_MUSIC_GENRE_ARFF);

        MachineLearningService.getInstance().evaluateModelWith10FoldCrossValidationFromTrainingData(baseFolder, featureFolder + "/" + MachineLearningService.WEKA_MUSIC_GENRE_ARFF);
    }

    public Map<String, String> predict(List<File> audioFileList, String baseFolder, String convertedFolder, String featureFolder) {
        FileService.getInstance().cleanOrCreateFolder(convertedFolder);
        FileService.getInstance().cleanOrCreateFolder(featureFolder + "/generated");
        audioFileList.forEach(audioFile -> {
            if (FileService.getInstance().isSupportedAudioFile(audioFile)) {
                AudioConversionService.getInstance().convertFile(convertedFolder, audioFile);
                File convertedFile = new File(convertedFolder + "/" + audioFile.getName() + "." + AudioFileFormat.Type.WAVE.getExtension());
                try (OutputStream featureValueOutputStream = new FileOutputStream(featureFolder + "/generated/" + convertedFile.getName() + ".xml")) {
                    AudioFeatureService.getInstance().extractFeature(new File[]{convertedFile}, featureValueOutputStream);
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        MachineLearningService.getInstance().prepareData(featureFolder);

        return MachineLearningService.getInstance().predictWithTrainedModel(baseFolder, featureFolder + "/" + MachineLearningService.WEKA_MUSIC_GENRE_ARFF);
    }

}
