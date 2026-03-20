package com.angrysurfer.grid.visualization.font;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FontLoader {
    private static FontLoader instance;
    private final Map<Character, LedFont> fontCache = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    
    // Map special characters to filenames
    private static final Map<Character, String> SPECIAL_CHAR_FILENAMES = new HashMap<>();
    static {
        SPECIAL_CHAR_FILENAMES.put('~', "tilde");
        SPECIAL_CHAR_FILENAMES.put('!', "exclamation");
        SPECIAL_CHAR_FILENAMES.put('@', "at");
        SPECIAL_CHAR_FILENAMES.put('#', "hash");
        SPECIAL_CHAR_FILENAMES.put('$', "dollar");
        SPECIAL_CHAR_FILENAMES.put('%', "percent");
        SPECIAL_CHAR_FILENAMES.put('^', "caret");
        SPECIAL_CHAR_FILENAMES.put('&', "ampersand");
        SPECIAL_CHAR_FILENAMES.put('*', "asterisk");
        SPECIAL_CHAR_FILENAMES.put('(', "lparen");
        SPECIAL_CHAR_FILENAMES.put(')', "rparen");
        SPECIAL_CHAR_FILENAMES.put('-', "minus");
        SPECIAL_CHAR_FILENAMES.put('_', "underscore");
        SPECIAL_CHAR_FILENAMES.put('=', "equals");
        SPECIAL_CHAR_FILENAMES.put('+', "plus");
        SPECIAL_CHAR_FILENAMES.put('[', "lbracket");
        SPECIAL_CHAR_FILENAMES.put('{', "lcurly");
        SPECIAL_CHAR_FILENAMES.put(']', "rbracket");
        SPECIAL_CHAR_FILENAMES.put('}', "rcurly");
        SPECIAL_CHAR_FILENAMES.put(';', "semicolon");
        SPECIAL_CHAR_FILENAMES.put(':', "colon");
        SPECIAL_CHAR_FILENAMES.put('\'', "quote");
        SPECIAL_CHAR_FILENAMES.put('"', "dquote");
        SPECIAL_CHAR_FILENAMES.put(',', "comma");
        SPECIAL_CHAR_FILENAMES.put('<', "lt");
        SPECIAL_CHAR_FILENAMES.put('.', "period");
        SPECIAL_CHAR_FILENAMES.put('>', "gt");
        SPECIAL_CHAR_FILENAMES.put('/', "slash");
        SPECIAL_CHAR_FILENAMES.put('?', "question");
    }

    public static FontLoader getInstance() {
        if (instance == null) {
            instance = new FontLoader();
        }
        return instance;
    }

    public LedFont getFont(char c) {
        return fontCache.computeIfAbsent(c, this::loadFont);
    }

    private LedFont loadFont(char c) {
        try {
            String filename;
            if (Character.isDigit(c)) {
                filename = String.valueOf(c);
            } else if (Character.isLetter(c)) {
                if (Character.isLowerCase(c)) {
                    filename = "lowercase_" + c;
                } else {
                    filename = String.valueOf(c);
                }
            } else {
                filename = SPECIAL_CHAR_FILENAMES.getOrDefault(c, "space");
            }
            
            String path = "/fonts/led/" + filename + ".json";
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                throw new RuntimeException("Font file not found: " + path);
            }
            return mapper.readValue(is, LedFont.class);
        } catch (Exception e) {
            throw new RuntimeException("Error loading font for character: " + c, e);
        }
    }
}
