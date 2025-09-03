package com.sandox.sandbox_service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils; // This class is deprecated
import java.util.Date;
import java.util.Calendar;
import java.net.URL;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;

public class TestExternalDeprecated {

    public static void main(String[] args) throws UnsupportedEncodingException {
        TestExternalDeprecated test = new TestExternalDeprecated();
        test.testDeprecatedMethods();
    }

    public void testDeprecatedMethods() throws UnsupportedEncodingException {
        // 1. Apache Commons Lang3 - StringEscapeUtils (deprecated since 3.6)
        String html = StringEscapeUtils.escapeHtml4("Hello & World"); // Deprecated

        // 2. Java Date API - Date constructor (deprecated since JDK 1.1)
        Date date1 = new Date(2023, 5, 15); // Deprecated constructor
        Date date2 = new Date("June 15, 2023"); // Deprecated constructor

        // 3. Calendar.getInstance() with deprecated methods
        Calendar cal = Calendar.getInstance();
        cal.setYear(2023); // Deprecated since JDK 1.1
        cal.setMonth(5);   // Deprecated since JDK 1.1
        cal.setDate(15);   // Deprecated since JDK 1.1

        // 4. URLDecoder.decode(String) - deprecated since JDK 1.4
        String decoded = URLDecoder.decode("hello%20world"); // Deprecated - missing charset

        // 5. Thread methods (already in your original test but worth including)
        Thread thread = new Thread();
        thread.stop();     // Deprecated since JDK 1.2
        thread.suspend();  // Deprecated since JDK 1.2
        thread.resume();   // Deprecated since JDK 1.2

        // 6. System.runFinalizersOnExit() - deprecated since JDK 1.2
        System.runFinalizersOnExit(true); // Deprecated and dangerous

        // 7. Runtime.runFinalizersOnExit() - deprecated since JDK 1.2
        Runtime.getRuntime().runFinalizersOnExit(true); // Deprecated

        System.out.println("Tested various deprecated methods from external libraries");
    }

    // Method using deprecated StringEscapeUtils
    public String escapeHtmlDeprecated(String input) {
        return StringEscapeUtils.escapeHtml4(input); // Deprecated
    }

    // Method using deprecated Date constructors
    public Date createDateDeprecated(int year, int month, int day) {
        return new Date(year, month, day); // Deprecated
    }

    // Method using deprecated URLDecoder
    public String decodeUrlDeprecated(String encoded) throws UnsupportedEncodingException {
        return URLDecoder.decode(encoded); // Deprecated - should specify charset
    }
}