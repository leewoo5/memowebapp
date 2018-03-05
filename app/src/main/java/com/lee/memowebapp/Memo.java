package com.lee.memowebapp;

import java.util.Date;

/**
 * Created by Administrator on 2018-03-05.
 */

public class Memo {

    private String txt, title, key;
    private Long createDate, updateDate;

    public Memo() {

    }

    public String getTxt() {
        return txt;
    }

    public void setTxt(String txt) {
        this.txt = txt;
    }

    public Long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Long createDate) {

        this.createDate = createDate;
    }

    public Long getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Long updateDate) {
        this.updateDate = updateDate;
    }

    public String getTitle() {
        if( txt != null) {
            if(txt.indexOf("\n") > -1) {
                return txt.substring(0, txt.indexOf("\n"));
            } else {
                return txt;
            }
        }
        return title;
    }

    public void setTitle(String title) {

        this.title = title;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
