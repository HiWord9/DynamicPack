package tests;

import com.adamcalculator.dynamicpack.InputValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class InputValidatorTest {

    @Test
    public void testContentId() {
        Assertions.assertFalse(InputValidator.isContentIdValid(""));
        Assertions.assertFalse(InputValidator.isContentIdValid(" "));
        Assertions.assertFalse(InputValidator.isContentIdValid("  "));
        Assertions.assertFalse(InputValidator.isContentIdValid("   32"));
        Assertions.assertFalse(InputValidator.isContentIdValid("test\ntest"));

        Assertions.assertTrue(InputValidator.isContentIdValid("__"));
        Assertions.assertTrue(InputValidator.isContentIdValid("_-"));
        Assertions.assertTrue(InputValidator.isContentIdValid("pack:megapack"));
        Assertions.assertTrue(InputValidator.isContentIdValid("1234567890"));
        Assertions.assertTrue(InputValidator.isContentIdValid("01"));
        Assertions.assertTrue(InputValidator.isContentIdValid("test_pack"));
        Assertions.assertTrue(InputValidator.isContentIdValid("super:mega_puper:"));
    }

    @Test
    public void testRemoteName() {
        Assertions.assertFalse(InputValidator.isPackNameValid("\n"));
        Assertions.assertTrue(InputValidator.isPackNameValid("__"));
    }

    @Test
    public void testUrls() {
        Assertions.assertTrue(InputValidator.isUrlValid("https://google.com"));
        Assertions.assertTrue(InputValidator.isUrlValid("https://324234.github.io/"));
        Assertions.assertTrue(InputValidator.isUrlValid("https://google.com/f"));
        Assertions.assertTrue(InputValidator.isUrlValid("https://google.net/fi/1/%s_1234567890-+.json?x=81723731+343+1"));
        Assertions.assertFalse(InputValidator.isUrlValid("https://google.com`"));
        Assertions.assertFalse(InputValidator.isUrlValid("https://google.com "));
        Assertions.assertFalse(InputValidator.isUrlValid("https://google.com /"));
        Assertions.assertFalse(InputValidator.isUrlValid("https://google.com*!@#$%^&*()"));
    }

    @Test
    public void testPaths() {

        InputValidator.throwIsPathInvalid("/file/p.txt");

        Assertions.assertThrows(SecurityException.class, () -> {
            InputValidator.throwIsPathInvalid("");
        });

        Assertions.assertThrows(SecurityException.class, () -> {
            InputValidator.throwIsPathInvalid(" ");
        });


        Assertions.assertThrows(SecurityException.class, () -> {
            InputValidator.throwIsPathInvalid("!/file/p.txt");
        });

        Assertions.assertThrows(SecurityException.class, () -> {
            InputValidator.throwIsPathInvalid(null);
        });
        Assertions.assertThrows(SecurityException.class, () -> {
            InputValidator.throwIsPathInvalid("$@#");
        });
        Assertions.assertThrows(SecurityException.class, () -> {
            InputValidator.throwIsPathInvalid("!\"/file/p.txt");
        });        Assertions.assertThrows(SecurityException.class, () -> {
            InputValidator.throwIsPathInvalid("!/fil*&^%544e/p.txt");
        });
        Assertions.assertThrows(SecurityException.class, () -> {
            byte[] b = new byte[128];
            new Random().nextBytes(b);
            InputValidator.throwIsPathInvalid(new String(b));
        });


        try {
            byte[] b = new byte[128];
            new Random().nextBytes(b);
            InputValidator.throwIsPathInvalid(new String(b));
        } catch (Exception e) {
            System.out.println(e);
        }

        Assertions.assertThrows(SecurityException.class, () -> {
            InputValidator.throwIsPathInvalid("~");
        });
        Assertions.assertThrows(SecurityException.class, () -> {
            InputValidator.throwIsPathInvalid("()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)()*)*&YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY)");
        });

    }
}
