package gxtproject.server.dao;

import com.mongodb.*;
import gxtproject.server.util.TimeUtil;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Pattern;

public class MongoFlickrDAO extends MongoDAO {

    private static final Logger logger = Logger.getLogger(MongoFlickrDAO.class);

    private static final String CONTACT_COLLECTION = "flickr_contact";
    public static final String COMMENT_COLLECTION = "flickr_comment";
    public static final String PHOTO_COMMENT_COLLECTION = "flickr_photo_comment";
    private static final String PHOTO_COLLECTION = "flickr_photo";
    public static final String ACTIVITY_COLLECTION = "flickr_activity";
    public static final String FAVORITES_COLLECTION = "flickr_favorites";
    private static final String FLICKR_USERS_COLLECTION = "flickr_users";


    public void saveFlickrUser(DBObject object) {
        object.put("_id", object.removeField("nsid"));
        database.getCollection(FLICKR_USERS_COLLECTION).save(object);
    }

    public void saveContact(String userId, String contactId) {
        database.getCollection(CONTACT_COLLECTION)
                .update(new BasicDBObject("_id", userId), new BasicDBObject("$addToSet", new BasicDBObject("contacts", contactId)), true, false);
    }

    public void removeContact(String userId, String contactId) {
        database.getCollection(CONTACT_COLLECTION)
                .update(new BasicDBObject("_id", userId), new BasicDBObject("$pull", new BasicDBObject("contacts", contactId)), true, false);
    }

    public void saveComment(DBObject comment) {
        Object commentId = comment.removeField("commentid");
        Object itemId = comment.get("itemId");
        comment.put("_id", commentId);
        String date = TimeUtil.getPureDate(comment.get("dateadded").toString());

        DBObject photoComment = new BasicDBObject("$addToSet", new BasicDBObject(date + ".comments", commentId));
        photoComment.put("$inc", new BasicDBObject("commentsCount", 1));
        photoComment.put("$set", new BasicDBObject("itemOwner", comment.get("itemOwner")));
        database.getCollection(PHOTO_COMMENT_COLLECTION)
                .update(new BasicDBObject("_id", itemId), photoComment, true, false);
        database.getCollection(COMMENT_COLLECTION).save(comment);
    }

    public void savePhoto(DBObject entity) {
        database.getCollection(PHOTO_COLLECTION).save(entity);
    }

    public void saveFavorites(DBObject entity) {
        entity.put("_id", entity.get("user") + "_" + entity.get("itemId"));
        Object itemId = entity.get("itemId");
        String date = TimeUtil.getPureDate(entity.get("dateadded").toString());

        DBObject photoFave = new BasicDBObject("$addToSet", new BasicDBObject(date + ".faves", entity.get("_id")));
        photoFave.put("$inc", new BasicDBObject("favesCount", 1));
        photoFave.put("$set", new BasicDBObject("itemOwner", entity.get("itemOwner")));
        database.getCollection(PHOTO_COMMENT_COLLECTION)
                .update(new BasicDBObject("_id", itemId), photoFave, true, false);

        database.getCollection(FAVORITES_COLLECTION).save(entity);
    }

    public void saveActivity(String id, String type, String value) {
//        logger.debug(String.format("id %s, type %s, value %s", id, type, value));
        database.getCollection(ACTIVITY_COLLECTION)
                .update(new BasicDBObject("_id", id), new BasicDBObject("$addToSet", new BasicDBObject(type, value)), true, false);
    }

    public BasicDBList readContactIdList(String userId) {
        DBObject user = database.getCollection(CONTACT_COLLECTION).findOne(new BasicDBObject("_id", userId));
        if (user != null) {
            return (BasicDBList) user.get("contacts");
        }
        return null;
    }

    public Map<String, DBObject> getUsersMap(Collection ids) {
        Map<String, DBObject> resultMap = new HashMap<String, DBObject>();
        BasicDBList objects = readObjectsByIds(FLICKR_USERS_COLLECTION, ids);
        for (Object userObject : objects) {
            DBObject user = (DBObject) userObject;
            resultMap.put((String) user.get("_id"), user);
        }
        return resultMap;
    }

    public BasicDBList getUsers(Collection ids) {
        return readObjectsByIds(FLICKR_USERS_COLLECTION, ids);
    }

    public BasicDBList getPhotos(Collection ids) {
        return readObjectsByIds(PHOTO_COLLECTION, ids);
    }

    public DBObject readCommentsByPhotoId(String id) {
        DBObject photo = database.getCollection(PHOTO_COMMENT_COLLECTION).findOne(new BasicDBObject("_id", id));
        if (photo == null) {
            photo=new BasicDBObject();
        }
        photo.removeField("favesCount");
        photo.removeField("itemOwner");
        photo.removeField("commentsCount");
        photo.removeField("_id");

        BasicDBList commentsIdList = new BasicDBList();
        BasicDBList dateList = new BasicDBList();
        BasicDBList valueList = new BasicDBList();
        for (String date : photo.keySet()) {
            DBObject dataFromDate = (DBObject) photo.get(date);
            if (dataFromDate.containsField("comments")) {
                Collection comments = (Collection<?>) dataFromDate.get("comments");
                dateList.add(date);
                valueList.add(comments.size());
                commentsIdList.addAll(comments);
            }
        }
        DBObject result = new BasicDBObject();
        result.put("comments", readComments(commentsIdList));
        result.put("dates", dateList);
        result.put("values", valueList);
        return result;
    }

    public BasicDBList readComments(Collection ids) {
        return readObjectsByIds(COMMENT_COLLECTION, ids);
    }

    public BasicDBList readPhotos(Collection ids) {
        return readObjectsByIds(PHOTO_COLLECTION, ids);
    }

