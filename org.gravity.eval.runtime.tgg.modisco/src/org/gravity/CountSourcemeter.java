package org.gravity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CountSourcemeter {
	public static void main(final String[] args) throws IOException {

		final var base = new File("/home/speldszus/Documents/git/gravity-eval-projects-ase16/statistics/");

		try (var writer = new BufferedWriter(new FileWriter(new File(base, "statistics.csv")))) {
			writer.write("project,LLOC,classes,enums,interfaces,types,methods,fields,members\n");
			for (final File project : Stream.of(base.listFiles()).sorted().collect(Collectors.toList())) {
				final var folder = new File(project, "java");
				if (folder.exists()) {
					final var valueFolder = Stream.of(folder.listFiles()).max(File::compareTo).get();

					writer.append('\n');
					final var name = project.getName();
					writer.append(name);

					final var lines = Files.readAllLines(new File(valueFolder, name+"-Package.csv").toPath());
					final var index = Arrays.asList(lines.get(0).split(",")).indexOf("\"LLOC\"");
					var lloc = 0;
					for(var i = 1; i < lines.size(); i++) {
						lloc += Integer.valueOf(lines.get(i).split(",")[index].replace("\"", ""));
					}
					writer.append(',');
					writer.append(Integer.toString(lloc));


					final var classes = append(writer, valueFolder, name, "-Class.csv");
					final var enums = append(writer, valueFolder, name, "-Enum.csv");
					final var interfaces = append(writer, valueFolder, name, "-Interface.csv");
					writer.append(',');
					writer.append(Integer.toString(classes + enums + interfaces));


					final var methods = append(writer, valueFolder, name, "-Method.csv");
					final var fields = append(writer, valueFolder, name, "-Attribute.csv");
					writer.append(',');
					writer.append(Integer.toString(methods + fields));
				}
			}
		}
	}

	public static int append(final BufferedWriter writer, final File valueFolder, final String name, final String key)
			throws IOException {
		writer.append(',');
		final var attributes = Files.readAllLines(new File(valueFolder, name + key).toPath()).size() - 1;
		writer.append(Integer.toString(attributes));
		return attributes;
	}
}
