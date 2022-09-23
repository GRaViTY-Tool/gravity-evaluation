package org.gravity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataPreparator {

	private static final String KIND = "size";
	private static final String INPUT = "data.txt";
	private static final String MEASURE = "Measure: ";

	public static void main(final String[] args) throws IOException {
		out = new File(new File("out"), KIND);
		out.mkdirs();
		final var values = readValues();
		writeProjects(values);
		writeSummary(values);

	}

	private static File out;

	private static void writeSummary(final Map<String, Map<String, List<String>>> values) throws IOException {
		try (var averageWriter = new BufferedWriter(new FileWriter(new File(out,"average.csv")));
				var medianWriter = new BufferedWriter(new FileWriter(new File(out, "median.csv")));
				var varianceWriter = new BufferedWriter(new FileWriter(new File(out,"variance.csv")));) {
			final List<String> categories = values.values().parallelStream()
					.flatMap(v -> v.keySet().parallelStream()).distinct().collect(Collectors.toList());

			append("project,",averageWriter, medianWriter, varianceWriter);
			append(String.join(",", categories),averageWriter, medianWriter, varianceWriter);
			newLine(averageWriter, medianWriter, varianceWriter);

			for (final Entry<String, Map<String, List<String>>> projectEntry : values.entrySet().parallelStream()
					.sorted((arg0, arg1) -> arg0.getKey().compareTo(arg1.getKey())).collect(Collectors.toList())) {
				append(projectEntry.getKey(),averageWriter, medianWriter, varianceWriter);
				final var projectValues = projectEntry.getValue();
				for (final String c : categories) {
					append(",",averageWriter, medianWriter, varianceWriter);
					if (projectValues.containsKey(c)) {
						final var data = projectValues.get(c).parallelStream()
								.mapToInt(Integer::parseInt).sorted().toArray();
						final var average = IntStream.of(data).average().getAsDouble();
						averageWriter.append(Double.toString(average));
						medianWriter.append(Integer.toString(data[data.length / 2]));
						var variance = 0D;
						for (final int element : data) {
							variance += Math.pow(element - average, 2);
						}
						variance /= data.length;
						varianceWriter.append(Double.toString(variance));
					}
				}
				newLine(averageWriter, medianWriter, varianceWriter);
			}
		}
	}

	private static void newLine(final BufferedWriter... writers)
			throws IOException {
		for(final BufferedWriter writer : writers) {
			writer.newLine();
		}
	}

	public static void append(final String value, final BufferedWriter... writers)
			throws IOException {
		for(final BufferedWriter writer : writers) {
			writer.append(value);
		}
	}

	private static void writeProjects(final Map<String, Map<String, List<String>>> values) throws IOException {
		for (final Entry<String, Map<String, List<String>>> projectEntry : values.entrySet()) {
			final var projectValues = projectEntry.getValue();
			final List<String> categories = new ArrayList<>(projectValues.keySet());
			try (var writer = new BufferedWriter(new FileWriter(new File(out,projectEntry.getKey() + ".csv")))) {
				writer.append(String.join(",", categories));
				writer.newLine();
				var valueIndex = 0;
				while (true) {
					var dataLeft = false;
					final List<String> nextData = new ArrayList<>(categories.size());
					for (final String c : categories) {
						final var categoryData = projectValues.get(c);
						if (categoryData.size() > valueIndex) {
							nextData.add(categoryData.get(valueIndex));
							dataLeft |= true;
						} else {
							nextData.add("");
						}
					}
					valueIndex++;
					if (dataLeft) {
						writer.append(String.join(",", nextData));
						writer.newLine();
					} else {
						break;
					}
				}
			}

		}
	}

	private static Map<String, Map<String, List<String>>> readValues() throws IOException {
		final Map<String, Map<String, List<String>>> values = new HashMap<>();
		Map<String, List<String>> projectMap = null;

		var index = 1;
		for (final String line : Files.readAllLines(Paths.get("in", KIND, INPUT))) {
			if (line.startsWith(MEASURE)) {
				projectMap = values.computeIfAbsent(line.substring(MEASURE.length()), k -> new HashMap<>());

			} else if ((projectMap != null) && (line.endsWith("ms") || Character.isDigit(line.charAt(line.length()-1)))) {
				final var data = line.substring(0, line.length() - 2).split(":");
				if (data.length == 2) {
					projectMap.computeIfAbsent(data[0].toLowerCase().trim(), k -> new LinkedList<>()).add(data[1].trim());
				} else {
					System.err.println("Illegal line (" + index + "): " + line);
				}
			} else {
				System.err.println("Illegal line (" + index + "): " + line);
			}
			index++;
		}
		return values;
	}

}
