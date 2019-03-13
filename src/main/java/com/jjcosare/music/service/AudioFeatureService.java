package com.jjcosare.music.service;

import com.jjcosare.music.Main;
import jAudioFeatureExtractor.ACE.DataTypes.Batch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by jjcosare on 3/9/19.
 */
public class AudioFeatureService {

    private static AudioFeatureService instance;

    private AudioFeatureService() {
    }

    public static AudioFeatureService getInstance() {
        if (instance == null) {
            instance = new AudioFeatureService();
        }
        return instance;
    }

    public static final String J_AUDIO_FEATURE_LIST = "j-audio-feature-list.xml";
    public static final String J_AUDIO_FEATURE_SETTINGS = "j-audio-feature-settings.xml";

    public void extractForTraining(String fromFolder, String toFolder) {
        FileService.getInstance().cleanOrCreateFolder(toFolder + "/generated");
        // Generate audio features from wav files
        for (Main.MusicGenre musicGenre : Main.MusicGenre.values()) {
            String genre = musicGenre.name().toLowerCase();
            try (OutputStream featureValueOutputStream = new FileOutputStream(toFolder + "/generated/" + genre + ".xml")) {
                //extractFeature(new File(fromFolder + "/" + genre).listFiles(), featureValueOutputStream);
                try (Stream<Path> genreFolder = Files.walk(Paths.get(fromFolder + "/" + genre))) {
                    List<File> fileList = new ArrayList<>();
                    genreFolder.forEach(audioFile -> {
                        File file = audioFile.toFile();
                        if (Files.isRegularFile(audioFile) && FileService.getInstance().isSupportedAudioFile(file)) {
                            fileList.add(file);
                        }
                    });
                    extractFeature(fileList.toArray(new File[fileList.size()]), featureValueOutputStream);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void extractFeature(File[] fileArray, OutputStream outputStream) {
        try {
            Batch batch = new Batch(J_AUDIO_FEATURE_LIST, null);
            batch.setRecordings(fileArray);
            batch.setSettings(J_AUDIO_FEATURE_SETTINGS); // generate in jAudio GUI for any changes
            batch.getDataModel().featureValue = outputStream;
            //batch.getAttributes().put("genre", new String[]{"classical", "rock", "pop", "jazz"});
            batch.execute();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
