package com.jjcosare.music.service;

import com.jjcosare.music.Main;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by jjcosare on 3/9/19.
 */
public class AudioConversionService {

    private static AudioConversionService instance;

    private AudioConversionService() {
    }

    public static AudioConversionService getInstance() {
        if (instance == null) {
            instance = new AudioConversionService();
        }
        return instance;
    }

    public void convertFolderForTraining(String fromFolder, String toFolder) {
        // Convert audio (tested on mp3 and flac) to wav files
        for (Main.MusicGenre musicGenre : Main.MusicGenre.values()) {
            String genre = musicGenre.name().toLowerCase();
            try (Stream<Path> audioFromFolder = Files.walk(Paths.get(fromFolder + "/" + genre))) {
                FileService.getInstance().cleanOrCreateFolder(toFolder + "/" + genre);
                audioFromFolder.forEach(audioFile -> {
                    if (Files.isRegularFile(audioFile)) {
                        audioToWav(toFolder, audioFile.toFile(), genre);
                    }
                });
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void convertFile(String toFolder, File audioFile) {
        if (audioFile.isFile()) {
            audioToWav(toFolder, audioFile, null);
        }
    }

    private void audioToWav(String toFolder, File audioFile, String genre) {
        try {
            String fileName = audioFile.getName().toLowerCase();
            File convertedFile = (genre != null) ? new File(toFolder + "/" + genre + "/" + audioFile.getName() + "." + AudioFileFormat.Type.WAVE.getExtension())
                    : new File(toFolder + "/" + audioFile.getName() + "." + AudioFileFormat.Type.WAVE.getExtension());
            if (!convertedFile.exists()) {
                if(!convertedFile.getParentFile().exists()){
                    convertedFile.getParentFile().mkdir();
                }
                if (fileName.endsWith(".wav")) {
                    Logger.getAnonymousLogger().log(Level.INFO, "Converting audio file to wav format: " + audioFile.getAbsolutePath());
                    try (OutputStream outputStream = new FileOutputStream(convertedFile)) {
                        Files.copy(audioFile.toPath(), outputStream);
                    }
                } else if (fileName.endsWith(".flac")) {
                    Logger.getAnonymousLogger().log(Level.INFO, "Converting audio file to wav format: " + audioFile.getAbsolutePath());
                    try (InputStream inputStream = new FileInputStream(audioFile)) {
                        OutputStream outputStream = new FileOutputStream(convertedFile);
                        FlacService.getInstance().decode(inputStream, outputStream);
                    }
                } else if (fileName.endsWith(".mp3")) {
                    Logger.getAnonymousLogger().log(Level.INFO, "Converting audio file to wav format: " + audioFile.getAbsolutePath());
                    try (AudioInputStream sourceInputStream = AudioSystem.getAudioInputStream(audioFile)) {
                        AudioFormat sourceAudioFormat = sourceInputStream.getFormat();
                        AudioFormat pcmAudioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                sourceAudioFormat.getSampleRate(),
                                16,
                                sourceAudioFormat.getChannels(),
                                sourceAudioFormat.getChannels() * 2,
                                sourceAudioFormat.getSampleRate(),
                                false);
                        AudioInputStream pcmAudioInputStream = AudioSystem.getAudioInputStream(pcmAudioFormat, sourceInputStream);
                        AudioSystem.write(pcmAudioInputStream, AudioFileFormat.Type.WAVE, convertedFile);
                    }
                } else {
                    Logger.getGlobal().log(Level.INFO, "File ignored, not supported for conversion: " + fileName);
                }
            }
        } catch (UnsupportedAudioFileException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
