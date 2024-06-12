package tests;

import com.adamcalculator.dynamicpack.InputValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class InputValidatorTest {

    @Test
    public void testContentId() {
        Assertions.assertFalse(InputValidator.isDynamicContentIdValid(""));
        Assertions.assertFalse(InputValidator.isDynamicContentIdValid(" "));
        Assertions.assertFalse(InputValidator.isDynamicContentIdValid("  "));
        Assertions.assertFalse(InputValidator.isDynamicContentIdValid("   32"));
        Assertions.assertFalse(InputValidator.isDynamicContentIdValid("test\ntest"));

        Assertions.assertTrue(InputValidator.isDynamicContentIdValid("__"));
        Assertions.assertTrue(InputValidator.isDynamicContentIdValid("_-"));
        Assertions.assertTrue(InputValidator.isDynamicContentIdValid("pack:megapack"));
        Assertions.assertTrue(InputValidator.isDynamicContentIdValid("1234567890"));
        Assertions.assertTrue(InputValidator.isDynamicContentIdValid("01"));
        Assertions.assertTrue(InputValidator.isDynamicContentIdValid("test_pack"));
        Assertions.assertTrue(InputValidator.isDynamicContentIdValid("super:mega_puper:"));
    }

    @Test
    public void testRemoteName() {
        Assertions.assertFalse(InputValidator.isDynamicPackNameValid("\n"));
        Assertions.assertTrue(InputValidator.isDynamicPackNameValid("__"));
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
