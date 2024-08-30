package com.cleanroommc.groovyscript.sandbox;

import net.minecraftforge.fml.common.Loader;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtil {

    private static final Pattern p = Pattern.compile("^(file:/*)?([a-z])(:/.*)$");

    public static String relativize(String rootPath, String longerThanRootPath) {
        longerThanRootPath = fixUri(longerThanRootPath, true);
        rootPath = fixPath(rootPath);

        if (File.separatorChar != '/') {
            longerThanRootPath = longerThanRootPath.replace('/', File.separatorChar);
        }
        return relativizeInternal(rootPath, longerThanRootPath);
    }

    private static String relativizeInternal(String rootPath, String longerThanRootPath) {
        int index = longerThanRootPath.indexOf(rootPath);
        if (index < 0) {
            throw new IllegalArgumentException("The path '" + longerThanRootPath + "' does not contain the root path '" + rootPath + "'");
        }
        return longerThanRootPath.substring(index + rootPath.length() + 1);
    }

    public static URI fixUri(URI uri) {
        return fixUri(uri.toString());
    }

    public static URI fixUri(String uri) {
        return URI.create(fixUri(uri, true));
    }

    public static String fixUri(String uri, boolean reEncode) {
        try {
            String s = uri, uri1;
            boolean decoded = false;
            boolean c;
            do {
                uri1 = s;
                s = URLDecoder.decode(uri1, StandardCharsets.UTF_8.displayName());
                c = !uri1.equals(s); // something was decoded
                decoded |= c;
            } while (c); // try to decode again to remove any possible nested decodings
            s = fixPath(s);
            if (decoded && reEncode) s = URLEncoder.encode(s, StandardCharsets.UTF_8.displayName());
            return s;
        } catch (UnsupportedEncodingException e) {
            return uri;
        }
    }

    public static String fixPath(String uri) {
        Matcher matcher = p.matcher(uri);
        if (!matcher.matches()) return uri;
        return matcher.replaceFirst("$1" + matcher.group(2).toUpperCase(Locale.ENGLISH) + "$3");
    }

    public static String getParent(String path) {
        int i = path.lastIndexOf(File.separatorChar);
        if (i <= 0) return StringUtils.EMPTY;
        path = path.substring(0, i);
        if (path.length() == 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') return StringUtils.EMPTY;
        return path;
    }

    public static String makePath(String... pieces) {
        if (pieces == null || pieces.length == 0) return StringUtils.EMPTY;
        if (pieces.length == 1) return sanitizePath(pieces[0]);
        StringBuilder builder = new StringBuilder();
        for (String piece : pieces) {
            if (piece != null && !piece.isEmpty()) {
                builder.append(sanitizePath(piece)).append(File.separatorChar);
            }
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    public static String sanitizePath(String path) {
        return path.replace(getOtherSeparatorChar(), File.separatorChar);
    }

    public static char getOtherSeparatorChar() {
        return File.separatorChar == '/' ? '\\' : '/';
    }

    public static File makeFile(String... pieces) {
        return new File(makePath(pieces));
    }

    public static String getMinecraftHome() {
        return Loader.instance().getConfigDir().getParent();
    }

    public static boolean mkdirs(File file) {
        if (file.isDirectory()) {
            return file.mkdirs();
        }
        return file.getParentFile().mkdirs();
    }

    public static boolean mkdirsAndFile(File file) {
        boolean b = mkdirs(file);
        if (file.isFile()) {
            try {
                Files.createFile(file.toPath());
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        return b;
    }
}
