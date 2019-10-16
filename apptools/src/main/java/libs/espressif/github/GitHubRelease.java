package libs.espressif.github;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GitHubRelease {

    public GitHubRelease(String releaseInfo) throws JSONException {
        JSONObject json = new JSONObject(releaseInfo);
        this.url = json.getString("url");
        this.assets_url = json.getString("assets_url");
        this.upload_url = json.getString("upload_url");
        this.html_url = json.getString("html_url");
        this.id = json.getLong("id");
        this.node_id = json.getString("node_id");
        this.tag_name = json.getString("tag_name");
        this.target_commitish = json.getString("target_commitish");
        this.name = json.getString("name");
        this.draft = json.getBoolean("draft");
        this.prerelease = json.getBoolean("prerelease");
        this.created_at = json.getString("created_at");
        this.published_at = json.getString("published_at");
        this.tarball_url = json.getString("tarball_url");
        this.zipball_url = json.getString("zipball_url");
        this.body = json.getString("body");

        JSONObject authorJSON = json.getJSONObject("author");
        this.author = new Author();
        this.author.login = authorJSON.getString("login");
        this.author.id = authorJSON.getLong("id");
        this.author.node_id = authorJSON.getString("node_id");
        this.author.avatar_url = authorJSON.getString("avatar_url");
        this.author.url = authorJSON.getString("url");
        this.author.html_url = authorJSON.getString("html_url");
        this.author.followers_url = authorJSON.getString("followers_url");
        this.author.following_url = authorJSON.getString("following_url");
        this.author.gists_url = authorJSON.getString("gists_url");
        this.author.starred_url = authorJSON.getString("starred_url");
        this.author.subscriptions_url = authorJSON.getString("subscriptions_url");
        this.author.organizations_url = authorJSON.getString("organizations_url");
        this.author.repos_url = authorJSON.getString("repos_url");
        this.author.events_url = authorJSON.getString("events_url");
        this.author.received_events_url = authorJSON.getString("received_events_url");
        this.author.type = authorJSON.getString("type");
        this.author.site_admin = authorJSON.getBoolean("site_admin");

        JSONArray assetArray = json.getJSONArray("assets");
        for (int i = 0; i < assetArray.length(); i++) {
            JSONObject assetJSON = assetArray.getJSONObject(i);
            Asset asset = new Asset();
            asset.url = assetJSON.getString("url");
            asset.id = assetJSON.getLong("id");
            asset.node_id = assetJSON.getString("node_id");
            asset.name = assetJSON.getString("name");
            asset.label = assetJSON.optString("label", null);
            asset.content_type = assetJSON.getString("content_type");
            asset.state = assetJSON.getString("state");
            asset.size = assetJSON.getLong("size");
            asset.download_count = assetJSON.getLong("download_count");
            asset.created_at = assetJSON.getString("created_at");
            asset.updated_at = assetJSON.getString("updated_at");
            asset.browser_download_url = assetJSON.getString("browser_download_url");
            JSONObject uploaderJSON = assetJSON.getJSONObject("uploader");
            asset.uploader = new Asset.Uploader();
            asset.uploader.login = uploaderJSON.getString("login");
            asset.uploader.id = uploaderJSON.getLong("id");
            asset.uploader.node_id = uploaderJSON.getString("node_id");
            asset.uploader.avatar_url = uploaderJSON.getString("avatar_url");
            asset.uploader.gravatar_id = uploaderJSON.getString("gravatar_id");
            asset.uploader.url = uploaderJSON.getString("url");
            asset.uploader.html_url = uploaderJSON.getString("html_url");
            asset.uploader.followers_url = uploaderJSON.getString("followers_url");
            asset.uploader.following_url = uploaderJSON.getString("following_url");
            asset.uploader.gists_url = uploaderJSON.getString("gists_url");
            asset.uploader.starred_url = uploaderJSON.getString("starred_url");
            asset.uploader.subscriptions_url = uploaderJSON.getString("subscriptions_url");
            asset.uploader.organizations_url = uploaderJSON.getString("organizations_url");
            asset.uploader.repos_url = uploaderJSON.getString("repos_url");
            asset.uploader.events_url = uploaderJSON.getString("events_url");
            asset.uploader.received_events_url = uploaderJSON.getString("received_events_url");
            asset.uploader.type = uploaderJSON.getString("type");
            asset.uploader.site_admin = uploaderJSON.getBoolean("site_admin");

            this.assets.add(asset);
        }
    }

    public String url;
    public String assets_url;
    public String upload_url;
    public String html_url;
    public long id;
    public String node_id;
    public String tag_name;
    public String target_commitish;
    public String name;
    public boolean draft;
    public Author author;
    public boolean prerelease;
    public String created_at;
    public String published_at;
    public final List<Asset> assets = new ArrayList<>();
    public String tarball_url;
    public String zipball_url;
    public String body;

    public static class Author {
        public String login;
        public long id;
        public String node_id;
        public String avatar_url;
        public String url;
        public String html_url;
        public String followers_url;
        public String following_url;
        public String gists_url;
        public String starred_url;
        public String subscriptions_url;
        public String organizations_url;
        public String repos_url;
        public String events_url;
        public String received_events_url;
        public String type;
        public boolean site_admin;
    }

    public static class Asset {
        public String url;
        public long id;
        public String node_id;
        public String name;
        public String label;
        public Uploader uploader;
        public String content_type;
        public String state;
        public long size;
        public long download_count;
        public String created_at;
        public String updated_at;
        public String browser_download_url;

        public static class Uploader {
            public String login;
            public long id;
            public String node_id;
            public String avatar_url;
            public String gravatar_id;
            public String url;
            public String html_url;
            public String followers_url;
            public String following_url;
            public String gists_url;
            public String starred_url;
            public String subscriptions_url;
            public String organizations_url;
            public String repos_url;
            public String events_url;
            public String received_events_url;
            public String type;
            public boolean site_admin;
        }
    }

}
