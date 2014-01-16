package gxtproject.server.controller;

import com.google.gwt.json.client.JSONObject;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import gxtproject.server.dao.MongoFlickrDAO;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/photo")
public class PhotoController {

    private static final Logger logger = Logger.getLogger(PhotoController.class);

    @Autowired
    MongoFlickrDAO flickrDAO;

    @RequestMapping(value = "/getPhotoCount", method = RequestMethod.GET)
    public
    @ResponseBody
    Long getPhotoCount() {
        return flickrDAO.getUserPhotoCount("109287411@N02");
    }


    @RequestMapping(value = "/getPhotosList.json", method = RequestMethod.GET)
    public
    @ResponseBody
    DBObject getPhotosList(Integer limit, Integer start, String callback) {
        BasicDBList photos = flickrDAO.readPhotosFromActivity(start, limit, "109287411@N02");
        DBObject result = new BasicDBObject("totalCount", flickrDAO.getUserPhotoCount("109287411@N02"));
        result.put("topics", photos);
        return result;
    }


    @RequestMapping(value = "/getCommentsByPhoto", method = RequestMethod.GET)
    public
    @ResponseBody
    DBObject getCommentsByPhoto(String photoId, HttpServletResponse response) {
        DBObject result = flickrDAO.readCommentsByPhotoId(photoId);
        return result;
    }
}
