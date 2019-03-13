package com.jjcosare.music.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by jjcosare on 3/9/19.
 */
public class FileService {

    private static FileService instance;

    private FileService() {
    }

    public static FileService getInstance() {
        if (instance == null) {
            instance = new FileService();
        }
        return instance;
    }

    public void cleanOrCreateFolder(String folder) {
        try {
            Path folderPath = Paths.get("./"+folder);
            if (Files.exists(folderPath)) {
                Files.walk(folderPath)
                        .filter(Files::isRegularFile)
                        .map(Path::toFile)
                        .forEach(File::delete);
            } else {
                Files.createDirectories(folderPath);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isSupportedAudioFile(File audioFile) {
        String audioFileName = audioFile.getName();
        return audioFile.isFile()
                && (audioFileName.endsWith(".wav")
                || audioFileName.endsWith(".mp3")
                || audioFileName.endsWith(".flac")
        );
    }

}
