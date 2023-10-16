package luzianu;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.open2jam.parsers.Chart;
import org.open2jam.parsers.Event;
import org.open2jam.parsers.Event.Channel;
import org.open2jam.parsers.Event.Flag;
import org.open2jam.parsers.EventList;

import luzianu.osu.AudioObject;
import luzianu.osu.HitObject;
import luzianu.osu.OsuChart;
import luzianu.osu.TimingPoint;

public class ChartConverter {

    public static void toOsu(Chart chart, String outRoot, float od, String serverTag, boolean useSV)
            throws IOException {
        System.out.println(
                "sample count: " + chart.getSamples().size() + "\t" + chart.getSource() + " " + chart.getArtist()
                        + " - "
                        + chart.getTitle());

        EventList eventList = chart.getEvents();
        List<TimingPoint> timingPoints = new ArrayList<>();
        List<HitObject> hitObjects = new ArrayList<>();
        List<AudioObject> audioObjects = new ArrayList<>();

        timingPoints.add(TimingPoint.simple(0, chart.getBPM()));

        double currentBpm = chart.getBPM();
        double lastBpmChangePosition = 0;
        int lastBpmChangeMeasure = 0;
        double timeOffset = 0;

        double[] timeSignatures = new double[eventList.get(eventList.size() - 1).getMeasure() + 1];
        for (int i = 0; i < timeSignatures.length; i++)
            timeSignatures[i] = 1.0;
        for (Event event : eventList) {
            if (event.getChannel() == Channel.TIME_SIGNATURE) {
                timeSignatures[event.getMeasure()] = event.getValue() <= 1e-10 ? 1.0 : event.getValue();
            }
        }

        for (Event event : eventList) {
            Channel channel = event.getChannel();

            if (event.getOffset() != 0) {
                System.err.println("offset != 0");
            }

            if (channel == Channel.STOP ||
                    channel == Channel.BGA ||
                    channel == Channel.MEASURE ||
                    channel == Channel.NONE) {

                System.err.println("unimplemented channel type " + channel.name());
                continue;
            }

            if (channel == Channel.BPM_CHANGE) {
                double prevBpm = currentBpm;
                currentBpm = event.getValue();

                for (int i = lastBpmChangeMeasure; i < event.getMeasure(); i++) {
                    timeOffset += (1 - lastBpmChangePosition) * 60000 * 4 / prevBpm * timeSignatures[i];

                    lastBpmChangePosition = 0;
                    lastBpmChangeMeasure = i + 1;
                }

                double deltaPosition = event.getPosition() - lastBpmChangePosition;

                timeOffset += deltaPosition * 60000 * 4 / prevBpm * timeSignatures[event.getMeasure()];

                lastBpmChangePosition = event.getPosition();
                lastBpmChangeMeasure = event.getMeasure();

                if (60000 / currentBpm >= 0.01) { // fix 0.0 beatLength infinite loading
                    timingPoints.add(TimingPoint.simple(timeOffset, currentBpm));
                    if (!useSV)
                        timingPoints.add(TimingPoint.noSV(timeOffset, currentBpm, chart.getBPM()));
                }
            }

            for (int i = lastBpmChangeMeasure; i < event.getMeasure(); i++) {
                timeOffset += (1 - lastBpmChangePosition) * 60000 * 4 / currentBpm * timeSignatures[i];

                lastBpmChangePosition = 0;
                lastBpmChangeMeasure = i + 1;
            }

            double time = timeOffset;
            double deltaBpmChangePosition = (event.getPosition() - lastBpmChangePosition);
            time += deltaBpmChangePosition * 60000 * 4 / currentBpm;

            int column = channel.ordinal() - 1; // 0 based

            if (column >= 0 && column < 7) { // playable
                if (event.getFlag() == Flag.NONE) {
                    hitObjects.add(HitObject.regular(column, (int) time));
                } else if (event.getFlag() == Flag.HOLD) {
                    hitObjects.add(HitObject.hold(column, (int) time, -1));
                } else if (event.getFlag() == Flag.RELEASE) {
                    boolean found = false;
                    for (int i = hitObjects.size() - 1; i >= 0; i--) {
                        HitObject hitObject = hitObjects.get(i);
                        if (hitObject._column == column && hitObject.endTime == -1) {
                            hitObject.endTime = (int) time;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // throw new RuntimeException("unconnected LN");
                        System.err.println("unconnected ln at " + time);
                    }
                }
            }

            if (channel != Channel.BPM_CHANGE && channel != Channel.MEASURE && channel != Channel.STOP
                    && channel != Channel.TIME_SIGNATURE && channel != Channel.NONE) {

                if (event.getFlag() == Flag.NONE || event.getFlag() == Flag.HOLD) {
                    if (event.getSample().pan != 0) {
                        // System.out.println("at time: " + time + " pan:" + event.getSample().pan);
                    }

                    audioObjects.add(new AudioObject((float) time, event.getSample()));
                }
            }

        }

        hitObjects = postProcessHitObject(hitObjects);

        OsuChart osuChart = new OsuChart();
        osuChart.timingPoints = timingPoints;
        osuChart.hitObjects = hitObjects;

        String name = chart.getArtist() + " - " + chart.getTitle();
        name = name.replaceAll("[/:\"*?<>|]", "").trim();
        while (name.endsWith("."))
            name = name.substring(0, name.length() - 1);

        String folder = "[_O2Jam_] " + name;

        String audioFilename = name + ".ogg";

        Paths.get(outRoot, folder).toFile().mkdirs();

        AudioMixer audioMixer = new AudioMixer(chart.getSamples(),
                chart.getSampleIndex(), folder, Paths.get(outRoot, folder, audioFilename).toAbsolutePath().toString());
        float musicOffset = audioMixer.mix(audioObjects);

        osuChart.export(Paths.get(outRoot, folder, name + ".osu").toAbsolutePath().toString(), chart,
                audioFilename,
                musicOffset, od, serverTag);
    }

    private static List<HitObject> postProcessHitObject(List<HitObject> hitObjects) {
        List<HitObject> processed = new ArrayList<>();
        hitObjects.sort((o1, o2) -> Float.compare(o1.time, o2.time));

        int[] holdEndTimes = new int[7];

        for (HitObject hitObject : hitObjects) {
            int col = hitObject._column;

            if (hitObject.endTime == Integer.MIN_VALUE) {
                // short note
                if (hitObject.time <= holdEndTimes[col]) {
                    // remove this object
                    // its a short note that is inbetween hold start and hold end
                    System.out.println("removed at " + hitObject.time);
                    continue;
                }
            } else {
                // hold

                if (hitObject.time <= holdEndTimes[col]) {
                    // remove this object
                    // its a short note that is inbetween hold start and hold end
                    System.out.println("[LN] removed at " + hitObject.time);

                    processed.stream().filter(o -> o._column == col && o.endTime == holdEndTimes[col]).findFirst()
                            .get().endTime = hitObject.endTime;
                    holdEndTimes[col] = hitObject.endTime;
                    continue;
                }

                holdEndTimes[col] = hitObject.endTime;
            }

            processed.add(hitObject);
        }

        return processed;
    }
}
