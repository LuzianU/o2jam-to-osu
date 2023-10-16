package luzianu;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.logging.Level;

import org.open2jam.parsers.Chart;
import org.open2jam.parsers.ChartList;
import org.open2jam.parsers.ChartParser;
import org.open2jam.parsers.utils.Logger;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Locale.setDefault(Locale.US);
        Logger.global.setLevel(Level.OFF);

        if (!new File("audio_sample_mixer.exe").exists()) {
            System.err.println(
                    "audio_sample_mixer.exe not found. Please download and put next to this .jar: https://github.com/LuzianU/audio-sample-mixer/releases/latest");
            System.exit(1);
        }

        String input_val = null;
        String output_val = null;
        String serverTag_val = "";
        float od_val = Float.NaN;
        boolean sv_val = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "--server":
                    i++;
                    if (i < args.length) {
                        serverTag_val = args[i];
                    } else {
                        throw new RuntimeException("Error: Expected a value after --server");
                    }
                    break;
                case "--od":
                    i++;
                    if (i < args.length) {
                        try {
                            od_val = Float.parseFloat(args[i]);
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("Error: Invalid float value after --od");
                        }
                    } else {
                        throw new RuntimeException("Error: Expected a value after --od");
                    }
                    break;
                case "-i":
                    i++;
                    if (i < args.length) {
                        input_val = args[i];
                    } else {
                        throw new RuntimeException("Error: Expected a value after -i");
                    }
                    break;
                case "-o":
                    i++;
                    if (i < args.length) {
                        output_val = args[i];
                    } else {
                        throw new RuntimeException("Error: Expected a value after -o");
                    }
                    break;
                case "--sv":
                    sv_val = true;
                    break;
            }
        }

        if (input_val == null || output_val == null) {
            throw new RuntimeException(
                    "Error: Please provide a input and output argument.\n\nRequired Arguments:\n-i <input_dir_or_ojn>\n-o <output_dir>\n\nOptional Arguments:\n--server <server_tag>\n--od <od_value>\n--sv");
        }

        // java moment
        final float od = od_val;
        final String serverTag = serverTag_val;
        final File input = new File(input_val);
        final File output = new File(output_val);
        final boolean sv = sv_val;

        try {
            if (input.isDirectory()) {
                Path startPath = input.toPath();
                java.nio.file.Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                        convert(path.toFile(), output, od, serverTag, sv);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                convert(input, output, od, serverTag, sv);
            }
        } catch (Exception e) {
            System.err.println("could not read input");
        }
    }

    private static void convert(final File input, final File output, final float od, final String serverTag,
            final boolean sv) {
        if (input.getName().endsWith(".ojn")) {
            try {
                ChartList chartList = ChartParser.parseFile(input);
                Chart chart = chartList.get(2);
                ChartConverter.toOsu(chart, output.getAbsolutePath().toString(), od, serverTag, sv);
            } catch (Exception e) {
                System.err.println("could not parse: " + input.getAbsolutePath().toString());
            }
        }
    }

}