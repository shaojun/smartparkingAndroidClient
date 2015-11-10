package com.SmartParking.WebService;

import com.SmartParking.WebServiceEntity.EntityBase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * mapping the rawResult from web to a WebServiceEntity.
 * whatever a single (contained in {}) or multiple (contained in []) json object returned from web,
 * the result would be parsed and returned as a List<EntityBase>, the caller side should have the knowledge to determine.
 * otherwise, use the dumpSingle(...)
 */
public class RestEntityResultDumper {
    //private Class<TEntity> clazz;

    public static <TEntity extends EntityBase> List<TEntity> dump(String rawResult, Class<TEntity> clazz) throws JSONException, IllegalAccessException, InstantiationException {
        //this.clazz = clazz;
        Object json = new JSONTokener(rawResult).nextValue();
        List<TEntity> list = new ArrayList<>();
        TEntity _ = clazz.newInstance();
        if (json instanceof JSONObject) {
            JSONObject node = (JSONObject) json;
            if (node.has("detail")) {
                _.setErrorMessage(node.getString("detail"));
            } else {
                list.add((TEntity) (_.loadSingleFromJson((JSONObject) json)));
            }
        } else if (json instanceof JSONArray) {
            JSONArray jsArray = (JSONArray) json;
            list.addAll((Collection<TEntity>) (_.loadMultipleFromJson(jsArray)));
        }

        return list;
    }
//
//    public EntityBase dumpSingle(String rawResult, Class<TEntity> clazz) throws JSONException, InstantiationException, IllegalAccessException {
//        return this.dump(rawResult, clazz).get(0);
//    }
}
