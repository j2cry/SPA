package ru.bioresourceslab;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.Logger;

// recognizer thread
public abstract class RecognizerThread extends Thread {
    public static final String COMMAND_END = "command_end";
    public static final String COMMAND_PREVIOUS = "command_back";
    public static final String COMMAND_NEXT = "command_next";

    private double rangeLower = 0;
    private double rangeUpper = 1.5;
//    private final Configuration configuration;
    private final LiveSpeechRecognizer recognizer;

    public static final String ACOUSTIC_MODEL =
            "resource:/cmusphinx-ru-5.2/";
    public static final String DICTIONARY_PATH =
            "resource:/cmusphinx-ru-5.2/spa.dic";
    public static final String LANGUAGE_MODEL =
            "resource:/cmusphinx-ru-5.2/spa.lm";
//    public static final String LANGUAGE_MODEL =
//            "resource:/cmusphinx-ru-5.2/3589.lm";
//    public static final String LANGUAGE_MODEL =
//            "resource:/cmusphinx-ru-5.2/6147.lm";

    private final Logger log = Logger.getLogger("SPA Logger");
    boolean pauseFlag = true;
    boolean isActive = true;

    String result = "";

    public RecognizerThread() throws IOException {
// initializing recognizer
        Configuration configuration = new Configuration();
        configuration.setAcousticModelPath(ACOUSTIC_MODEL);
        configuration.setDictionaryPath(DICTIONARY_PATH);
        configuration.setLanguageModelPath(LANGUAGE_MODEL);
        recognizer = new LiveSpeechRecognizer(configuration);
        this.setDaemon(true);
    }

    public void setRanges(double lower, double upper) {
        this.rangeLower = lower;
        this.rangeUpper = upper;
    }

    protected boolean interpretWeight(@NotNull String source) {       // interpret the result and set it to 'result'. Returns TRUE if is a weight
        String s = source.replaceFirst(" ", ".");   // replace first 'space' to 'dot'
        String res = s.replace(" ", "");    // delete all spaces from string

        // analyze if the weight is correct
        try {
            double weight = Double.parseDouble(res);
            if ((weight > rangeLower) & (weight < rangeUpper)) {
                result = String.valueOf(weight);
                return true;
            }
        } catch (NumberFormatException e) {
            // if res have 'dot'-char, then it is not a command & not weight
            if (res.indexOf('.') > -1) {
                result = "n/a";
                return false;
            }
        }
        result = res;
        return false;
    }

    @Override
    public void run() {
        recognizer.startRecognition(true);
        log.fine("Поток распознавателя запущен.");
        while (isActive) {
            uiRefresh();
            // check if paused
            if (this.pauseFlag) {
                recognizer.stopRecognition();
                log.fine("Поток распознавателя приостановлен.");
                // locker
                synchronized (recognizer) {
                    try {
                        recognizer.wait();
                    } catch (InterruptedException e) {
                        log.severe("Прерывание потока: locker object is invalid.");
                    }
                } // end synchronized
                recognizer.startRecognition(true);
                log.fine("Поток распознавателя возобновлен.");
            } // end if
            // как отменить это действие при получении pauseFlag?
            String utterance = recognizer.getResult().getHypothesis();

            analyze(utterance);
            log.info(result);
        }
        log.info("Поток распознавателя завершен.");
        recognizer.stopRecognition();
    }

    public boolean paused() {
        return pauseFlag;
    }

    public void proceed() {
        pauseFlag = false;
        uiRefresh();
        synchronized (recognizer) {
            recognizer.notify();
        }
    }

    public void pause() {
        pauseFlag = true;
        uiRefresh();
    }

    public void close() {
        this.isActive = false;
    }

    protected void uiRefresh() {}

    protected abstract void analyze(String command);
}
