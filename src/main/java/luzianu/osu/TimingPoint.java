package luzianu.osu;

public class TimingPoint {
    double time;
    double beatLength;
    int meter;
    int sampleSet;
    int sampleIndex;
    int volume;
    boolean uninherited;
    int effects;

    public TimingPoint(double time, double beatLength, int meter, int sampleSet, int sampleIndex, int volume,
            boolean uninherited, int effects) {
        this.time = time;
        this.beatLength = beatLength;
        this.meter = meter;
        this.sampleSet = sampleSet;
        this.sampleIndex = sampleIndex;
        this.volume = volume;
        this.uninherited = uninherited;
        this.effects = effects;
    }

    public static TimingPoint simple(double time, double bpm) {
        return new TimingPoint(time, 60000 / bpm, 4, 0, 0, 30, true, 0);
    }

    public static TimingPoint noSV(double time, double bpm, double chartBpm) {
        double factor = chartBpm / bpm;
        return new TimingPoint(time, -100 / factor, 0, 0, 0, 30, false, 0);
    }

    @Override
    public String toString() {
        return String.format("%.2f,%.2f,%d,%d,%d,%d,%d,%d", time, beatLength, meter, sampleSet, sampleIndex, volume,
                uninherited ? 1 : 0, effects);
    }
}