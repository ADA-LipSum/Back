package com.ada.proj.service;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Service;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private static final PolicyFactory POLICY;

    static {
        // 허용할 HTML 요소/속성 정책 구성
        HtmlPolicyBuilder b = new HtmlPolicyBuilder()
                .allowElements(
                        "p", "br", "pre", "code", "blockquote",
                        "ul", "ol", "li",
                        "h1", "h2", "h3", "h4", "h5", "h6",
                        "hr", "strong", "b", "em", "i",
                        "a",
                        // GFM 테이블 지원
                        "table", "thead", "tbody", "tr", "th", "td"
                )
                .allowAttributes("href").onElements("a")
                .allowStandardUrlProtocols()
                .requireRelNofollowOnLinks()
                .allowAttributes("target").matching(Pattern.compile("_blank|_self|_parent|_top")).onElements("a")
                // highlight.js를 위한 class 허용 (language-xxx)
                .allowAttributes("class").matching(Pattern.compile("(?i)(language|lang)-[a-z0-9_\\-\\+]+")).onElements("code", "pre")
                // 테이블 정렬 등 단순 속성은 보수적으로 제외
                ;
        POLICY = b.toFactory();
    }

    public MarkdownService() {
        MutableDataSet opts = new MutableDataSet();
    opts.set(Parser.EXTENSIONS, List.of(
        TablesExtension.create(),
        AutolinkExtension.create(),
        StrikethroughExtension.create(),
        AnchorLinkExtension.create()
    ));
        this.parser = Parser.builder(opts).build();
        this.renderer = HtmlRenderer.builder(opts)
                .escapeHtml(false)
                .build();
    }

    public String renderToSafeHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        Node doc = parser.parse(markdown);
        String html = renderer.render(doc);
        return POLICY.sanitize(html);
    }
}
