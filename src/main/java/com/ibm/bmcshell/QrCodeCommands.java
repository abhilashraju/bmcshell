package com.ibm.bmcshell;

import com.google.zxing.WriterException;
import com.ibm.bmcshell.QRCodeGenerator;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@ShellComponent
public class QrCodeCommands extends CommonCommands {
    private final QRCodeGenerator qrCodeGenerator;

    public QrCodeCommands() throws IOException {
        super();
        this.qrCodeGenerator = new QRCodeGenerator();
    }

    @ShellMethod(key = "generate.qrcode", value = "Generate a QR code from a given text")
    public void generateQRCode(@ShellOption(value = {"--text", "-t"}) String text) {
        try {
            BufferedImage qrCodeImage = qrCodeGenerator.generateQRCodeImage(text);
            File outputfile = new File("qrcode.png");
            ImageIO.write(qrCodeImage, "png", outputfile);
            System.out.println("QR Code generated and saved as qrcode.png");
        } catch (WriterException | IOException e) {
            e.printStackTrace();
        }
    }
}