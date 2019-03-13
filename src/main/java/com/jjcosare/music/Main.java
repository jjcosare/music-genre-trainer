package com.jjcosare.music;

import com.jjcosare.music.service.FileService;
import com.jjcosare.music.service.MusicGenreService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by jjcosare on 3/7/19.
 */
public class Main {


    public static final String AUDIO_FOR_TRAINING = "etc/audio";

    public static final String GENERATED_BASE_FOLDER = "target/jjcosare";
    public static final String GENERATED_TRAIN_CONVERTED = GENERATED_BASE_FOLDER + "/train/converted";
    public static final String GENERATED_TRAIN_FEATURE = GENERATED_BASE_FOLDER + "/train/feature";
    public static final String GENERATED_PREDICT_CONVERTED = GENERATED_BASE_FOLDER + "/predict/converted";
    public static final String GENERATED_PREDICT_FEATURE = GENERATED_BASE_FOLDER + "/predict/feature";

    public enum MusicGenre {
        CLASSICAL,
        JAZZ,
        POP,
        ROCK
    }

    public static void main(String[] args) throws Exception {

        FileService.getInstance().cleanOrCreateFolder(GENERATED_BASE_FOLDER);

        // Train model
        MusicGenreService.getInstance().train(AUDIO_FOR_TRAINING, GENERATED_BASE_FOLDER, GENERATED_TRAIN_CONVERTED, GENERATED_TRAIN_FEATURE);

        // Predict genre based on trained model
        File folder = new File(AUDIO_FOR_TRAINING);
        List<File> fileList = new ArrayList<>();
        try (Stream<Path> audioFromFolder = Files.walk(folder.toPath())) {
            audioFromFolder.forEach(audioFile -> {
                if (Files.isRegularFile(audioFile) && FileService.getInstance().isSupportedAudioFile(audioFile.toFile())) {
                    fileList.add(audioFile.toFile());
                }
            });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Map<String, String> predictionMap = MusicGenreService.getInstance().predict(fileList, GENERATED_BASE_FOLDER, GENERATED_PREDICT_CONVERTED, GENERATED_PREDICT_FEATURE);
        if(predictionMap != null && predictionMap.size() > 0){
            predictionMap.forEach((musicTitle, musicGenre) -> {
                Logger.getGlobal().log(Level.INFO,musicTitle +" "+ musicGenre);
            });
        }
    }

}
