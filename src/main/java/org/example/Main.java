package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {

        String folder = "pdf";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                Main.class.getClassLoader().getResourceAsStream(folder)))) {
            List<File> files = br.lines()
                    .map(x -> new File(getResourceFile(folder + File.separator + x)))
                    .collect(Collectors.toList());

            for (File file : files) {
                try (PDDocument document = PDDocument.load(file)) {

                    if (!document.isEncrypted()) {

                        System.out.println("\nFile " + file.getName());

                        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                        stripper.setSortByPosition(true);

                        PDFTextStripper tStripper = new PDFTextStripper();

                        String pdfFileInText = tStripper.getText(document);

                        // split by whitespace
                        String lines[] = pdfFileInText.split("\\r?\\n");
                        List<String> parsedLines = new ArrayList<>();
                        boolean isRaw = false;
                        String curLine = "";
                        for (String line : lines) {
                            if (line.startsWith("Страница")) {
                                isRaw = false;
                                if (!curLine.equals("")) {
                                    parsedLines.add(curLine);
                                    curLine = "";
                                }
                            }
                            if (isRaw) {
                                if (line.length() >= 10 && line.charAt(2) == '.' && line.charAt(5) == '.') {
                                    if (!curLine.equals("")) {
                                        parsedLines.add(curLine);
                                    }
                                    curLine = line;
                                } else {
                                    curLine += " " + line;
                                }
                            }
                            if (line.equals("без НДС"))
                                isRaw = true;
                        }

                        Map<LocalDate, Long> traffic = new HashMap<>();

                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                        for (String line : parsedLines) {
                            LocalDate date = LocalDate.parse(line.substring(0, 10), dtf);
                            LocalDate startOfMonth = date.withDayOfMonth(1);
                            for (String word : line.split(" ")) {
                                if (word.endsWith("Kb")) {
                                    Long curKb = Long.parseLong(word.substring(0, word.length() - 2));
                                    traffic.put(startOfMonth, traffic.computeIfAbsent(startOfMonth, (x) -> 0L) + curKb);
                                }
                            }
                        }

                        for (LocalDate localDate : traffic.keySet().stream().sorted().collect(Collectors.toList())) {
                            Long res = traffic.get(localDate);
                            System.out.println("Month:" + localDate.format(dtf) +
                                    " Total: " + res + "Kb, Gb:" +
                                    String.format("%.2f", 1. * res / 1024 / 1024));
                        }

                    }

                }
            }

        }


    }

    private static String getResourceFile(String fileName) {
        return Main.class.getClassLoader().getResource(fileName).getFile();
    }
}
