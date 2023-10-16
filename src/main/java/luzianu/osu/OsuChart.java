package luzianu.osu;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.open2jam.parsers.Chart;

public class OsuChart {
    public List<TimingPoint> timingPoints;
    public List<HitObject> hitObjects;

    public OsuChart() {
        timingPoints = new ArrayList<>();
        hitObjects = new ArrayList<>();
    }

    private static void exportImage(BufferedImage image, String filePath, String formatName) {
        try {
            File file = new File(filePath);
            ImageIO.write(image, formatName, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void export(String filePath, Chart chart, String audioFilename, float musicOffset, float od, float hp,
            String serverTag) {
        String bgString = "";
        if (chart.hasCover()) {
            String parent = new File(filePath).getParentFile().getAbsolutePath();
            BufferedImage cover = chart.getCover();
            String coverFile = Paths.get(parent, "bg.png").toAbsolutePath().toString();

            if (cover != null) {
                exportImage(cover, coverFile, "png");
            }
            bgString = "0,0,\"bg.png\",0,0";
        }

        String hpString = "" + hp;

        String odString = "7";

        if (chart.getLevel() >= 80)
            odString = "6";
        if (chart.getLevel() >= 100)
            odString = "5";

        if (!Float.isNaN(od))
            odString = "" + od;

        String levelString = "[" + chart.getLevel() + "]";

        if (!serverTag.isEmpty()) {
            levelString = "[" + serverTag + "] " + levelString;
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filePath),
                StandardCharsets.UTF_8)) {
            writer.write(String.format(
                    "osu file format v14\n" +
                            "\n" +
                            "[General]\n" +
                            "AudioFilename: %s\n" +
                            "AudioLeadIn: 0\n" +
                            "PreviewTime: -1\n" +
                            "Countdown: 0\n" +
                            "SampleSet: Soft\n" +
                            "StackLeniency: 0.7\n" +
                            "Mode: 3\n" +
                            "LetterboxInBreaks: 0\n" +
                            "SpecialStyle: 1\n" +
                            "WidescreenStoryboard: 0\n" +
                            "\n" +
                            "[Editor]\n" +
                            "DistanceSpacing: 1\n" +
                            "BeatDivisor: 6\n" +
                            "GridSize: 4\n" +
                            "TimelineZoom: 1\n" +
                            "\n" +
                            "[Metadata]\n" +
                            "Title:%s\n" +
                            "Title:%s\n" +
                            "Artist:%s\n" +
                            "ArtistUnicode:%s\n" +
                            "Creator:%s\n" +
                            "Version:[O2Jam] %s\n" +
                            "Source:O2Jam\n" +
                            "Tags:%s 5ynt3ck convert %s\n" +
                            "\n" +
                            "[Difficulty]\n" +
                            "HPDrainRate:%s\n" +
                            "CircleSize:7\n" +
                            "OverallDifficulty:%s\n" +
                            "ApproachRate:0\n" +
                            "SliderMultiplier:1\n" +
                            "SliderTickRate:1\n" +
                            "\n" +
                            "[Events]\n" +
                            "%s\n",
                    audioFilename, chart.getTitle(),
                    chart.getTitle(),
                    chart.getArtist(), chart.getArtist(), chart.getNoter(), levelString,
                    (serverTag.isEmpty() ? "O2Jam" : ("O2Jam " + serverTag)),
                    chart.getSource().getName(), hpString, odString, bgString));

            writer.write("[TimingPoints]\n");
            int inheritedCount = 0;
            for (TimingPoint t : timingPoints) {
                if (!t.uninherited) {
                    inheritedCount++;
                }
            }
            for (int i = 0; i < timingPoints.size(); i++) {
                timingPoints.get(i).time -= musicOffset;
                timingPoints.get(i).time = Math.max(0, timingPoints.get(i).time);
                if (i + 1 < timingPoints.size()) {
                    TimingPoint next = timingPoints.get(i + 1);
                    if (next.time <= musicOffset && next.uninherited) {
                        continue;
                    }
                }
                if (i == timingPoints.size() - 1 && !timingPoints.get(i).uninherited && inheritedCount <= 1) {
                    continue;
                }
                writer.write(timingPoints.get(i).toString());
                writer.write("\n");
            }
            // for (TimingPoint t : timingPoints) {
            // t.time -= musicOffset;
            // t.time = Math.max(0, t.time);
            // writer.write(t.toString());
            // writer.write("\n");
            // }

            writer.write("[HitObjects]\n");
            for (HitObject t : hitObjects) {
                t.time -= musicOffset;
                if (t.endTime > 0)
                    t.endTime -= musicOffset;
                writer.write(t.toString());
                writer.write("\n");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
