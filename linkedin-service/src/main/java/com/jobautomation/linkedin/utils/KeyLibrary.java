package com.jobautomation.linkedin.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class KeyLibrary {

    public static String getStringAtIndex(String value, int index) {
        try {
            String[] arr = value.split("[!@#`~#$%^*()_+,.:;' ]");
            if (index >= 0 && index < arr.length) {
                return arr[index];
            } else {
                return "";
            }
        } catch (Exception e) {
            System.out.println("Failed Due to Cause of :" + e.getMessage());
            return null;
        }
    }

    public static String getStringAtIndex(String value, int index, String splitby) {
        try {
            String[] arr = value.split(splitby);
            if (index >= 0 && index < arr.length) {
                return arr[index];
            } else {
                return "";
            }
        } catch (Exception e) {
            System.out.println("Failed Due to Cause of :" + e.getMessage());
            return null;
        }
    }

    public static String getRequiredString(String value, String start, String end) {
        int startIndex = value.indexOf(start);
        if (startIndex != -1) {
            int endIndex = value.indexOf(end, startIndex);
            if (endIndex != -1) {
                value = value.substring(startIndex, endIndex + 1);
            }
        }
        return value;
    }

    public static String getWebelementList(Elements elements, String attribute, String cssQuery) {
        StringBuilder buffer = new StringBuilder();
        for (Element product : elements) {
            String temp = product.select(cssQuery).attr(attribute);
            if (buffer.length() > 0) {
                buffer.append(" ; ");
            }
            buffer.append(temp);
        }
        return buffer.toString();
    }

    public static Document getJsoupDocument(String url) {
        Document doc = null;
        try {
            doc = Jsoup.connect(url)
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .referrer("http://www.google.com")
                    .timeout((int) ((Math.random() * 2000) + 3000))
                    .get();
        } catch (Exception e) {
            System.out.println("Failed due to :" + e.getMessage());
        }
        return doc;
    }
}