    public void insertCommentDependencies(BasicDBList comments) {
        Map<String, List<DBObject>> map = new HashMap<String, List<DBObject>>();
        BasicDBList photoIds = new BasicDBList();

        for (Object objectComment : comments) {
            DBObject comment = (DBObject) objectComment;
            String photoId = (String) comment.get("itemId");

            photoIds.add(photoId);
            if (!map.containsKey(photoId)) {
                map.put(photoId, new ArrayList<DBObject>());
            }
            map.get(photoId).add(comment);
        }

        BasicDBList photos = readPhotos(photoIds);
        for (Object objectPhoto : photos) {
            DBObject photo = (DBObject) objectPhoto;
            String idPhoto = (String) photo.get("_id");
            for (DBObject comment : map.get(idPhoto)) {
                comment.put("photo", photo);
            }
        }
    }

    public BasicDBList readCommentsFromActivity(String userId) {
        BasicDBList commentIdList = new BasicDBList();
        DBCursor cursor = database.getCollection(ACTIVITY_COLLECTION).find(new BasicDBObject("_id", Pattern.compile("^" + userId)), new BasicDBObject("comment", 1));
        try {
            while (cursor.hasNext()) {
                DBObject activity = cursor.next();
                if (activity.get("comment") != null) {
                    DBObject comments = (DBObject) activity.get("comment");
                    commentIdList.addAll(comments.keySet());
                }
            }
        } finally {
            cursor.close();
        }

        return readComments(commentIdList);
    }

    public long getUserPhotoCount(String userId) {
        long result = 0L;
        DBCursor cursor = database.getCollection(ACTIVITY_COLLECTION).find(new BasicDBObject("_id", Pattern.compile("^" + userId)), new BasicDBObject("photos", 1));
        try {
            while (cursor.hasNext()) {
                DBObject activity = cursor.next();
                if (activity.get("photos") != null) {
                    result += ((BasicDBList) activity.get("photos")).size();

                }
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public long getUserCommentsCount(String userId) {
        long result = 0L;
        DBCursor cursor = database.getCollection(ACTIVITY_COLLECTION).find(new BasicDBObject("_id", Pattern.compile("^" + userId)), new BasicDBObject("comment", 1));
        try {
            while (cursor.hasNext()) {
                DBObject activity = cursor.next();
                if (activity.get("comment") != null) {
                    DBObject comments = (DBObject) activity.get("comment");
                    result += comments.keySet().size();

                }
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public BasicDBList readPhotosFromActivity(int start, int length, String userId) {
        int end = start + length;
        int iterator = 0;
        Set<String> photoIdSet = new HashSet<String>();
        DBCursor cursor = database.getCollection(ACTIVITY_COLLECTION).find(new BasicDBObject("_id", Pattern.compile("^" + userId)), new BasicDBObject("photos", 1));
        try {
            while (cursor.hasNext()) {
                DBObject activity = cursor.next();
                if (activity.get("photos") != null) {
                    BasicDBList photos = (BasicDBList) activity.get("photos");
                    if ((iterator + photos.size()) < start) {
                        iterator += photos.size();
                        continue;
                    }
                    for (Object photoObject : photos) {
                        if (iterator < start) {
                            iterator++;
                            continue;
                        }
                        if (iterator > end) {
                            return readPhotos(photoIdSet);
                        }

                        if (!photoIdSet.contains(photoObject)) {
                            photoIdSet.add(photoObject.toString());
                            iterator++;
                        }
                    }
                }
            }
        } finally {
            cursor.close();
        }

        return readPhotos(photoIdSet);
    }

    public BasicDBList readCommentsFromActivity(int start, int length, String userId) {
        int end = start + length;
        int iterator = 0;
        Set<String> commentIdSet = new HashSet<String>();
        DBCursor cursor = database.getCollection(ACTIVITY_COLLECTION).find(new BasicDBObject("_id", Pattern.compile("^" + userId)), new BasicDBObject("comment", 1));
        try {
            while (cursor.hasNext()) {
                DBObject activity = cursor.next();
                if (activity.get("comment") != null) {
                    BasicDBList comments = new BasicDBList();
                    comments.addAll(((DBObject) activity.get("comment")).keySet());
                    if ((iterator + comments.size()) < start) {
                        iterator += comments.size();
                        continue;
                    }
                    for (Object commentObject : comments) {
                        if (iterator < start) {
                            iterator++;
                            continue;
                        }
                        if (iterator > end) {
                            return readComments(commentIdSet);
                        }

                        if (!commentIdSet.contains(commentObject)) {
                            commentIdSet.add(commentObject.toString());
                            iterator++;
                        }
                    }
                }
            }
        } finally {
            cursor.close();
        }
        return readComments(commentIdSet);
    }

    private DBObject readObjectById(String collectionName, String objectId) {
        return database.getCollection(collectionName).findOne(new BasicDBObject("_id", objectId));
    }

    private BasicDBList readObjectsByIds(String collectionName, Collection ids) {
        BasicDBList result = new BasicDBList();
        DBCursor cursor = database.getCollection(collectionName).find(new BasicDBObject("_id", new BasicDBObject("$in", ids)));
        try {
            while (cursor.hasNext()) {
                result.add(cursor.next());
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public BasicDBList getActiveUsers(String userId) {
        List<DBObject> result = new ArrayList<DBObject>();
        DBObject object = database.getCollection("analyzed_activity").findOne(new BasicDBObject("_id", userId));
        DBObject value = (DBObject) object.get("value");
        return (BasicDBList) value.get("stat");
    }

    public BasicDBList getActivePhotos(String user) {
        DBObject object = database.getCollection("analyzed_photo_activity").findOne(new BasicDBObject("_id", user));
        return (BasicDBList) object.get("photos");

    }
}
