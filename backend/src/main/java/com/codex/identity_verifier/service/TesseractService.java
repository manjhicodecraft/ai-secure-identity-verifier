package com.codex.identity_verifier.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

@Service
public class TesseractService {

    @Value("${tesseract.enabled:false}")
    private boolean tesseractEnabled;

    @Value("${tesseract.command:tesseract}")
    private String tesseractCommand;

    public String extractText(byte[] imageData) {
        if (!tesseractEnabled || imageData == null || imageData.length == 0) {
            return "";
        }
        File input = null;
        File outputBase = null;
        try {
            input = File.createTempFile("ocr-input-", ".png");
            outputBase = File.createTempFile("ocr-output-", "");
            Files.write(input.toPath(), imageData);

            ProcessBuilder pb = new ProcessBuilder(
                    tesseractCommand,
                    input.getAbsolutePath(),
                    outputBase.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0) {
                return "";
            }
            File outputTxt = new File(outputBase.getAbsolutePath() + ".txt");
            if (!outputTxt.exists()) {
                return "";
            }
            return Files.readString(outputTxt.toPath(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        } finally {
            if (input != null && input.exists()) {
                input.delete();
            }
            if (outputBase != null) {
                File txt = new File(outputBase.getAbsolutePath() + ".txt");
                if (txt.exists()) {
                    txt.delete();
                }
                if (outputBase.exists()) {
                    outputBase.delete();
                }
            }
        }
    }
}
