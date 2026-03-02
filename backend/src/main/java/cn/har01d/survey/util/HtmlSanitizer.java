package cn.har01d.survey.util;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

public class HtmlSanitizer {

    private static final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.IMAGES)
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.TABLES)
            .and(Sanitizers.STYLES);

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        return POLICY.sanitize(html);
    }
}
