package ir.pi0.irproject;

import ir.pi0.irproject.repository.WordDict;
import ir.pi0.irproject.utils.HighLighter;
import ir.pi0.irproject.utils.Util;
import org.nikkii.embedhttp.HttpServer;
import org.nikkii.embedhttp.handler.HttpRequestHandler;
import org.nikkii.embedhttp.impl.HttpRequest;
import org.nikkii.embedhttp.impl.HttpResponse;
import org.nikkii.embedhttp.impl.HttpStatus;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class WebUI {

    HttpServer server = new HttpServer();

    HighLighter highLighter;
    HighLighter highLighter_bold;
    WordDict wordDict;
    File base;

    private static final Logger LOG = Logger.getLogger(WebUI.class.getName());

    String template;
    static String content_holder_a = "<!-- CONTENTA -->";
    static String content_holder_b = "<!-- CONTENTB -->";

    public WebUI(String path) throws Exception {

        highLighter = new HighLighter("<span style='background-color:yellow'>", "</span>");
        highLighter_bold = new HighLighter("<b>", "</b>");

        wordDict = new WordDict(new File(path), false, false);
        base = new File(path + ".data/articles");

        template = Util.readFully(new File("www/index.html"));


        server.addRequestHandler(new HttpRequestHandler() {


            String getContent(Integer id) {
                return Util.readFully(new File(base, String.valueOf(id)));
            }

            @Override
            public HttpResponse handleRequest(HttpRequest request) {

                Map<String, Object> get = request.getGetData();
                if (get == null)
                    get = new HashMap<>();

                String q = (String) get.get("q");
                if (q == null)
                    return makeResponse("", "<h2>برای شروع عبارت مورد نظر خود را وارد نمایید</h2>");

                StringBuilder res = new StringBuilder();

                String show_id = (String) get.get("id");

                if (show_id == null) {
                    //Only show list


                    long startTime = System.currentTimeMillis();
                    List<Integer> r = wordDict.query(q, 10);
                    long stopTime = System.currentTimeMillis();
                    long elapsedTime = stopTime - startTime;

                    res.append("جست و جو شده در ").append("<b>").append(elapsedTime).append("</b>").append("هزارم ثانیه").append("<br><br>");


                    for (Integer i : r) {

                        String c = highLighter_bold.highlight(getContent(i), q);
                        int start_index = c.indexOf('<');
                        start_index -= 50;
                        if(start_index<0)
                            start_index=0;
                        c = "... "+ c.substring(start_index, start_index + 100);

                        res.append("<a href=?id=").append(i).append("&q=").append(q).append(">")
                                .append("مقاله شماره ی ").append(i).append("</a><br>");
                        res.append("<p>").append(c).append("</p>").append("<br>");
                        res.append("<hl>");

                    }

                } else {
                    //Detail mode
                    res.append("<a href='?q=").append(q).append("'>").append("بازشگت به فهرست نتایج").append("</a>");
                    String highlight = highLighter.highlight(getContent(Integer.parseInt(show_id)), q);
                    res.append("<h1>").append("مقاله شماره ی ").append(show_id).append("</h1><br><hl>");
                    res.append(highlight);
                    res.append("<br><br>");
                }

                return makeResponse(q, res.toString());
            }
        });

        server.bind(3030);
        server.start();

        System.out.println("Ready and listening on port http://localhost:3030");
        System.out.println("Hit Enter to stop server.\n");
        try {
            System.in.read();
        } catch (Throwable ignored) {
        }
    }

    HttpResponse makeResponse(String a, String b) {
        return new HttpResponse(HttpStatus.OK, template.replace(content_holder_a, a).replace(content_holder_b, b));

    }


    void query(String query, int limit) {

        List<Integer> r = wordDict.query(query, 10);
        int id = r.get(0);


    }


}
