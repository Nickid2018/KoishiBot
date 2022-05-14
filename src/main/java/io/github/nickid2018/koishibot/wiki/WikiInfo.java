package io.github.nickid2018.koishibot.wiki;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

public class WikiInfo {

    public static final String WIKI_META = "action=query&format=json&meta=siteinfo&siprop=extensions";
    public static final String QUERY_PAGE = "action=query&format=json&inprop=url&iiprop=url&prop=info%7Cimageinfo%7Cextracts%7Cpageprops&" +
                                            "ppprop=description%7Cdisplaytitle%7Cdisambiguation%7Cinfoboxes&explaintext&" +
                                            "exsectionformat=plain&exchars=200&redirects";
    public static final String QUERY_PAGE_NOE = "action=query&format=json&inprop=url&iiprop=url&prop=info%7Cimageinfo&redirects";
    public static final String WIKI_SEARCH = "action=query&format=json&list=search&srwhat=text&srlimit=1&srenablerewrite";

    private final String url;
    private boolean available;
    private boolean useTextExtracts;

    public WikiInfo(String url) {
        this.url = url;
    }

    public boolean notAvailable() {
        if (!available) {
            try {
                JsonObject object = WebUtil.fetchDataInJson(new HttpGet(url + WIKI_META)).getAsJsonObject();
                JsonArray extensions = object.getAsJsonObject("query").getAsJsonArray("extensions");
                for (JsonElement element : extensions) {
                    String name = element.getAsJsonObject().get("name").getAsString();
                    if (name.equals("TextExtracts")) {
                        useTextExtracts = true;
                        break;
                    }
                }
                available = true;
            } catch (IOException e) {
                return true;
            }
        }
        return false;
    }

    public PageInfo parsePageInfo(String title, int pageID) throws IOException {
        if (notAvailable())
            throw new IOException("无法连接到wiki");

        JsonObject query;
        String queryFormat = useTextExtracts ? QUERY_PAGE : QUERY_PAGE_NOE;
        if (title == null)
            query = WebUtil.fetchDataInJson(new HttpGet(url + queryFormat + "&pageid=" + pageID))
                    .getAsJsonObject().getAsJsonObject("query");
        else
            query = WebUtil.fetchDataInJson(new HttpGet(url + queryFormat + "&titles=" + URLEncoder.encode(title, "UTF-8")))
                    .getAsJsonObject().getAsJsonObject("query");

        PageInfo pageInfo = new PageInfo();
        pageInfo.title = title;
        tryRedirect(query.get("redirects"), pageInfo);
        tryRedirect(query.get("normalized"), pageInfo);

        JsonObject pages = query.getAsJsonObject("pages");
        for (Map.Entry<String, JsonElement> entry : pages.entrySet()) {
            String id = entry.getKey();
            JsonObject object = entry.getValue().getAsJsonObject();
            pageInfo.url = object.get("fullurl").getAsString();
            if (object.has("missing")) {
                if (title != null)
                    return search(title);
                throw new IOException("无法找到页面，可能不存在或页面为文件");
            }
            if (useTextExtracts) {
                pageInfo.shortDescription = resolveText(object.get("extract").getAsString().trim());
            } else {
                // ...
            }
        }

        return pageInfo;
    }

    private PageInfo search(String key) throws IOException {
        JsonObject data = WebUtil.fetchDataInJson(new HttpGet(
                url + WIKI_SEARCH + "&srsearch=" + key)).getAsJsonObject();
        JsonArray search = data.getAsJsonObject("query").getAsJsonArray("search");
        PageInfo info = new PageInfo();
        info.isSearched = true;
        if (search.size() != 0)
            info.title = search.get(0).getAsJsonObject().get("title").getAsString();
        return info;
    }

    private void tryRedirect(JsonElement entry, PageInfo info) {
        if (entry instanceof JsonArray && info.title != null) {
            for (JsonElement element : (JsonArray) entry) {
                JsonObject object = element.getAsJsonObject();
                String from = object.get("from").getAsString();
                if (from.equalsIgnoreCase(info.title)) {
                    String to = element.getAsJsonObject().get("to").getAsString();
                    info.titlePast = info.title;
                    info.title = to;
                    info.redirected = true;
                    break;
                }
            }
        }
    }

    private String resolveText(String source) {
        int index = 0;
        int blankets = 0;
        boolean quote = false;
        boolean subQuote = false;
        INDEX_FIND: for (; index < source.length(); index++) {
            char now = source.charAt(index);
            switch (now) {
                case '《':
                case '<':
                case '[':
                case '{':
                    blankets++;
                    break;
                case '>':
                case ']':
                case '}':
                case '》':
                    blankets--;
                    break;
                case '"':
                    quote = !quote;
                    break;
                case '\'':
                    subQuote = !subQuote;
                    break;
                case '?':
                case '!':
                case '.':
                case '？':
                case '！':
                case '。':
                    if (blankets == 0 && !quote && !subQuote)
                        break INDEX_FIND;
            }
        }
        return source.substring(0, index + 1);
    }
}
