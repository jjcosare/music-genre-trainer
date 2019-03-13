package com.jjcosare.music.service;

import org.jflac.FLACDecoder;
import org.jflac.PCMProcessor;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;
import org.jflac.util.WavWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by jjcosare on 3/7/19.
 */
public class FlacService implements PCMProcessor {

    private static FlacService instance;

    private FlacService() {
    }

    public static FlacService getInstance() {
        if (instance == null) {
            instance = new FlacService();
        }
        return instance;
    }

    private WavWriter wavWriter;

    public void decode(InputStream inputStream, OutputStream outputStream) throws IOException {
        this.wavWriter = new WavWriter(outputStream);
        FLACDecoder flacDecoder = new FLACDecoder(inputStream);
        flacDecoder.addPCMProcessor(this);
        flacDecoder.decode();
    }

    public void processStreamInfo(StreamInfo streamInfo) {
        try {
            this.wavWriter.writeHeader(streamInfo);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void processPCM(ByteData byteData) {
        try {
            this.wavWriter.writePCM(byteData);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
