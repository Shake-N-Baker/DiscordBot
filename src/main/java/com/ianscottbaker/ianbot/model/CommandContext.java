package com.ianscottbaker.ianbot.model;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

public class CommandContext {
    private final IBUser user;
    private final MongoCollection<IBUser> userCollection;
    private final Document updateQuery;
    private final UpdateOptions updateOptions;

    public CommandContext(IBUser user, MongoCollection<IBUser> userCollection, Document updateQuery, UpdateOptions updateOptions) {
        this.user = user;
        this.userCollection = userCollection;
        this.updateQuery = updateQuery;
        this.updateOptions = updateOptions;
    }

    public IBUser getUser() { return user; }
    public MongoCollection<IBUser> getUserCollection() { return userCollection; }
    public Document getUpdateQuery() { return updateQuery; }
    public UpdateOptions getUpdateOptions() { return updateOptions; }
}
