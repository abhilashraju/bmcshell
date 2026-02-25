package com.ibm.bmcshell;

public class ColorPrinter {
    public static String colorize(String text, String color) {
        return "\033[" + color + "m" + text + "\033[0m";
    }
    public static String addColor(String text, String color) {
        switch(color.toLowerCase()) {
            case "red":
                return red(text);
            case "green":
                return green(text);
            case "yellow":
                return yellow(text);
            case "blue":
                return blue(text);
            case "magenta":
                return magenta(text);
            case "cyan":
                return cyan(text);
            case "white":
                return white(text);
            default:
                return text; // No color applied
        }
    }
    public static String red(String text) {
        return colorize(text, "31");
    }

    public static String green(String text) {
        return colorize(text, "32");
    }

    public static String yellow(String text) {
        return colorize(text, "33");
    }

    public static String blue(String text) {
        return colorize(text, "34");
    }

    public static String magenta(String text) {
        return colorize(text, "35");
    }

    public static String cyan(String text) {
        return colorize(text, "36");
    }
    public static String white(String text) {
        return colorize(text, "37");
    }
    public static String gray(String text) {
        return colorize(text, "90");
    }
    public static String bold(String text) {
        return "\033[1m" + text + "\033[0m";
    }
    public static String underline(String text) {
        return "\033[4m" + text + "\033[0m";
    }
    public static String reset(String text) {
        return "\033[0m" + text;
    }
    public static String background(String text, String color) {
        return "\033[" + color + "m" + text + "\033[0m";
    }
    public static String bgRed(String text) {
        return background(text, "41");
    }
    public static String bgGreen(String text) {
        return background(text, "42");
    }
    public static String bgYellow(String text) {
        return background(text, "43");
    }
    public static String bgBlue(String text) {
        return background(text, "44");
    }
    public static String bgMagenta(String text) {
        return background(text, "45");
    }
    public static String bgCyan(String text) {
        return background(text, "46");
    }
    public static String bgWhite(String text) {
        return background(text, "47");
    }
    public static String bgBlack(String text) {
        return background(text, "40");
    }
    public static String bgBlackGreenFg(String text) {
        return "\033[40;32m" + text + "\033[0m";
    }
    public static String boldRed(String text) {
        return bold(red(text));
    }
    public static String boldGreen(String text) {
        return bold(green(text));
    }
    public static String boldYellow(String text) {
        return bold(yellow(text));
    }
    public static String boldBlue(String text) {
        return bold(blue(text));
    }
    public static String boldMagenta(String text) {
        return bold(magenta(text));
    }
}
