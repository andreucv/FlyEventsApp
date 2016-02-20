package com.pragmapure.flyevents.classes;

import com.orm.SugarRecord;

/**
 * Created by xiscosastre on 20/02/16.
 */
public class Photo extends SugarRecord {

    String filename;
    String idEvent;
    Boolean uploaded;

    public Photo() {
    }

    public Photo(String filename, String id, Boolean uploaded) {
        this.filename = filename;
        this.idEvent = id;
        this.uploaded = uploaded;
    }

    public Photo(String filename, String id) {
        this.filename = filename;
        this.idEvent = id;
        this.uploaded = false;
    }

    public void markUploaded() {
        this.uploaded = true;
        this.save();
    }
}
