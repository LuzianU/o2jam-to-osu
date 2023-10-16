package luzianu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.open2jam.parsers.Event.SoundSample;
import org.open2jam.parsers.utils.SampleData;

import luzianu.osu.AudioObject;

public class AudioMixer {
    private Map<Integer, SampleData> samples;
    private Map<Integer, String> sampleIndices;
    private String outputFile;
    private String folderName;

    public AudioMixer(Map<Integer, SampleData> samples, Map<Integer, String> sampleIndices, String folderName,
            String outputFile) {
        this.samples = samples;
        this.sampleIndices = sampleIndices;
        this.outputFile = outputFile;
        this.folderName = folderName;
    }

    public static String removeFileExtension(String filename, boolean removeAllExtensions) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        String extPattern = "(?<!^)[.]" + (removeAllExtensions ? ".*" : "[^.]*$");
        return filename.replaceAll(extPattern, "");
    }

    public boolean checkIfSamplesUsed(List<AudioObject> audioObjects) {
        for (AudioObject audioObject : audioObjects) {
            try {
                SoundSample sample = audioObject.sample;

                if (!sampleIndices.containsKey(sample.sample_id) || !samples.containsKey(sample.sample_id))
                    continue;

                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public float mix(List<AudioObject> audioObjects) throws IOException {
        String temp = System.getProperty("java.io.tmpdir");
        File folder = Paths.get(temp, "o2jam-to-osu", folderName).toFile();
        folder.mkdirs();

        List<String> lines = new ArrayList<>();

        float timeFirst = 0;
        String pathFirst = null;

        float musicOffset = 0;

        for (AudioObject audioObject : audioObjects) {
            try {
                SoundSample sample = audioObject.sample;

                if (!sampleIndices.containsKey(sample.sample_id) || !samples.containsKey(sample.sample_id))
                    continue;

                String name = sampleIndices.get(sample.sample_id);

                name = name.replaceAll("[/:\"*?<>|]", "").trim();

                SampleData data = samples.get(sample.sample_id);

                Path path = Paths.get(folder.getAbsolutePath(), name);
                // data.copyTo(new FileOutputStream(path.toFile()));

                data.copyToFolder(folder);
                // writeInputStreamToFile(data.getInputStream(),
                // path.toAbsolutePath().toString());

                String line = audioObject.time + "," + sample.volume + "," + sample.pan + ",\""
                        + path.toAbsolutePath() + "\"";

                if (lines.isEmpty()) {
                    pathFirst = path.toAbsolutePath().toString();
                    timeFirst = audioObject.time;
                }

                lines.add(line);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (lines.size() == 0) {
            throw new RuntimeException("no audio sample found/used.");
        }

        if (lines.size() == 1) {
            musicOffset = timeFirst;

            File sourceFile = new File(pathFirst);
            File destinationFile = new File(this.outputFile);

            InputStream in = new FileInputStream(sourceFile);
            OutputStream out = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int length;

            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        } else {
            Path csvPath = Paths.get(folder.toString(), folderName + ".csv");
            Path outputPath = Paths.get(outputFile);
            Files.write(csvPath, lines, StandardCharsets.UTF_8);
            callMixer(csvPath, outputPath);
        }

        deleteDirectory(folder);

        return musicOffset;
    }

    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private void callMixer(Path csvPath, Path outputPath) {
        try {
            // Define the path to your .exe file
            String exePath = "audio-sample-mixer.exe";

            // Define the arguments to pass to the executable
            String[] arguments = {
                    exePath,
                    "-i", csvPath.toAbsolutePath().toString(),
                    "-o", outputPath.toAbsolutePath().toString()
            };

            // Create a ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(arguments);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                // System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
