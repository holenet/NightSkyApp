package com.holenet.nightsky;

import android.util.Log;

import com.holenet.nightsky.item.BaseLog;
import com.holenet.nightsky.item.Comment;
import com.holenet.nightsky.item.FileItem;
import com.holenet.nightsky.item.ImageLog;
import com.holenet.nightsky.item.Music;
import com.holenet.nightsky.item.Piece;
import com.holenet.nightsky.item.Post;
import com.holenet.nightsky.item.TextLog;
import com.holenet.nightsky.item.Watch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Parser {
    public static String getMetaDataHTML(String res, String name) {
        try {
            Document doc = Jsoup.parse(res);
            Elements metas = doc.select("meta[name="+name+"]");
            if(metas.size()==0)
                return null;
            Element meta = metas.get(0);
            return meta.attr("content");
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<FileItem> getFileListHTML(String res) {
        try {
            List<FileItem> fileItems = new ArrayList<>();
            Document doc = Jsoup.parse(res);

            Elements files = doc.select("div[class=post]");
            for(Element file: files) {
                String filePath = file.child(1).attr("href");
                String author = file.child(2).text().split(": ")[1];
                String description = file.child(3).text().split(": ")[1];
                String datetime = file.child(4).text().split(": ")[1];
                fileItems.add(new FileItem(Integer.parseInt(filePath.split("/")[filePath.split("/").length-1]), description));
            }
            return fileItems;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<FileItem> getFileListJSON(String res) {
        try {
            List<FileItem> fileItems = new ArrayList<>();
            JSONArray ja = new JSONArray(res);
            for(int i=0; i<ja.length(); i++) {
                JSONObject jo = ja.getJSONObject(i);
                FileItem file = new FileItem(jo.getInt("id"), jo.getString("name"));
                file.setType(FileItem.FileType.valueOf(jo.getString("type")));
                fileItems.add(file);
            }
            return fileItems;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Music> getMusicListJSON(String res) {
        try {
            List<Music> musicItems = new ArrayList<>();
            JSONArray ja = new JSONArray(res);
            for(int i=0; i<ja.length(); i++) {
                JSONObject jo = ja.getJSONObject(i);
                Music music = new Music(jo.getString("title"), jo.getString("artist"), jo.getString("album"), jo.getString("path"));
                music.setServerId(jo.getInt("id"));
                musicItems.add(music);
            }
            return musicItems;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Post> getPostListdHTML(String res) {
        try {
            List<Post> postItems = new ArrayList<>();
            Document doc = Jsoup.parse(res);

            Elements posts = doc.select("div[class=post]");
            for(Element post: posts) {
                String datetime = post.child(0).child(0).text();
                String title = post.child(1).text();
                String text = post.child(2).text();
                int sepIndex = datetime.indexOf(',', 10);
                // TO/DO: parse author
                Post postItem = new Post(0, title, "", new String[] {datetime.substring(0, sepIndex), datetime.substring(sepIndex+2)}, text);
                postItem.setCommentCount(Integer.valueOf(post.text().split(":")[post.text().split(":").length-1].replace(" ", "")));
                postItems.add(postItem);
            }
            return postItems;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Post> getPostListJSON(String res) {
        try {
            List<Post> postItems = new ArrayList<>();
            JSONArray ja = new JSONArray(res);
            for(int i=0; i<ja.length(); i++) {
                JSONObject jo = ja.getJSONObject(i);
                Post post = new Post(jo.getInt("id"), jo.getString("title"), jo.getString("author"), getDatetime(jo.getString("datetime")), jo.getString("text"));
                post.setCommentCount(jo.getInt("comment_count"));
                postItems.add(post);
            }
            return postItems;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<String> getErrorListHTML(String res) {
        try {
            List<String> errors = new ArrayList<>();
            Document doc = Jsoup.parse(res);

            Elements errorLists = doc.select("ul[class=errorlist]");
            for(Element errorList: errorLists) {
                String error = errorList.child(0).text();
                errors.add(0, error);
                Log.e("error", error);
            }
            return errors;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Post getPostJSON(String res) {
        Post post = null;
        try {
            JSONObject jo = new JSONObject(res);
            JSONArray ja = jo.getJSONArray("comments");
            List<Comment> comments = new ArrayList<>();
            for(int i=0; i<ja.length(); i++) {
                JSONObject o = ja.getJSONObject(i);
                comments.add(new Comment(o.getInt("id"), o.getString("author"), getDatetime(o.getString("datetime")), o.getString("text")));
            }
            post = new Post(jo.getInt("id"), jo.getString("title"), jo.getString("author"), getDatetime(jo.getString("datetime")), jo.getString("text"), comments);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return post;
    }

    public static Post getPostSimpleJSON(String res) {
        Post post = null;
        try {
            JSONObject jo = new JSONObject(res);
            post = new Post();
            post.setTitle(jo.getString("title"));
            post.setAuthor(jo.getString("author"));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return post;
    }

    public static Post getRecentPostSimpleJSON(String res) {
        try {
            JSONObject jo = new JSONObject(res);
            Post post = new Post();
            post.setId(jo.getInt("id"));
            post.setAuthor(jo.getString("author"));
            return post;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<BaseLog> getLogListJSON(String res) {
        try {
            List<BaseLog> logItems = new ArrayList<>();
            JSONArray jaLogs = new JSONArray(res);
            for(int i=0; i<jaLogs.length(); i++) {
                BaseLog log = getLogJSON(jaLogs.getJSONObject(i));
                if(log!=null)
                    logItems.add(log);
            }
            return logItems;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static BaseLog getLogJSON(String res) {
        try {
            JSONObject joLog = new JSONObject(res);
            return getLogJSON(joLog);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static BaseLog getLogJSON(JSONObject joLog) throws Exception {
        int pk = joLog.getInt("pk");
        String createdAt = getCleanDatetime(joLog.getString("created_at"));
        String type = joLog.getString("type");
        Watch watch = null;
        String watchPkString = joLog.getString("watch_pk");
        if(!"null".equals(watchPkString)) {
            watch = new Watch(Integer.parseInt(watchPkString));
        }
        BaseLog log;
        if("text".equals(type)) {
            log = new TextLog(pk, createdAt, watch, joLog.getString("text"));
        } else {
            log = new ImageLog(pk, createdAt, watch, joLog.getString("image_path"));
        }
        return log;
    }

    public static List<String> getDatesJSON(String res) {
        try {
            List<String> dateItems = new ArrayList<>();
            JSONArray jaDates = new JSONArray(res);
            for(int i=0; i<jaDates.length(); i++) {
                dateItems.add(jaDates.getString(i));
            }
            return dateItems;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Piece getPieceJSON(String res) {
        try {
            JSONObject joPiece = new JSONObject(res);
            return getPieceJSON(joPiece);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Piece getPieceJSON(JSONObject joPiece) throws Exception {
        int pk = joPiece.getInt("pk");
        String title = joPiece.getString("title");
        String comment = joPiece.getString("comment");
        // TODO: extract other properties
        return new Piece(pk, title, comment);
    }

    public static Watch getWatchJSON(String res) {
        try {
            JSONObject joWatch = new JSONObject(res);
            return getWatchJSON(joWatch);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Watch getWatchJSON(JSONObject joWatch) throws Exception {
        int pk = joWatch.getInt("pk");
        int piecePk = joWatch.getInt("piece_pk");
        int start = joWatch.getInt("start");
        int end = joWatch.getInt("end");
        String date = joWatch.getString("date");
        return new Watch(pk, new Piece(piecePk), start, end, date);
    }

    private static String getCleanDatetime(String res) {
        return res.replace("T", " ");
    }

    public static String getDate(String datetime) {
        return datetime.split(" ")[0];
    }

    public static String getTime(String datetime) {
        return datetime.split(" ")[1];
    }

    public static String getSimpleTime(String datetime) {
        return datetime.split(" ")[1].split("\\.")[0];
    }

    private static String[] getDatetime(String res) {
        return new String[] {res.split(" ")[0], res.split(" ")[1].split("\\.")[0]};
    }

    public static String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
    }
}
