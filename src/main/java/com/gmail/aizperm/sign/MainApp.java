package com.gmail.aizperm.sign;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;

public class MainApp {
    private static int inCount = 0;
    private static int outCount = 0;

    public static void main(String[] args) throws IOException {
        Config localConfig = getConfig();
        Path inPath = Paths.get(localConfig.getInputPath());
        Path outPath = Paths.get(localConfig.getOutPath());
        Files.walkFileTree(inPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path filename = file.getFileName();
                if (filename.toString().endsWith(".jpg")) {
                    inCount++;
                    System.out.println("Finded file " + file);
                    Path relativePath = inPath.relativize(file);
                    Path newFile = outPath.resolve(relativePath);
                    newFile.getParent().toFile().mkdirs();
                    boolean done = false;
                    int count = 0;
                    while (!done && count < 5) {
                        String inAbsolutePath = file.toAbsolutePath().toString();
                        String outAbsolutePath = newFile.toAbsolutePath().toString();
                        try {
                            drawText(inAbsolutePath, outAbsolutePath);
                            done = true;
                        } catch (Throwable localThrowable) {
                            System.err.println(localThrowable);
                        } finally {
                            count++;
                        }
                    }
                    if (done) {
                        System.out.println("File completed '" + newFile + "'");
                        outCount++;
                    } else System.out.println("File not completed " + newFile + "'");
                }
                return FileVisitResult.CONTINUE;
            }
        });
        if (inCount == outCount)
            System.out.println("All files signed: " + outCount);
        else System.out.println("WARNING! In files size: " + inCount + " but signed: " + outCount);

    }

    private static Config getConfig() throws IOException {
        Config localConfig = new Config();
        InputStream inStream = null;
        File localFile = new File("prefs.properties");
        if (localFile.exists())
            inStream = new FileInputStream(localFile);
        else inStream = MainApp.class.getResourceAsStream("/prefs.properties");

        if (inStream != null) {
            Properties localProperties = new Properties();
            localProperties.load(inStream);
            String str1 = localProperties.getProperty("scale");
            if (str1 != null) {
                localConfig.setScale(Float.parseFloat(str1));
            }
            String str2 = localProperties.getProperty("font");
            if (str2 != null) {
                localConfig.setFontFamily(str2);
            }
            String str3 = localProperties.getProperty("color");
            if (str3 != null) {
                localConfig.setColor(str3);
            }
            String str4 = localProperties.getProperty("input");
            if (str4 != null) {
                localConfig.setInputPath(str4);
            }
            String str5 = localProperties.getProperty("output");
            if (str5 != null) {
                localConfig.setOutPath(str5);
            }
            String glyphColor = localProperties.getProperty("glyphColor");
            if (glyphColor != null) {
                localConfig.setGlyphColor(glyphColor);
            }
        }
        return localConfig;
    }

    private static void drawText(String inAbsolutePath, String outAbsolutePath) throws IOException, FontFormatException {
        System.out.println("Start image: " + inAbsolutePath);
        BufferedImage localBufferedImage = ImageIO.read(new File(inAbsolutePath));
        Graphics localGraphics = localBufferedImage.getGraphics();
        int i = localBufferedImage.getHeight();
        int j = localBufferedImage.getWidth();
        Config localConfig = getConfig();
        int k = (int) (i * localConfig.getScale());
        localGraphics.setColor(Color.decode(localConfig.getColor()));
        FontPref localFontPref = FontPref.builder().systemFont().fontFamily(localConfig.getFontFamily()).size(k).type(0).build();
        localFontPref.apply(localGraphics);
        String str = "Лесная сказка";
        int m = localGraphics.getFontMetrics().stringWidth(str);
        int n = j - m;
        localGraphics.drawString(str, n - 10, i - 10);
        if ((localGraphics instanceof Graphics2D)) {
            Graphics2D graphics = (Graphics2D) localGraphics;
            Font font = localFontPref.createFont();
            GlyphVector glyphVector = font.createGlyphVector(graphics.getFontRenderContext(), str);
            Shape shape = glyphVector.getOutline();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            BasicStroke localBasicStroke = new BasicStroke(1.0F);
            graphics.setColor(Color.decode(localConfig.getGlyphColor()));
            graphics.setStroke(localBasicStroke);
            graphics.translate(n - 10, i - 10);
            graphics.draw(shape);
        }
        ImageWriter iWriter = (ImageWriter) ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam writeParam = ((ImageWriter) iWriter).getDefaultWriteParam();
        writeParam.setCompressionMode(2);
        writeParam.setCompressionQuality(1.0F);
        ImageOutputStream iOut = ImageIO.createImageOutputStream(new File(outAbsolutePath));
        iWriter.setOutput(iOut);
        IIOImage iImage = new IIOImage(localBufferedImage, null, null);
        iWriter.write(null, iImage, writeParam);
        iWriter.dispose();
        System.out.println("End: " + inAbsolutePath);
    }

    private static void printAllFonts() {
        String[] arrayOfString1 = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String str : arrayOfString1) {
            System.out.println(str);
        }
    }

    static class FontPref {
        boolean systemFont;
        String fontFamily;
        File path;
        int size;
        int type;

        private FontPref(Builder paramBuilder) {
            this.systemFont = paramBuilder.systemFont;
            this.fontFamily = paramBuilder.fontFamily;
            this.path = paramBuilder.path;
            this.size = paramBuilder.size;
            this.type = paramBuilder.type;
        }

        public Font createFont()
                throws IOException, FontFormatException {
            if (this.systemFont) {
                return new Font(this.fontFamily, this.type, this.size);
            }
            File localFile = new File("Testo.ttf");
            System.out.println(localFile.getAbsolutePath());
            Font localFont = Font.createFont(0, localFile);
            GraphicsEnvironment localGraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            localGraphicsEnvironment.registerFont(localFont);
            return localFont;
        }

        void apply(Graphics paramGraphics)
                throws IOException, FontFormatException {
            paramGraphics.setFont(createFont());
        }

        static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            boolean systemFont;
            String fontFamily;
            File path;
            int size;
            int type;

            Builder systemFont() {
                this.systemFont = true;
                return this;
            }

            Builder fontFamily(String paramString) {
                this.fontFamily = paramString;
                return this;
            }

            Builder path(File paramFile) {
                this.path = paramFile;
                return this;
            }

            Builder size(int paramInt) {
                this.size = paramInt;
                return this;
            }

            Builder type(int paramInt) {
                this.type = paramInt;
                return this;
            }

            MainApp.FontPref build() {
                return new MainApp.FontPref(this);
            }
        }
    }
}
