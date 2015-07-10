package org.esa.cci.sst.assessment;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.PositionInParagraph;
import org.apache.poi.xwpf.usermodel.TextSegement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

class PoiWordDocument {

    public static final double DEFAULT_SCALE = 0.2;
    private final XWPFDocument document;

    public PoiWordDocument() {
        document = new XWPFDocument();
    }

    public PoiWordDocument(URL url) throws IOException, URISyntaxException {
        this(new File(url.toURI()));
    }

    public PoiWordDocument(File file) throws IOException {
        final FileInputStream inputStream = new FileInputStream(file);
        document = new XWPFDocument(inputStream);
    }

    public void save(File targetFile) throws IOException {
        final FileOutputStream outputStream = new FileOutputStream(targetFile);
        document.write(outputStream);
    }

    public boolean containsVariable(String variable) {
        final List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (XWPFParagraph paragraph : paragraphs) {
            final TextSegement textSegement = paragraph.searchText(variable, new PositionInParagraph());
            if (textSegement != null) {
                return true;
            }
        }
        return false;
    }

    public void replaceWithText(String variable, String text) {
        final List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (XWPFParagraph paragraph : paragraphs) {
            final TextSegement textSegement = paragraph.searchText(variable, new PositionInParagraph());
            if (textSegement != null) {
                replaceVariable(variable, text, paragraph, textSegement);
            }
        }
    }

    public void replaceParagraphText(String variable, String paragraphText) {
        final List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (XWPFParagraph paragraph : paragraphs) {
            final TextSegement textSegement = paragraph.searchText(variable, new PositionInParagraph());
            if (textSegement != null) {
                replaceParagraphText(paragraphText, paragraph);
            }
        }
    }

    public void replaceWithFigure(String variable, File imageFile) throws IOException, InvalidFormatException {
        final List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (XWPFParagraph paragraph : paragraphs) {
            final TextSegement textSegement = paragraph.searchText(variable, new PositionInParagraph());
            if (textSegement != null) {
                replaceVariable(imageFile, paragraph, textSegement, DEFAULT_SCALE);
            }
        }
    }

    public void replaceWithFigure(String variable, File imageFile, double scale) throws IOException, InvalidFormatException {
        final List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (XWPFParagraph paragraph : paragraphs) {
            final TextSegement textSegement = paragraph.searchText(variable, new PositionInParagraph());
            if (textSegement != null) {
                replaceVariable(imageFile, paragraph, textSegement, scale);
            }
        }
    }

    private void replaceParagraphText(String paragraphText, XWPFParagraph paragraph) {
        final List<XWPFRun> runs = paragraph.getRuns();
        // set text to first run;
        final XWPFRun firstRun = runs.get(0);
        firstRun.setText(paragraphText, 0);

        clearRunText(runs, 1, runs.size());
    }


    private void replaceVariable(String variable, String text, XWPFParagraph paragraph, TextSegement textSegement) {
        final List<XWPFRun> runs = paragraph.getRuns();
        final int beginRun = textSegement.getBeginRun();
        final int endRun = textSegement.getEndRun();

        if (beginRun == endRun) {
            // replace single run variable
            final XWPFRun run = runs.get(beginRun);
            final String runText = run.getText(run.getTextPosition());
            final String replaced = runText.replace(variable, text);
            run.setText(replaced, 0);
        } else {
            // variable is spread over multiple runs
            // assemble all runs that contain the variable
            final StringBuilder builder = new StringBuilder();
            for (int runPos = beginRun; runPos <= endRun; runPos++) {
                final XWPFRun run = runs.get(runPos);
                builder.append(run.getText(run.getTextPosition()));
            }
            final String connectedRuns = builder.toString();
            final String replaced = connectedRuns.replace(variable, text);

            // The first Run receives the replaced String of all connected Runs
            final XWPFRun partOne = runs.get(beginRun);
            partOne.setText(replaced, 0);

            clearRunText(runs, beginRun, endRun + 1);
        }
    }

    private void replaceVariable(File image, XWPFParagraph paragraph, TextSegement textSegement, double scale) throws IOException, InvalidFormatException {
        final List<XWPFRun> runs = paragraph.getRuns();
        final int beginRun = textSegement.getBeginRun();
        final int endRun = textSegement.getEndRun();

        FileInputStream inputStream = new FileInputStream(image);
        final BufferedImage bufferedImage = ImageIO.read(inputStream);
        final int width = Units.toEMU(bufferedImage.getWidth() * scale);
        final int height = Units.toEMU(bufferedImage.getHeight() * scale);
        inputStream.close();

        inputStream = new FileInputStream(image);
        if (beginRun == endRun) {
            // replace single run variable
            final XWPFRun run = runs.get(beginRun);
            run.setText("", 0);
            run.addPicture(inputStream, XWPFDocument.PICTURE_TYPE_PNG, image.getName(), width, height);
        } else {
            // variable is spread over multiple runs

            final XWPFRun partOne = runs.get(beginRun);
            partOne.setText("", 0);
            partOne.addPicture(inputStream, XWPFDocument.PICTURE_TYPE_PNG, image.getName(), width, height);

            clearRunText(runs, beginRun, endRun + 1);
        }
    }

    private void clearRunText(List<XWPFRun> runs, int beginRun, int endRun) {
        for (int runPos = beginRun + 1; runPos < endRun; runPos++) {
            XWPFRun partNext = runs.get(runPos);
            partNext.setText("", 0);
        }
    }
}
