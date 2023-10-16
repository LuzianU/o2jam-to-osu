package luzianu.osu;

import org.open2jam.parsers.Event.SoundSample;

public class AudioObject {
    public float time;
    public SoundSample sample;

    public AudioObject(float time, SoundSample sample) {
        this.time = time;
        this.sample = sample;
    }

}
