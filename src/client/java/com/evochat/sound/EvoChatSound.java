package com.evochat.sound;

import com.evochat.config.EvoConfig;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Plays a short notification "ding" for private messages.
 *
 * Important: this uses Java's audio line directly, so its loudness is
 * independent from Minecraft in-game sound sliders.
 */
public class EvoChatSound {

    // Debounce: don't spam the sound if many ЛС lines arrive at once
    private static long lastPlayTime = 0;
    private static final long DEBOUNCE_MS = 500;

    // Sound parameters
    private static final float SAMPLE_RATE = 44100f;
    private static final int TONE_MS = 130;
    private static final double FREQ_START = 1240.0;
    private static final double FREQ_END = 980.0;

    public static void playIfEnabled() {
        EvoConfig cfg = EvoConfig.get();
        if (!cfg.soundEnabled) return;

        float volume = Math.max(0f, Math.min(1f, cfg.soundVolume));
        if (volume <= 0.0001f) return;

        long now = System.currentTimeMillis();
        if (now - lastPlayTime < DEBOUNCE_MS) return;
        lastPlayTime = now;

        // Play asynchronously so chat handling/rendering never stalls.
        Thread t = new Thread(() -> playDing(volume), "evochat-notify-sound");
        t.setDaemon(true);
        t.start();
    }

    private static void playDing(float volume) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        int sampleCount = (int) (SAMPLE_RATE * (TONE_MS / 1000.0));
        byte[] data = new byte[sampleCount * 2];

        for (int i = 0; i < sampleCount; i++) {
            double t = i / SAMPLE_RATE;
            double lerp = i / (double) sampleCount;
            double freq = FREQ_START + (FREQ_END - FREQ_START) * lerp;

            // Soft exponential tail for a pleasant chime.
            double envelope = Math.exp(-5.0 * t / (TONE_MS / 1000.0));
            double sample = Math.sin(2.0 * Math.PI * freq * t) * envelope;

            short pcm = (short) (sample * 32767.0 * volume);
            data[i * 2] = (byte) (pcm & 0xFF);
            data[i * 2 + 1] = (byte) ((pcm >> 8) & 0xFF);
        }

        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();
            line.write(data, 0, data.length);
            line.drain();
        } catch (LineUnavailableException ignored) {
            // If audio line is unavailable, silently skip notification sound.
        }
    }
}
