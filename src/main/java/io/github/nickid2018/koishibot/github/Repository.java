package io.github.nickid2018.koishibot.github;

import java.io.Serializable;
import java.util.Date;

public class Repository implements Serializable {

    public String name;
    public String owner;

    public Date createTime;
    public Date updatedTime;
    public Date pushedTime;

    public String language;
    public String description;

    public long subscribers;
    public long watchers;
    public long issues;
    public long issuesOpen;
    public long forks;
}
