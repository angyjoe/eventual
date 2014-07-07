package controllers

import scala.concurrent.ExecutionContext.Implicits.global

import models.Celebrity
import models.Celebrity.celebrityFormat
import models.Celebrity.CelebrityBSONReader
import models.Celebrity.CelebrityBSONWriter
import models.Name
import models.Name.nameFormat
import models.Name.NameBSONWriter
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONDocumentIdentity
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONObjectIDIdentity
import reactivemongo.bson.BSONStringHandler
import reactivemongo.bson.Producer.nameValue2Producer

/*
 * Author: Sari Haj Hussein
 */

object Celebrities extends Controller with MongoController {
  val collection = db[BSONCollection]("celebrities")

  /** list all celebrities */
  def index = Action.async {
    val cursor = collection.find(
      BSONDocument(), BSONDocument()).cursor[Celebrity] // get all the fields of all the celebrities
    val futureList = cursor.collect[List]()
    futureList.map {
      celebrities => Ok(Json.toJson(celebrities))
    } // convert it to a JSON and return it
  }

  /** create a celebrity from the given JSON */
  def create() = Action.async(parse.json) {
    request =>
      val nameJSON = request.body.\("name")
      val name = nameFormat.reads(nameJSON).get
      val website = request.body.\("website").toString().replace("\"", "")
      val bio = request.body.\("bio").toString().replace("\"", "")
      val celebrity = Celebrity(Option(BSONObjectID.generate), name, website, bio) // create the celebrity
      collection.insert(celebrity).map(
        _ => Ok(Json.toJson(celebrity))) // return the created celebrity in a JSON
  }

  /** retrieve the celebrity for the given id as JSON */
  def show(id: String) = Action.async(parse.empty) {
    request =>
      val objectID = new BSONObjectID(id) // get the corresponding BSONObjectID
    // get the celebrity having this id (there will be 0 or 1 result)
    val futureCelebrity = collection.find(BSONDocument("_id" -> objectID)).one[Celebrity]
      futureCelebrity.map {
        celebrity => Ok(Json.toJson(celebrity))
      }
  }

  /** update the celebrity for the given id from the JSON body */
  def update(id: String) = Action.async(parse.json) {
    request =>
      val objectID = new BSONObjectID(id) // get the corresponding BSONObjectID
    val nameJSON = request.body.\("name")
      val name = nameFormat.reads(nameJSON).get
      val website = request.body.\("website").toString().replace("\"", "")
      val bio = request.body.\("bio").toString().replace("\"", "")
      val modifier = BSONDocument(// create the modifier celebrity
        "$set" -> BSONDocument(
          "name" -> name,
          "website" -> website,
          "bio" -> bio))
      collection.update(BSONDocument("_id" -> objectID), modifier).map(
        _ => Ok(Json.toJson(Celebrity(Option(objectID), name, website, bio)))) // return the modified celebrity in a JSON
  }

  /** delete a celebrity for the given id */
  def delete(id: String) = Action.async(parse.empty) {
    request =>
      val objectID = new BSONObjectID(id) // get the corresponding BSONObjectID
      collection.remove(BSONDocument("_id" -> objectID)).map(// remove the celebrity
        _ => Ok(Json.obj())).recover {
        case _ => InternalServerError
      } // and return an empty JSON while recovering from errors if any
  }
}
