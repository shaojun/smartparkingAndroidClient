package com.SmartParking.WebService;

import android.util.Base64;

import com.SmartParking.Task.Action;
import com.SmartParking.Task.Task;
import com.SmartParking.Util.Tuple;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Think on 10/25/2015.
 */
public class BulkRestClient extends Action<List<String>> {
    List<RestClient> clients = new ArrayList<>();
    private String httpAction = "";

    public BulkRestClient(List<String> urls, String userName, String pwd, String httpAction, Object sharedState) {
        super(sharedState);
        this.httpAction = httpAction;
        String auth = new String(Base64.encode((userName + ":" + pwd).getBytes(), Base64.URL_SAFE | Base64.NO_WRAP));

        for (int i = 0; i < urls.size(); i++) {
            RestClient _ = new RestClient(urls.get(i));
            _.AddHeader("content-type", "application/json");
            _.AddHeader("Authorization", "Basic " + auth);
            this.clients.add(_);
        }
    }

    @Override
    public List<String> execute(Task ownerTask) throws Exception {
        for(RestClient c: this.clients) {
            c.Execute(this.httpAction);
        }

        List<String> results = new ArrayList<>();
        for(RestClient c: this.clients) {
            results.add(c.getResponse());
        }

        return results;
    }
}
