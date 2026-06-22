/*
 * BIMROCKET
 *
 * Copyright (C) 2021-2026, Ajuntament de Sant Feliu de Llobregat
 *
 * This program is licensed and may be used, modified and redistributed under
 * the terms of the European Public License (EUPL), either version 1.1 or (at
 * your option) any later version as soon as they are approved by the European
 * Commission.
 *
 * Alternatively, you may redistribute and/or modify this program under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either  version 3 of the License, or (at your option)
 * any later version.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the licenses for the specific language governing permissions, limitations
 * and more details.
 *
 * You should have received a copy of the EUPL1.1 and the LGPLv3 licenses along
 * with this program; if not, you may find them at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 * http://www.gnu.org/licenses/
 * and
 * https://www.gnu.org/licenses/lgpl.txt
 */
package org.bimrocket.service.bcf.store.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bimrocket.api.bcf.BcfComment;
import org.bimrocket.api.bcf.BcfDocumentReference;
import org.bimrocket.api.bcf.BcfExtensions;
import org.bimrocket.api.bcf.BcfProject;
import org.bimrocket.api.bcf.BcfTopic;
import org.bimrocket.api.bcf.BcfViewpoint;
import org.bimrocket.dao.Dao;
import org.bimrocket.dao.expression.Expression;
import org.bimrocket.dao.expression.OrderByExpression;
import org.bimrocket.dao.mongo.MongoDao;
import org.bimrocket.dao.mongo.MongoDaoConnection;
import org.bimrocket.dao.mongo.MongoExpressionGenerator;
import org.bimrocket.service.bcf.store.BcfDaoConnection;
import static org.bimrocket.service.security.SecurityConstants.ADMIN_ROLE;
import org.bson.Document;

/**
 *
 * @author realor
 */
public class BcfMongoDaoConnection extends MongoDaoConnection
  implements BcfDaoConnection
{
  protected BcfMongoDaoConnection(MongoClient mongoClient, MongoDatabase db)
  {
    super(mongoClient, db);
  }

  @Override
  public List<BcfProject> findProjects(
    Expression filter, List<OrderByExpression> orderByList, Set<String> roleIds)
  {
    MongoCollection<BcfProject> collection
      = db.getCollection("BcfProject", BcfProject.class);

    var generator = new MongoExpressionGenerator();

    List<Document> aggregate = generator.generateAggregate(filter, orderByList);

    if (!roleIds.contains(ADMIN_ROLE))
    {
      aggregate.add(
        new Document("$lookup",
          new Document("from", "BcfExtensions")
            .append("localField", "_id")
            .append("foreignField", "_id")
            .append("pipeline", List.of(
              new Document("$match",
                new Document("$expr",
                  new Document("$ne", List.of(
                    new Document("$setIntersection", List.of(
                      new Document("$cond", List.of(
                        new Document("$eq", List.of(
                          new Document("$size", "$readRoleIds"),
                          0
                        )),
                        List.of("EVERYONE"),
                        "$readRoleIds"
                      )),
                      new Document("$literal", roleIds)
                    )),
                    List.of()
                  ))
                )
              ),
              new Document("$limit", 1)
            ))
            .append("as", "extensions")
        ));

      aggregate.add(
        new Document("$match",
          new Document("extensions.0",
            new Document("$exists", true))));
    }

    List<BcfProject> projects = new ArrayList<>();
    try (var cursor = collection.aggregate(session, aggregate).cursor())
    {
      while (cursor.hasNext())
      {
        projects.add(cursor.next());
      }
    }
    return projects;
  }

  @Override
  public Dao<BcfProject, String> getProjectDao()
  {
    return new MongoDao<>(session, db, BcfProject.class);
  }

  @Override
  public Dao<BcfExtensions, String> getExtensionsDao()
  {
    return new MongoDao<>(session, db, BcfExtensions.class);
  }

  @Override
  public Dao<BcfTopic, String> getTopicDao()
  {
    return new MongoDao<>(session, db, BcfTopic.class);
  }

  @Override
  public Dao<BcfComment, String> getCommentDao()
  {
    return new MongoDao<>(session, db, BcfComment.class);
  }

  @Override
  public Dao<BcfViewpoint, String> getViewpointDao()
  {
    return new MongoDao<>(session, db, BcfViewpoint.class);
  }

  @Override
  public Dao<BcfDocumentReference, String> getDocumentReferenceDao()
  {
    return new MongoDao<>(session, db, BcfDocumentReference.class);
  }
}
