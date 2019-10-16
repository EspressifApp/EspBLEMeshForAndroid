package libs.espressif.github;

import androidx.annotation.NonNull;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GitHubGetLatestReleaseTask {
    private HttpURLConnection mConnection;

    private final String mAccount;
    private final String mRepository;

    public GitHubGetLatestReleaseTask(@NonNull String account, @NonNull String repository) {
        mAccount = account;
        mRepository = repository;
    }

    private String getURL() {
        return String.format("https://api.github.com/repos/%s/%s/releases/latest", mAccount, mRepository);
    }

    public GitHubRelease execute() {
        try {
            if (mConnection != null) {
                mConnection.disconnect();
            }

            URL url = new URL(getURL());
            mConnection = (HttpURLConnection) url.openConnection();

            int code = mConnection.getResponseCode();
            if (code != 200) {
                return null;
            }

            InputStream is = mConnection.getInputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            while (true) {
                int read = is.read(buf);
                if (read == -1) {
                    break;
                }

                os.write(buf, 0, read);
            }

            return new GitHubRelease(os.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (mConnection != null) {
                mConnection.disconnect();
                mConnection = null;
            }
        }
        return null;
    }

    public void cancel() {
        if (mConnection != null) {
            mConnection.disconnect();
        }
    }
}
