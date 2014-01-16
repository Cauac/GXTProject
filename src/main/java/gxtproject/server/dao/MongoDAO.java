package gxtproject.server.dao;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

public abstract class MongoDAO implements InitializingBean {
    private MongoClient mongoClient;
    private String dbName;
    protected DB database;


    @Required
    public void setMongoClient(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Required
    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        database = mongoClient.getDB(dbName);
    }
}
